package com.f1live.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.f1live.app.R
import com.f1live.app.data.repository.DriverProfile
import com.f1live.app.ui.UiState
import com.f1live.app.ui.teamColor
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

fun driverAge(dateOfBirth: String?): Int? = try {
    if (dateOfBirth.isNullOrBlank()) null
    else Period.between(LocalDate.parse(dateOfBirth), LocalDate.now()).years
} catch (e: Exception) {
    null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(
    state: UiState<DriverProfile>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    photoPath: String? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.driver_profile_title)) },
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
                is UiState.Success -> ProfileContent(state.data, photoPath)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileContent(profile: DriverProfile, photoPath: String? = null) {
    val d = profile.driver
    val s = profile.stats
    val age = driverAge(d.dateOfBirth)
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(teamColor(null)),
                        contentAlignment = Alignment.Center
                    ) {
                        val placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_driver_placeholder)
                        val photoModel: Any? = photoPath?.let { java.io.File(it) } ?: profile.photoUrl
                        if (photoModel != null) {
                            coil.compose.AsyncImage(
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(photoModel)
                                    .memoryCacheKey("photo_${d.driverId}")
                                    .diskCacheKey("photo_${d.driverId}")
                                    .build(),
                                contentDescription = stringResource(R.string.driver_photo),
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape),
                                placeholder = placeholder,
                                error = placeholder,
                                fallback = placeholder
                            )
                        }
                        if (photoModel == null) {
                            Text(
                                text = d.code ?: d.familyName.take(3).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${d.givenName} ${d.familyName}".uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val death = profile.deathDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    Text(
                        text = buildList {
                            d.permanentNumber?.let { add("n°$it") }
                            d.nationality?.let { add(it) }
                            if (death != null && d.dateOfBirth != null) {
                                val birth = runCatching { LocalDate.parse(d.dateOfBirth!!) }.getOrNull()
                                if (birth != null) {
                                    add(stringResource(R.string.years_range, birth.year, death.year))
                                    add(stringResource(R.string.age_at_death, Period.between(birth, death).years))
                                }
                            } else {
                                age?.let { add(stringResource(R.string.years_old, it)) }
                            }
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    d.dateOfBirth?.let {
                        Text(
                            text = stringResource(R.string.born_on, formatBirthDate(it)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (death != null) {
                        Text(
                            text = stringResource(R.string.died_on, formatBirthDate(death.toString())),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    s.currentTeam?.let {
                        Spacer(Modifier.height(6.dp))
                        AssistChip(onClick = {}, label = { Text(it) })
                    }
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.palmares),
                style = MaterialTheme.typography.titleMedium
            )
        }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(Icons.Default.EmojiEvents, stringResource(R.string.stat_wins), s.wins.toString(), Color(0xFFFFD54F))
                StatChip(Icons.Default.Flag, stringResource(R.string.stat_poles), s.poles.toString(), MaterialTheme.colorScheme.primary)
                StatChip(Icons.Default.Star, stringResource(R.string.stat_podiums), s.podiums.toString(), Color(0xFFC0C0C0))
                StatChip(Icons.Default.Timer, stringResource(R.string.stat_fastest_laps), s.fastestLaps.toString(), Color(0xFFB388FF))
                StatChip(Icons.Default.SportsMotorsports, stringResource(R.string.stat_races), s.races.toString(), MaterialTheme.colorScheme.secondary)
                StatChip(Icons.Default.EmojiEvents, stringResource(R.string.stat_points), formatPoints(s.points), MaterialTheme.colorScheme.primary)
                StatChip(Icons.Default.EmojiEvents, stringResource(R.string.stat_seasons), s.seasons.toString(), MaterialTheme.colorScheme.secondary)
                s.bestChampionship?.let {
                    StatChip(
                        Icons.Default.EmojiEvents,
                        stringResource(R.string.stat_best_championship),
                        "P$it",
                        Color(0xFFFFD54F)
                    )
                }
                if (s.championships > 0) {
                    StatChip(Icons.Default.EmojiEvents, stringResource(R.string.stat_world_titles), s.championships.toString(), Color(0xFFFFD54F))
                }
            }
        }
        if (s.recentForm.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.recent_results),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    s.recentForm.forEach { (race, pos) ->
                        AssistChip(
                            onClick = {},
                            label = { Text("$pos · $race", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}

private fun formatPoints(p: Double): String =
    if (p == p.toLong().toDouble()) p.toLong().toString() else String.format(Locale.getDefault(), "%.1f", p)

private fun formatBirthDate(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))
} catch (e: Exception) {
    iso
}

@Composable
private fun StatChip(icon: ImageVector, label: String, value: String, tint: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Column {
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
