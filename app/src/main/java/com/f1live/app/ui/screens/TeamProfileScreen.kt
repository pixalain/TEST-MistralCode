package com.f1live.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.f1live.app.R
import com.f1live.app.data.repository.ConstructorProfile
import com.f1live.app.ui.UiState
import com.f1live.app.ui.teamColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeamProfileScreen(
    state: UiState<ConstructorProfile>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDriverClick: (String, String?) -> Unit = { _, _ -> }
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.team_profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (state) {
                is UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(state.message, onRefresh)
                is UiState.Success -> TeamContent(state.data, onDriverClick)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeamContent(p: ConstructorProfile, onDriverClick: (String, String?) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(teamColor(p.constructor.constructorId)),
                            contentAlignment = Alignment.Center
                        ) {
                            val logoModel: Any? = p.logoPath?.let { java.io.File(it) } ?: p.logoUrl
                            if (logoModel != null) {
                                coil.compose.AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(logoModel)
                                        .memoryCacheKey("clogo_${p.constructor.constructorId}")
                                        .diskCacheKey("clogo_${p.constructor.constructorId}")
                                        .build(),
                                    contentDescription = stringResource(R.string.team_logo),
                                    modifier = Modifier.size(72.dp).clip(CircleShape),
                                    placeholder = androidx.compose.ui.res.painterResource(R.drawable.ic_driver_placeholder),
                                    error = androidx.compose.ui.res.painterResource(R.drawable.ic_driver_placeholder),
                                    fallback = androidx.compose.ui.res.painterResource(R.drawable.ic_driver_placeholder)
                                )
                            } else {
                                Text(
                                    text = p.constructor.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = p.constructor.name.uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = listOfNotNull(
                                p.position?.let { "P$it" },
                                p.constructor.nationality,
                                p.season,
                                p.points?.let { "$it${stringResource(R.string.pts)}" }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatChipT(Icons.Default.EmojiEvents, stringResource(R.string.wins), p.wins.toString(), Color(0xFFFFD54F))
                    StatChipT(Icons.Default.Star, stringResource(R.string.stat_podiums_team), p.podiums.toString(), Color(0xFFC0C0C0))
                    StatChipT(Icons.Default.Flag, stringResource(R.string.stat_best_result), p.bestResult ?: "—", MaterialTheme.colorScheme.primary)
                    StatChipT(Icons.Default.EmojiEvents, stringResource(R.string.stat_constructor_titles), p.titles.toString(), Color(0xFFFFD54F))
                }
            }
            if (p.drivers.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.team_drivers),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                items(p.drivers, key = { it.Driver.driverId }) { d ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clickable { onDriverClick(d.Driver.driverId, p.constructor.name) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = d.position ?: "-",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${d.Driver.givenName} ${d.Driver.familyName}".uppercase(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(text = d.points, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = stringResource(R.string.pts), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (p.recentForm.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.team_recent_form),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                item { RecentFormBars(p.recentForm, teamColor(p.constructor.constructorId)) }
            }
        }
    }
}

@Composable
private fun RecentFormBars(form: List<Pair<String, Double>>, color: Color) {
    val max = form.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        form.forEach { (label, pts) ->
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .fillMaxWidth(pts.toFloat() / max.toFloat())
                            .background(color, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(formatPts(pts), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatPts(p: Double): String =
    if (p == p.toLong().toDouble()) p.toLong().toString() else String.format(java.util.Locale.getDefault(), "%.1f", p)

@Composable
private fun StatChipT(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, tint: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Column {
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
