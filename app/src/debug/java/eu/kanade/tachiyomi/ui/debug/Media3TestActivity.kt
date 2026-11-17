package eu.kanade.tachiyomi.ui.debug

import android.media.MediaDrm
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
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
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.drm.KeyRequestInfo
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.view.setComposeContent
import kotlinx.coroutines.delay
import tachiyomi.i18n.animiru.AMMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

// AM (MEDIA3_DEBUG_TEST) -->
@androidx.annotation.OptIn(UnstableApi::class)
class Media3TestActivity : BaseActivity() {
    private var player by mutableStateOf<ExoPlayer?>(null)
    private var playerView: PlayerView? = null
    private var status by mutableStateOf(R.string.media3_test_ready)
    private var errorCode by mutableStateOf<Int?>(null)
    private var keysLoaded by mutableStateOf(false)
    private var firstFrame by mutableStateOf(false)
    private var positionMs by mutableLongStateOf(0L)
    private var deviceLevel by mutableStateOf<String?>(null)
    private var multiTrack by mutableStateOf(false)
    private var audioTracks by mutableStateOf<List<DebugTrack>>(emptyList())
    private var subtitleTracks by mutableStateOf<List<DebugTrack>>(emptyList())
    private var selectedSubtitle by mutableStateOf<DebugTrack?>(null)

    init {
        registerSecureActivity(this, forceSecure = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        positionMs = savedInstanceState?.getLong(POSITION_KEY) ?: 0L
        deviceLevel = readDeviceLevel()
        setComposeContent { TestScreen() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(POSITION_KEY, player?.currentPosition ?: positionMs)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        // No background/PiP ownership in this proof. Resume always requires a user action.
        releasePlayer()
        super.onPause()
    }

    override fun onDestroy() {
        releasePlayer()
        playerView = null
        super.onDestroy()
    }

    private fun readDeviceLevel(): String? = try {
        if (MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID)) {
            val drm = MediaDrm(C.WIDEVINE_UUID)
            try {
                drm.getPropertyString("securityLevel").takeIf { it in setOf("L1", "L2", "L3") }
            } finally {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    drm.close()
                } else {
                    @Suppress("DEPRECATION")
                    drm.release()
                }
            }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    private fun startPlayback(useMultiTrack: Boolean = multiTrack) {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        releasePlayer(updateStatus = false)
        multiTrack = useMultiTrack
        errorCode = null
        keysLoaded = false
        firstFrame = false
        status = R.string.media3_test_buffering
        try {
            if (!MediaDrm.isCryptoSchemeSupported(C.WIDEVINE_UUID)) {
                errorCode = PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED
                return
            }
            // Separate from Animiru's account-aware HTTP client: this fixture needs no credentials.
            val http = DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(15_000)
                .setAllowCrossProtocolRedirects(false)
            val engine = ExoPlayer.Builder(this)
                .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(http))
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .setHandleAudioBecomingNoisy(true)
                .build()
            player = engine
            engine.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (this@Media3TestActivity.player !== engine || errorCode != null) return
                    status = when (player.playbackState) {
                        Player.STATE_BUFFERING -> R.string.media3_test_buffering
                        Player.STATE_ENDED -> R.string.media3_test_ended
                        Player.STATE_READY -> if (player.isPlaying) {
                            R.string.media3_test_playing
                        } else {
                            R.string.media3_test_paused
                        }
                        else -> R.string.media3_test_ready
                    }
                    playerView?.keepScreenOn = player.isPlaying
                }

                override fun onPlayerError(error: PlaybackException) {
                    errorCode = error.errorCode
                    Log.i(TAG, "Playback failed: ${error.errorCodeName}")
                    releasePlayer(updateStatus = false)
                }

                override fun onTracksChanged(tracks: Tracks) {
                    if (this@Media3TestActivity.player !== engine) return
                    audioTracks = tracks.toDebugTracks(C.TRACK_TYPE_AUDIO)
                    subtitleTracks = tracks.toDebugTracks(C.TRACK_TYPE_TEXT)
                    selectedSubtitle = subtitleTracks.firstOrNull { it.selected }
                }
            })
            engine.addAnalyticsListener(object : AnalyticsListener {
                override fun onDrmKeysLoaded(
                    eventTime: AnalyticsListener.EventTime,
                    keyRequestInfo: KeyRequestInfo,
                ) {
                    if (player !== engine) return
                    keysLoaded = true
                    Log.i(TAG, "Widevine keys loaded")
                }

                override fun onRenderedFirstFrame(
                    eventTime: AnalyticsListener.EventTime,
                    output: Any,
                    renderTimeMs: Long,
                ) {
                    if (player !== engine) return
                    firstFrame = true
                    Log.i(TAG, "First video frame rendered")
                }
            })
            engine.setMediaItem(
                MediaItem.Builder()
                    .setUri(
                        if (multiTrack) {
                            WidevineTestAsset.MULTI_TRACK_MANIFEST
                        } else {
                            WidevineTestAsset.MANIFEST
                        },
                    )
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                            .setLicenseUri(
                                if (multiTrack) {
                                    WidevineTestAsset.MULTI_TRACK_LICENSE
                                } else {
                                    WidevineTestAsset.LICENSE
                                },
                            )
                            .build(),
                    )
                    .build(),
                positionMs,
            )
            engine.prepare()
            engine.play()
        } catch (_: Exception) {
            errorCode = PlaybackException.ERROR_CODE_UNSPECIFIED
            releasePlayer(updateStatus = false)
        }
    }

    private fun releasePlayer(updateStatus: Boolean = true) {
        val engine = player ?: return
        positionMs = if (engine.playbackState == Player.STATE_ENDED) 0L else engine.currentPosition.coerceAtLeast(0L)
        player = null
        playerView?.player = null
        playerView?.keepScreenOn = false
        engine.release()
        keysLoaded = false
        firstFrame = false
        audioTracks = emptyList()
        subtitleTracks = emptyList()
        selectedSubtitle = null
        if (updateStatus && errorCode == null) status = R.string.media3_test_released
        Log.i(TAG, "Player released at ${positionMs / 1000} s")
    }

    @Composable
    private fun TestScreen() {
        val engine = player
        LaunchedEffect(engine) {
            while (engine != null) {
                positionMs = engine.currentPosition.coerceAtLeast(0L)
                delay(1000)
            }
        }
        Scaffold(
            topBar = {
                AppBar(
                    title = mokoStringResource(AMMR.strings.pref_media3_test),
                    navigateUp = { finish() },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.media3_test_description), style = MaterialTheme.typography.bodyMedium)
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((LocalConfiguration.current.screenHeightDp * 0.34f).coerceIn(180f, 320f).dp),
                    factory = { context ->
                        PlayerView(context).apply {
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            setShowSubtitleButton(false)
                            setKeepContentOnPlayerReset(false)
                            playerView = this
                        }
                    },
                    update = { view -> view.player = engine },
                    onRelease = { view ->
                        view.player = null
                        if (playerView === view) playerView = null
                    },
                )
                Text(stringResource(R.string.media3_test_limits), style = MaterialTheme.typography.bodySmall)
                val failure = errorCode
                if (failure != null) {
                    Text(
                        stringResource(WidevineTestAsset.errorMessage(failure)),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.media3_test_error_code, PlaybackException.getErrorCodeName(failure)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(stringResource(status), style = MaterialTheme.typography.titleMedium)
                }
                if (engine == null) {
                    Button(onClick = ::startPlayback) {
                        Text(
                            stringResource(
                                if (failure != null) R.string.media3_test_retry else R.string.media3_test_start,
                            ),
                        )
                    }
                    Button(onClick = { startPlayback(useMultiTrack = true) }) {
                        Text(stringResource(R.string.media3_test_multi_track_start))
                    }
                }
                if (engine != null && multiTrack && (audioTracks.isNotEmpty() || subtitleTracks.isNotEmpty())) {
                    Text("Available Media3 tracks", style = MaterialTheme.typography.titleSmall)
                    audioTracks.forEach { track ->
                        Button(onClick = { selectTrack(engine, track) }) {
                            Text("Audio: ${track.label}")
                        }
                    }
                    Button(onClick = { clearSubtitles(engine) }) {
                        Text("Subtitles: off")
                    }
                    subtitleTracks.forEach { track ->
                        Button(onClick = { selectTrack(engine, track) }) {
                            Text("Subtitle: ${track.label}")
                        }
                    }
                }
                Text(stringResource(R.string.media3_test_diagnostics), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(
                        R.string.media3_test_device_level,
                        deviceLevel ?: stringResource(R.string.media3_test_unknown),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(
                        if (keysLoaded) R.string.media3_test_license_loaded else R.string.media3_test_license_waiting,
                    ),
                )
                Text(
                    stringResource(
                        if (firstFrame) R.string.media3_test_frame_rendered else R.string.media3_test_frame_waiting,
                    ),
                )
                Text(stringResource(R.string.media3_test_position, positionMs / 1000))
                Text(stringResource(R.string.media3_test_policy), style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    private fun selectTrack(engine: ExoPlayer, track: DebugTrack) {
        engine.trackSelectionParameters = engine.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(
                TrackSelectionOverride(
                    engine.currentTracks.groups[track.groupIndex].mediaTrackGroup,
                    track.trackIndex,
                ),
            )
            .build()
        if (track.type == C.TRACK_TYPE_TEXT) {
            selectedSubtitle = track
        }
    }

    private fun clearSubtitles(engine: ExoPlayer) {
        engine.trackSelectionParameters = engine.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .build()
        selectedSubtitle = null
    }

    private companion object {
        const val TAG = "Media3Test"
        const val POSITION_KEY = "media3_test_position"
    }
}
// <-- AM (MEDIA3_DEBUG_TEST)

private data class DebugTrack(
    val type: Int,
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val selected: Boolean,
)

private fun Tracks.toDebugTracks(trackType: Int): List<DebugTrack> = groups.mapIndexedNotNull { groupIndex, group ->
    if (group.type != trackType) return@mapIndexedNotNull null
    List(group.length) { trackIndex ->
        val format = group.getTrackFormat(trackIndex)
        DebugTrack(
            type = trackType,
            groupIndex = groupIndex,
            trackIndex = trackIndex,
            label = format.label ?: format.language ?: "Track ${trackIndex + 1}",
            selected = group.isTrackSelected(trackIndex),
        )
    }
}.flatten()
