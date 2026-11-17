package eu.kanade.tachiyomi.ui.debug

import androidx.media3.common.PlaybackException
import eu.kanade.tachiyomi.R

// AM (MEDIA3_DEBUG_TEST) -->
/** Public fixture from androidx/media 1.11.1 demos/main/src/main/assets/media.exolist.json. */
internal object WidevineTestAsset {
    const val MANIFEST = "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd"
    const val LICENSE = "https://proxy.uat.widevine.com/proxy?video_id=2015_tears&provider=widevine_test"
    const val MULTI_TRACK_MANIFEST = "https://storage.googleapis.com/shaka-demo-assets/sintel-widevine/dash.mpd"
    const val MULTI_TRACK_LICENSE = "https://cwip-shaka-proxy.appspot.com/no_auth"

    // Never display exception messages: production license errors may contain credential-bearing URLs.
    fun errorMessage(errorCode: Int): Int = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_TIMEOUT,
        -> R.string.media3_test_error_network
        PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
        PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED,
        -> R.string.media3_test_error_license
        PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> R.string.media3_test_error_provisioning
        PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED,
        PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION,
        -> R.string.media3_test_error_unsupported
        in 4000..5999 -> R.string.media3_test_error_decoder
        in 3000..3999 -> R.string.media3_test_error_manifest
        else -> R.string.media3_test_error_generic
    }
}
// <-- AM (MEDIA3_DEBUG_TEST)
