package com.f1live.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.f1live.app.R
import com.f1live.app.data.model.ConstructorStanding
import com.f1live.app.data.repository.ConstructorRoundPoints
import com.f1live.app.ui.UiState
import com.f1live.app.ui.teamColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCompareScreen(
    teams: List<ConstructorStanding>,
    state: UiState<List<ConstructorRoundPoints>>,
    id1: String,
    id2: String,
    onPick: (String, String) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    var expandedA by remember { mutableStateOf(false) }
    var expandedB by remember { mutableStateOf(false) }
    val nameA = teams.firstOrNull { it.Constructor.constructorId == id1 }?.Constructor?.name ?: id1
    val nameB = teams.firstOrNull { it.Constructor.constructorId == id2 }?.Constructor?.name ?: id2

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compare_teams)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(expanded = expandedA, onExpandedChange = { expandedA = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = nameA, onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(R.string.compare_pick_a)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedA) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedA, onDismissRequest = { expandedA = false }) {
                            teams.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.Constructor.name) },
                                    onClick = { onPick(t.Constructor.constructorId, id2); expandedA = false }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(expanded = expandedB, onExpandedChange = { expandedB = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = nameB, onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(R.string.compare_pick_b)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedB) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedB, onDismissRequest = { expandedB = false }) {
                            teams.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.Constructor.name) },
                                    onClick = { onPick(id1, t.Constructor.constructorId); expandedB = false }
                                )
                            }
                        }
                    }
                }
            }
            item {
                when (state) {
                    is UiState.Loading -> LoadingBox()
                    is UiState.Error -> ErrorBox(state.message, onRefresh)
                    is UiState.Success -> {
                        val (a, b) = state.data
                        Text(
                            stringResource(R.string.compare_chart_title, "${a.name} vs ${b.name}"),
                            style = MaterialTheme.typography.titleMedium
                        )
                        CompareChart(a, b)
                        RoundLabels(a, b)
                        Legend(a, b)
                        Spacer(Modifier.height(8.dp))
                        val totalA = a.rounds.sumOf { it.second }
                        val totalB = b.rounds.sumOf { it.second }
                        Text("${a.name} : ${fmt(totalA)} pts — ${b.name} : ${fmt(totalB)} pts", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(a: ConstructorRoundPoints, b: ConstructorRoundPoints) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(teamColor(a.constructorId)))
            Spacer(Modifier.width(4.dp))
            Text(a.name, style = MaterialTheme.typography.labelMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(teamColor(b.constructorId)))
            Spacer(Modifier.width(4.dp))
            Text(b.name, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CompareChart(a: ConstructorRoundPoints, b: ConstructorRoundPoints) {
    val maxRound = maxOf(a.rounds.size, b.rounds.size).coerceAtLeast(1)
    val allRounds = (1..maxRound).map { "R$it" }
    val aMap = a.rounds.toMap()
    val bMap = b.rounds.toMap()
    // cumulative points per round
    val aCum = mutableListOf<Double>()
    val bCum = mutableListOf<Double>()
    var ca = 0.0; var cb = 0.0
    allRounds.forEach { r ->
        ca += aMap[r] ?: 0.0
        cb += bMap[r] ?: 0.0
        aCum.add(ca); bCum.add(cb)
    }
    val maxY = maxOf(aCum.maxOrNull() ?: 0.0, bCum.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        val w = size.width
        val h = size.height
        val pad = 24f
        fun x(i: Int) = pad + (w - 2 * pad) * i / (maxRound - 1).coerceAtLeast(1)
        fun y(v: Double) = h - pad - (h - 2 * pad) * (v / maxY).toFloat()
        fun drawLine(cum: List<Double>, color: Color) {
            val path = Path()
            cum.forEachIndexed { i, v ->
                if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v))
            }
            drawPath(path, color, style = Stroke(width = 5f))
            cum.forEachIndexed { i, v -> drawCircle(color, radius = 6f, center = Offset(x(i), y(v))) }
        }
        drawLine(aCum, teamColor(a.constructorId))
        drawLine(bCum, teamColor(b.constructorId))
    }
}

@Composable
private fun RoundLabels(a: ConstructorRoundPoints, b: ConstructorRoundPoints) {
    val maxRound = maxOf(a.rounds.size, b.rounds.size).coerceAtLeast(1)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (1..maxRound).forEach { i ->
            if (i % 4 == 1 || maxRound <= 8) {
                Text("R$i", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(Modifier.width(16.dp))
            }
        }
    }
}

private fun fmt(p: Double): String =
    if (p == p.toLong().toDouble()) p.toLong().toString() else String.format(java.util.Locale.getDefault(), "%.1f", p)
