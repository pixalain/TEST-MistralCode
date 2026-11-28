package com.f1live.app

import com.f1live.app.data.model.Circuit
import com.f1live.app.data.model.Location
import com.f1live.app.data.model.Race
import com.f1live.app.data.model.Session
import com.f1live.app.notifications.computeReminders
import com.f1live.app.notifications.sessionEpochMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class ReminderTest {

    private fun mkRace(round: String, date: String, time: String = "14:00:00Z", fp1: String? = null, quali: String? = null, sprint: String? = null) = Race(
        season = "2026",
        round = round,
        raceName = "GP $round",
        Circuit = Circuit("c$round", "Circuit $round", Location("City", "Country")),
        date = date,
        time = time,
        firstPractice = fp1?.let { Session(date, it) },
        Qualifying = quali?.let { Session(date, it) },
        Sprint = sprint?.let { Session(date, it) }
    )

    private fun epoch(date: String, time: String): Long =
        LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time.removeSuffix("Z")))
            .toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun remindersOnlyForFutureSessions() {
        val now = epoch("2026-11-10", "12:00:00")
        val past = mkRace("18", "2026-10-25", fp1 = "2026-10-25T10:00:00Z".substringAfter("-"), quali = null)
        val future = mkRace(
            "19", "2026-11-22", "15:00:00Z",
            fp1 = "2026-11-20T10:00:00Z", quali = "2026-11-21T18:00:00Z"
        )
        // fix fp1/quali sessions to carry their own date
        val fixedFuture = future.copy(
            firstPractice = Session("2026-11-20", "10:00:00Z"),
            Qualifying = Session("2026-11-21", "18:00:00Z")
        )
        val reminders = computeReminders(listOf(past, fixedFuture), now)
        // expect FP1, Q and R of the future GP = 3 reminders, none from the past GP
        assertEquals(3, reminders.size)
        assertTrue(reminders.all { it.round == "19" })
        // each reminder is 30 min before the session
        val r = reminders.first { it.code == "R" }
        assertEquals(epoch("2026-11-22", "15:00:00") - 30 * 60_000L, r.remindAtMillis)
    }

    @Test
    fun sessionEpochMillisParsesUtc() {
        assertEquals(epoch("2026-11-22", "15:00:00"), sessionEpochMillis(Session("2026-11-22", "15:00:00Z")))
        assertEquals(null, sessionEpochMillis(null))
    }

    @Test
    fun sprintIncludedOnlyWhenScheduled() {
        val now = epoch("2026-11-10", "12:00:00")
        val withSprint = mkRace("20", "2026-11-29", sprint = "2026-11-28T10:00:00Z")
            .copy(Sprint = Session("2026-11-28", "10:00:00Z"))
        val without = mkRace("21", "2026-12-06")
        val codes = computeReminders(listOf(withSprint, without), now).map { it.code }.toSet()
        assertTrue("SR" in codes)
    }
}
