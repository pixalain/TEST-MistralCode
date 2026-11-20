package com.f1live.app

import com.f1live.app.data.repository.F1Repository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RaceWeekendTest {

    @Test
    fun lasVegas2026WeekendProgram() = runBlocking {
        val repo = F1Repository()
        val w = repo.raceWeekendSessions("2026", 21)
        println("RACE=${w.raceName} sessions=${w.sessions.size}")
        w.sessions.forEach { s ->
            println("${s.code} ${s.title} held=${s.held} ts=${s.timestamp} top=${s.entries.take(3).joinToString(" | ") { "P${it.position} ${it.driverCode} ${it.time ?: ""}" }}")
        }
        assertTrue(w.raceName.contains("Las Vegas"))
        assertTrue("expected FP1", w.sessions.any { it.code == "FP1" })
        assertTrue("expected FP2", w.sessions.any { it.code == "FP2" })
        assertTrue("expected Race", w.sessions.any { it.code == "R" })
        // FP1/FP2 of 2026-11-20 already held (server now ~2026-11-20), race on 11-22 not yet
        assertTrue(w.sessions.first { it.code == "FP1" }.held)
        assertTrue(w.sessions.first { it.code == "FP2" }.held)
        assertFalse(w.sessions.first { it.code == "R" }.held)
    }

    @Test
    fun brazil2026WeekendHasFp1Results() = runBlocking {
        val repo = F1Repository()
        val w = repo.raceWeekendSessions("2026", 20)
        val fp1 = w.sessions.first { it.code == "FP1" }
        println("BRAZIL FP1 held=${fp1.held} entries=${fp1.entries.size} top=${fp1.entries.firstOrNull()}")
        assertTrue(fp1.held)
        assertTrue("FP1 results expected for Brazil", fp1.entries.isNotEmpty())
        assertTrue(fp1.entries.first().driverCode.isNotBlank())
    }
}
