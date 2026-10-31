package com.f1live.app

import com.f1live.app.data.model.AlphaEnvelope
import com.f1live.app.data.model.AlphaSessionResults
import com.f1live.app.data.model.ConstructorStandingsData
import com.f1live.app.data.model.DriverStandingsData
import com.f1live.app.data.model.RacesData
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonParsingTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val driverStandingsJson = """
        {"MRData":{"series":"f1","StandingsTable":{"season":"2026","round":"18","StandingsLists":[{"season":"2026","round":"18","DriverStandings":[{"position":"1","positionText":"1","points":"365","wins":"9","Driver":{"driverId":"antonelli","permanentNumber":"12","code":"ANT","givenName":"Andrea Kimi","familyName":"Antonelli","dateOfBirth":"2006-08-25","nationality":"Italian"},"Constructors":[{"constructorId":"mercedes","name":"Mercedes","nationality":"German"}]}]}]}}}
    """.trimIndent()

    private val raceJson = """
        {"MRData":{"RaceTable":{"season":"2026","Races":[{"season":"2026","round":"3","raceName":"Japanese Grand Prix","Circuit":{"circuitId":"suzuka","circuitName":"Suzuka Circuit","Location":{"locality":"Suzuka","country":"Japan"}},"date":"2026-03-29","time":"05:00:00Z","Results":[{"number":"12","position":"1","positionText":"1","points":"25","Driver":{"driverId":"antonelli","givenName":"Andrea Kimi","familyName":"Antonelli"},"Constructor":{"constructorId":"mercedes","name":"Mercedes"},"grid":"1","laps":"53","status":"Finished","Time":{"millis":"5710000","time":"1:35:10.000"}}]}]}}}
    """.trimIndent()

    private val constructorsJson = """
        {"MRData":{"StandingsTable":{"season":"2026","StandingsLists":[{"ConstructorStandings":[{"position":"1","points":"607","wins":"12","Constructor":{"constructorId":"mercedes","name":"Mercedes"}}]}]}}}
    """.trimIndent()

    @Test
    fun parsesDriverStandings() {
        val env = json.decodeFromString<com.f1live.app.data.model.Envelope<DriverStandingsData>>(driverStandingsJson)
        val list = env.MRData.standingsTable.StandingsLists.first().DriverStandings!!
        assertEquals("2026", env.MRData.standingsTable.season)
        assertEquals(1, list.size)
        val d = list.first()
        assertEquals("Antonelli", d.Driver.familyName)
        assertEquals("365", d.points)
        assertEquals("mercedes", d.Constructors.first().constructorId)
    }

    @Test
    fun parsesRaceResults() {
        val env = json.decodeFromString<com.f1live.app.data.model.Envelope<RacesData>>(raceJson)
        val race = env.MRData.raceTable.Races.first()
        assertEquals("Japanese Grand Prix", race.raceName)
        assertEquals("Suzuka", race.locality)
        val r = race.Results!!.first()
        assertEquals("1", r.displayPosition)
        assertEquals("1:35:10.000", r.Time?.time)
        assertEquals("25", r.points)
    }

    @Test
    fun parsesAlphaFpResults() {
        val alphaFp = """
            {"metadata":{"timestamp":"2026-10-31T21:56:40Z"},"data":{"code":"FP1","title":"Practice 1","local_timestamp":"2026-03-27 11:30:00+09:00","results":[{"driver":{"id":"driver_dPPwWNxA","abbreviation":"RUS","given_name":"George","family_name":"Russell"},"team":{"name":"Mercedes","primary_color":"#00D7B6"},"position":1,"position_text":"1","time":"1:31.666","is_classified":true,"laps":27,"car_number":63}]}}
        """.trimIndent()
        val env = json.decodeFromString<AlphaEnvelope<AlphaSessionResults>>(alphaFp)
        val body = env.data!!
        assertEquals("FP1", body.code)
        val row = body.results.first()
        assertEquals("George Russell", row.driver.fullName)
        assertEquals("1:31.666", row.time)
        assertEquals("1", row.displayPosition)
        assertEquals("#00D7B6", row.team?.primary_color)
    }

    @Test
    fun parsesConstructorStandings() {
        val env = json.decodeFromString<com.f1live.app.data.model.Envelope<ConstructorStandingsData>>(constructorsJson)
        val c = env.MRData.standingsTable.StandingsLists.first().ConstructorStandings!!.first()
        assertEquals("Mercedes", c.Constructor.name)
        assertEquals("607", c.points)
        assertTrue(c.position == "1")
    }
}
