package eu.kanade.tachiyomi.ui.player.media3

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.model.VideoDrmScheme
import okhttp3.Headers

// AM (MEDIA3_DRM_PLAYER) -->
@OptIn(UnstableApi::class)
class Media3DrmPlayer(
    context: Context,
    private val listener: Listener,
) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null

    fun attach(view: PlayerView) {
        playerView = view
        view.player = player
    }

    fun detach(view: PlayerView) {
        if (playerView === view) playerView = null
        view.player = null
    }

    fun load(video: Video, startPositionSeconds: Long) {
        val drm = video.drm ?: return
        require(video.videoUrl.startsWith("https://")) { "Protected media URL must use HTTPS" }
        require(drm.licenseUrl.startsWith("https://")) { "DRM license URL must use HTTPS" }
        release(notify = false)
        listener.onLoading()

        val mediaFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(video.headers.toMap())
            .setAllowCrossProtocolRedirects(false)
        val engine = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(appContext)
                    .setDataSourceFactory(mediaFactory),
            )
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = engine
        playerView?.player = engine
        engine.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (player !== engine) return
                    when (playbackState) {
                        Player.STATE_READY -> listener.onReady(engine.duration.coerceAtLeast(0L))
                        Player.STATE_ENDED -> listener.onEnded()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (player !== engine) return
                    listener.onPlayingChanged(isPlaying)
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (player !== engine) return
                    listener.onError(error.errorCodeName)
                    release(notify = false)
                }

                override fun onRenderedFirstFrame() {
                    if (player !== engine) return
                    listener.onFirstFrame()
                }

                override fun onTracksChanged(tracks: Tracks) {
                    if (player !== engine) return
                    val rawSubs = tracks.toPlayerTracks(C.TRACK_TYPE_TEXT)
                    val hasSelected = rawSubs.any { it.selection > -1 }
                    val noneTrack = Media3PlayerTrack(
                        trackType = C.TRACK_TYPE_TEXT,
                        groupIndex = -1,
                        trackIndex = -1,
                        title = "None",
                        language = "none",
                        selection = if (!hasSelected) 0 else -1,
                    )
                    listener.onTracksChanged(
                        audio = tracks.toPlayerTracks(C.TRACK_TYPE_AUDIO),
                        subtitles = listOf(noneTrack) + rawSubs,
                    )
                }
            },
        )
        val subtitleConfigs = video.subtitleTracks.filter {
            it.lang != "none" && it.url.isNotBlank()
        }.map { track ->
            val uri = Uri.parse(track.url)
            val mimeType = if (track.url.contains(".vtt")) {
                MimeTypes.TEXT_VTT
            } else {
                MimeTypes.TEXT_SSA
            }
            val isEnglish = track.lang.startsWith("en", ignoreCase = true)
            MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(mimeType)
                .setLanguage(track.lang)
                .setLabel(getLanguageDisplayName(track.lang))
                .setSelectionFlags(if (isEnglish) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }

        engine.setMediaItem(
            MediaItem.Builder()
                .setUri(video.videoUrl)
                .setMimeType(MimeTypes.APPLICATION_MPD)
                .setSubtitleConfigurations(subtitleConfigs)
                .setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                        .setLicenseUri(Uri.parse(drm.licenseUrl))
                        .setLicenseRequestHeaders(drm.licenseHeaders.toMap())
                        .build(),
                )
                .build(),
            startPositionSeconds * 1000L,
        )

        val params = engine.trackSelectionParameters.buildUpon()
        val res = video.resolution
        if (res != null && res > 0) {
            params.setMaxVideoSize(Int.MAX_VALUE, res)
                .setMinVideoSize(0, res)
        } else {
            params.clearVideoSizeConstraints()
        }
        engine.trackSelectionParameters = params.build()

        engine.prepare()
        engine.play()
    }

    fun pause() {
        player?.pause()
    }

    fun play() {
        player?.play()
    }

    fun currentPositionMs(): Long = player?.currentPosition?.coerceAtLeast(0L) ?: 0L

    fun durationMs(): Long = player?.duration?.coerceAtLeast(0L) ?: 0L

    fun seekTo(positionSeconds: Int) {
        player?.seekTo(positionSeconds.coerceAtLeast(0) * 1000L)
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
    }

    fun getPlaybackSpeed(): Float = player?.playbackParameters?.speed ?: 1.0f

    fun setResizeMode(resizeMode: Int) {
        playerView?.resizeMode = resizeMode
    }

    fun setVideoResolution(height: Int?) {
        val engine = player ?: return
        val parameters = engine.trackSelectionParameters.buildUpon()
        if (height == null || height <= 0) {
            parameters.clearVideoSizeConstraints()
        } else {
            parameters.setMaxVideoSize(Int.MAX_VALUE, height)
                .setMinVideoSize(0, height)
        }
        engine.trackSelectionParameters = parameters.build()
    }

    fun selectTrack(track: Media3PlayerTrack) {
        val engine = player ?: return
        val parameters = engine.trackSelectionParameters.buildUpon()
        if (track.trackType == C.TRACK_TYPE_TEXT) {
            if (track.groupIndex == -1 || track.language == "none" || track.selection > -1) {
                parameters.clearOverridesOfType(C.TRACK_TYPE_TEXT)
                parameters.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                val group = engine.currentTracks.groups.getOrNull(track.groupIndex) ?: return
                parameters.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                parameters.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
            }
        } else {
            val group = engine.currentTracks.groups.getOrNull(track.groupIndex) ?: return
            parameters.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex))
        }
        engine.trackSelectionParameters = parameters.build()
    }

    fun release(notify: Boolean = true) {
        val engine = player ?: return
        val position = engine.currentPosition.coerceAtLeast(0L)
        val duration = engine.duration.coerceAtLeast(0L)
        player = null
        playerView?.player = null
        playerView?.keepScreenOn = false
        engine.release()
        if (notify) listener.onReleased(position, duration)
    }

    interface Listener {
        fun onLoading()
        fun onReady(durationMs: Long)
        fun onPlayingChanged(isPlaying: Boolean)
        fun onFirstFrame()
        fun onTracksChanged(audio: List<Media3PlayerTrack>, subtitles: List<Media3PlayerTrack>)
        fun onEnded()
        fun onError(errorCode: String)
        fun onReleased(positionMs: Long, durationMs: Long)
    }
}

private fun Tracks.toPlayerTracks(trackType: Int): List<Media3PlayerTrack> = groups.mapIndexedNotNull {
        groupIndex,
        group,
    ->
    if (group.type != trackType) return@mapIndexedNotNull null
    List(group.length) { trackIndex ->
        val format = group.getTrackFormat(trackIndex)
        val title = format.label?.ifBlank { null }
            ?: format.language?.let { getLanguageDisplayName(it) }
            ?: ""
        Media3PlayerTrack(
            trackType = trackType,
            groupIndex = groupIndex,
            trackIndex = trackIndex,
            title = title,
            language = format.language.orEmpty(),
            selection = if (group.isTrackSelected(trackIndex)) 0 else -1,
        )
    }
}.flatten()

fun getLanguageDisplayName(locale: String): String {
    return when (locale.lowercase()) {
        "en-us", "en" -> "English"
        "es-419", "es-la" -> "Spanish (Latin America)"
        "es-es", "es" -> "Spanish (Spain)"
        "pt-br", "pt" -> "Portuguese (Brazil)"
        "fr-fr", "fr" -> "French"
        "de-de", "de" -> "German"
        "it-it", "it" -> "Italian"
        "ar-sa", "ar-me", "ar" -> "Arabic"
        "ru-ru", "ru" -> "Russian"
        "hi-in", "hi" -> "Hindi"
        "ta-in", "ta" -> "Tamil"
        "te-in", "te" -> "Telugu"
        "id-id", "id" -> "Indonesian"
        "ms-my", "ms" -> "Malay"
        "th-th", "th" -> "Thai"
        "vi-vn", "vi" -> "Vietnamese"
        "zh-cn", "zh-hk", "zh-tw", "zh" -> "Chinese"
        "ko-kr", "ko" -> "Korean"
        "pl-pl", "pl" -> "Polish"
        "ja-jp", "ja" -> "Japanese"
        else -> locale
    }
}

private fun Headers?.toMap(): Map<String, String> = this?.let { headers ->
    buildMap {
        headers.names().forEach { name -> put(name, headers.values(name).joinToString(", ")) }
    }
}.orEmpty()
// <-- AM (MEDIA3_DRM_PLAYER)
