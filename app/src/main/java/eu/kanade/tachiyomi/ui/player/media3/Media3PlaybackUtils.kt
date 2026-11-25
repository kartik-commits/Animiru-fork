package eu.kanade.tachiyomi.ui.player.media3

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
}
