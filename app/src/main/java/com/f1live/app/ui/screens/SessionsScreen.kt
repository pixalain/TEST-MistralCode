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
import androidx.compose.material.icons.filled.Timer
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
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SessionInfo(
    val key: String,
    val labelRes: Int,
    val session: Session?,
    val isRace: Boolean = false
) {
    val alphaCode: String
        get() = when (key) {
            "fp1" -> "FP1"
            "fp2" -> "FP2"
            "fp3" -> "FP3"
            "sq" -> "SQ"
            "sprint" -> "SR"
            "quali" -> "Q"
            else -> "R"
        }
}

fun raceSessions(race: Race): List<SessionInfo> {
    val hasSprint = race.Sprint != null
    return listOfNotNull(
        race.firstPractice?.let { SessionInfo("fp1", R.string.session_fp1, it) },
        race.secondPractice?.let { SessionInfo("fp2", R.string.session_fp2, it) },
        race.thirdPractice?.let { SessionInfo("fp3", R.string.session_fp3, it) },
        if (hasSprint) {
            (race.sprintQualifying ?: race.sprintShootout)?.let {
                SessionInfo("sq", R.string.session_sprint_qualifying, it)
            }
        } else null,
        race.Sprint?.let { SessionInfo("sprint", R.string.session_sprint, it) },
        race.Qualifying?.let { SessionInfo("quali", R.string.session_qualifying, it) },
        SessionInfo("race", R.string.session_race, Session(race.date, race.time), isRace = true)
    )
}

fun sessionDateTime(s: Session?): LocalDateTime? = try {
    if (s == null) null
    else LocalDateTime.of(
        LocalDate.parse(s.date),
        LocalTime.parse((s.time ?: "00:00:00").removeSuffix("Z"))
    )
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

enum class SessionStatus { NOT_SCHEDULED, UPCOMING, IN_PROGRESS, DONE }

fun sessionStatus(info: SessionInfo, now: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)): SessionStatus {
    val start = sessionDateTime(info.session) ?: return SessionStatus.NOT_SCHEDULED
    val durationHours = if (info.isRace) 3 else 1
    return when {
        now.isBefore(start) -> SessionStatus.UPCOMING
        now.isBefore(start.plusHours(durationHours.toLong())) -> SessionStatus.IN_PROGRESS
        else -> SessionStatus.DONE
    }
}

/**
 * The race whose weekend is currently running, or the most recent one.
 */
fun findCurrentOrLastRace(races: List<Race>): Race? {
    val now = LocalDateTime.now(ZoneOffset.UTC)
    val sorted = races.sortedBy { it.date }
    val current = sorted.firstOrNull { race ->
        val start = sessionDateTime(race.firstPractice ?: Session(race.date, race.time))
        val end = sessionDateTime(Session(race.date, race.time))?.plusHours(4)
        start != null && end != null && !now.isBefore(start) && now.isBefore(end)
    }
    if (current != null) return current
    return sorted.lastOrNull { race ->
        val end = sessionDateTime(Session(race.date, race.time))?.plusHours(4) ?: return@lastOrNull false
        !now.isBefore(end)
    } ?: sorted.lastOrNull()
}

@Composable
fun SessionsScreen(
    state: UiState<RacesUi>,
    onRefresh: () -> Unit,
    onRaceClick: (Race, String) -> Unit
) {
    when (state) {
        is UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(state.message, onRefresh)
        is UiState.Success -> {
            val race = findCurrentOrLastRace(state.data.races)
            if (race == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.all_sessions_done))
                }
                return
            }
            val now = LocalDateTime.now(ZoneOffset.UTC)
            val sessions = raceSessions(race)
            val anyInProgress = sessions.any { sessionStatus(it, now) == SessionStatus.IN_PROGRESS }
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = stringResource(
                            if (anyInProgress) R.string.sessions_in_progress else R.string.last_sessions,
                            race.raceName
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "${race.circuitName} — ${race.locality}, ${race.country} · ${formatRaceDate(race.date)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(sessions, key = { it.key }) { info ->
                    SessionCard(
                        info = info,
                        status = sessionStatus(info, now),
                        onClick = { onRaceClick(race, info.alphaCode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(info: SessionInfo, status: SessionStatus, onClick: () -> Unit) {
    val (icon, tint, labelRes) = when (status) {
        SessionStatus.IN_PROGRESS -> Triple(Icons.Default.Timer, MaterialTheme.colorScheme.primary, R.string.session_in_progress)
        SessionStatus.DONE -> Triple(Icons.Default.CheckCircle, Color(0xFF2E7D32), R.string.session_done)
        SessionStatus.UPCOMING -> Triple(Icons.Default.Schedule, MaterialTheme.colorScheme.onSurfaceVariant, R.string.session_upcoming)
        SessionStatus.NOT_SCHEDULED -> Triple(Icons.Default.RadioButtonUnchecked, MaterialTheme.colorScheme.onSurfaceVariant, R.string.session_not_scheduled)
    }

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
            val badgeIcon: ImageVector = if (info.isRace) Icons.Default.Flag else icon
            val badgeTint: Color = if (info.isRace) MaterialTheme.colorScheme.primary else tint
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = badgeTint,
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
                    text = if (info.session != null) formatSessionWhen(info.session)
                           else stringResource(R.string.session_not_scheduled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = tint
                )
                if (status == SessionStatus.DONE) {
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
