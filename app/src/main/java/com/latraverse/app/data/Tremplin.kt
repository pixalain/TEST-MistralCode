package com.latraverse.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.time.LocalDate

data class TremplinEdition(
    val year: Int,
    val edition: Int,
    val date: LocalDate?,
    val winners: List<String>,
    val participants: List<String>,
    val imageUrl: String? = null,
    val eventUrl: String? = null,
    val upcoming: Boolean = false
)

object TremplinRepository {

    private const val TREMPLIN_URL = "https://www.latraverse.org/Tremplin"
    private const val SITE = "https://www.latraverse.org/"

    private val client = OkHttpClient()

    private val DATE_SPAN = Regex("""<span class="date">([^<]+)</span>""")
    private val TITLE_SPAN = Regex("""<span class="titre">([^<]+)</span>""")
    private val IMG_URL = Regex("""vignetteimage[^>]*url\('([^']+)'""")
    private val UPCOMING = Regex(
        """<span class="inscription">(\d+)\s*e\s*TREMPLIN[^<]*</span>\s*<span class="date">([^<]+)</span>"""
    )

    private fun editionForYear(year: Int) = year - 2002

    val bundled: List<TremplinEdition> = listOf(
        TremplinEdition(2026, 24, LocalDate.of(2026, 12, 13), emptyList(), emptyList(), upcoming = true),
        TremplinEdition(2025, 23, LocalDate.of(2025, 12, 14), listOf("Oscard Ferdinand"), emptyList(), eventUrl = SITE + "Laureat-2025-Oscard-Ferdinand"),
        TremplinEdition(2024, 22, LocalDate.of(2024, 12, 8), listOf("Masstø"), emptyList(), eventUrl = SITE + "Laureat-2024-Massto"),
        TremplinEdition(2023, 21, LocalDate.of(2023, 12, 3), listOf("Gogo Juice"), emptyList(), eventUrl = SITE + "Laureat-2023-Gogo-Juice"),
        TremplinEdition(2022, 20, LocalDate.of(2022, 12, 4), listOf("DNVR"), emptyList(), eventUrl = SITE + "Laureat-2022-DNVR"),
        TremplinEdition(2021, 19, LocalDate.of(2021, 12, 3), listOf("JARL"), emptyList(), eventUrl = SITE + "Laureat-2021-JARL"),
        TremplinEdition(2020, 18, LocalDate.of(2020, 12, 9), listOf("Hobbo's", "Lady'Stealer"), listOf("Lady'Stealer", "Hobbo's", "Masstø", "Living West"), eventUrl = SITE + "Laureats-2020-Hobbo-s-Lady-Stealer"),
        TremplinEdition(2019, 17, LocalDate.of(2019, 12, 1), listOf("Fuel Humor"), emptyList(), eventUrl = SITE + "Laureat-2019-Fuel-Humor"),
        TremplinEdition(2018, 16, LocalDate.of(2018, 12, 2), listOf("KMGB"), emptyList(), eventUrl = SITE + "Laureat-2018-KMGB"),
        TremplinEdition(2017, 15, LocalDate.of(2017, 12, 3), listOf("Dom Ferrer"), emptyList(), eventUrl = SITE + "Laureat-2017-Dom-Ferrer"),
        TremplinEdition(2016, 14, LocalDate.of(2016, 11, 27), listOf("The Barnguys"), emptyList(), eventUrl = SITE + "Laureat-2016-The-Barnguys"),
        TremplinEdition(2015, 13, LocalDate.of(2015, 11, 29), listOf("My Silly Dogfish"), emptyList(), eventUrl = SITE + "Laureat-2015-My-Silly-Dogfish"),
        TremplinEdition(2014, 12, LocalDate.of(2014, 11, 30), listOf("Jacobson & The Naughty Brothers"), emptyList(), eventUrl = SITE + "Laureat-2014-Jacobson-The-Naughty"),
        TremplinEdition(2013, 11, LocalDate.of(2013, 12, 1), listOf("The Swinging Dice"), emptyList(), eventUrl = SITE + "Laureat-2013-The-Swinging-Dice"),
        TremplinEdition(2012, 10, LocalDate.of(2012, 12, 2), listOf("Alex Massmedia"), emptyList(), eventUrl = SITE + "Laureat-2012-Alex-Massmedia"),
        TremplinEdition(2011, 9, LocalDate.of(2011, 11, 27), listOf("Grand Square Dance", "Curtis Johnson Band"), emptyList(), eventUrl = SITE + "Laureats-2011-Grand-Square-Dance"),
        TremplinEdition(2010, 8, LocalDate.of(2010, 11, 28), listOf("Tysran"), emptyList(), eventUrl = SITE + "Laureat-2010-Tysran"),
        TremplinEdition(2009, 7, LocalDate.of(2009, 11, 29), listOf("Alex Mirey", "Peeble"), emptyList(), eventUrl = SITE + "Laureats-2009-Alex-Mirey-Peeble"),
        TremplinEdition(2008, 6, LocalDate.of(2008, 11, 30), listOf("New Line Up"), emptyList(), eventUrl = SITE + "Laureat-2008-New-Line-Up"),
        TremplinEdition(2007, 5, LocalDate.of(2007, 12, 2), listOf("Midwest"), emptyList(), eventUrl = SITE + "Laureat-2007-Midwest"),
        TremplinEdition(2006, 4, LocalDate.of(2006, 12, 3), listOf("Little Rom & The Noisy Lovers"), emptyList(), eventUrl = SITE + "Laureat-2006-Little-Rom-The-Noisy"),
        TremplinEdition(2005, 3, LocalDate.of(2005, 12, 4), listOf("Buddy Blues"), emptyList(), eventUrl = SITE + "Laureat-2005-Buddy-Blues"),
        TremplinEdition(2004, 2, LocalDate.of(2004, 11, 13), listOf("Jeff Treguer Trio"), emptyList(), eventUrl = SITE + "Laureat-2004-Jeff-Treguer-Trio"),
        TremplinEdition(2003, 1, LocalDate.of(2003, 10, 12), listOf("Spoonful"), emptyList(), eventUrl = SITE + "Laureat-2003-Spoonful")
    )

    private val participantsByYear = mapOf(
        2020 to listOf("Lady'Stealer", "Hobbo's", "Masstø", "Living West")
    )

    suspend fun fetchEditions(): List<TremplinEdition> = withContext(Dispatchers.IO) {
        runCatching {
            val request = okhttp3.Request.Builder().url(TREMPLIN_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val html = response.body?.string() ?: return@withContext emptyList()
                val parsed = parseTremplinPage(html)
                if (parsed.isEmpty()) emptyList() else parsed
            }
        }.getOrElse { emptyList() }
    }

    fun parseTremplinPage(html: String): List<TremplinEdition> {
        val editions = mutableListOf<TremplinEdition>()
        UPCOMING.find(html)?.let { m ->
            val editionNumber = m.groupValues[1].toIntOrNull() ?: return@let
            val date = RemoteSource.parseDate(m.groupValues[2])
            editions.add(
                TremplinEdition(
                    year = date?.year ?: (editionNumber + 2002),
                    edition = editionNumber,
                    date = date,
                    winners = emptyList(),
                    participants = emptyList(),
                    upcoming = true
                )
            )
        }
        val block = Regex("""<div id="vignette">([\s\S]*?)</li>""")
        block.findAll(html).forEach { b ->
            val content = b.groupValues[1]
            val title = decode(TITLE_SPAN.find(content)?.groupValues?.get(1) ?: return@forEach).trim()
            val m = Regex("""Lauréats?\s+(\d{4})\s*:\s*(.+)""").find(title) ?: return@forEach
            val year = m.groupValues[1].toIntOrNull() ?: return@forEach
            val winners = m.groupValues[2].split("/").map { it.trim() }.filter { it.isNotEmpty() }
            val date = DATE_SPAN.find(content)?.groupValues?.get(1)?.let { RemoteSource.parseDate(it) }
            val image = IMG_URL.find(content)?.groupValues?.get(1)?.let { SITE + it }
            editions.add(
                TremplinEdition(
                    year = year,
                    edition = editionForYear(year),
                    date = date,
                    winners = winners,
                    participants = participantsByYear[year] ?: emptyList(),
                    imageUrl = image
                )
            )
        }
        return editions.sortedByDescending { it.year }
    }

    private fun decode(s: String): String = s
        .replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .replace("&#8217;", "'")
        .replace("&#171;", "«")
        .replace("&#187;", "»")
}
