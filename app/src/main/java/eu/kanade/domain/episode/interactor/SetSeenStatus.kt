package eu.kanade.domain.episode.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.domain.download.interactor.DeleteDownload
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.model.EpisodeUpdate
import tachiyomi.domain.episode.repository.EpisodeRepository
import tachiyomi.domain.source.service.SourceManager

@Inject
class SetSeenStatus(
    private val downloadPreferences: DownloadPreferences,
    private val deleteDownload: DeleteDownload,
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
    private val sourceManager: SourceManager,
) {

    private val mapper = { episode: Episode, seen: Boolean ->
        EpisodeUpdate(episode.id) {
            this.seen = seen
            if (!seen) lastSecondSeen = 0
        }
    }

    suspend fun await(seen: Boolean, vararg episodes: Episode): Result {
        val episodesToUpdate = episodes.filter {
            when (seen) {
                true -> !it.seen
                false -> it.seen || it.lastSecondSeen > 0
            }
        }
        if (episodesToUpdate.isEmpty()) {
            return Result.NoEpisodes
        }

        try {
            episodeRepository.updateAll(
                episodesToUpdate.map { mapper(it, seen) },
            )
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            return Result.InternalError(e)
        }

        if (seen && downloadPreferences.removeAfterMarkedAsSeen.get()) {
            episodesToUpdate
                // Episodes here still carry their pre-update seen state; downstream
                // deletion filtering (excluded categories) relies on the new state.
                .map { it.copy(seen = true) }
                .groupBy { it.animeId }
                .forEach { (animeId, episodes) ->
                    deleteDownload.awaitAll(
                        anime = animeRepository.getAnimeById(animeId),
                        episodes = episodes.toTypedArray(),
                    )
                }
        }

        // AM --> Sync seen/unseen state to source
        episodesToUpdate.groupBy { it.animeId }.forEach { (animeId, episodes) ->
            runCatching {
                val anime = animeRepository.getAnimeById(animeId) ?: return@forEach
                val source = sourceManager.getOrStub(anime.source)
                try {
                    val batchMethod = source.javaClass.getMethod(
                        "setEpisodesSeen",
                        List::class.java,
                        java.lang.Boolean.TYPE,
                    )
                    batchMethod.invoke(source, episodes.map { it.url }, seen)
                } catch (_: NoSuchMethodException) {
                    for (ep in episodes) {
                        try {
                            if (seen) {
                                val markMethod = source.javaClass.getMethod(
                                    "savePlayhead",
                                    String::class.java,
                                    java.lang.Long.TYPE,
                                    java.lang.Long.TYPE,
                                    java.lang.Boolean.TYPE,
                                )
                                markMethod.invoke(source, ep.url, 0L, 0L, true)
                            } else {
                                val deleteMethod = source.javaClass.getMethod("deletePlayhead", String::class.java)
                                deleteMethod.invoke(source, ep.url)
                            }
                        } catch (_: NoSuchMethodException) {
                            // Source does not support playhead sync
                        }
                    }
                }
            }
        }
        // <-- AM

        return Result.Success
    }

    suspend fun await(animeId: Long, seen: Boolean): Result {
        return await(
            seen = seen,
            episodes = episodeRepository
                .getEpisodeByAnimeId(animeId)
                .toTypedArray(),
        )
    }

    suspend fun await(anime: Anime, seen: Boolean) =
        await(anime.id, seen)

    sealed interface Result {
        data object Success : Result
        data object NoEpisodes : Result
        data class InternalError(val error: Throwable) : Result
    }
}
