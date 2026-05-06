package com.dn0ne.player.app.presentation.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.dn0ne.player.R
import com.dn0ne.player.app.data.repository.TrackRepository
import com.dn0ne.player.app.data.repository.TrackStatsRepository
import com.dn0ne.player.app.domain.track.Track
import com.dn0ne.player.app.domain.track.TrackStats
import com.dn0ne.player.app.presentation.components.CoverArt
import com.dn0ne.player.app.presentation.components.NoteCard
import com.dn0ne.player.app.presentation.components.topbar.ColumnWithCollapsibleTopBar
import com.dn0ne.player.core.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TOP_LIMIT = 10

private data class TrackWithStats(val track: Track, val stats: TrackStats)
private data class ArtistStats(
    val artist: String,
    val totalListeningMs: Long,
    val playCount: Int,
)

private data class StatsViewState(
    val totalListeningMs: Long,
    val totalPlays: Int,
    val totalSkips: Int,
    val tracksWithStats: Int,
    val mostPlayed: List<TrackWithStats>,
    val mostListenedTo: List<TrackWithStats>,
    val recentlyPlayed: List<TrackWithStats>,
    val topArtists: List<ArtistStats>,
) {
    val isEmpty: Boolean get() = tracksWithStats == 0
}

@Composable
fun LibraryStatsScreen(
    settings: Settings,
    statsRepository: TrackStatsRepository,
    trackRepository: TrackRepository,
    onBackClick: () -> Unit,
    onOpenPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var collapseFraction by remember { mutableFloatStateOf(0f) }

    ColumnWithCollapsibleTopBar(
        topBarContent = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = context.resources.getString(R.string.back),
                )
            }

            Text(
                text = context.resources.getString(R.string.stats_title),
                fontSize = lerp(
                    MaterialTheme.typography.titleLarge.fontSize,
                    MaterialTheme.typography.displaySmall.fontSize,
                    collapseFraction,
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp),
            )
        },
        collapseFraction = { collapseFraction = it },
        contentPadding = PaddingValues(horizontal = 28.dp),
        contentHorizontalAlignment = Alignment.CenterHorizontally,
        contentVerticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        val isTrackingEnabled by settings.trackPlayStats.collectAsState()

        if (!isTrackingEnabled) {
            DisabledStub(onOpenPrivacyClick = onOpenPrivacyClick)
            return@ColumnWithCollapsibleTopBar
        }

        val allStats by statsRepository.observeAll()
            .collectAsState(initial = emptyList())

        // Tracks come from MediaStore via a synchronous IO call, so wrap
        // it in produceState. Re-loaded if the user comes back to this
        // screen after a library scan.
        val tracksByUri by produceState<Map<String, Track>>(
            initialValue = emptyMap(),
            key1 = allStats.size,
        ) {
            value = withContext(Dispatchers.IO) {
                trackRepository.getTracks().associateBy { it.uri.toString() }
            }
        }

        val viewState = remember(allStats, tracksByUri) {
            buildViewState(allStats, tracksByUri)
        }

        if (viewState.isEmpty) {
            EmptyStub()
            return@ColumnWithCollapsibleTopBar
        }

        SummaryCard(viewState)

        if (viewState.mostPlayed.isNotEmpty()) {
            StatsSection(
                title = context.resources.getString(R.string.stats_section_most_played),
                content = {
                    viewState.mostPlayed.forEach { item ->
                        StatsTrackRow(
                            track = item.track,
                            metric = pluralPlays(context, item.stats.playCount),
                        )
                    }
                },
            )
        }

        if (viewState.mostListenedTo.isNotEmpty()) {
            StatsSection(
                title = context.resources.getString(R.string.stats_section_most_listened),
                content = {
                    viewState.mostListenedTo.forEach { item ->
                        StatsTrackRow(
                            track = item.track,
                            metric = formatDuration(item.stats.totalListeningMs),
                        )
                    }
                },
            )
        }

        if (viewState.recentlyPlayed.isNotEmpty()) {
            StatsSection(
                title = context.resources.getString(R.string.stats_section_recently_played),
                content = {
                    viewState.recentlyPlayed.forEach { item ->
                        StatsTrackRow(
                            track = item.track,
                            metric = pluralPlays(context, item.stats.playCount),
                        )
                    }
                },
            )
        }

        if (viewState.topArtists.isNotEmpty()) {
            StatsSection(
                title = context.resources.getString(R.string.stats_section_top_artists),
                content = {
                    viewState.topArtists.forEach { item ->
                        StatsArtistRow(
                            artist = item.artist,
                            metric = formatDuration(item.totalListeningMs),
                        )
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun buildViewState(
    stats: List<TrackStats>,
    tracksByUri: Map<String, Track>,
): StatsViewState {
    if (stats.isEmpty()) {
        return StatsViewState(
            totalListeningMs = 0,
            totalPlays = 0,
            totalSkips = 0,
            tracksWithStats = 0,
            mostPlayed = emptyList(),
            mostListenedTo = emptyList(),
            recentlyPlayed = emptyList(),
            topArtists = emptyList(),
        )
    }

    // Drop rows whose track is no longer in the library — we have no
    // sensible way to render them. The total count still reflects only
    // resolved entries so the summary card matches what the user sees in
    // the lists below.
    val resolved = stats.mapNotNull { row ->
        val track = tracksByUri[row.uri] ?: return@mapNotNull null
        TrackWithStats(track = track, stats = row)
    }

    val totalListeningMs = resolved.sumOf { it.stats.totalListeningMs }
    val totalPlays = resolved.sumOf { it.stats.playCount }
    val totalSkips = resolved.sumOf { it.stats.skipCount }

    val mostPlayed = resolved
        .filter { it.stats.playCount > 0 }
        .sortedWith(compareByDescending<TrackWithStats> { it.stats.playCount }.thenByDescending { it.stats.totalListeningMs })
        .take(TOP_LIMIT)

    val mostListenedTo = resolved
        .filter { it.stats.totalListeningMs > 0 }
        .sortedByDescending { it.stats.totalListeningMs }
        .take(TOP_LIMIT)

    val recentlyPlayed = resolved
        .filter { it.stats.lastPlayedAt != null }
        .sortedByDescending { it.stats.lastPlayedAt ?: 0L }
        .take(TOP_LIMIT)

    val topArtists = resolved
        .filter { !it.track.artist.isNullOrBlank() && it.stats.totalListeningMs > 0 }
        .groupBy { it.track.artist!! }
        .map { (artist, items) ->
            ArtistStats(
                artist = artist,
                totalListeningMs = items.sumOf { it.stats.totalListeningMs },
                playCount = items.sumOf { it.stats.playCount },
            )
        }
        .sortedByDescending { it.totalListeningMs }
        .take(TOP_LIMIT)

    return StatsViewState(
        totalListeningMs = totalListeningMs,
        totalPlays = totalPlays,
        totalSkips = totalSkips,
        tracksWithStats = resolved.size,
        mostPlayed = mostPlayed,
        mostListenedTo = mostListenedTo,
        recentlyPlayed = recentlyPlayed,
        topArtists = topArtists,
    )
}

@Composable
private fun SummaryCard(state: StatsViewState) {
    val context = LocalContext.current
    NoteCard(
        label = context.resources.getString(R.string.stats_summary_title),
        leadingIcon = Icons.Rounded.Insights,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SummaryRow(
            icon = Icons.Rounded.Schedule,
            label = context.resources.getString(R.string.stats_summary_total_time),
            value = formatDuration(state.totalListeningMs),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SummaryRow(
            icon = Icons.Rounded.PlayArrow,
            label = context.resources.getString(R.string.stats_summary_plays_skips),
            value = context.resources.getString(
                R.string.stats_summary_plays_skips_value,
                state.totalPlays,
                state.totalSkips,
            ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SummaryRow(
            icon = Icons.Rounded.History,
            label = context.resources.getString(R.string.stats_summary_tracks),
            value = state.tracksWithStats.toString(),
        )
    }
}

@Composable
private fun SummaryRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
        )
        content()
    }
}

@Composable
private fun StatsTrackRow(track: Track, metric: String) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        CoverArt(
            uri = track.coverArtUri,
            modifier = Modifier
                .size(44.dp)
                .clip(ShapeDefaults.Small),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title ?: context.resources.getString(R.string.unknown_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist ?: context.resources.getString(R.string.unknown_artist),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = metric,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatsArtistRow(artist: String, metric: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = artist,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = metric,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DisabledStub(onOpenPrivacyClick: () -> Unit) {
    val context = LocalContext.current
    NoteCard(
        label = context.resources.getString(R.string.stats_disabled_title),
        leadingIcon = Icons.Rounded.Insights,
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeDefaults.Medium),
    ) {
        Text(
            text = context.resources.getString(R.string.stats_disabled_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeDefaults.Small)
                .background(color = MaterialTheme.colorScheme.tertiaryContainer)
                .clickable(onClick = onOpenPrivacyClick)
                .padding(vertical = 12.dp, horizontal = 16.dp),
        ) {
            Text(
                text = context.resources.getString(R.string.stats_disabled_action),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun EmptyStub() {
    val context = LocalContext.current
    NoteCard(
        label = context.resources.getString(R.string.stats_empty_title),
        leadingIcon = Icons.Rounded.Insights,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = context.resources.getString(R.string.stats_empty_body),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0m"
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        totalMinutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}

private fun pluralPlays(context: android.content.Context, count: Int): String =
    context.resources.getQuantityString(R.plurals.stats_plays, count, count)
