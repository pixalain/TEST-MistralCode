package com.f1live.app

import com.f1live.app.data.repository.F1Repository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamProfileTest {

    @Test
    fun redBullProfile2026() = runBlocking {
        val repo = F1Repository()
        val p = repo.constructorProfile("red_bull", "2026")
        println("TEAM=${p.constructor.name} pos=${p.position} pts=${p.points} wins=${p.wins} podiums=${p.podiums} titles=${p.titles}")
        println("DRIVERS=${p.drivers.map { it.Driver.driverId }}")
        println("RECENT=${p.recentForm}")
        println("LOGO=${p.logoUrl}")
        assertTrue(p.constructor.name.startsWith("Red Bull"))
        assertTrue(p.points != null)
        assertTrue("expected drivers", p.drivers.size >= 2)
        assertTrue("expected recent form", p.recentForm.isNotEmpty())
        assertTrue("expected logo", p.logoUrl != null)
        assertTrue("expected titles since 2018 > 0 (Red Bull won 2022-2025)", p.titles >= 2)
    }

    @Test
    fun compareTwoTeams() = runBlocking {
        val repo = F1Repository()
        val a = repo.constructorPointsByRound("red_bull", "2026")
        val b = repo.constructorPointsByRound("mercedes", "2026")
        println("A=${a.name} rounds=${a.rounds.size} total=${a.rounds.sumOf { it.second }}")
        println("B=${b.name} rounds=${b.rounds.size} total=${b.rounds.sumOf { it.second }}")
        assertTrue(a.rounds.isNotEmpty())
        assertTrue(b.rounds.isNotEmpty())
        assertTrue(a.rounds.sumOf { it.second } > 0)
    }
}
