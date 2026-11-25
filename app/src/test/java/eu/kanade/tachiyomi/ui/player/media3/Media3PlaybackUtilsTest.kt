package eu.kanade.tachiyomi.ui.player.media3

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Media3PlaybackUtilsTest {
    @Test
    fun `mergeQualityWithActiveDub preserves active dub label when switching quality`() {
        val merged = Media3PlaybackUtils.mergeQualityWithActiveDub(
            qualityTitle = "1080p",
            activeTitle = "Auto (Adaptive) [English Dub]",
        )
        assertEquals("1080p [English Dub]", merged)
    }

    @Test
    fun `mergeQualityWithActiveDub replaces stale quality dub tag with active dub`() {
        val merged = Media3PlaybackUtils.mergeQualityWithActiveDub(
            qualityTitle = "720p [Japanese (Original)]",
            activeTitle = "1080p [Hindi Dub]",
        )
        assertEquals("720p [Hindi Dub]", merged)
    }

    @Test
    fun `mergeQualityWithActiveDub handles titles without dub suffix`() {
        val merged = Media3PlaybackUtils.mergeQualityWithActiveDub(
            qualityTitle = "720p",
            activeTitle = "1080p",
        )
        assertEquals("720p", merged)
    }
}
