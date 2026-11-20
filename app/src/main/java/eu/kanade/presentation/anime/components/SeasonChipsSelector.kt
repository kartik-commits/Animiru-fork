package eu.kanade.presentation.anime.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.util.formatEpisodeNumber
import eu.kanade.tachiyomi.ui.anime.AnimeSeasonItem
import tachiyomi.domain.anime.model.Anime
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SeasonChipsSelector(
    parentAnime: Anime,
    selectedSeasonAnime: Anime?,
    seasons: List<AnimeSeasonItem>,
    selectedSeasonId: Long?,
    onSelectSeason: (Long) -> Unit,
    itemCount: Int?,
    modifier: Modifier = Modifier,
) {
    if (seasons.isEmpty()) return

    // Sort seasons chronologically ascending: Season 1 -> Season 2 -> Season 3
    val sortedSeasons = remember(seasons) {
        seasons.sortedWith(
            compareBy<AnimeSeasonItem> {
                val num = it.seasonAnime.anime.seasonNumber
                if (num >= 0.0) num else Double.MAX_VALUE
            }
                .thenBy { it.seasonAnime.anime.seasonSourceOrder }
                .thenBy { it.seasonAnime.anime.id },
        )
    }

    val listState = rememberLazyListState()
    LaunchedEffect(selectedSeasonId, sortedSeasons) {
        val index = sortedSeasons.indexOfFirst { it.seasonAnime.anime.id == selectedSeasonId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        // Horizontal Chip Selector Row (Chronological order)
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(
                items = sortedSeasons,
                key = { it.seasonAnime.anime.id },
            ) { item ->
                val isSelected = item.seasonAnime.anime.id == selectedSeasonId
                val anime = item.seasonAnime.anime
                val chipLabel = remember(parentAnime.title, anime.title, anime.seasonNumber) {
                    formatSeasonChipLabel(
                        parentTitle = parentAnime.title,
                        seasonTitle = anime.title,
                        seasonNumber = anime.seasonNumber,
                    )
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectSeason(anime.id) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = chipLabel,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 160.dp),
                            )
                            if (item.unseenCount > 0L) {
                                Surface(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.size(18.dp),
                                ) {
                                    Text(
                                        text = "${item.unseenCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onPrimary
                                        },
                                        modifier = Modifier.padding(1.dp),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = null,
                )
            }
        }

        // Active Season Header: Clean Typography with Full Crunchyroll Title & Stats
        val activeItem = sortedSeasons.find { it.seasonAnime.anime.id == selectedSeasonId }
        val fullSeasonTitle = selectedSeasonAnime?.title
            ?: activeItem?.seasonAnime?.anime?.title
            ?: parentAnime.title
        val currentUnseenCount = activeItem?.unseenCount ?: 0L

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        ) {
            Text(
                text = fullSeasonTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val subtitle = buildString {
                if (itemCount != null) {
                    append(pluralStringResource(AYMR.plurals.anime_num_episodes, count = itemCount, itemCount))
                } else {
                    append(stringResource(AYMR.strings.episodes))
                }
                if (currentUnseenCount > 0L) {
                    append(" • ")
                    append(currentUnseenCount)
                    append(" ")
                    append(stringResource(AYMR.strings.action_filter_unseen).lowercase())
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun formatSeasonChipLabel(
    parentTitle: String,
    seasonTitle: String,
    seasonNumber: Double,
): String {
    var label = seasonTitle.trim()

    // 1. Remove parent title prefix if present (case-insensitive)
    val trimmedParent = parentTitle.trim()
    if (label.startsWith(trimmedParent, ignoreCase = true)) {
        label = label.substring(trimmedParent.length).trim()
        label = label.trimStart(':', '-', '–', '—', '|', '·', ' ', '/', '\\').trim()
    }

    // 2. Remove show name without subtitles if parentTitle has a colon or hyphen
    // e.g. parent: "Demon Slayer: Kimetsu no Yaiba", season: "Demon Slayer Mugen Train"
    val baseParent = trimmedParent.substringBefore(':').substringBefore('-').substringBefore('–').trim()
    if (baseParent.length > 3 && label.startsWith(baseParent, ignoreCase = true)) {
        label = label.substring(baseParent.length).trim()
        label = label.trimStart(':', '-', '–', '—', '|', '·', ' ', '/', '\\').trim()
    }

    // 3. Remove trailing episode range like (1-61) or (Episodes 1-61), if non-empty name remains
    val rangeRegex = Regex("""\s*\((?:episodes?\s*|ep\s*)?\d+\s*[-–~]\s*\d+\w*\)$""", RegexOption.IGNORE_CASE)
    val withoutRange = rangeRegex.replace(label, "").trim()
    if (withoutRange.isNotBlank()) {
        label = withoutRange
    }

    // 4. Fallback if label became empty or identical to parent
    if (label.isBlank() || label.equals(parentTitle, ignoreCase = true)) {
        return if (seasonNumber >= 0) {
            "Season ${formatEpisodeNumber(seasonNumber)}"
        } else {
            seasonTitle
        }
    }

    return label
}
