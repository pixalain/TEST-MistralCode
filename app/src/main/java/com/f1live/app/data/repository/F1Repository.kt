package com.f1live.app.data.repository

import com.f1live.app.data.api.AlphaService
import com.f1live.app.data.api.ApiClient
import com.f1live.app.data.api.JolpicaService
import com.f1live.app.data.model.AlphaEnvelope
import com.f1live.app.data.model.AlphaResultRow
import com.f1live.app.data.model.ConstructorStanding
import com.f1live.app.data.model.Driver
import com.f1live.app.data.model.DriverStanding
import com.f1live.app.data.model.QualifyingResult
import com.f1live.app.data.model.Race
import com.f1live.app.data.model.Result
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

@kotlinx.serialization.Serializable
data class SessionResults(
    val code: String,
    val title: String,
    val timestamp: String? = null,
    val rows: List<AlphaResultRow>
)

@kotlinx.serialization.Serializable
data class RaceDetail(
    val race: Race,
    val results: List<Result>,
    val sprint: List<Result>,
    val qualifying: List<QualifyingResult> = emptyList(),
    val alphaSessions: List<SessionResults> = emptyList()
)

@kotlinx.serialization.Serializable
data class DriverStats(
    val races: Int = 0,
    val wins: Int = 0,
    val podiums: Int = 0,
    val poles: Int = 0,
    val fastestLaps: Int = 0,
    val points: Double = 0.0,
    val championships: Int = 0,
    val bestChampionship: String? = null,
    val seasons: Int = 0,
    val currentTeam: String? = null,
    val recentForm: List<Pair<String, String>> = emptyList(),
    val sprintWins: Int = 0,
    val sprintPoints: Double = 0.0
)

@kotlinx.serialization.Serializable
data class DriverProfile(
    val driver: Driver,
    val stats: DriverStats,
    val photoUrl: String? = null,
    val deathDate: String? = null
)

@kotlinx.serialization.Serializable
data class LiveLapRow(
    val position: Int,
    val driverCode: String,
    val driverName: String,
    val teamName: String,
    val lapTime: String?,
    val isFastest: Boolean
)

@kotlinx.serialization.Serializable
data class LiveLaps(
    val round: String,
    val raceName: String,
    val lastLap: Int,
    val rows: List<LiveLapRow>
)



data class Cached<T>(val data: T, val fromCache: Boolean, val cachedAt: Long = 0L)

class F1Repository(
    private val service: JolpicaService = ApiClient.service,
    private val alpha: AlphaService = ApiClient.alpha,
    private val cache: F1Cache? = null
) {

    suspend fun driverStandings(season: String = "current"): Cached<Pair<String, List<DriverStanding>>> {
        val key = F1Cache.KEY_STANDINGS + "_" + season
        val raw = try {
            withRetry429 {
                if (season == "current") service.driverStandings() else service.seasonDriverStandings(season)
            }
        } catch (e: Exception) {
            val cached = cache?.load(key)
                ?: throw e
            val data = F1Json.driverStandings(
                kotlinx.serialization.json.Json.parseToJsonElement(cached.first).jsonObject
            )
            val table = data.standingsTable
            return Cached(
                table.season to (table.StandingsLists.firstOrNull()?.DriverStandings.orEmpty()),
                fromCache = true,
                cachedAt = cached.second
            )
        }
        cache?.save(key, raw.toString())
        val data = F1Json.driverStandings(raw)
        val table = data.standingsTable
        return Cached(table.season to (table.StandingsLists.firstOrNull()?.DriverStandings.orEmpty()), fromCache = false)
    }

    suspend fun constructorStandings(season: String = "current"): Cached<Pair<String, List<ConstructorStanding>>> {
        val key = F1Cache.KEY_CONSTRUCTORS + "_" + season
        val raw = try {
            withRetry429 {
                if (season == "current") service.constructorStandings() else service.seasonConstructorStandings(season)
            }
        } catch (e: Exception) {
            val cached = cache?.load(key) ?: throw e
            val data = F1Json.constructorStandings(
                kotlinx.serialization.json.Json.parseToJsonElement(cached.first).jsonObject
            )
            val table = data.standingsTable
            return Cached(
                table.season to (table.StandingsLists.firstOrNull()?.ConstructorStandings.orEmpty()),
                fromCache = true,
                cachedAt = cached.second
            )
        }
        cache?.save(key, raw.toString())
        val data = F1Json.constructorStandings(raw)
        val table = data.standingsTable
        return Cached(table.season to (table.StandingsLists.firstOrNull()?.ConstructorStandings.orEmpty()), fromCache = false)
    }

    suspend fun seasonSchedule(season: String = "current"): Cached<Pair<String, List<Race>>> {
        val key = F1Cache.KEY_SCHEDULE + "_" + season
        val raw = try {
            withRetry429 {
                if (season == "current") service.seasonSchedule() else service.seasonScheduleFor(season)
            }
        } catch (e: Exception) {
            val cached = cache?.load(key) ?: throw e
            val data = F1Json.races(
                kotlinx.serialization.json.Json.parseToJsonElement(cached.first).jsonObject
            )
            return Cached(data.raceTable.season to data.raceTable.Races, fromCache = true, cachedAt = cached.second)
        }
        cache?.save(key, raw.toString())
        val data = F1Json.races(raw)
        return Cached(data.raceTable.season to data.raceTable.Races, fromCache = false)
    }


    suspend fun driverPhotoUrl(driver: com.f1live.app.data.model.Driver): String? {
        val name = "${driver.givenName} ${driver.familyName}"
        val cacheKey = "photo_" + driver.driverId
        cache?.load(cacheKey)?.let { (raw, _) -> return raw }
        for (lang in listOf("en", "fr")) {
            try {
                val api = "https://$lang.wikipedia.org/w/api.php?action=query&prop=pageimages&piprop=original&format=json&redirects=1&titles=" +
                    java.net.URLEncoder.encode(name, "UTF-8")
                val raw = withRetry429 { ApiClient.wikipedia.pageImages(api) }
                parseWikipediaPhoto(raw)?.let {
                    cache?.save(cacheKey, it)
                    return it
                }
            } catch (e: Exception) { /* try next language */ }
        }
        return null
    }


    suspend fun driverDeathDate(driver: com.f1live.app.data.model.Driver): String? {
        val name = "${driver.givenName} ${driver.familyName}"
        val cacheKey = "death_" + driver.driverId
        cache?.load(cacheKey)?.let { (raw, _) -> return raw.ifBlank { null } }
        for (lang in listOf("en", "fr")) {
            try {
                val search = "https://www.wikidata.org/w/api.php?action=wbsearchentities&search=" +
                    java.net.URLEncoder.encode(name, "UTF-8") +
                    "&language=$lang&format=json&limit=1&type=item"
                val qid = parseWikidataEntityId(withRetry429 { ApiClient.wikidata.api(search) }) ?: continue
                val claims = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=$qid&props=claims&format=json"
                val death = parseWikidataDeathDate(withRetry429 { ApiClient.wikidata.api(claims) })
                cache?.save(cacheKey, death ?: "")
                return death
            } catch (e: Exception) { /* next language */ }
        }
        cache?.save(cacheKey, "")
        return null
    }

    private suspend fun <T> withRetry429(maxRetries: Int = 5, block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 429 && attempt < maxRetries) {
                    attempt++
                    val waitMs = (e.response()?.headers()?.get("Retry-After")?.toLongOrNull() ?: (2L * attempt)) * 1000 + 250
                    delay(waitMs)
                    continue
                }
                throw e
            } catch (e: java.io.IOException) {
                // transient network errors (DNS, SSL, timeouts): backoff and retry
                if (attempt < maxRetries) {
                    attempt++
                    delay(1000L * attempt * attempt)
                    continue
                }
                throw e
            }
        }
    }

    private suspend fun alphaRoundId(season: String, round: String): String? = try {
        val year = season.toIntOrNull() ?: return null
        val rounds = withRetry429 { alpha.rounds(year) }.data ?: return null
        rounds.firstOrNull { it.number == round.toIntOrNull() }?.id
    } catch (e: Exception) {
        null
    }

    private suspend fun fetchAlphaSession(
        roundId: String,
        code: String,
        title: String
    ): SessionResults? = try {
        val envelope: AlphaEnvelope<com.f1live.app.data.model.AlphaSessionResults> =
            withRetry429 { alpha.sessionResults(roundId, code) }
        val body = envelope.data ?: return null
        SessionResults(
            code = code,
            title = body.title ?: title,
            timestamp = body.local_timestamp ?: body.timestamp,
            rows = body.results
        )
    } catch (e: Exception) {
        null
    }

    suspend fun raceResults(round: String, season: String = "current"): Cached<RaceDetail> = coroutineScope {
        val key = F1Cache.raceKey(season, round)
        val raw = try {
            withRetry429 {
                if (season == "current") service.raceResults(round) else service.seasonRaceResults(season, round)
            }
        } catch (e: Exception) {
            val cached = cache?.load(key) ?: throw e
            val detail = F1Json.json.decodeFromString(kotlinx.serialization.serializer<RaceDetail>(), cached.first)
            return@coroutineScope Cached(detail, fromCache = true, cachedAt = cached.second)
        }
        val data = F1Json.races(raw)
        val race = data.raceTable.Races.firstOrNull()
            ?: throw IllegalStateException("Grand Prix introuvable (round $round)")
        val results = race.Results.orEmpty()

        val sprintDeferred = async {
            try {
                F1Json.races(if (season == "current") service.sprintResults(round) else service.seasonSprintResults(season, round)).raceTable.Races.firstOrNull()
                    ?.sprintResults.orEmpty()
            } catch (e: Exception) {
                emptyList<Result>()
            }
        }
        val qualifyingDeferred = async {
            try {
                F1Json.races(if (season == "current") service.qualifyingResults(round) else service.seasonQualifyingResults(season, round)).raceTable.Races.firstOrNull()
                    ?.QualifyingResults.orEmpty()
            } catch (e: Exception) {
                emptyList<QualifyingResult>()
            }
        }

        val alphaDeferred = async {
            val roundId = alphaRoundId(race.season, round) ?: return@async emptyList<SessionResults>()
            val hasSprint = race.Sprint != null
            val codes = buildList {
                add("FP1" to "Practice 1")
                add("FP2" to "Practice 2")
                add("FP3" to "Practice 3")
                add("Q" to "Qualifying")
                if (hasSprint) {
                    add("SQ" to "Sprint Qualifying")
                    add("SR" to "Sprint")
                }
                add("R" to "Race")
            }
            codes.mapIndexed { i, (code, title) ->
                async {
                    delay(300L * i)
                    fetchAlphaSession(roundId, code, title)
                        ?: SessionResults(code, title, null, emptyList())
                }
            }.awaitAll()
        }

        val detail = RaceDetail(
            race = race,
            results = results,
            sprint = sprintDeferred.await(),
            qualifying = qualifyingDeferred.await(),
            alphaSessions = alphaDeferred.await()
        )
        runCatching {
            cache?.save(key, F1Json.json.encodeToString(kotlinx.serialization.serializer<RaceDetail>(), detail))
        }
        Cached(detail, fromCache = false)
    }

    suspend fun driverProfile(driverId: String, currentTeam: String? = null): DriverProfile = coroutineScope {
        val infoDeferred = async { F1Json.drivers(service.driverInfo(driverId)).driverTable.Drivers.first() }
        val seasonsDeferred = async {
            try {
                F1Json.seasons(service.driverSeasons(driverId)).seasonTable.Seasons.map { it.season }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val resultsDeferred = async {
            try {
                aggregateResults(driverId)
            } catch (e: Exception) {
                DriverStats(currentTeam = currentTeam, recentForm = emptyList())
            }
        }

        val qualifyingDeferred = async {
            try {
                countPoles(driverId)
            } catch (e: Exception) {
                0
            }
        }

        val bestChampDeferred = async {
            try {
                championshipRecord(driverId, seasonsDeferred.await())
            } catch (e: Exception) {
                0 to null
            }
        }

        val driver = infoDeferred.await()
        val base = resultsDeferred.await()
        val poles = qualifyingDeferred.await()
        val (titles, bestChamp) = bestChampDeferred.await()
        val seasonsCount = seasonsDeferred.await().size

        DriverProfile(
            driver = driver,
            stats = base.copy(
                poles = poles,
                championships = titles,
                bestChampionship = bestChamp,
                seasons = seasonsCount
            ),
            photoUrl = runCatching { driverPhotoUrl(driver) }.getOrNull(),
            deathDate = runCatching { driverDeathDate(driver) }.getOrNull()
        )
    }

    private suspend fun aggregateResults(driverId: String): DriverStats {
        var offset = 0
        var total = Int.MAX_VALUE
        var wins = 0
        var podiums = 0
        var fastestLaps = 0
        var points = 0.0
        val recent = mutableListOf<Pair<String, String>>()
        var firstPage = true
        while (offset < total && offset < 500) {
            val raw = withRetry429 { service.driverResults(driverId, limit = 100, offset = offset) }
            val page = F1Json.races(raw)
            total = F1Json.total(raw)?.toIntOrNull() ?: page.raceTable.Races.size
            val races = page.raceTable.Races
            if (races.isEmpty()) break
            for (r in races) {
                val res = r.Results.orEmpty().firstOrNull { it.Driver.driverId == driverId } ?: continue
                points += res.points?.toDoubleOrNull() ?: 0.0
                val pos = res.position?.toIntOrNull()
                if (pos == 1) wins++
                if (pos != null && pos <= 3) podiums++
                if (res.FastestLap?.rank == "1") fastestLaps++
                if (firstPage && recent.size < 10) {
                    recent.add("${r.season} ${r.raceName}" to res.displayPosition)
                }
            }
            offset += races.size
            firstPage = false
        }
        var sprintPoints = 0.0
        var sprintWins = 0
        try {
            var sOffset = 0
            var sTotal = Int.MAX_VALUE
            while (sOffset < sTotal && sOffset < 500) {
                val raw = withRetry429 { service.driverSprint(driverId, limit = 100, offset = sOffset) }
                val page = F1Json.races(raw)
                val sRaces = page.raceTable.Races
                if (sRaces.isEmpty()) break
                sTotal = F1Json.total(raw)?.toIntOrNull() ?: sRaces.size
                for (r in sRaces) {
                    val res = r.sprintResults.orEmpty().firstOrNull { it.Driver.driverId == driverId } ?: continue
                    sprintPoints += res.points?.toDoubleOrNull() ?: 0.0
                    if (res.position?.toIntOrNull() == 1) sprintWins++
                }
                sOffset += sRaces.size
            }
        } catch (e: Exception) {
        }
        return DriverStats(
            races = total.coerceAtMost(offset),
            wins = wins,
            podiums = podiums,
            fastestLaps = fastestLaps,
            points = points + sprintPoints,
            sprintWins = sprintWins,
            sprintPoints = sprintPoints,
            recentForm = recent
        )
    }

    private suspend fun countPoles(driverId: String): Int {
        var offset = 0
        var poles = 0
        var totalQ = Int.MAX_VALUE
        while (offset < totalQ && offset < 500) {
            val raw = withRetry429 { service.driverQualifying(driverId, limit = 100, offset = offset) }
            val page = F1Json.races(raw)
            val races = page.raceTable.Races
            if (races.isEmpty()) break
            totalQ = F1Json.total(raw)?.toIntOrNull() ?: 0
            for (r in races) {
                val q = r.QualifyingResults.orEmpty().firstOrNull { it.Driver.driverId == driverId } ?: continue
                if (q.position == "1") poles++
            }
            offset += races.size
        }
        return poles
    }

    suspend fun liveLaps(round: String, season: String): LiveLaps {
        val roundId = alphaRoundId(season, round)
            ?: throw IllegalStateException("GP introuvable côté API live")
        val env = withRetry429 { alpha.raceLaps(roundId) }
        val data = env.data ?: throw IllegalStateException("Pas de données live")
        val laps = data.laps
        if (laps.isEmpty()) throw IllegalStateException("Aucun tour disponible")
        val lastLap = laps.maxOf { it.number }
        val rows = laps
            .filter { it.number == lastLap }
            .sortedBy { it.position }
            .map { lap ->
                val drv = lap.driver_id?.let { data.drivers_by_id[it] }
                val team = lap.team_id?.let { data.teams_by_id[it] }
                LiveLapRow(
                    position = lap.position,
                    driverCode = drv?.abbreviation ?: "?",
                    driverName = drv?.fullName ?: "?",
                    teamName = team?.name ?: "",
                    lapTime = lap.time_display,
                    isFastest = lap.is_entry_fastest_lap
                )
            }
        return LiveLaps(round, "", lastLap, rows)
    }

    private suspend fun championshipRecord(driverId: String, seasons: List<String>): Pair<Int, String?> {
        if (seasons.isEmpty()) return 0 to null
        val currentSeason = try {
            F1Json.driverStandings(service.driverStandings()).standingsTable.season
        } catch (e: Exception) {
            null
        }
        var titles = 0
        var best: Int? = null
        for (season in seasons.sorted()) {
            if (season == currentSeason) continue
            val raw = try {
                withRetry429 { service.seasonDriverStandings(season) }
            } catch (e: Exception) {
                continue
            }
            val data = F1Json.driverStandings(raw)
            val entry = data.standingsTable.StandingsLists.firstOrNull()
                ?.DriverStandings.orEmpty()
                .firstOrNull { it.Driver.driverId == driverId }
            if (entry == null) continue
            val posText = entry.positionText?.trim()
            val pos = if (posText != null && posText.all { it.isDigit() }) posText.toInt()
                      else entry.position?.trim()?.toIntOrNull()
                      ?: continue
            if (pos == 1) titles++
            if (best == null || pos < best) best = pos
        }
        return titles to best?.toString()
    }
}

// Extracts the original page image URL from a Wikipedia API response (pure, testable)
fun parseWikipediaPhoto(json: kotlinx.serialization.json.JsonObject): String? {
    val pages = json["query"]?.jsonObject?.get("pages")?.jsonObject ?: return null
    return pages.values.firstOrNull()?.jsonObject
        ?.get("original")?.jsonObject
        ?.get("source")?.jsonPrimitive?.content
}


// Pure parsers for Wikidata API responses (unit-testable)
fun parseWikidataEntityId(json: kotlinx.serialization.json.JsonObject): String? =
    json["search"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content

fun parseWikidataDeathDate(json: kotlinx.serialization.json.JsonObject): String? {
    val entities = json["entities"]?.jsonObject ?: return null
    val claims = entities.values.firstOrNull()?.jsonObject?.get("claims")?.jsonObject ?: return null
    val p570 = claims["P570"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
    val time = p570["mainsnak"]?.jsonObject
        ?.get("datavalue")?.jsonObject
        ?.get("value")?.jsonObject
        ?.get("time")?.jsonPrimitive?.content ?: return null
    // "+1994-05-01T00:00:00Z" -> "1994-05-01"
    return Regex("^(\\+?\\d{4,})-(\\d{2})-(\\d{2})").find(time)?.let { "${it.groupValues[1].removePrefix("+")}-${it.groupValues[2]}-${it.groupValues[3]}" }
}
