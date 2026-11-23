package eu.kanade.domain.track.interactor

import android.content.Context
import dev.zacsweers.metro.Inject
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.domain.track.service.DelayedTrackingUpdateWorker
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.season.interactor.GetAnimeSeasonsByParentId
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.UpsertTrack

@Inject
class TrackEpisode(
    private val getTracks: GetTracks,
    private val trackerManager: TrackerManager,
    private val upsertTrack: UpsertTrack,
    private val delayedTrackingStore: DelayedTrackingStore,
    private val getAnime: GetAnime,
    private val getAnimeSeasonsByParentId: GetAnimeSeasonsByParentId,
    private val getEpisodesByAnimeId: GetEpisodesByAnimeId,
) {

    suspend fun await(context: Context, animeId: Long, episodeNumber: Double, setupJobOnFailure: Boolean = true) {
        coroutineScope {
            var tracks = getTracks.await(animeId)
            var targetEpisodeNumber = episodeNumber

            if (tracks.isEmpty()) {
                val anime = getAnime.await(animeId)
                val parentId = anime?.parentId
                if (parentId != null) {
                    val parentTracks = getTracks.await(parentId)
                    if (parentTracks.isNotEmpty()) {
                        tracks = parentTracks
                        try {
                            val seasons = getAnimeSeasonsByParentId.await(parentId)
                            val currentSeasonOrder =
                                anime.seasonNumber.takeIf { it >= 0 } ?: anime.seasonSourceOrder.toDouble()
                            val priorSeasons = seasons.filter {
                                val order = it.anime.seasonNumber.takeIf { s -> s >= 0 }
                                    ?: it.anime.seasonSourceOrder.toDouble()
                                order < currentSeasonOrder
                            }
                            val priorEpCount = priorSeasons.sumOf { getEpisodesByAnimeId.await(it.anime.id).size }
                            if (priorEpCount > 0 &&
                                parentTracks.any { it.totalEpisodes == 0L || it.totalEpisodes > episodeNumber }
                            ) {
                                targetEpisodeNumber = priorEpCount + episodeNumber
                            }
                        } catch (e: Exception) {
                            logcat(LogPriority.ERROR, e)
                        }
                    }
                }
            }

            if (tracks.isEmpty()) return@coroutineScope

            tracks.mapNotNull { track ->
                val service = trackerManager.get(track.trackerId)
                if (service == null || !service.isLoggedIn || targetEpisodeNumber <= track.lastEpisodeSeen) {
                    return@mapNotNull null
                }

                async {
                    runCatching {
                        try {
                            val updatedTrack = service.refresh(track.toDbTrack())
                                .toDomainTrack(idRequired = true)!!
                                .copy(lastEpisodeSeen = targetEpisodeNumber)
                            service.update(updatedTrack.toDbTrack(), true)
                            upsertTrack.await(updatedTrack)
                            delayedTrackingStore.remove(track.id)
                        } catch (e: Exception) {
                            delayedTrackingStore.add(track.id, targetEpisodeNumber)
                            if (setupJobOnFailure) {
                                DelayedTrackingUpdateWorker.setupTask(context)
                            }
                            throw e
                        }
                    }
                }
            }
                .awaitAll()
                .mapNotNull { it.exceptionOrNull() }
                .forEach { logcat(LogPriority.WARN, it) }
        }
    }
}
