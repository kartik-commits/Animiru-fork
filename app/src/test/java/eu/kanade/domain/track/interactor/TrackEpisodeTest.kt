package eu.kanade.domain.track.interactor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.track.model.Track

class TrackEpisodeTest {

    private fun fakeAnime(id: Long, seasonNumber: Double, sourceOrder: Long = 0L): Anime =
        Anime.create().copy(
            id = id,
            parentId = 100L,
            seasonNumber = seasonNumber,
            seasonSourceOrder = sourceOrder,
        )

    private fun fakeTrack(totalEpisodes: Long): Track = Track(
        id = 1L,
        animeId = 100L,
        trackerId = 1L,
        remoteId = 1L,
        libraryId = null,
        title = "Series",
        lastEpisodeSeen = 0.0,
        totalEpisodes = totalEpisodes,
        status = 0L,
        score = 0.0,
        remoteUrl = "",
        startDate = null,
        finishDate = null,
        private = false,
    )

    @Test
    fun `filterPriorSeasons excludes specials with seasonNumber zero when resolving numbered season`() {
        val specials = fakeAnime(id = 1L, seasonNumber = 0.0, sourceOrder = 0L)
        val season1 = fakeAnime(id = 2L, seasonNumber = 1.0, sourceOrder = 1L)
        val season2 = fakeAnime(id = 3L, seasonNumber = 2.0, sourceOrder = 2L)
        val season3 = fakeAnime(id = 4L, seasonNumber = 3.0, sourceOrder = 3L)

        val priorForS2 = TrackEpisode.filterPriorSeasons(season2, listOf(specials, season1, season2, season3))
        assertEquals(listOf(season1), priorForS2)

        val priorForS3 = TrackEpisode.filterPriorSeasons(season3, listOf(specials, season1, season2, season3))
        assertEquals(listOf(season1, season2), priorForS3)

        val priorForSpecials = TrackEpisode.filterPriorSeasons(specials, listOf(specials, season1, season2))
        assertTrue(priorForSpecials.isEmpty())
    }

    @Test
    fun `filterPriorSeasons falls back to seasonSourceOrder when seasonNumber is unset`() {
        val s0 = fakeAnime(id = 1L, seasonNumber = -1.0, sourceOrder = 0L)
        val s1 = fakeAnime(id = 2L, seasonNumber = -1.0, sourceOrder = 1L)
        val s2 = fakeAnime(id = 3L, seasonNumber = -1.0, sourceOrder = 2L)

        assertEquals(listOf(s0, s1), TrackEpisode.filterPriorSeasons(s2, listOf(s0, s1, s2)))
    }

    @Test
    fun `resolveOffsetEpisodeNumber adds offset for airing and combined trackers only`() {
        assertEquals(
            15.0,
            TrackEpisode.resolveOffsetEpisodeNumber(
                priorEpisodeCount = 12,
                episodeNumber = 3.0,
                parentTracks = listOf(fakeTrack(totalEpisodes = 0L)),
            ),
        )
        assertEquals(
            15.0,
            TrackEpisode.resolveOffsetEpisodeNumber(
                priorEpisodeCount = 12,
                episodeNumber = 3.0,
                parentTracks = listOf(fakeTrack(totalEpisodes = 24L)),
            ),
        )
        assertEquals(
            3.0,
            TrackEpisode.resolveOffsetEpisodeNumber(
                priorEpisodeCount = 12,
                episodeNumber = 3.0,
                parentTracks = listOf(fakeTrack(totalEpisodes = 12L)),
            ),
        )
    }
}
