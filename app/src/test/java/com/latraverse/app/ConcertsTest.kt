package com.latraverse.app

import com.latraverse.app.data.Concerts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ConcertsTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 13)

    @Test
    fun `upcoming contains only concerts from today onwards`() {
        val upcoming = Concerts.upcoming(today)
        assertTrue(upcoming.all { !it.date.isBefore(today) })
        assertTrue(upcoming.any { it.artist == "The Last Internationale" })
        assertEquals(upcoming.size, 6)
    }

    @Test
    fun `past contains only concerts before today`() {
        val past = Concerts.past(today)
        assertTrue(past.all { it.date.isBefore(today) })
        assertTrue(past.any { it.artist == "Yaniss Odua" })
        assertEquals(past.size, 10)
    }

    @Test
    fun `past is sorted most recent first and upcoming chronologically`() {
        val past = Concerts.past(today)
        val upcoming = Concerts.upcoming(today)
        assertEquals(past.first().artist, "Chicago Blues Festival")
        assertEquals(upcoming.first().artist, "Carolyn Wonderland")
        assertTrue(upcoming == upcoming.sortedBy { it.date })
    }

    @Test
    fun `every concert has artist, time and genre`() {
        Concerts.all.forEach {
            assertTrue(it.artist.isNotBlank())
            assertTrue(it.time.isNotBlank())
            assertTrue(it.genre.isNotBlank())
        }
    }
}
