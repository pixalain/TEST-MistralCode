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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.f1live.app.R
import com.f1live.app.data.model.Race
import com.f1live.app.data.model.Session
import com.f1live.app.ui.RacesUi
import com.f1live.app.ui.UiState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SessionInfo(
    val key: String,
    val labelRes: Int,
    val session: Session?,
    val isRace: Boolean = false
)

fun raceSessions(race: Race): List<SessionInfo> = listOf(
    SessionInfo("fp1", R.string.session_fp1, race.firstPractice),
    SessionInfo("fp2", R.string.session_fp2, race.secondPractice),
    SessionInfo("fp3", R.string.session_fp3, race.thirdPractice),
    SessionInfo("sprint", R.string.session_sprint, race.Sprint),
    SessionInfo("quali", R.string.session_qualifying, race.Qualifying),
    SessionInfo("race", R.string.session_race, Session(race.date, race.time), isRace = true)
)

fun sessionDateTime(s: Session?): LocalDateTime? = try {
    if (s == null) null
    else LocalDateTime.of(
        LocalDate.parse(s.date),
        java.time.LocalTime.parse(s.time ?: "00:00:00Z".removeSuffix("Z"))
    ).atOffset(ZoneOffset.UTC).toLocalDateTime()
} catch (e: Exception) {
    null
}

fun formatSessionWhen(s: Session): String {
    val dt = sessionDateTime(s) ?: return s.date
    val local = dt.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    return "${local.format(dateFmt)} · ${local.format(timeFmt)}"
}

fun findNextRace(races: List<Race>): Race? {
    val now = LocalDateTime.now(ZoneOffset.UTC)
    return races
        .sortedBy { it.date }
        .firstOrNull { race ->
            val end = sessionDateTime(Session(race.date, race.time))?.plusHours(4) ?: LocalDateTime.MAX
            end.isAfter(now)
        }
}

@Composable
fun SessionsScreen(
    state: UiState<RacesUi>,
    onRefresh: () -> Unit,
    onRaceClick: (Race) -> Unit
) {
    when (state) {
        is UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(state.message, onRefresh)
        is UiState.Success -> {
            val next = findNextRace(state.data.races)
            if (next == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.all_sessions_done))
                }
                return
            }
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.next_race_sessions, next.raceName),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "${next.circuitName} — ${next.locality}, ${next.country} · ${formatRaceDate(next.date)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(raceSessions(next), key = { it.key }) { info ->
                    SessionCard(info = info, onClick = { onRaceClick(next) })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(info: SessionInfo, onClick: () -> Unit) {
    val now = LocalDateTime.now(ZoneOffset.UTC)
    val when_ = sessionDateTime(info.session)
    val done = when_ != null && when_.plusHours(if (info.isRace) 4 else 2).isBefore(now)
    val scheduled = info.session != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon: ImageVector
            val tint: Color
            when {
                info.isRace -> { icon = Icons.Default.Flag; tint = MaterialTheme.colorScheme.primary }
                done -> { icon = Icons.Default.CheckCircle; tint = Color(0xFF2E7D32) }
                scheduled -> { icon = Icons.Default.Schedule; tint = MaterialTheme.colorScheme.onSurfaceVariant }
                else -> { icon = Icons.Default.RadioButtonUnchecked; tint = MaterialTheme.colorScheme.onSurfaceVariant }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(info.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (scheduled) formatSessionWhen(info.session!!)
                           else stringResource(R.string.session_not_scheduled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (scheduled) {
                    Text(
                        text = stringResource(if (done) R.string.session_done else R.string.session_upcoming),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (done) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                }
                if (done && info.isRace) {
                    Text(
                        text = stringResource(R.string.view_results),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
