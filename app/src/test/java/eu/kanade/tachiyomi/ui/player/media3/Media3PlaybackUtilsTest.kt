package eu.kanade.tachiyomi.ui.player.media3

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    @Test
    fun `withUpdatedVersionGuid injects versionGuid when omitted from episode JSON`() {
        val updated = Media3PlaybackUtils.withUpdatedVersionGuid(
            episodeUrl = """{"seriesId":"S1","seasonId":"SE1","episodeId":"EP1"}""",
            versionGuid = "G-ENG-123",
        )
        val obj = Json.parseToJsonElement(updated).jsonObject
        assertEquals("EP1", obj["episodeId"]?.jsonPrimitive?.content)
        assertEquals("G-ENG-123", obj["versionGuid"]?.jsonPrimitive?.content)
    }

    @Test
    fun `withUpdatedVersionGuid replaces null and existing versionGuid`() {
        val fromNull = Media3PlaybackUtils.withUpdatedVersionGuid(
            episodeUrl = """{"episodeId":"EP1","versionGuid":null}""",
            versionGuid = "G-JPN-456",
        )
        assertEquals(
            "G-JPN-456",
            Json.parseToJsonElement(fromNull).jsonObject["versionGuid"]?.jsonPrimitive?.content,
        )

        val fromOld = Media3PlaybackUtils.withUpdatedVersionGuid(
            episodeUrl = """{"episodeId":"EP1","versionGuid":"G-OLD"}""",
            versionGuid = "G-NEW",
        )
        assertEquals(
            "G-NEW",
            Json.parseToJsonElement(fromOld).jsonObject["versionGuid"]?.jsonPrimitive?.content,
        )
    }

    @Test
    fun `withUpdatedVersionGuid leaves non-JSON URLs untouched`() {
        val rawUrl = "https://example.com/watch/ep-1"
        assertEquals(rawUrl, Media3PlaybackUtils.withUpdatedVersionGuid(rawUrl, "G-123"))
    }
}
