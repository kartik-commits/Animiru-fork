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
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.interactor.GetEpisodesByAnimeId
import tachiyomi.domain.season.interactor.GetAnimeSeasonsByParentId
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.UpsertTrack
import tachiyomi.domain.track.model.Track

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

    data class ResolvedTrackingTarget(
        val targetAnimeId: Long,
        val tracks: List<Track>,
        val targetEpisodeNumber: Double,
    )

    suspend fun resolveTracking(animeId: Long, episodeNumber: Double): ResolvedTrackingTarget {
        val directTracks = getTracks.await(animeId)
        if (directTracks.isNotEmpty()) {
            return ResolvedTrackingTarget(
                targetAnimeId = animeId,
                tracks = directTracks,
                targetEpisodeNumber = episodeNumber,
            )
        }

        val anime = getAnime.await(animeId)
        val parentId = anime?.parentId ?: return ResolvedTrackingTarget(animeId, emptyList(), episodeNumber)
        val parentTracks = getTracks.await(parentId)
        if (parentTracks.isEmpty()) {
            return ResolvedTrackingTarget(animeId, emptyList(), episodeNumber)
        }

        val targetEpisodeNumber = try {
            val seasons = getAnimeSeasonsByParentId.await(parentId).map { it.anime }
            val priorSeasons = filterPriorSeasons(anime, seasons)
            val priorEpCount = priorSeasons.sumOf { getEpisodesByAnimeId.await(it.id).size }
            resolveOffsetEpisodeNumber(priorEpCount, episodeNumber, parentTracks)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            episodeNumber
        }

        return ResolvedTrackingTarget(
            targetAnimeId = animeId,
            tracks = parentTracks,
            targetEpisodeNumber = targetEpisodeNumber,
        )
    }

    suspend fun await(context: Context, animeId: Long, episodeNumber: Double, setupJobOnFailure: Boolean = true) {
        coroutineScope {
            val resolved = resolveTracking(animeId, episodeNumber)
            val tracks = resolved.tracks
            val targetEpisodeNumber = resolved.targetEpisodeNumber

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

    companion object {
        internal fun filterPriorSeasons(currentAnime: Anime, allSeasons: List<Anime>): List<Anime> {
            val currentSeasonNum = currentAnime.seasonNumber
            if (currentSeasonNum == 0.0) return emptyList()
            return allSeasons.filter { season ->
                if (season.id == currentAnime.id) return@filter false
                if (currentSeasonNum > 0.0) {
                    season.seasonNumber > 0.0 && season.seasonNumber < currentSeasonNum
                } else {
                    season.seasonNumber < 0.0 && season.seasonSourceOrder < currentAnime.seasonSourceOrder
                }
            }
        }

        internal fun resolveOffsetEpisodeNumber(
            priorEpisodeCount: Int,
            episodeNumber: Double,
            parentTracks: List<Track>,
        ): Double {
            if (priorEpisodeCount <= 0) return episodeNumber
            val supportsOffset = parentTracks.any {
                it.totalEpisodes == 0L || it.totalEpisodes > priorEpisodeCount
            }
            return if (supportsOffset) priorEpisodeCount + episodeNumber else episodeNumber
        }
    }
}
