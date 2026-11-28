package eu.kanade.tachiyomi.util.source

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

internal data class RemotePlayheadState(
    val playheadSeconds: Long,
    val isFullyWatched: Boolean,
)

/**
 * Caches reflected playhead/seen-sync [Method] handles per source class so playback setup,
 * periodic progress writes, and batch seen toggles avoid repeated method lookups and duplicate
 * remote playhead requests.
 */
internal object SourcePlayheadSync {

    private data class CachedMethods(
        val getPlayheadState: Method?,
        val isEpisodeFullyWatched: Method?,
        val getPlayhead: Method?,
        val savePlayhead: Method?,
        val deletePlayhead: Method?,
        val setEpisodesSeen: Method?,
        val onPlaybackPaused: Method?,
        val onPlaybackResumed: Method?,
        val onPlaybackStopped: Method?,
    )

    private val methodCache = ConcurrentHashMap<Class<*>, CachedMethods>()

    fun fetchPlayheadState(source: Any, episodeUrl: String): RemotePlayheadState? {
        val methods = methodsFor(source.javaClass)

        methods.getPlayheadState?.let { method ->
            val raw = invokeUnwrapped(method, source, episodeUrl) as? LongArray ?: return null
            return when {
                raw.size >= 2 -> RemotePlayheadState(
                    playheadSeconds = raw[0].coerceAtLeast(0L),
                    isFullyWatched = raw[1] != 0L,
                )
                raw.size == 1 -> RemotePlayheadState(
                    playheadSeconds = raw[0].coerceAtLeast(0L),
                    isFullyWatched = false,
                )
                else -> null
            }
        }

        if (methods.isEpisodeFullyWatched == null && methods.getPlayhead == null) {
            return null
        }

        val fullyWatched =
            (methods.isEpisodeFullyWatched?.let { invokeUnwrapped(it, source, episodeUrl) } as? Boolean) == true
        if (fullyWatched) {
            return RemotePlayheadState(playheadSeconds = 0L, isFullyWatched = true)
        }

        val playheadSec =
            (methods.getPlayhead?.let { invokeUnwrapped(it, source, episodeUrl) } as? Long)
                ?.coerceAtLeast(0L)
                ?: 0L
        return RemotePlayheadState(playheadSeconds = playheadSec, isFullyWatched = false)
    }

    fun savePlayhead(
        source: Any,
        episodeUrl: String,
        positionSeconds: Long,
        totalSeconds: Long,
        fullyWatched: Boolean,
    ) {
        val method = methodsFor(source.javaClass).savePlayhead ?: return
        invokeUnwrapped(method, source, episodeUrl, positionSeconds, totalSeconds, fullyWatched)
    }

    fun setEpisodesSeen(
        source: Any,
        episodeUrls: List<String>,
        seen: Boolean,
    ) {
        if (episodeUrls.isEmpty()) return
        val methods = methodsFor(source.javaClass)

        methods.setEpisodesSeen?.let { batchMethod ->
            invokeUnwrapped(batchMethod, source, episodeUrls, seen)
            return
        }

        if (seen) {
            val markMethod = methods.savePlayhead ?: return
            for (url in episodeUrls) {
                invokeUnwrapped(markMethod, source, url, 0L, 0L, true)
            }
        } else {
            val deleteMethod = methods.deletePlayhead ?: return
            for (url in episodeUrls) {
                invokeUnwrapped(deleteMethod, source, url)
            }
        }
    }

    fun notifyPlaybackPaused(source: Any) {
        val method = methodsFor(source.javaClass).onPlaybackPaused ?: return
        invokeUnwrapped(method, source)
    }

    fun notifyPlaybackResumed(source: Any, positionSeconds: Long) {
        val method = methodsFor(source.javaClass).onPlaybackResumed ?: return
        invokeUnwrapped(method, source, positionSeconds)
    }

    fun notifyPlaybackStopped(source: Any) {
        val method = methodsFor(source.javaClass).onPlaybackStopped ?: return
        invokeUnwrapped(method, source)
    }

    private fun methodsFor(clazz: Class<*>): CachedMethods =
        methodCache.computeIfAbsent(clazz) { target ->
            CachedMethods(
                getPlayheadState = target.findMethodOrNull("getPlayheadState", String::class.java),
                isEpisodeFullyWatched = target.findMethodOrNull("isEpisodeFullyWatched", String::class.java),
                getPlayhead = target.findMethodOrNull("getPlayhead", String::class.java),
                savePlayhead = target.findMethodOrNull(
                    "savePlayhead",
                    String::class.java,
                    java.lang.Long.TYPE,
                    java.lang.Long.TYPE,
                    java.lang.Boolean.TYPE,
                ),
                deletePlayhead = target.findMethodOrNull("deletePlayhead", String::class.java),
                setEpisodesSeen = target.findMethodOrNull(
                    "setEpisodesSeen",
                    List::class.java,
                    java.lang.Boolean.TYPE,
                ),
                onPlaybackPaused = target.findMethodOrNull("onPlaybackPaused"),
                onPlaybackResumed = target.findMethodOrNull("onPlaybackResumed", java.lang.Long.TYPE),
                onPlaybackStopped = target.findMethodOrNull("onPlaybackStopped"),
            )
        }

    private fun Class<*>.findMethodOrNull(name: String, vararg parameterTypes: Class<*>): Method? =
        try {
            getMethod(name, *parameterTypes)
        } catch (_: NoSuchMethodException) {
            null
        }

    private fun invokeUnwrapped(method: Method, target: Any, vararg args: Any?): Any? =
        try {
            method.invoke(target, *args)
        } catch (e: InvocationTargetException) {
            throw (e.cause ?: e)
        }
}
