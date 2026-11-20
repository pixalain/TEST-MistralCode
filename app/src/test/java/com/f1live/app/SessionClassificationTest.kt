package com.f1live.app

import com.f1live.app.data.repository.F1Repository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionClassificationTest {

    @Test
    fun brazil2026Fp1HasClassification() = runBlocking {
        val repo = F1Repository()
        val d = repo.raceResults("20", "2026").data
        val fp1 = d.alphaSessions.first { it.code == "FP1" }
        println("FP1 rows=${fp1.rows.size}")
        fp1.rows.take(5).forEach { println("  P${it.displayPosition} ${it.driver.abbreviation} ${it.time ?: ""}") }
        assertTrue("FP1 classification expected", fp1.rows.size >= 20)
        assertTrue((fp1.rows.first().driver.abbreviation ?: "").isNotBlank())
    }

    @Test
    fun lasVegas2026DetailLoads() = runBlocking {
        val repo = F1Repository()
        val d = repo.raceResults("21", "2026").data
        println("R21 race=${d.race.raceName} alphaSessions=${d.alphaSessions.map { it.code to it.rows.size }}")
        assertTrue(d.alphaSessions.isNotEmpty())
    }
}
