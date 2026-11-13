package com.latraverse.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object RemoteSource {

    private const val AGENDA_URL = "https://www.latraverse.org/Agenda"
    private const val SITE = "https://www.latraverse.org/"

    private val client = OkHttpClient()

    private val BLOCK = Regex("<div id=\"vignette\">([\\s\\S]*?)</li>")
    private val IMG = Regex("vignetteimage[^>]*url\\('([^']+)'\\)")
    private val DATE = Regex("<span class=\"date\">([^<]+)</span>")
    private val TITLE = Regex("<span class=\"titre\">([^<]+)</span>")
    private val SUB = Regex("<span class=\"soustitre\">([^<]*)</span>")
    private val TIME = Regex("<span class=\"heure\">([^<]+)</span>")
    private val GENRE = Regex("<span class=\"genre\">([^<]*)</span>")
    private val BUY = Regex("bouton-acheter\" href=\"([^\"]+)\"")
    private val EVENT_HREF = Regex("""<a href="([^"]+)"><div class="vignetteimage""")
    private val EVENT_TEXT = Regex("""<span class="texte">([\s\S]*?)</span>""")
    private val PHOTO_CREDIT = Regex("""<span class="credit">([\s\S]*?)</span>""")
    private val YOUTUBE = Regex("""youtube\.com/embed/([A-Za-z0-9_-]{11})""")

    private val MONTHS = mapOf(
        "janvier" to 1, "février" to 2, "fevrier" to 2, "mars" to 3, "avril" to 4,
        "mai" to 5, "juin" to 6, "juillet" to 7, "août" to 8, "aout" to 8,
        "septembre" to 9, "octobre" to 10, "novembre" to 11, "décembre" to 12, "decembre" to 12
    )

    private const val FEED_URL = "https://www.latraverse.org/spip.php?page=backend"

    private val FEED_ITEM = Regex("""<item[\s\S]*?</item>""")
    private val FEED_TITLE = Regex("""<title>([^<]+)</title>""")
    private val FEED_LINK = Regex("""<link>([^<]+)</link>""")
    private val FEED_DATE = Regex("""<dc:date>([^<]+)</dc:date>""")
    private val FEED_IMAGE = Regex("""src='([^']+)'""")

    suspend fun fetchUpcoming(): List<Concert> = withContext(Dispatchers.IO) {
        val fromHtml = fetchAgendaHtml()
        if (fromHtml.isNotEmpty()) return@withContext fromHtml
        fetchFeedUpcoming()
    }

    private suspend fun fetchAgendaHtml(): List<Concert> = withContext(Dispatchers.IO) {
        runCatching {
            val request = okhttp3.Request.Builder().url(AGENDA_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val html = response.body?.string() ?: return@withContext emptyList()
                parseAgenda(html)
            }
        }.getOrElse { emptyList() }
    }

    suspend fun fetchFeedUpcoming(): List<Concert> = withContext(Dispatchers.IO) {
        runCatching {
            val request = okhttp3.Request.Builder().url(FEED_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val xml = response.body?.string() ?: return@withContext emptyList()
                parseFeed(xml)
            }
        }.getOrElse { emptyList() }
    }

    fun parseFeed(xml: String): List<Concert> =
        FEED_ITEM.findAll(xml).mapNotNull { item ->
            val content = item.value
            val artist = decode(FEED_TITLE.find(content)?.groupValues?.get(1) ?: return@mapNotNull null).trim()
            if (artist.isEmpty() || artist.equals("La Traverse", ignoreCase = true)) return@mapNotNull null
            val eventUrl = FEED_LINK.find(content)?.groupValues?.get(1)?.trim()?.ifEmpty { null }
            val isoDate = FEED_DATE.find(content)?.groupValues?.get(1) ?: return@mapNotNull null
            val instant = runCatching { java.time.Instant.parse(isoDate) }.getOrNull() ?: return@mapNotNull null
            val zoned = instant.atZone(java.time.ZoneId.of("Europe/Paris"))
            val image = FEED_IMAGE.find(content)?.groupValues?.get(1)
            Concert(
                artist = artist,
                date = zoned.toLocalDate(),
                time = "%02dh%02d".format(zoned.hour, zoned.minute),
                genre = "Concert",
                support = null,
                description = null,
                imageUrl = image,
                ticketUrl = null,
                eventUrl = eventUrl
            )
        }.filter { !it.isPast }.sortedBy { it.date }.toList()

    fun parseAgenda(html: String): List<Concert> =
        BLOCK.findAll(html).mapNotNull { block ->
            val content = block.groupValues[1]
            val date = parseDate(DATE.find(content)?.groupValues?.get(1) ?: return@mapNotNull null)
                ?: return@mapNotNull null
            val artist = decode(TITLE.find(content)?.groupValues?.get(1) ?: return@mapNotNull null).trim()
            if (artist.isEmpty()) return@mapNotNull null
            val support = decode(SUB.find(content)?.groupValues?.get(1) ?: "").trim()
                .removePrefix("+").trim().ifEmpty { null }
            val time = decode(TIME.find(content)?.groupValues?.get(1) ?: "").trim().ifEmpty { null }
            val genre = decode(GENRE.find(content)?.groupValues?.get(1) ?: "").trim().ifEmpty { null }
            val image = IMG.find(content)?.groupValues?.get(1)?.let { SITE + it }
            val ticket = BUY.find(content)?.groupValues?.get(1)
            val eventUrl = EVENT_HREF.find(content)?.groupValues?.get(1)?.let { SITE + it }
            Concert(
                artist = artist,
                date = date,
                time = time ?: "20h30",
                genre = genre ?: "Concert",
                support = support,
                description = null,
                imageUrl = image,
                ticketUrl = ticket,
                eventUrl = eventUrl
            )
        }.sortedBy { it.date }.toList()

    data class EventDetails(
        val description: String?,
        val photoCredit: String?,
        val youtubeId: String?
    )

    suspend fun fetchEventDetails(eventUrl: String): EventDetails = withContext(Dispatchers.IO) {
        runCatching {
            val request = okhttp3.Request.Builder().url(eventUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext EventDetails(null, null, null)
                val html = response.body?.string() ?: return@withContext EventDetails(null, null, null)
                parseEventDetails(html)
            }
        }.getOrElse { EventDetails(null, null, null) }
    }

    fun parseEventDetails(html: String): EventDetails {
        val description = EVENT_TEXT.find(html)?.groupValues?.get(1)?.let { htmlToText(it) }
        val credit = PHOTO_CREDIT.find(html)?.groupValues?.get(1)?.let { htmlToText(it) }?.trim()
        val youtube = YOUTUBE.find(html)?.groupValues?.get(1)
        return EventDetails(description, credit?.ifEmpty { null }, youtube)
    }

    internal fun htmlToText(html: String): String = html
        .replace(Regex("<br[^>]*>"), "\n")
        .replace("</p>", "\n\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .replace("&laquo;", "«")
        .replace("&raquo;", "»")
        .replace("&eacute;", "é")
        .replace("&egrave;", "è")
        .replace("&ecirc;", "ê")
        .replace("&agrave;", "à")
        .replace("&ocirc;", "ô")
        .replace("&#8217;", "'")
        .replace("&#171;", "«")
        .replace("&#187;", "»")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

    internal fun parseDate(raw: String): LocalDate? {
        val m = Regex("(\\d{1,2})\\s+([a-zéûè]+)\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(raw.trim())
            ?: return null
        val day = m.groupValues[1].toInt()
        val month = MONTHS[m.groupValues[2].lowercase(Locale.FRENCH)] ?: return null
        val year = m.groupValues[3].toInt()
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun decode(s: String): String = s
        .replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .replace("&laquo;", "«")
        .replace("&raquo;", "»")
        .replace("&eacute;", "é")
        .replace("&egrave;", "è")
        .replace("&ecirc;", "ê")
        .replace("&agrave;", "à")
        .replace("&ocirc;", "ô")
        .replace("&#8217;", "'")
        .replace("&#171;", "«")
        .replace("&#187;", "»")
}

object ConcertRepository {

    private fun key(c: Concert) = c.date.toString() + "|" + c.artist.lowercase().trim()

    fun withArchive(bundled: List<Concert>, archive: List<Concert>): List<Concert> {
        val keys = bundled.map { key(it) }.toSet()
        return bundled + archive.filter { key(it) !in keys }
    }

    fun merge(bundled: List<Concert>, remote: List<Concert>, today: LocalDate = LocalDate.now()): List<Concert> {
        val past = bundled.filter { it.date.isBefore(today) }
        val remoteKeys = remote.map { key(it) }.toSet()
        val bundledByKey = bundled.associateBy { key(it) }
        val remoteEnriched = remote.map { r ->
            val b = bundledByKey[key(r)]
            r.copy(description = r.description ?: b?.description)
        }
        val bundledUpcoming = bundled.filter { !it.date.isBefore(today) && key(it) !in remoteKeys }
        return past + bundledUpcoming + remoteEnriched
    }

    fun upcoming(all: List<Concert>, today: LocalDate = LocalDate.now()): List<Concert> =
        all.filter { !it.date.isBefore(today) }
            .distinctBy { it.date.toString() + "|" + it.time }
            .sortedBy { it.date }

    fun past(all: List<Concert>, today: LocalDate = LocalDate.now()): List<Concert> =
        all.filter { it.date.isBefore(today) }
            .distinctBy { it.date.toString() + "|" + it.time }
            .sortedByDescending { it.date }

    fun nowLabel(): String =
        java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
}
