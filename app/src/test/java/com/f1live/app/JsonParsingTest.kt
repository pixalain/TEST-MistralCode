package com.f1live.app

import com.f1live.app.data.model.AlphaEnvelope
import com.f1live.app.data.repository.F1Json
import kotlinx.serialization.json.Json as KJson
import com.f1live.app.data.model.AlphaSessionResults
import com.f1live.app.data.model.ConstructorStandingsData
import com.f1live.app.data.model.DriverStandingsData
import com.f1live.app.data.model.Circuit
import com.f1live.app.data.model.Location
import com.f1live.app.data.model.Race
import com.f1live.app.data.model.RacesData
import com.f1live.app.data.model.Session
import com.f1live.app.ui.screens.SessionInfo
import com.f1live.app.ui.screens.SessionStatus
import com.f1live.app.ui.screens.findCurrentOrLastRace
import com.f1live.app.ui.screens.sessionDateTime
import com.f1live.app.ui.screens.sessionStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

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
        val obj = Json.parseToJsonElement(driverStandingsJson).jsonObject
        val data = F1Json.driverStandings(obj)
        val list = data.standingsTable.StandingsLists.first().DriverStandings!!
        assertEquals("2026", data.standingsTable.season)
        assertEquals(1, list.size)
        val d = list.first()
        assertEquals("Antonelli", d.Driver.familyName)
        assertEquals("365", d.points)
        assertEquals("mercedes", d.Constructors.first().constructorId)
    }

    @Test
    fun parsesRaceResults() {
        val obj = Json.parseToJsonElement(raceJson).jsonObject
        val data = F1Json.races(obj)
        val race = data.raceTable.Races.first()
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
    fun sessionStatusTransitions() {
        val fp = SessionInfo("fp1", 0, Session("2026-10-30", "18:30:00Z"))
        val atStart = LocalDateTime.of(2026, 10, 30, 18, 30, 0)
        assertEquals(SessionStatus.IN_PROGRESS, sessionStatus(fp, atStart.plusMinutes(15)))
        assertEquals(SessionStatus.UPCOMING, sessionStatus(fp, atStart.minusHours(2)))
        assertEquals(SessionStatus.DONE, sessionStatus(fp, atStart.plusHours(2)))
        assertEquals(SessionStatus.NOT_SCHEDULED, sessionStatus(SessionInfo("x", 0, null), atStart))
    }

    @Test
    fun sessionDateTimeParsesUtc() {
        val dt = sessionDateTime(Session("2026-10-30", "18:30:00Z"))
        assertNotNull(dt)
        assertEquals(LocalDateTime.of(2026, 10, 30, 18, 30), dt)
    }

    @Test
    fun currentOrLastRaceFallsBackToMostRecent() {
        fun mk(round: String, date: String) = Race(
            season = "2026", round = round, raceName = "GP$round",
            Circuit = Circuit("c", "Circuit", Location("City", "Country")),
            date = date, time = "14:00:00Z",
            firstPractice = Session(date, "10:00:00Z")
        )
        val races = listOf(mk("18", "2026-10-25"), mk("19", "2026-11-01"))
        val r = findCurrentOrLastRace(races)
        assertNotNull(r)
    }

    @Test
    fun parsesConstructorStandings() {
        val obj = Json.parseToJsonElement(constructorsJson).jsonObject
        val data = F1Json.constructorStandings(obj)
        val c = data.standingsTable.StandingsLists.first().ConstructorStandings!!.first()
        assertEquals("Mercedes", c.Constructor.name)
        assertEquals("607", c.points)
        assertTrue(c.position == "1")
    }
}
