package com.f1live.app

import com.f1live.app.data.repository.F1Repository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentFormTest {

    @Test
    fun recentResultsAreTheMostRecentOnes() = runBlocking {
        val repo = F1Repository()
        val profile = repo.driverProfile("lewis_hamilton", null)
        println("HAMILTON recentForm:")
        profile.stats.recentForm.forEach { println("  ${it.second} · ${it.first}") }
        assertTrue("recentForm empty", profile.stats.recentForm.isNotEmpty())
        val firstSeason = profile.stats.recentForm.first().first.take(4).toIntOrNull()
        assertTrue(
            "first recent result should be a recent season (>=2024), got ${profile.stats.recentForm.first().first}",
            firstSeason != null && firstSeason >= 2024
        )
        // must NOT contain 2007 anymore
        assertTrue(profile.stats.recentForm.none { it.first.startsWith("2007") })
    }
}
