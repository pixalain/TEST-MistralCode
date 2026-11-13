package com.f1live.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.f1live.app.R
import com.f1live.app.data.model.AlphaResultRow
import com.f1live.app.data.model.Result
import com.f1live.app.data.repository.RaceDetail
import com.f1live.app.data.repository.SessionResults
import com.f1live.app.ui.UiState
import com.f1live.app.ui.teamColor

private data class DetailTab(val labelRes: Int, val code: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceDetailScreen(
    round: String,
    initialTab: String = "",
    state: UiState<RaceDetail>,
    liveState: UiState<com.f1live.app.data.repository.LiveLaps> = UiState.Loading,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshLive: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.race_results_title, round)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (state) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(state.message, onRefresh)
                is UiState.Success -> DetailContent(state.data, initialTab, round, liveState, onRefreshLive)
            }
        }
    }
}

@Composable
private fun LiveTab(round: String, onRefreshLive: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(round) { onRefreshLive() }
    androidx.compose.runtime.LaunchedEffect(round) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            onRefreshLive()
        }
    }
}

@Composable
fun LiveLapsContent(liveState: UiState<com.f1live.app.data.repository.LiveLaps>) {
    when (liveState) {
        is UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(liveState.message) {}
        is UiState.Success -> {
            val live = liveState.data
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text(
                        text = "🔴 Live — tour ${live.lastLap}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(live.rows, key = { "live-${it.position}" }) { row ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${row.position}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(30.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.driverCode.uppercase(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (row.teamName.isNotBlank()) {
                                    Text(
                                        text = row.teamName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (row.isFastest) {
                                Text(text = "🏁", style = MaterialTheme.typography.labelLarge)
                            }
                            row.lapTime?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (row.isFastest) Color(0xFFB388FF) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: RaceDetail,
    initialTab: String = "",
    round: String = "",
    liveState: UiState<com.f1live.app.data.repository.LiveLaps> = UiState.Loading,
    onRefreshLive: () -> Unit = {}
) {
    val gridEntries = gridEntries(detail)
    val legacyTabs = buildList {
        if (detail.qualifying.isNotEmpty()) add(DetailTab(R.string.qualifying_tab, "QUALI_LEGACY"))
        if (detail.sprint.isNotEmpty()) add(DetailTab(R.string.sprint_tab, "SPRINT_LEGACY"))
        add(DetailTab(R.string.race_tab, null))
        if (gridEntries.isNotEmpty()) add(DetailTab(R.string.starting_grid_tab, "GRID"))
    }
    val hasSprint = detail.race.Sprint != null || detail.sprint.isNotEmpty()
    val alphaTabs = detail.alphaSessions
        .filter { s -> hasSprint || (s.code != "SQ" && s.code != "SR") }
        .map { s ->
            DetailTab(
                when (s.code) {
                    "FP1" -> R.string.session_fp1
                    "FP2" -> R.string.session_fp2
                    "FP3" -> R.string.session_fp3
                    "Q" -> R.string.session_qualifying
                    "SQ" -> R.string.session_sprint_qualifying
                    "SR" -> R.string.sprint_tab
                    "R" -> R.string.race_tab
                    else -> R.string.session_race
                },
                s.code
            )
        }
    val alphaOrdered = alphaTabs.sortedBy { t ->
        when (t.code) {
            "FP1" -> 10
            "FP2" -> 20
            "FP3" -> 30
            "SQ" -> 40
            "SR" -> 50
            "Q" -> 80
            "R" -> 90
            else -> 60
        }
    }
    val hasRace = detail.alphaSessions.any { it.code == "R" && it.rows.isNotEmpty() } || detail.results.isNotEmpty()
    val seasonOk = (detail.race.season.toIntOrNull() ?: 2026) >= 2018 // alpha live data starts 2018
    val liveTab = if (hasRace && seasonOk) DetailTab(R.string.live_tab, "LIVE") else null
    val tabs = if (detail.alphaSessions.isNotEmpty()) {
        (if (gridEntries.isNotEmpty()) alphaOrdered + DetailTab(R.string.starting_grid_tab, "GRID") else alphaOrdered) +
            listOfNotNull(liveTab)
    } else legacyTabs + listOfNotNull(liveTab)

    val initialIndex = remember(tabs.size, initialTab) {
        if (initialTab.isBlank()) 0
        else tabs.indexOfFirst { it.code == initialTab }.takeIf { it >= 0 } ?: 0
    }
    var tab by remember(tabs.size) { mutableIntStateOf(initialIndex) }
    val current = tabs[tab.coerceAtMost(tabs.lastIndex)]

    Column {
        val many = tabs.size > 4
        if (many) {
            ScrollableTabRow(selectedTabIndex = tab.coerceAtMost(tabs.lastIndex), edgePadding = 12.dp) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(t.labelRes)) })
                }
            }
        } else {
            TabRow(selectedTabIndex = tab.coerceAtMost(tabs.lastIndex)) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(t.labelRes)) })
                }
            }
        }
        when (current.code) {
            null -> ResultsList(detail.race, detail.results)
            "SPRINT_LEGACY" -> ResultsList(detail.race, detail.sprint)
            "QUALI_LEGACY" -> LegacyQualifyingList(detail.race, detail.qualifying)
            "GRID" -> StartingGridList(detail.race, gridEntries)
            "LIVE" -> {
                LiveTab(round, onRefreshLive)
                LiveLapsContent(liveState)
            }
            else -> {
                val session = detail.alphaSessions.firstOrNull { it.code == current.code }
                if (session == null) {
                    ResultsList(detail.race, detail.results)
                } else {
                    AlphaResultsList(detail.race, session)
                }
            }
        }
    }
}

private data class GridEntry(
    val pos: Int,
    val name: String,
    val code: String?,
    val team: String,
    val teamId: String?
)

private fun gridEntries(detail: RaceDetail): List<GridEntry> {
    val fromResults = detail.results.mapNotNull { r ->
        val g = r.grid?.toIntOrNull()
        if (g != null && g > 0) {
            GridEntry(
                g,
                "${r.Driver.givenName} ${r.Driver.familyName}",
                r.Driver.code,
                r.Constructor.name,
                r.Constructor.constructorId
            )
        } else null
    }
    if (fromResults.isNotEmpty()) return fromResults.sortedBy { it.pos }
    return detail.qualifying.mapNotNull { q ->
        val p = q.position?.toIntOrNull()
        if (p != null) {
            GridEntry(
                p,
                "${q.Driver.givenName} ${q.Driver.familyName}",
                q.Driver.code,
                q.Constructor.name,
                q.Constructor.constructorId
            )
        } else null
    }.sortedBy { it.pos }
}

@Composable
private fun StartingGridList(race: com.f1live.app.data.model.Race, entries: List<GridEntry>) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_results_yet))
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = "${race.raceName} — ${stringResource(R.string.starting_grid_tab)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(entries, key = { "grid-${it.pos}" }) { e ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${e.pos}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(30.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(teamColor(e.teamId))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = (e.code ?: e.name.take(3)).uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${e.name} · ${e.team}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FastestLapBadge(time: String?, isRaceFastest: Boolean) {
    if (time.isNullOrBlank()) return
    Text(
        text = stringResource(R.string.fastest_lap_label, time),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (isRaceFastest) FontWeight.Bold else FontWeight.Normal,
        color = if (isRaceFastest) Color(0xFFB388FF) else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun GridDelta(grid: Int?, positionText: String) {
    val pos = positionText.toIntOrNull()
    if (grid == null || grid <= 0) {
        Text(
            text = stringResource(R.string.no_grid),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    if (pos == null) return
    val delta = grid - pos
    val (text, color) = when {
        delta > 0 -> stringResource(R.string.places_gained, delta) to Color(0xFF2E7D32)
        delta < 0 -> stringResource(R.string.places_lost, -delta) to Color(0xFFC62828)
        else -> "=" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

@Composable
private fun ResultsList(race: com.f1live.app.data.model.Race, results: List<Result>) {
    if (results.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_results_yet))
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = "${race.raceName} — ${formatRaceDate(race.date)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(results, key = { "${it.Driver.driverId}-${it.position}" }) { r ->
                ResultRow(r)
            }
        }
    }
}

@Composable
private fun AlphaResultsList(race: com.f1live.app.data.model.Race, session: SessionResults) {
    if (session.rows.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.no_results_yet),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${race.raceName} — ${session.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = "${race.raceName} — ${session.title}" +
                        (session.timestamp?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(session.rows, key = { "${it.driver.id}-${it.position}" }) { row ->
                AlphaRow(row)
            }
        }
    }
}

@Composable
private fun LegacyQualifyingList(race: com.f1live.app.data.model.Race, qualifying: List<com.f1live.app.data.model.QualifyingResult>) {
    if (qualifying.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_results_yet))
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = "${race.raceName} — ${stringResource(R.string.session_qualifying)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(qualifying, key = { "${it.Driver.driverId}-${it.position}" }) { q ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = q.position ?: "-",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(30.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(teamColor(q.Constructor.constructorId))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${q.Driver.givenName} ${q.Driver.familyName}".uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = q.Constructor.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        q.bestTime?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlphaRow(row: AlphaResultRow) {
    val colorHex = row.team?.primary_color
    val dotColor = colorHex?.let {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrDefault(
            teamColor(null)
        )
    } ?: teamColor(null)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.displayPosition,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp)
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.driver.fullName.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val sub = buildList {
                    row.team?.name?.let { add(it) }
                    row.status?.let { add(it.lowercase().replaceFirstChar { c -> c.uppercase() }) }
                    row.laps?.let { add("$it tours") }
                }.joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                row.time?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                GridDelta(row.gridPosition, row.displayPosition)
                FastestLapBadge(row.fastestLap, row.fastestLapRank == 1)
            }
        }
    }
}

@Composable
private fun ResultRow(r: Result) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = r.displayPosition,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp)
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(teamColor(r.Constructor.constructorId))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${r.Driver.givenName} ${r.Driver.familyName}".uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${r.Constructor.name} · ${r.status ?: ""}" +
                        (r.Time?.time?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                r.points?.let {
                    Text(
                        text = "+$it",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (it.toDoubleOrNull() != 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                GridDelta(r.grid?.toIntOrNull(), r.displayPosition)
                FastestLapBadge(r.FastestLap?.Time?.time, r.FastestLap?.rank == "1")
            }
        }
    }
}
