package com.latraverse.app

import com.latraverse.app.data.Concert
import com.latraverse.app.data.ConcertRepository
import com.latraverse.app.data.RemoteSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RemoteSourceTest {

    private val fixture = """
        <li>
            <div id="vignette">
                <a href="Carolyn-Wonderland"><div class="vignetteimage" style="background: url('IMG/logo/carolyn_wonderland.png?1781010190') no-repeat center center; background-size: cover;"></div></a><br />
                <a href="Carolyn-Wonderland"><span class="date">vendredi 13 novembre 2026</span></a><br />
                <a href="Carolyn-Wonderland"><span class="titre">Carolyn Wonderland</span></a><br />
                <a href="Carolyn-Wonderland"><span class="soustitre">+ Nina Attal</span></a><br />
                <a href="Carolyn-Wonderland"><span class="heure">20h30</span></a><br />
                <a href="Carolyn-Wonderland"><span class="genre">Blues</span></a><br />
                <a class="bouton-acheter" href="https://billetterie.latraverse.org/agenda/26-Carolyn-Wonderland-Nina-Attal?session=26" target="_blank">ACHETER</a>
            </div>
        </li>
        <li>
            <div id="vignette">
                <a href="Magma-852"><div class="vignetteimage" style="background: url('IMG/logo/plan_de_travail_2.png?1779976246') no-repeat center center; background-size: cover;"></div></a><br />
                <a href="Magma-852"><span class="date">mardi 17 novembre 2026</span></a><br />
                <a href="Magma-852"><span class="titre">Magma</span></a><br />
                <a href="Magma-852"><span class="soustitre"></span></a><br />
                <a href="Magma-852"><span class="heure">20h30</span></a><br />
                <a href="Magma-852"><span class="genre">Musique Zeuhl</span></a><br />
                <a class="bouton-acheter" href="https://billetterie.latraverse.org/agenda/31-MAGMA?session=31" target="_blank">ACHETER</a>
            </div>
        </li>
    """

    @Test
    fun `parses agenda events with all fields`() {
        val events = RemoteSource.parseAgenda(fixture)
        assertEquals(2, events.size)
        val first = events[0]
        assertEquals("Carolyn Wonderland", first.artist)
        assertEquals(LocalDate.of(2026, 11, 13), first.date)
        assertEquals("20h30", first.time)
        assertEquals("Blues", first.genre)
        assertEquals("Nina Attal", first.support)
        assertTrue(first.imageUrl!!.startsWith("https://www.latraverse.org/IMG/logo/"))
        assertTrue(first.ticketUrl!!.startsWith("https://billetterie.latraverse.org/"))
        assertNull(events[1].support)
    }

    @Test
    fun `parses french dates`() {
        assertEquals(LocalDate.of(2026, 12, 13), RemoteSource.parseDate("dimanche 13 décembre 2026"))
        assertEquals(LocalDate.of(2027, 2, 12), RemoteSource.parseDate("vendredi 12 février 2027"))
        assertNull(RemoteSource.parseDate("date inconnue"))
    }

    @Test
    fun `merge keeps past archives, dedupes and enriches remote events`() {
        val today = LocalDate.of(2026, 11, 13)
        val bundled = listOf(
            Concert("Old", LocalDate.of(2025, 12, 6), "20h30", "Blues", description = "archive"),
            Concert("Carolyn Wonderland", LocalDate.of(2026, 11, 13), "20h30", "Blues", description = "desc locale"),
            Concert("Local Only", LocalDate.of(2026, 12, 20), "20h30", "Rock")
        )
        val remote = listOf(
            Concert("Carolyn Wonderland", LocalDate.of(2026, 11, 13), "20h30", "Blues", ticketUrl = "https://billetterie.latraverse.org/x")
        )
        val merged = ConcertRepository.merge(bundled, remote, today)
        assertEquals(3, merged.size)
        val carolyn = merged.first { it.artist == "Carolyn Wonderland" }
        assertEquals("desc locale", carolyn.description)
        assertEquals("https://billetterie.latraverse.org/x", carolyn.ticketUrl)
        assertTrue(merged.any { it.artist == "Old" })
        assertTrue(merged.any { it.artist == "Local Only" })
    }

    @Test
    fun `upcoming and past lists drop concerts sharing the same date and time`() {
        val today = LocalDate.of(2026, 11, 13)
        val all = listOf(
            Concert("Artist A", LocalDate.of(2026, 11, 13), "20h30", "Blues"),
            Concert("Artist B (doublon)", LocalDate.of(2026, 11, 13), "20h30", "Rock"),
            Concert("Artist C autre heure", LocalDate.of(2026, 11, 13), "18h00", "Jazz"),
            Concert("Old 1", LocalDate.of(2025, 12, 6), "20h30", "Blues"),
            Concert("Old 2 (doublon)", LocalDate.of(2025, 12, 6), "20h30", "Soul")
        )
        val upcoming = ConcertRepository.upcoming(all, today)
        val past = ConcertRepository.past(all, today)
        assertEquals(2, upcoming.size)
        assertEquals("Artist A", upcoming[0].artist)
        assertEquals("Artist C autre heure", upcoming[1].artist)
        assertEquals(1, past.size)
    }
}
