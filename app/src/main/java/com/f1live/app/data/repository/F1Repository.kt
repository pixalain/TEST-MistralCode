package com.f1live.app.data.repository

import com.f1live.app.data.api.ApiClient
import com.f1live.app.data.model.ConstructorStanding
import com.f1live.app.data.model.DriverStanding
import com.f1live.app.data.model.Race
import com.f1live.app.data.model.Result

data class RaceDetail(
    val race: Race,
    val results: List<Result>,
    val sprint: List<Result>,
    val qualifying: List<com.f1live.app.data.model.QualifyingResult> = emptyList()
)

class F1Repository(
    private val service: com.f1live.app.data.api.JolpicaService = ApiClient.service
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

    suspend fun raceResults(round: String): RaceDetail {
        val resp = service.raceResults(round)
        val race = resp.MRData.raceTable.Races.first()
        val results = race.Results.orEmpty()
        val sprint = try {
            service.sprintResults(round).MRData.raceTable.Races.firstOrNull()?.Sprint.orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
        val qualifying = try {
            service.qualifyingResults(round).MRData.raceTable.Races.firstOrNull()
                ?.QualifyingResults.orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
        return RaceDetail(race, results, sprint, qualifying)
    }
}
