package eu.kanade.tachiyomi.animesource.model

import androidx.compose.runtime.Immutable
import okhttp3.Headers

// AM (MEDIA3_DRM_CONTRACT) -->
enum class VideoDrmScheme {
    WIDEVINE,
}

/**
 * Declares DRM license acquisition for a protected video.
 *
 * [licenseHeaders] apply only to license requests. The host must never merge
 * [Video.headers] into license requests automatically.
 */
@Immutable
data class VideoDrm(
    val scheme: VideoDrmScheme,
    val licenseUrl: String,
    val licenseHeaders: Headers? = null,
) {
    override fun toString(): String =
        "VideoDrm(scheme=$scheme, licenseUrl=<redacted>, licenseHeaders=<redacted>)"
}
// <-- AM (MEDIA3_DRM_CONTRACT)
