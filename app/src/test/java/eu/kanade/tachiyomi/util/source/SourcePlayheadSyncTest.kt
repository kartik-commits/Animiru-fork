package eu.kanade.tachiyomi.util.source

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SourcePlayheadSyncTest {

    private class SingleCallSource {
        var stateCalls = 0
        var legacyFullyWatchedCalls = 0
        var legacyPlayheadCalls = 0
        var savedCalls = mutableListOf<List<Any?>>()
        var batchCalls = mutableListOf<Pair<List<String>, Boolean>>()
        var pausedCalls = 0
        var resumedPositions = mutableListOf<Long>()
        var stoppedCalls = 0

        @Suppress("unused")
        fun getPlayheadState(episodeUrl: String): LongArray {
            stateCalls++
            return if (episodeUrl == "ep-watched") longArrayOf(1200L, 1L) else longArrayOf(345L, 0L)
        }

        @Suppress("unused")
        fun isEpisodeFullyWatched(@Suppress("UNUSED_PARAMETER") episodeUrl: String): Boolean {
            legacyFullyWatchedCalls++
            return false
        }

        @Suppress("unused")
        fun getPlayhead(@Suppress("UNUSED_PARAMETER") episodeUrl: String): Long {
            legacyPlayheadCalls++
            return 999L
        }

        @Suppress("unused")
        fun savePlayhead(episodeUrl: String, posSec: Long, totalSec: Long, fullyWatched: Boolean) {
            savedCalls.add(listOf(episodeUrl, posSec, totalSec, fullyWatched))
        }

        @Suppress("unused")
        fun setEpisodesSeen(episodeUrls: List<String>, seen: Boolean) {
            batchCalls.add(episodeUrls to seen)
        }

        @Suppress("unused")
        fun onPlaybackPaused() {
            pausedCalls++
        }

        @Suppress("unused")
        fun onPlaybackResumed(positionSeconds: Long) {
            resumedPositions.add(positionSeconds)
        }

        @Suppress("unused")
        fun onPlaybackStopped() {
            stoppedCalls++
        }
    }

    private class LegacySource(private val fullyWatched: Boolean, private val playhead: Long) {
        var fullyWatchedCalls = 0
        var playheadCalls = 0
        val saved = mutableListOf<String>()
        val deleted = mutableListOf<String>()

        @Suppress("unused")
        fun isEpisodeFullyWatched(@Suppress("UNUSED_PARAMETER") episodeUrl: String): Boolean {
            fullyWatchedCalls++
            return fullyWatched
        }

        @Suppress("unused")
        fun getPlayhead(@Suppress("UNUSED_PARAMETER") episodeUrl: String): Long {
            playheadCalls++
            return playhead
        }

        @Suppress("unused")
        fun savePlayhead(
            episodeUrl: String,
            @Suppress("UNUSED_PARAMETER") posSec: Long,
            @Suppress("UNUSED_PARAMETER") totalSec: Long,
            @Suppress("UNUSED_PARAMETER") fullyWatched: Boolean,
        ) {
            saved.add(episodeUrl)
        }

        @Suppress("unused")
        fun deletePlayhead(episodeUrl: String) {
            deleted.add(episodeUrl)
        }
    }

    private class PlainSource

    private class FailingSource {
        @Suppress("unused")
        fun getPlayheadState(@Suppress("UNUSED_PARAMETER") episodeUrl: String): LongArray {
            throw IllegalStateException("remote error")
        }
    }

    @Test
    fun `fetchPlayheadState uses single getPlayheadState call without calling legacy methods`() {
        val source = SingleCallSource()

        val partial = SourcePlayheadSync.fetchPlayheadState(source, "ep-partial")
        assertEquals(RemotePlayheadState(playheadSeconds = 345L, isFullyWatched = false), partial)

        val watched = SourcePlayheadSync.fetchPlayheadState(source, "ep-watched")
        assertEquals(RemotePlayheadState(playheadSeconds = 1200L, isFullyWatched = true), watched)

        assertEquals(2, source.stateCalls)
        assertEquals(0, source.legacyFullyWatchedCalls)
        assertEquals(0, source.legacyPlayheadCalls)
    }

    @Test
    fun `fetchPlayheadState falls back cleanly for legacy sources and short circuits watched episodes`() {
        val watchedSource = LegacySource(fullyWatched = true, playhead = 400L)
        val watchedState = SourcePlayheadSync.fetchPlayheadState(watchedSource, "ep-1")
        assertTrue(watchedState!!.isFullyWatched)
        assertEquals(1, watchedSource.fullyWatchedCalls)
        assertEquals(0, watchedSource.playheadCalls)

        val resumeSource = LegacySource(fullyWatched = false, playhead = 400L)
        val resumeState = SourcePlayheadSync.fetchPlayheadState(resumeSource, "ep-2")
        assertFalse(resumeState!!.isFullyWatched)
        assertEquals(400L, resumeState.playheadSeconds)
        assertEquals(1, resumeSource.fullyWatchedCalls)
        assertEquals(1, resumeSource.playheadCalls)
    }

    @Test
    fun `fetchPlayheadState returns null for sources without playhead support`() {
        assertNull(SourcePlayheadSync.fetchPlayheadState(PlainSource(), "ep-1"))
    }

    @Test
    fun `setEpisodesSeen uses batch method when present and per-episode fallback otherwise`() {
        val batchSource = SingleCallSource()
        SourcePlayheadSync.setEpisodesSeen(batchSource, listOf("ep-1", "ep-2"), seen = true)
        assertEquals(listOf(listOf("ep-1", "ep-2") to true), batchSource.batchCalls)
        assertTrue(batchSource.savedCalls.isEmpty())

        val legacySource = LegacySource(fullyWatched = false, playhead = 0L)
        SourcePlayheadSync.setEpisodesSeen(legacySource, listOf("ep-1", "ep-2"), seen = true)
        SourcePlayheadSync.setEpisodesSeen(legacySource, listOf("ep-3"), seen = false)
        assertEquals(listOf("ep-1", "ep-2"), legacySource.saved)
        assertEquals(listOf("ep-3"), legacySource.deleted)
    }

    @Test
    fun `savePlayhead invokes source savePlayhead method`() {
        val source = SingleCallSource()
        SourcePlayheadSync.savePlayhead(source, "ep-9", 120L, 1440L, false)
        assertEquals(listOf(listOf<Any?>("ep-9", 120L, 1440L, false)), source.savedCalls)
    }

    @Test
    fun `playback lifecycle notifications invoke source methods and no-op when absent`() {
        val source = SingleCallSource()
        SourcePlayheadSync.notifyPlaybackPaused(source)
        SourcePlayheadSync.notifyPlaybackResumed(source, 321L)
        SourcePlayheadSync.notifyPlaybackStopped(source)

        assertEquals(1, source.pausedCalls)
        assertEquals(listOf(321L), source.resumedPositions)
        assertEquals(1, source.stoppedCalls)

        val plain = PlainSource()
        SourcePlayheadSync.notifyPlaybackPaused(plain)
        SourcePlayheadSync.notifyPlaybackResumed(plain, 10L)
        SourcePlayheadSync.notifyPlaybackStopped(plain)
    }

    @Test
    fun `underlying exceptions are unwrapped from InvocationTargetException`() {
        val source = FailingSource()
        val ex = assertThrows(IllegalStateException::class.java) {
            SourcePlayheadSync.fetchPlayheadState(source, "ep-1")
        }
        assertEquals("remote error", ex.message)
    }
}
