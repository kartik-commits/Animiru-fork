package eu.kanade.tachiyomi.ui.player.controls

/** Controls exposed by the active playback engine and content policy. */
data class PlayerControlCapabilities(
    val subtitles: Boolean = true,
    val audioTracks: Boolean = true,
    val quality: Boolean = true,
    val more: Boolean = true,
    val cast: Boolean = true,
    val pictureInPicture: Boolean = true,
    val aspectRatio: Boolean = true,
    val screenshots: Boolean = true,
) {
    companion object {
        val Media3Drm = PlayerControlCapabilities(
            subtitles = true,
            audioTracks = true,
            more = false,
            cast = false,
            pictureInPicture = false,
            screenshots = false,
        )
    }
}
