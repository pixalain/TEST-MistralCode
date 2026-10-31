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
    state: UiState<RaceDetail>,
    onBack: () -> Unit,
    onRefresh: () -> Unit
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
                is UiState.Success -> DetailContent(state.data)
            }
        }
    }
}

@Composable
private fun DetailContent(detail: RaceDetail) {
    val legacyTabs = buildList {
        add(DetailTab(R.string.race_tab, null))
        if (detail.sprint.isNotEmpty()) add(DetailTab(R.string.sprint_tab, "SPRINT_LEGACY"))
        if (detail.qualifying.isNotEmpty()) add(DetailTab(R.string.qualifying_tab, "QUALI_LEGACY"))
    }
    val alphaTabs = detail.alphaSessions.map { s ->
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
    val tabs = if (alphaTabs.isNotEmpty()) alphaTabs else legacyTabs

    var tab by remember(tabs.size) { mutableIntStateOf(0) }
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
            Text(stringResource(R.string.no_results_yet))
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
                            text = q.position,
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
            row.time?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
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
            r.points?.let {
                Text(
                    text = "+$it",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (it.toDoubleOrNull() != 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
