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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class SessionResults(
    val code: String,
    val title: String,
    val timestamp: String? = null,
    val rows: List<AlphaResultRow>
)

data class RaceDetail(
    val race: Race,
    val results: List<Result>,
    val sprint: List<Result>,
    val qualifying: List<QualifyingResult> = emptyList(),
    val alphaSessions: List<SessionResults> = emptyList()
)

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
    val recentForm: List<Pair<String, String>> = emptyList()
)

data class DriverProfile(
    val driver: Driver,
    val stats: DriverStats
)

class F1Repository(
    private val service: JolpicaService = ApiClient.service,
    private val alpha: AlphaService = ApiClient.alpha
) {

    suspend fun driverStandings(): Pair<String, List<DriverStanding>> {
        val resp = service.driverStandings()
        val table = resp.MRData.standingsTable
        val list = table.StandingsLists.firstOrNull()?.DriverStandings.orEmpty()
        return table.season to list
    }

    suspend fun constructorStandings(): Pair<String, List<ConstructorStanding>> {
        val resp = service.constructorStandings()
        val table = resp.MRData.standingsTable
        val list = table.StandingsLists.firstOrNull()?.ConstructorStandings.orEmpty()
        return table.season to list
    }

    suspend fun seasonSchedule(): Pair<String, List<Race>> {
        val resp = service.seasonSchedule()
        return resp.MRData.raceTable.season to resp.MRData.raceTable.Races
    }

    private suspend fun alphaRoundId(season: String, round: String): String? = try {
        val year = season.toIntOrNull() ?: return null
        val rounds = alpha.rounds(year).data ?: return null
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
            alpha.sessionResults(roundId, code)
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

    suspend fun raceResults(round: String): RaceDetail = coroutineScope {
        val resp = service.raceResults(round)
        val race = resp.MRData.raceTable.Races.first()
        val results = race.Results.orEmpty()

        val sprintDeferred = async {
            try {
                service.sprintResults(round).MRData.raceTable.Races.firstOrNull()?.Sprint.orEmpty()
            } catch (e: Exception) {
                emptyList<Result>()
            }
        }
        val qualifyingDeferred = async {
            try {
                service.qualifyingResults(round).MRData.raceTable.Races.firstOrNull()
                    ?.QualifyingResults.orEmpty()
            } catch (e: Exception) {
                emptyList<QualifyingResult>()
            }
        }

        val alphaDeferred = async {
            val roundId = alphaRoundId(race.season, round) ?: return@async emptyList<SessionResults>()
            val codes = listOf(
                "FP1" to "Practice 1",
                "FP2" to "Practice 2",
                "FP3" to "Practice 3",
                "Q" to "Qualifying",
                "SQ" to "Sprint Qualifying",
                "SR" to "Sprint",
                "R" to "Race"
            )
            codes.map { (code, title) ->
                async {
                    fetchAlphaSession(roundId, code, title)
                        ?: SessionResults(code, title, null, emptyList())
                }
            }.awaitAll()
        }

        RaceDetail(
            race = race,
            results = results,
            sprint = sprintDeferred.await(),
            qualifying = qualifyingDeferred.await(),
            alphaSessions = alphaDeferred.await()
        )
    }

    suspend fun driverProfile(driverId: String, currentTeam: String? = null): DriverProfile = coroutineScope {
        val infoDeferred = async { service.driverInfo(driverId).MRData.driverTable.Drivers.first() }

        val resultsDeferred = async {
            var offset = 0
            var total = Int.MAX_VALUE
            var wins = 0
            var podiums = 0
            var fastestLaps = 0
            var points = 0.0
            val seasons = mutableSetOf<String>()
            val recent = mutableListOf<Pair<String, String>>()
            while (offset < total && offset < 1000) {
                val page = service.driverResults(driverId, limit = 100, offset = offset)
                val races = page.MRData.raceTable.Races
                if (races.isEmpty()) break
                total = page.MRData.total?.toIntOrNull() ?: races.size
                for (r in races) {
                    seasons.add(r.season)
                    for (res in r.Results.orEmpty()) {
                        if (res.Driver.driverId != driverId) continue
                        points += res.points?.toDoubleOrNull() ?: 0.0
                        val pos = res.position?.toIntOrNull()
                        if (pos == 1) wins++
                        if (pos != null && pos <= 3) podiums++
                        if (res.FastestLap?.rank == "1") fastestLaps++
                        if (offset == 0 && recent.size < 10) {
                            recent.add("${r.season} ${r.raceName}" to (res.displayPosition))
                        }
                    }
                }
                offset += races.size
            }
            DriverStats(
                races = total.coerceAtMost(offset),
                wins = wins,
                podiums = podiums,
                fastestLaps = fastestLaps,
                points = points,
                seasons = seasons.size,
                currentTeam = currentTeam,
                recentForm = recent
            )
        }

        val qualifyingDeferred = async {
            var offset = 0
            var poles = 0
            while (offset < 1000) {
                val page = try {
                    service.driverQualifying(driverId, limit = 100, offset = offset)
                } catch (e: Exception) {
                    break
                }
                val races = page.MRData.raceTable.Races
                if (races.isEmpty()) break
                for (r in races) {
                    for (q in r.QualifyingResults.orEmpty()) {
                        if (q.Driver.driverId == driverId && q.position == "1") poles++
                    }
                }
                offset += races.size
                val totalQ = page.MRData.total?.toIntOrNull() ?: 0
                if (offset >= totalQ) break
            }
            poles
        }

        val bestChampDeferred = async { bestChampionshipPosition(driverId) }

        val driver = infoDeferred.await()
        val base = resultsDeferred.await()
        val poles = qualifyingDeferred.await()
        val bestChamp = bestChampDeferred.await()

        DriverProfile(
            driver = driver,
            stats = base.copy(
                poles = poles,
                championships = if (bestChamp == "1") 1 else 0,
                bestChampionship = bestChamp
            )
        )
    }

    private suspend fun bestChampionshipPosition(driverId: String): String? {
        val seasons = mutableSetOf<String>()
        var offset = 0
        while (offset < 1000) {
            val page = service.driverResults(driverId, limit = 100, offset = offset)
            val races = page.MRData.raceTable.Races
            if (races.isEmpty()) break
            races.forEach { seasons.add(it.season) }
            offset += races.size
            val total = page.MRData.total?.toIntOrNull() ?: 0
            if (offset >= total) break
        }
        var best: Int? = null
        for (season in seasons.sortedDescending()) {
            val page = try {
                service.seasonDriverStandings(season)
            } catch (e: Exception) {
                continue
            }
            val pos = page.MRData.standingsTable.StandingsLists.firstOrNull()
                ?.DriverStandings.orEmpty()
                .firstOrNull { it.Driver.driverId == driverId }
                ?.position?.toIntOrNull()
            if (pos != null && (best == null || pos < best)) {
                best = pos
                if (best == 1) break
            }
        }
        return best?.toString()
    }
}
