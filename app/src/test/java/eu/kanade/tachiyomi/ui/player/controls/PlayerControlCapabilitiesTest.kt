package eu.kanade.tachiyomi.ui.player.controls

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerControlCapabilitiesTest {
    @Test
    fun `standard player retains all existing capabilities`() {
        val capabilities = PlayerControlCapabilities()

        assertTrue(capabilities.subtitles)
        assertTrue(capabilities.audioTracks)
        assertTrue(capabilities.quality)
        assertTrue(capabilities.more)
        assertTrue(capabilities.cast)
        assertTrue(capabilities.pictureInPicture)
        assertTrue(capabilities.aspectRatio)
        assertTrue(capabilities.screenshots)
    }

    @Test
    fun `Media3 DRM exposes only truthfully wired controls`() {
        val capabilities = PlayerControlCapabilities.Media3Drm

        assertTrue(capabilities.subtitles)
        assertTrue(capabilities.audioTracks)
        assertTrue(capabilities.quality)
        assertFalse(capabilities.more)
        assertFalse(capabilities.cast)
        assertTrue(capabilities.pictureInPicture)
        assertTrue(capabilities.aspectRatio)
        assertFalse(capabilities.screenshots)
    }
}
