package eu.kanade.tachiyomi.ui.player.media3

import eu.kanade.tachiyomi.ui.player.PlayerTrack

data class Media3PlayerTrack(
    val trackType: Int,
    val groupIndex: Int,
    val trackIndex: Int,
    override val title: String,
    override val language: String,
    override val selection: Int,
    val url: String? = null,
) : PlayerTrack {
    override val displayId: Int = trackIndex + 1
    override val loading: Boolean = false
    override val error: Boolean = false
}
