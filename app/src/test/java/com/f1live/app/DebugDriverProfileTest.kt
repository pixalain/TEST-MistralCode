package com.f1live.app

import com.f1live.app.data.repository.F1Repository
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DebugDriverProfileTest {

    @Test
    fun debugHamiltonProfile() {
        runBlocking {
            val repo = F1Repository()
            try {
                val p = repo.driverProfile("hamilton", "Ferrari")
                println("PROFILE_OK name=${p.driver.givenName} ${p.driver.familyName} " +
                    "races=${p.stats.races} wins=${p.stats.wins} podiums=${p.stats.podiums} " +
                    "poles=${p.stats.poles} fl=${p.stats.fastestLaps} pts=${p.stats.points} " +
                    "titles=${p.stats.championships} best=${p.stats.bestChampionship} seasons=${p.stats.seasons}")
            } catch (e: Exception) {
                println("PROFILE_FAIL ${e::class.java.name}: ${e.message}")
            }
        }
    }

    @Test
    fun debugAntonelliProfile() {
        runBlocking {
            val repo = F1Repository()
            try {
                val p = repo.driverProfile("antonelli", "Mercedes")
                println("PROFILE_OK name=${p.driver.givenName} ${p.driver.familyName} " +
                    "races=${p.stats.races} wins=${p.stats.wins} podiums=${p.stats.podiums} " +
                    "poles=${p.stats.poles} fl=${p.stats.fastestLaps} pts=${p.stats.points} " +
                    "titles=${p.stats.championships} best=${p.stats.bestChampionship} seasons=${p.stats.seasons}")
            } catch (e: Exception) {
                println("PROFILE_FAIL ${e::class.java.name}: ${e.message}")
            }
        }
    }
}
