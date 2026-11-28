package com.f1live.app

import com.f1live.app.data.repository.F1Repository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonTest {

    @Test
    fun season2024StandingsAndSchedule() {
        runBlocking {
            val repo = F1Repository()
            val standings = repo.driverStandings("2024")
            assertEquals("2024", standings.data.first)
            assertEquals("max_verstappen", standings.data.second.first().Driver.driverId)
            val schedule = repo.seasonSchedule("2024")
            assertEquals("2024", schedule.data.first)
            assertEquals(24, schedule.data.second.size)
            val race = repo.raceResults("1", "2024")
            assertTrue(race.data.race.raceName.contains("Bahrain"))
            assertTrue(race.data.results.isNotEmpty())
        }
    }
}
