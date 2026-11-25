package eu.kanade.tachiyomi.ui.player.utils

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AniSkipApiTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parseAniListMalId extracts positive idMal and handles null or missing data`() {
        assertEquals(
            40748L,
            AniSkipApi.parseAniListMalId(json, """{"data":{"Media":{"idMal":40748}}}"""),
        )
        assertNull(AniSkipApi.parseAniListMalId(json, """{"data":{"Media":{"idMal":null}}}"""))
        assertNull(AniSkipApi.parseAniListMalId(json, """{"data":{"Media":null}}"""))
        assertNull(AniSkipApi.parseAniListMalId(json, """{"data":{"Media":{"idMal":0}}}"""))
        assertNull(AniSkipApi.parseAniListMalId(json, "invalid-json"))
    }
}
