package eu.kanade.tachiyomi.ui.debug

import androidx.media3.common.PlaybackException
import eu.kanade.tachiyomi.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.net.URI

// AM (MEDIA3_PROTOTYPE) -->
class WidevineTestAssetTest {
    @Test
    fun `fixture uses only documented HTTPS test destinations without credentials`() {
        val manifest = URI(WidevineTestAsset.MANIFEST)
        val license = URI(WidevineTestAsset.LICENSE)
        val multiTrackManifest = URI(WidevineTestAsset.MULTI_TRACK_MANIFEST)
        val multiTrackLicense = URI(WidevineTestAsset.MULTI_TRACK_LICENSE)
        assertEquals("https", manifest.scheme)
        assertEquals("storage.googleapis.com", manifest.host)
        assertEquals("/wvmedia/cenc/h264/tears/tears.mpd", manifest.path)
        assertEquals("https", license.scheme)
        assertEquals("proxy.uat.widevine.com", license.host)
        assertEquals("video_id=2015_tears&provider=widevine_test", license.query)
        assertNull(manifest.userInfo)
        assertNull(license.userInfo)
        assertEquals("https", multiTrackManifest.scheme)
        assertEquals("storage.googleapis.com", multiTrackManifest.host)
        assertEquals("/shaka-demo-assets/sintel-widevine/dash.mpd", multiTrackManifest.path)
        assertEquals("https", multiTrackLicense.scheme)
        assertEquals("cwip-shaka-proxy.appspot.com", multiTrackLicense.host)
        assertNull(multiTrackManifest.userInfo)
        assertNull(multiTrackLicense.userInfo)
    }

    @Test
    fun `license and provisioning failures have distinct guidance`() {
        assertEquals(
            R.string.media3_test_error_license,
            WidevineTestAsset.errorMessage(PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED),
        )
        assertEquals(
            R.string.media3_test_error_license,
            WidevineTestAsset.errorMessage(PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED),
        )
        assertEquals(
            R.string.media3_test_error_provisioning,
            WidevineTestAsset.errorMessage(PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED),
        )
    }

    @Test
    fun `unsupported and revoked devices never suggest downgrade`() {
        listOf(
            PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED,
            PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION,
        ).forEach { code ->
            assertEquals(R.string.media3_test_error_unsupported, WidevineTestAsset.errorMessage(code))
        }
    }

    @Test
    fun `network decoding manifest and unknown errors are safely classified`() {
        assertEquals(
            R.string.media3_test_error_network,
            WidevineTestAsset.errorMessage(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED),
        )
        assertEquals(
            R.string.media3_test_error_decoder,
            WidevineTestAsset.errorMessage(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED),
        )
        assertEquals(
            R.string.media3_test_error_manifest,
            WidevineTestAsset.errorMessage(PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED),
        )
        assertEquals(R.string.media3_test_error_generic, WidevineTestAsset.errorMessage(Int.MAX_VALUE))
    }
}
// <-- AM (MEDIA3_PROTOTYPE)
