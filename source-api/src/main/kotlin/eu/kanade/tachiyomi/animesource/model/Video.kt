package eu.kanade.tachiyomi.animesource.model

import android.net.Uri
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY
import okhttp3.Headers

@Serializable
data class Track(val url: String, val lang: String)

@Serializable
enum class ChapterType {
    Opening,
    Ending,
    Recap,
    MixedOp,
    Other,
}

@Serializable
data class TimeStamp(
    val start: Double,
    val end: Double,
    val name: String,
    val type: ChapterType = ChapterType.Other,
)

@Stable // mutability only concerns the downloader
data class Video(
    var videoUrl: String = "",
    val videoTitle: String = "",
    val resolution: Int? = null,
    val bitrate: Int? = null,
    val headers: Headers? = null,
    val preferred: Boolean = false,
    val subtitleTracks: List<Track> = emptyList(),
    val audioTracks: List<Track> = emptyList(),
    val timestamps: List<TimeStamp> = emptyList(),
    val mpvArgs: List<Pair<String, String>> = emptyList(),
    val ffmpegStreamArgs: List<Pair<String, String>> = emptyList(),
    val ffmpegVideoArgs: List<Pair<String, String>> = emptyList(),
    val internalData: String = "",
    val initialized: Boolean = false,
    val memo: JsonObject = JsonObject.EMPTY,
    // AM (MEDIA3_DRM_CONTRACT) -->
    val drm: VideoDrm? = null,
    // <-- AM (MEDIA3_DRM_CONTRACT)
) {

    // TODO(16): Remove after ext lib bump
    @Deprecated("Use videoTitle instead", ReplaceWith("videoTitle"))
    val quality: String
        get() = videoTitle

    // TODO(16): Remove after ext lib bump
    val url: String
        get() = videoPageUrl

    // TODO(16): Remove after ext lib bump
    private var videoPageUrl: String = ""

    // TODO(16): Remove after ext lib bump
    constructor(
        url: String,
        quality: String,
        videoUrl: String?,
        headers: Headers? = null,
        subtitleTracks: List<Track> = emptyList(),
        audioTracks: List<Track> = emptyList(),
    ) : this(
        videoTitle = quality,
        videoUrl = videoUrl ?: "null",
        headers = headers,
        subtitleTracks = subtitleTracks,
        audioTracks = audioTracks,
    ) {
        this.videoPageUrl = url
    }

    // TODO(16): Remove after ext lib bump
    @Suppress("UNUSED_PARAMETER")
    constructor(
        url: String,
        quality: String,
        videoUrl: String?,
        uri: Uri? = null,
        headers: Headers? = null,
    ) : this(url, quality, videoUrl, headers)

    // AM (MEDIA3_DRM_CONTRACT) -->
    // Ext lib 17 constructor. A defaulted overload also preserves its old synthetic mask descriptor.
    @Deprecated("Used only for compatibility with ext lib 17, do not use", level = DeprecationLevel.HIDDEN)
    constructor(
        videoUrl: String = "",
        videoTitle: String = "",
        resolution: Int? = null,
        bitrate: Int? = null,
        headers: Headers? = null,
        preferred: Boolean = false,
        subtitleTracks: List<Track> = emptyList(),
        audioTracks: List<Track> = emptyList(),
        timestamps: List<TimeStamp> = emptyList(),
        mpvArgs: List<Pair<String, String>> = emptyList(),
        ffmpegStreamArgs: List<Pair<String, String>> = emptyList(),
        ffmpegVideoArgs: List<Pair<String, String>> = emptyList(),
        internalData: String = "",
        initialized: Boolean = false,
        memo: JsonObject = JsonObject.EMPTY,
    ) : this(
        videoUrl, videoTitle, resolution, bitrate, headers, preferred, subtitleTracks, audioTracks, timestamps, mpvArgs,
        ffmpegStreamArgs, ffmpegVideoArgs, internalData, initialized, memo, null,
    )
    // <-- AM (MEDIA3_DRM_CONTRACT)

    // Ext lib 16 constructor
    @Deprecated("Used only for compatibility with ext lib 16, do not use", level = DeprecationLevel.HIDDEN)
    constructor(
        videoUrl: String = "",
        videoTitle: String = "",
        resolution: Int? = null,
        bitrate: Int? = null,
        headers: Headers? = null,
        preferred: Boolean = false,
        subtitleTracks: List<Track> = emptyList(),
        audioTracks: List<Track> = emptyList(),
        timestamps: List<TimeStamp> = emptyList(),
        mpvArgs: List<Pair<String, String>> = emptyList(),
        ffmpegStreamArgs: List<Pair<String, String>> = emptyList(),
        ffmpegVideoArgs: List<Pair<String, String>> = emptyList(),
        internalData: String = "",
        initialized: Boolean = false,
    ) : this(
        videoUrl, videoTitle, resolution, bitrate, headers, preferred, subtitleTracks, audioTracks, timestamps, mpvArgs,
        ffmpegStreamArgs, ffmpegVideoArgs, internalData, initialized, JsonObject.EMPTY,
    )

    // AM (MEDIA3_DRM_CONTRACT) -->
    // Ext lib 17 copy. Preserve state unknown to old callers.
    @Deprecated("Used only for compatibility with ext lib 17, do not use", level = DeprecationLevel.HIDDEN)
    fun copy(
        videoUrl: String = this.videoUrl,
        videoTitle: String = this.videoTitle,
        resolution: Int? = this.resolution,
        bitrate: Int? = this.bitrate,
        headers: Headers? = this.headers,
        preferred: Boolean = this.preferred,
        subtitleTracks: List<Track> = this.subtitleTracks,
        audioTracks: List<Track> = this.audioTracks,
        timestamps: List<TimeStamp> = this.timestamps,
        mpvArgs: List<Pair<String, String>> = this.mpvArgs,
        ffmpegStreamArgs: List<Pair<String, String>> = this.ffmpegStreamArgs,
        ffmpegVideoArgs: List<Pair<String, String>> = this.ffmpegVideoArgs,
        internalData: String = this.internalData,
        initialized: Boolean = this.initialized,
        memo: JsonObject = this.memo,
    ): Video = Video(
        videoUrl = videoUrl,
        videoTitle = videoTitle,
        resolution = resolution,
        bitrate = bitrate,
        headers = headers,
        preferred = preferred,
        subtitleTracks = subtitleTracks,
        audioTracks = audioTracks,
        timestamps = timestamps,
        mpvArgs = mpvArgs,
        ffmpegStreamArgs = ffmpegStreamArgs,
        ffmpegVideoArgs = ffmpegVideoArgs,
        internalData = internalData,
        initialized = initialized,
        memo = memo,
        drm = drm,
    )
    // <-- AM (MEDIA3_DRM_CONTRACT)

    // Ext lib 16 copy video
    @Deprecated("Used only for compatibility with ext lib 16, do not use", level = DeprecationLevel.HIDDEN)
    fun copy(
        videoUrl: String = this.videoUrl,
        videoTitle: String = this.videoTitle,
        resolution: Int? = this.resolution,
        bitrate: Int? = this.bitrate,
        headers: Headers? = this.headers,
        preferred: Boolean = this.preferred,
        subtitleTracks: List<Track> = this.subtitleTracks,
        audioTracks: List<Track> = this.audioTracks,
        timestamps: List<TimeStamp> = this.timestamps,
        mpvArgs: List<Pair<String, String>> = this.mpvArgs,
        ffmpegStreamArgs: List<Pair<String, String>> = this.ffmpegStreamArgs,
        ffmpegVideoArgs: List<Pair<String, String>> = this.ffmpegVideoArgs,
        internalData: String = this.internalData,
        initialized: Boolean = this.initialized,
    ): Video = Video(
        videoUrl = videoUrl,
        videoTitle = videoTitle,
        resolution = resolution,
        bitrate = bitrate,
        headers = headers,
        preferred = preferred,
        subtitleTracks = subtitleTracks,
        audioTracks = audioTracks,
        timestamps = timestamps,
        mpvArgs = mpvArgs,
        ffmpegStreamArgs = ffmpegStreamArgs,
        ffmpegVideoArgs = ffmpegVideoArgs,
        internalData = internalData,
        initialized = initialized,
        // AM (MEDIA3_DRM_CONTRACT) -->
        memo = memo,
        drm = drm,
        // <-- AM (MEDIA3_DRM_CONTRACT)
    )

    // AM (MEDIA3_DRM_CONTRACT) -->
    // Ext lib 14 data-class copy bridge. Preserve fields unknown to v14 callers.
    @Deprecated("Used only for compatibility with ext lib 14, do not use", level = DeprecationLevel.HIDDEN)
    fun copy(
        url: String = this.url,
        quality: String = this.quality,
        videoUrl: String? = this.videoUrl,
        headers: Headers? = this.headers,
        subtitleTracks: List<Track> = this.subtitleTracks,
        audioTracks: List<Track> = this.audioTracks,
    ): Video = Video(
        videoUrl = videoUrl ?: "null",
        videoTitle = quality,
        resolution = resolution,
        bitrate = bitrate,
        headers = headers,
        preferred = preferred,
        subtitleTracks = subtitleTracks,
        audioTracks = audioTracks,
        timestamps = timestamps,
        mpvArgs = mpvArgs,
        ffmpegStreamArgs = ffmpegStreamArgs,
        ffmpegVideoArgs = ffmpegVideoArgs,
        internalData = internalData,
        initialized = initialized,
        memo = memo,
        drm = drm,
    ).also { it.videoPageUrl = url }
    // <-- AM (MEDIA3_DRM_CONTRACT)

    @Transient
    @Volatile
    var status: State = State.QUEUE
        set(value) {
            field = value
        }

    fun usesHttpServer(): Boolean {
        if (localUrl.find(videoUrl) != null) {
            return true
        }

        if (audioTracks.any { localUrl.find(it.url) != null }) {
            return true
        }

        if (subtitleTracks.any { localUrl.find(it.url) != null }) {
            return true
        }

        return false
    }

    fun copyHttpServer(port: Int): Video {
        val newHost = "http://localhost:$port"
        return this.copy(
            videoUrl = localUrl.replace(videoUrl, newHost),
            subtitleTracks = subtitleTracks.map {
                it.copy(url = localUrl.replace(it.url, newHost))
            },
            audioTracks = audioTracks.map {
                it.copy(url = localUrl.replace(it.url, newHost))
            },
        )
    }

    enum class State {
        QUEUE,
        LOAD_VIDEO,
        READY,
        ERROR,
    }

    companion object {
        const val MPV_ARGS_TAG = "ANIYOMI_MPV_ARGS"
        private val localUrl = Regex("""http:\/\/localhost:1(?!\d)""")
    }
}

// AM (MEDIA3_DRM_CONTRACT) -->
@Serializable
data class SerializableVideoDrm(
    val scheme: VideoDrmScheme,
    // Decode and discard credentials from state written by earlier prototype builds.
    val licenseUrl: String? = null,
    val licenseHeaders: List<Pair<String, String>>? = null,
) {
    override fun toString(): String =
        "SerializableVideoDrm(scheme=$scheme, licenseUrl=<redacted>, licenseHeaders=<redacted>)"
}
// <-- AM (MEDIA3_DRM_CONTRACT)

@Serializable
data class SerializableVideo(
    val videoUrl: String = "",
    val videoTitle: String = "",
    val resolution: Int? = null,
    val bitrate: Int? = null,
    val headers: List<Pair<String, String>>? = null,
    val preferred: Boolean = false,
    val subtitleTracks: List<Track> = emptyList(),
    val audioTracks: List<Track> = emptyList(),
    val timestamps: List<TimeStamp> = emptyList(),
    val mpvArgs: List<Pair<String, String>> = emptyList(),
    val ffmpegStreamArgs: List<Pair<String, String>> = emptyList(),
    val ffmpegVideoArgs: List<Pair<String, String>> = emptyList(),
    val internalData: String = "",
    val initialized: Boolean = false,
    val memo: JsonObject = JsonObject.EMPTY,
    // AM (MEDIA3_DRM_CONTRACT) -->
    val drm: SerializableVideoDrm? = null,
    // <-- AM (MEDIA3_DRM_CONTRACT)
) {

    companion object {
        fun List<Video>.serialize(): String =
            Json.encodeToString(
                this.map { vid ->
                    SerializableVideo(
                        vid.videoUrl,
                        vid.videoTitle,
                        vid.resolution,
                        vid.bitrate,
                        vid.headers?.toList(),
                        vid.preferred,
                        vid.subtitleTracks,
                        vid.audioTracks,
                        vid.timestamps,
                        vid.mpvArgs,
                        vid.ffmpegStreamArgs,
                        vid.ffmpegVideoArgs,
                        vid.internalData,
                        vid.initialized,
                        vid.memo,
                        // AM (MEDIA3_DRM_CONTRACT) -->
                        vid.drm?.let { drm -> SerializableVideoDrm(drm.scheme) },
                        // <-- AM (MEDIA3_DRM_CONTRACT)
                    )
                },
            )

        fun String.toVideoList(): List<Video> =
            Json.decodeFromString<List<SerializableVideo>>(this)
                .map { sVid ->
                    Video(
                        sVid.videoUrl,
                        sVid.videoTitle,
                        sVid.resolution,
                        sVid.bitrate,
                        sVid.headers
                            ?.flatMap { it.toList() }
                            ?.let { Headers.headersOf(*it.toTypedArray()) },
                        sVid.preferred,
                        sVid.subtitleTracks,
                        sVid.audioTracks,
                        sVid.timestamps,
                        sVid.mpvArgs,
                        sVid.ffmpegStreamArgs,
                        sVid.ffmpegVideoArgs,
                        sVid.internalData,
                        sVid.initialized && sVid.drm == null,
                        sVid.memo,
                        // AM (MEDIA3_DRM_CONTRACT) -->
                        sVid.drm?.let { drm ->
                            VideoDrm(
                                scheme = drm.scheme,
                                licenseUrl = "",
                            )
                        },
                        // <-- AM (MEDIA3_DRM_CONTRACT)
                    )
                }
    }
}
