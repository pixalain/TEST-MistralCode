package com.f1live.app.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.f1live.app.data.model.ConstructorStanding
import com.f1live.app.data.model.DriverStanding
import com.f1live.app.ui.StandingsUi
import com.f1live.app.ui.UiState
import com.f1live.app.ui.teamColor

@Composable
fun StandingsScreen(
    state: UiState<StandingsUi>,
    onRefresh: () -> Unit,
    onDriverClick: (String, String?) -> Unit = { _, _ -> },
    onTeamClick: (String) -> Unit = {},
    onCompareClick: () -> Unit = {},
    lastUpdated: Long? = null
) {
    var tab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text(stringResource(R.string.tab_drivers)) }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text(stringResource(R.string.tab_teams)) }
            )
        }
        when (state) {
            is UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(state.message, onRefresh)
            is UiState.Success -> {
                if (tab == 0) {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            SeasonHeader(state.data.season, state.fromCache, lastUpdated)
                        }
                        items(state.data.drivers, key = { it.Driver.driverId }) { d ->
                            DriverRow(d, onClick = { onDriverClick(d.Driver.driverId, d.Constructors.firstOrNull()?.name) })
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            SeasonHeader(state.data.season, state.fromCache, lastUpdated)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                androidx.compose.material3.TextButton(onClick = onCompareClick) {
                                    Text(stringResource(R.string.compare_teams))
                                }
                            }
                        }
                        items(state.data.constructors, key = { it.Constructor.constructorId }) { c ->
                            val teamDrivers = state.data.drivers.filter { d ->
                                d.Constructors.any { it.constructorId == c.Constructor.constructorId }
                            }
                            TeamRow(c, teamDrivers, onClick = { onTeamClick(c.Constructor.constructorId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FreshnessBadge(fromCache: Boolean, timestamp: Long?) {
    if (timestamp == null || timestamp <= 0L) return
    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    val date = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    Text(
        text = if (fromCache) stringResource(R.string.offline_cached, date, time)
               else stringResource(R.string.updated_at, time),
        style = MaterialTheme.typography.labelSmall,
        color = if (fromCache) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SeasonHeader(season: String, fromCache: Boolean = false, timestamp: Long? = null) {
    Text(
        text = stringResource(R.string.season_classification, season),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    FreshnessBadge(fromCache, timestamp)
}

@Composable
private fun DriverRow(d: DriverStanding, onClick: () -> Unit = {}) {
    val team = d.Constructors.firstOrNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = d.position ?: d.positionText ?: "-",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(teamColor(team?.constructorId))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${d.Driver.givenName} ${d.Driver.familyName}".uppercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                team?.name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!d.wins.isNullOrBlank() && d.wins != "0") {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = stringResource(R.string.wins),
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(text = d.wins, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = d.points,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.pts),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TeamRow(
    c: ConstructorStanding,
    teamDrivers: List<com.f1live.app.data.model.DriverStanding> = emptyList(),
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = c.position ?: c.positionText ?: "-",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(teamColor(c.Constructor.constructorId))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = c.Constructor.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = c.Constructor.nationality ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (teamDrivers.isNotEmpty()) {
                    Text(
                        text = teamDrivers.joinToString(" · ") { d ->
                            "${d.Driver.familyName} ${d.points}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!c.wins.isNullOrBlank() && c.wins != "0") {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = stringResource(R.string.wins),
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(text = c.wins, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = c.points,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.pts),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.error_prefix, message),
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
