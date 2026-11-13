package com.f1live.app

import com.f1live.app.data.model.Circuit
import com.f1live.app.data.model.Location
import com.f1live.app.data.model.Race
import com.f1live.app.data.repository.NewsItem
import com.f1live.app.data.repository.normalize
import com.f1live.app.data.repository.parseRss
import com.f1live.app.data.repository.raceKeywords
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

import org.junit.Test

class NewsTest {

    private fun race(name: String, locality: String) = Race(
        season = "2026", round = "19", raceName = name,
        Circuit = Circuit("c", "Circuit", Location(locality, "Country")),
        date = "2026-11-22", time = "15:00:00Z"
    )

    @Test
    fun keywordsCoverFrenchAndEnglish() {
        val kws = raceKeywords(race("Mexico City Grand Prix", "Mexico City"))
        assertTrue(kws.any { it.contains("mexique") })
        assertTrue(kws.any { it.contains("mexico") })
        val us = raceKeywords(race("United States Grand Prix", "Austin"))
        assertTrue(us.any { it.contains("etats unis") || it.contains("usa") })
        assertTrue(us.any { it.contains("austin") })
    }

    @Test
    fun normalizeStripsAccents() {
        assertEquals("etats-unis", normalize("États-Unis"))
        assertTrue(normalize("Brésil – São Paulo").contains("bresil"))
        assertTrue(normalize("Brésil – São Paulo").contains("sao paulo"))
    }

    @Test
    fun parseRssHandlesCdataAndEntities() {
        val xml = """
            <rss><channel><title>t</title>
            <item><title><![CDATA[Grand Prix du <b>Mexique</b> : les essais]]></title>
            <link>https://example.com/a</link><pubDate>Wed, 11 Nov 2026 10:00:00 GMT</pubDate></item>
            <item><title>F1 &#8211; Hamilton &#224; Monza</title>
            <link>https://example.com/b</link></item>
            </channel></rss>
        """.trimIndent()
        val items = parseRss(xml, "TestSource")
        assertEquals(2, items.size)
        assertEquals("Grand Prix du Mexique : les essais", items[0].title)
        assertEquals("https://example.com/a", items[0].link)
        assertEquals("F1 – Hamilton à Monza", items[1].title)
        assertEquals("TestSource", items[0].source)
    }

    @Test
    fun liveNewsFilteringWorks() {
        runBlocking {
            val race = race("United States Grand Prix", "Austin")
            val news = com.f1live.app.data.repository.fetchNewsForRace(race, 10)
            println("NEWS_COUNT=${news.size}")
            news.take(5).forEach { println("NEWS: ${it.title} [${it.source}]") }
            assertTrue("expected at least some news", news.size in 1..10)
            assertTrue(news.all { it.title.isNotBlank() })
        }
    }
}
