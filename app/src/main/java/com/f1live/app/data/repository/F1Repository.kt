package com.f1live.app.data.repository

import com.f1live.app.data.api.AlphaService
import com.f1live.app.data.api.ApiClient
import com.f1live.app.data.api.JolpicaService
import com.f1live.app.data.model.AlphaEnvelope
import com.f1live.app.data.model.AlphaResultRow
import com.f1live.app.data.model.ConstructorStanding
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
}
