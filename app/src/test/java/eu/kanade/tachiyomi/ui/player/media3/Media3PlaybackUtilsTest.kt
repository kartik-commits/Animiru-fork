package eu.kanade.tachiyomi.ui.player.media3

import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    @Test
    fun `inferStreamMimeType detects HLS m3u8 with query string and defaults to DASH mpd`() {
        assertEquals(
            MimeTypes.APPLICATION_M3U8,
            Media3PlaybackUtils.inferStreamMimeType("https://cdn.example.com/master.m3u8?Policy=abc"),
        )
        assertEquals(
            MimeTypes.APPLICATION_MPD,
            Media3PlaybackUtils.inferStreamMimeType("https://cdn.example.com/manifest.mpd?Policy=abc"),
        )
    }

    @Test
    fun `inferSubtitleMimeType detects vtt srt ttml and falls back to ssa`() {
        assertEquals(
            MimeTypes.TEXT_VTT,
            Media3PlaybackUtils.inferSubtitleMimeType("https://cdn.example.com/subs/en-US.vtt?token=1"),
        )
        assertEquals(
            MimeTypes.APPLICATION_SUBRIP,
            Media3PlaybackUtils.inferSubtitleMimeType("https://cdn.example.com/subs/es.SRT"),
        )
        assertEquals(
            MimeTypes.APPLICATION_TTML,
            Media3PlaybackUtils.inferSubtitleMimeType("https://cdn.example.com/subs/fr.ttml#fragment"),
        )
        assertEquals(
            MimeTypes.TEXT_SSA,
            Media3PlaybackUtils.inferSubtitleMimeType("https://cdn.example.com/subs/en-US.ass?token=1"),
        )
    }

    @Test
    fun `resolveInitialSubtitleTrack honors none override exact match family match and fallback`() {
        val en = Media3PlayerTrack(C.TRACK_TYPE_TEXT, -1, 0, "English", "en-US", -1)
        val esLa = Media3PlayerTrack(C.TRACK_TYPE_TEXT, -1, 1, "Spanish (Latin America)", "es-419", -1)
        val tracks = listOf(en, esLa)

        assertNull(Media3PlaybackUtils.resolveInitialSubtitleTrack(tracks, "none") { en })
        assertEquals(esLa, Media3PlaybackUtils.resolveInitialSubtitleTrack(tracks, "es-419") { en })
        assertEquals(esLa, Media3PlaybackUtils.resolveInitialSubtitleTrack(tracks, "es-ES") { en })
        assertEquals(esLa, Media3PlaybackUtils.resolveInitialSubtitleTrack(tracks, null) { esLa })
    }
}
