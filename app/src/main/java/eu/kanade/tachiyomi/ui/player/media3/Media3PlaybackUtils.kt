package eu.kanade.tachiyomi.ui.player.media3

import androidx.media3.common.MimeTypes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

internal object Media3PlaybackUtils {
    private val bracketTagRegex = Regex("""\[.*?\]""")
    private val trailingDubRegex = Regex("""\[[^\]]+\]\s*$""")

    fun stripBracketSuffixes(title: String): String =
        title.replace(bracketTagRegex, "").trim().ifBlank { "Auto (Adaptive)" }

    fun mergeQualityWithActiveDub(qualityTitle: String, activeTitle: String?): String {
        val baseTitle = stripBracketSuffixes(qualityTitle)
        val dubSuffix = activeTitle?.let { trailingDubRegex.find(it)?.value?.trim() }
        return if (!dubSuffix.isNullOrBlank()) "$baseTitle $dubSuffix" else baseTitle
    }

    fun withUpdatedVersionGuid(episodeUrl: String, versionGuid: String): String {
        if (versionGuid.isBlank()) return episodeUrl
        val trimmed = episodeUrl.trim()
        if (!trimmed.startsWith("{")) return episodeUrl
        return try {
            val jsonObj = Json.parseToJsonElement(trimmed).jsonObject
            val updated = jsonObj.toMutableMap().apply {
                this["versionGuid"] = JsonPrimitive(versionGuid)
            }
            JsonObject(updated).toString()
        } catch (_: Exception) {
            episodeUrl
        }
    }

    fun inferStreamMimeType(url: String): String {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith(".m3u8") || path.contains(".m3u8/") -> MimeTypes.APPLICATION_M3U8
            else -> MimeTypes.APPLICATION_MPD
        }
    }

    fun inferSubtitleMimeType(url: String): String {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith(".vtt") -> MimeTypes.TEXT_VTT
            path.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
            path.endsWith(".ttml") || path.endsWith(".dfxp") -> MimeTypes.APPLICATION_TTML
            path.endsWith(".ssa") || path.endsWith(".ass") -> MimeTypes.TEXT_SSA
            else -> MimeTypes.TEXT_SSA
        }
    }

    fun resolveInitialSubtitleTrack(
        tracks: List<Media3PlayerTrack>,
        sessionPreferredLang: String?,
        fallbackSelection: () -> Media3PlayerTrack?,
    ): Media3PlayerTrack? {
        if (sessionPreferredLang == "none") return null
        if (!sessionPreferredLang.isNullOrBlank()) {
            val exact = tracks.firstOrNull { it.language.equals(sessionPreferredLang, ignoreCase = true) }
            if (exact != null) return exact
            val baseLang = sessionPreferredLang.substringBefore('-')
            val familyMatch = tracks.firstOrNull {
                it.language.isNotBlank() &&
                    it.language.substringBefore('-').equals(baseLang, ignoreCase = true)
            }
            if (familyMatch != null) return familyMatch
        }
        return fallbackSelection()
    }
}
