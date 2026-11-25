package eu.kanade.tachiyomi.ui.player.media3

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
}
