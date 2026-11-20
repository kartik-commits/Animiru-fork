package tachiyomi.domain.episode.interactor

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.repository.EpisodeRepository

@Inject
class GetEpisodesByAnimeId(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun subscribe(animeId: Long, applyScanlatorFilter: Boolean = false): Flow<List<Episode>> {
        return episodeRepository.getEpisodeByAnimeIdAsFlow(animeId, applyScanlatorFilter)
    }

    suspend fun await(animeId: Long, applyScanlatorFilter: Boolean = false): List<Episode> {
        return try {
            episodeRepository.getEpisodeByAnimeId(animeId, applyScanlatorFilter)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }
}
