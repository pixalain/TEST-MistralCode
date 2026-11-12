package com.f1live.app.data.repository

import com.f1live.app.data.model.Race
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale

@kotlinx.serialization.Serializable
data class NewsItem(
    val title: String,
    val source: String,
    val link: String,
    val date: String? = null
)

object NewsFeeds {
    val FR = listOf(
        "https://fr.motorsport.com/rss/f1/news/" to "Motorsport.com FR",
        "https://f1only.fr/feed/" to "F1Only",
        "https://www.autohebdo.fr/feed/" to "AutoHebdo"
    )
}

private val http = OkHttpClient.Builder()
    .addInterceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("User-Agent", "F1LiveApp/1.0.0").build())
    }
    .build()

suspend fun fetchFeed(url: String): String = withRetryHttp {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { it.body?.string() ?: "" }
    }
}

private suspend fun <T> withRetryHttp(block: suspend () -> T): T {
    var last: Exception? = null
    repeat(3) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            last = e
            if (attempt < 2) kotlinx.coroutines.delay(1000L * (attempt + 1))
        }
    }
    throw last!!
}

private val ENTITY = Regex("&#(\\d+);|&(\\w+);")

fun decodeEntities(s: String): String = ENTITY.replace(s) { m ->
    if (m.groupValues[1].isNotEmpty()) m.groupValues[1].toInt().toChar().toString()
    else when (m.groupValues[2]) {
        "amp" -> "&"; "quot" -> "\""; "lt" -> "<"; "gt" -> ">"; "apos" -> "'"; "nbsp" -> " "
        else -> m.value
    }
}

private fun clean(s: String): String {
    var t = Regex("<!\\[CDATA\\[(.*?)\\]\\]>", RegexOption.DOT_MATCHES_ALL).replace(s) { it.groupValues[1] }
    t = Regex("<[^>]+>").replace(t, "")
    return decodeEntities(t).trim()
}

fun parseRss(xml: String, source: String): List<NewsItem> {
    val items = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL).findAll(xml).toList()
    return items.mapNotNull { m ->
        val block = m.groupValues[1]
        fun tag(name: String): String? =
            Regex("<$name>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL)
                .find(block)?.groupValues?.get(1)?.let { clean(it) }?.takeIf { it.isNotBlank() }
        val title = tag("title") ?: return@mapNotNull null
        NewsItem(
            title = title,
            source = source,
            link = tag("link") ?: "",
            date = tag("pubDate") ?: tag("date")
        )
    }
}

fun normalize(s: String): String =
    Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

private val FR_TRANSLATIONS = mapOf(
    "australian" to "australie", "chinese" to "chine", "japanese" to "japon",
    "canadian" to "canada", "barcelona" to "barcelone espagne", "spanish" to "espagne",
    "austrian" to "autriche", "british" to "grande bretagne angleterre", "belgian" to "belgique",
    "hungarian" to "hongrie", "dutch" to "pays bas hollande", "italian" to "italie monza",
    "azerbaijan" to "azerbaidjan bakou", "bahrain" to "bahrein", "singapore" to "singapour",
    "united states" to "etats unis usa austin", "mexico city" to "mexico mexique", "mexico" to "mexique",
    "brazil" to "bresil sao paulo", "saudi arabian" to "arabie saoudite", "abu dhabi" to "abu dhabi",
    "qatar" to "qatar losail", "las vegas" to "las vegas"
)

fun raceKeywords(race: Race): List<String> {
    val base = race.raceName.replace(Regex("(?i)grand prix"), "").trim().lowercase(Locale.ROOT)
    val kws = mutableSetOf<String>()
    kws.add(normalize(base))
    race.locality?.let { kws.add(normalize(it.lowercase(Locale.ROOT))) }
    FR_TRANSLATIONS[base.lowercase(Locale.ROOT)]?.split(" ")?.forEach { kws.add(normalize(it)) }
    // also match each word of the english name
    base.split(Regex("\\s+")).filter { it.length > 3 }.forEach { kws.add(normalize(it)) }
    return kws.filter { it.isNotBlank() }
}

suspend fun fetchNewsForRace(race: Race, limit: Int = 10): List<NewsItem> = coroutineScope {
    val all = NewsFeeds.FR.map { (url, source) ->
        async {
            runCatching { parseRss(fetchFeed(url), source) }.getOrDefault(emptyList())
        }
    }.awaitAll().flatten()
        .distinctBy { it.title + it.link }
        .sortedByDescending { it.date ?: "" }
    val kws = raceKeywords(race)
    val related = all.filter { item ->
        val text = normalize(item.title)
        kws.any { it in text }
    }
    (related + all.filter { it !in related }).distinctBy { it.title }.take(limit)
}
