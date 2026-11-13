package com.example.infoconcert.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException

class InfoConcertRepository {

    companion object {
        private const val BASE = "https://www.infoconcert.com"
        private val DATE_PATTERN = Regex(
            "(Lundi|Mardi|Mercredi|Jeudi|Vendredi|Samedi|Dimanche)\\s+\\d{1,2}\\s+" +
                "(janvier|f[ée]vrier|mars|avril|mai|juin|juillet|ao[ûu]t|septembre|octobre|novembre|d[ée]cembre)\\s+(\\d{4})" +
                "(?:\\s*[àa]\\s*(\\d{1,2}h\\d{2}))?",
            RegexOption.IGNORE_CASE
        )
        private val CITY_PATTERN = Regex("(.+?)\\s*\\((\\d+[AB]?)\\)")
        private val VENUE_STOP_WORDS = listOf(
            "À partir de", "A partir de", "Gratuit", "Réserver", "Reserver",
            "Annulé", "Annule", "Plus réservable", "Plus reservable",
            "d'infos", "d'info", "Première partie", "Dans le cadre du festival"
        )
    }

    @Throws(IOException::class)
    fun loadConcerts(department: Department): List<Concert> {
        val doc = Jsoup.connect(department.url)
            .userAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")
            .referrer(BASE)
            .timeout(20000)
            .get()
        return parseConcerts(doc)
    }

    private fun parseConcerts(doc: Document): List<Concert> {
        val concerts = LinkedHashMap<String, Concert>()
        val concertLinks = doc.select("a[href]").filter {
            it.attr("abs:href").contains("$BASE/concert/") && it.attr("href").endsWith(".html")
        }
        for (link in concertLinks) {
            val infoUrl = link.attr("abs:href")
            if (concerts.containsKey(infoUrl)) continue
            val block = findConcertBlock(link) ?: continue
            val concert = parseConcertBlock(block, link, infoUrl) ?: continue
            concerts[infoUrl] = concert
        }
        return concerts.values.toList()
    }

    private fun findConcertBlock(link: Element): Element? {
        var current: Element? = link
        while (current != null) {
            val text = current.text()
            if (DATE_PATTERN.containsMatchIn(text) && text.length < 2000) return current
            current = current.parent()
        }
        return null
    }

    private fun parseConcertBlock(block: Element, link: Element, infoUrl: String): Concert? {
        val text = block.text()
        val dateMatch = DATE_PATTERN.find(text) ?: return null
        val name = link.text().trim().ifEmpty {
            block.select("h1,h2,h3,h4,strong,b").firstOrNull()?.text()?.trim().orEmpty()
        }.ifEmpty { return null }

        val city = findCity(block) ?: ""
        val venue = extractVenue(text, dateMatch)
        val price = extractPrice(text)
        val bookingUrl = findBookingUrl(block)

        return Concert(
            name = name,
            city = city,
            date = dateMatch.value.substringBefore(" à ").trim(),
            time = dateMatch.groups[4]?.value,
            venue = venue,
            price = price,
            bookingUrl = bookingUrl,
            infoUrl = infoUrl
        )
    }

    private fun findCity(block: Element): String? {
        var sibling: Element? = block.previousElementSibling()
        var parent: Element? = block.parent()
        var hops = 0
        while (hops < 6) {
            while (sibling != null) {
                val match = CITY_PATTERN.find(sibling.text())
                if (match != null) return match.groupValues[1].trim()
                sibling = sibling.previousElementSibling()
            }
            if (parent == null) break
            sibling = parent.previousElementSibling()
            parent = parent.parent()
            hops++
        }
        return null
    }

    private fun extractVenue(text: String, dateMatch: MatchResult): String {
        val after = text.substring(dateMatch.range.last + 1).trim()
        var venue = after
        for (stop in VENUE_STOP_WORDS) {
            val index = venue.indexOf(stop, ignoreCase = true)
            if (index > 0) venue = venue.substring(0, index)
        }
        return venue.trim().trimEnd(' ', ',')
    }

    private fun extractPrice(text: String): String? {
        val match = Regex("(?:[ÀA] partir de|Gratuit)\\s*([0-9]+[.,][0-9]{2}\\s*€?)?", RegexOption.IGNORE_CASE)
            .find(text) ?: return null
        return if (match.groupValues[1].isNotBlank()) "À partir de ${match.groupValues[1].trim()}" else "Gratuit"
    }

    private fun findBookingUrl(block: Element): String? {
        val candidates = block.select("a[href]")
        val booking = candidates.firstOrNull { a ->
            val label = a.text().trim().lowercase()
            label.contains("réserver") || label.contains("reserver") || label.contains("billetterie")
        } ?: candidates.firstOrNull { a ->
            val href = a.attr("abs:href").lowercase()
            href.contains("reservation") || href.contains("billetterie") || href.contains("billet")
        }
        return booking?.attr("abs:href")?.takeIf { it.startsWith("http") }
    }
}
