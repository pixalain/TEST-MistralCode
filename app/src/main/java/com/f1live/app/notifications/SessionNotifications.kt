package com.f1live.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.f1live.app.MainActivity
import com.f1live.app.R
import com.f1live.app.data.model.Race
import com.f1live.app.data.model.Session
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

const val CHANNEL_SESSIONS = "sessions"

fun ensureNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_SESSIONS,
            context.getString(R.string.notif_channel_sessions),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_sessions_desc)
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }
}

fun notificationsEnabled(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

data class Reminder(
    val round: String,
    val code: String,
    val label: String,
    val raceName: String,
    val sessionAtMillis: Long,
    val remindAtMillis: Long
)

fun sessionEpochMillis(session: Session?): Long? {
    if (session == null) return null
    return try {
        val time = session.time?.removeSuffix("Z")?.let { LocalTime.parse(it) } ?: LocalTime.NOON
        LocalDateTime.of(LocalDate.parse(session.date), time).toInstant(ZoneOffset.UTC).toEpochMilli()
    } catch (e: Exception) {
        null
    }
}

private fun raceSessions(race: Race): List<Pair<String, Session?>> = listOf(
    "FP1" to race.firstPractice,
    "Q" to race.Qualifying,
    "SR" to race.Sprint,
    "R" to Session(race.date, race.time)
)

private fun labelFor(code: String, context: Context? = null): String = when (code) {
    "FP1" -> "FP1"
    "Q" -> "Qualifications"
    "SR" -> "Sprint"
    "R" -> "Course"
    else -> code
}

/**
 * Pure function: computes which session reminders to schedule.
 * Only FP1 / Qualifying / Sprint / Race of upcoming weekends, max [maxReminders].
 */
fun computeReminders(races: List<Race>, now: Long, leadMinutes: Long = 30, maxReminders: Int = 6): List<Reminder> {
    val out = mutableListOf<Reminder>()
    for (race in races.sortedBy { it.date }) {
        for ((code, session) in raceSessions(race)) {
            if (code == "FP1" && race.firstPractice == null) continue
            val at = sessionEpochMillis(session) ?: continue
            if (at <= now) continue
            val remindAt = at - leadMinutes * 60_000L
            if (remindAt <= now) continue
            out.add(
                Reminder(
                    round = race.round,
                    code = code,
                    label = labelFor(code),
                    raceName = race.raceName,
                    sessionAtMillis = at,
                    remindAtMillis = remindAt
                )
            )
        }
    }
    return out.sortedBy { it.remindAtMillis }.take(maxReminders)
}

fun scheduleSessionReminders(context: Context, races: List<Race>, now: Long = System.currentTimeMillis()) {
    if (!notificationsEnabled(context)) return
    val wm = WorkManager.getInstance(context)
    for (r in computeReminders(races, now)) {
        val delay = r.remindAtMillis - now
        if (delay <= 0) continue
        val work = OneTimeWorkRequestBuilder<SessionReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    "title" to "${r.raceName} — ${r.label}",
                    "text" to "Dans 30 min (${sessionTimeLabel(r.sessionAtMillis)})",
                    "notifId" to (r.round.hashCode() * 31 + r.code.hashCode())
                )
            )
            .build()
        wm.enqueueUniqueWork("rem_${r.round}_${r.code}", ExistingWorkPolicy.REPLACE, work)
    }
}

private fun sessionTimeLabel(millis: Long): String =
    java.text.SimpleDateFormat("EEE d MMM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(millis))

class SessionReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: return Result.failure()
        val text = inputData.getString("text") ?: ""
        val id = inputData.getInt("notifId", (System.currentTimeMillis() % Int.MAX_VALUE).toInt())
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            applicationContext, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_SESSIONS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .build()
        return try {
            NotificationManagerCompat.from(applicationContext).notify(id, notif)
            Result.success()
        } catch (e: SecurityException) {
            Result.failure()
        }
    }
}
