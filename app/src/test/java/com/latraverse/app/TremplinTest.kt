package com.latraverse.app

import com.latraverse.app.data.TremplinRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TremplinTest {

    private val pageFixture = """
        <div id="prochain-tremplin">
            <span class="inscription">24e TREMPLIN DE LA TRAVERSE</span>
            <span class="date">dimanche 13 décembre 2026</span><br />
        </div>
        <ul>
        <li>
            <div id="vignette">
                <a href="Laureat-2025-Oscard-Ferdinand"><div class="vignetteimage" style="background: url('IMG/logo/oscard.jpg?1') no-repeat center center;"></div></a><br />
                <a href="Laureat-2025-Oscard-Ferdinand"><span class="date">dimanche 14 décembre 2025</span></a><br />
                <a href="Laureat-2025-Oscard-Ferdinand"><span class="titre">Lauréat 2025&nbsp;: Oscard Ferdinand</span></a><br />
            </div>
        </li>
        <li>
            <div id="vignette">
                <a href="Laureats-2020-Hobbo-s-Lady-Stealer"><div class="vignetteimage" style="background: url('IMG/logo/hobbo.jpg?1') no-repeat center center;"></div></a><br />
                <a href="Laureats-2020-Hobbo-s-Lady-Stealer"><span class="date">mercredi 9 décembre 2020</span></a><br />
                <a href="Laureats-2020-Hobbo-s-Lady-Stealer"><span class="titre">Lauréats 2020&nbsp;: Hobbo&#8217;s / Lady&#8217;Stealer</span></a><br />
            </div>
        </li>
        <li>
            <div id="vignette">
                <a href="Agenda"><span class="titre">Autre chose</span></a><br />
            </div>
        </li>
        </ul>
    """

    @Test
    fun `parses tremplin page with upcoming edition and winners`() {
        val editions = TremplinRepository.parseTremplinPage(pageFixture)
        assertEquals(3, editions.size)
        val upcoming = editions.first { it.upcoming }
        assertEquals(24, upcoming.edition)
        assertEquals(LocalDate.of(2026, 12, 13), upcoming.date)
        val e2025 = editions.first { it.year == 2025 }
        assertEquals(23, e2025.edition)
        assertEquals(listOf("Oscard Ferdinand"), e2025.winners)
        val e2020 = editions.first { it.year == 2020 }
        assertEquals(listOf("Hobbo's", "Lady'Stealer"), e2020.winners)
        assertTrue(editions == editions.sortedByDescending { it.year })
    }

    @Test
    fun `bundled editions cover 2003 to 2026 with winners`() {
        val bundled = TremplinRepository.bundled
        assertEquals(24, bundled.size)
        assertEquals(1, bundled.last().edition)
        assertEquals(2003, bundled.last().year)
        assertTrue(bundled.none { it.year in 2004..2025 && it.winners.isEmpty() })
        assertTrue(bundled.first().upcoming)
    }

    @Test
    fun `photo galleries are available per edition up to five photos`() {
        val photos = TremplinRepository.photosByYear
        assertTrue(photos.isNotEmpty())
        photos.values.forEach { gallery ->
            assertTrue(gallery.size <= 5)
            gallery.forEach { url ->
                assertTrue(url.startsWith("https://www.latraverse.org/"))
            }
        }
        assertTrue((photos[2012]?.size ?: 0) == 5)
    }
}
