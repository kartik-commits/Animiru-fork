package eu.kanade.tachiyomi.ui.player

import animiru.domain.player.model.VideoTrack

/** Engine-neutral track model consumed by Animiru's shared track sheets. */
interface PlayerTrack : VideoTrack {
    val selection: Int
    val displayId: Int?
    val loading: Boolean
    val error: Boolean
}
