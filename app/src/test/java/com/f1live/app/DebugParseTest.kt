package com.f1live.app

import com.f1live.app.data.model.RacesData
import com.f1live.app.data.repository.F1Json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugParseTest {

    @Test
    fun parseDriverResultsPage() {
        val sample = """
            {"MRData":{"series":"f1","limit":"100","offset":"0","total":"398","RaceTable":{"driverId":"hamilton","Races":[{"season":"2007","round":"1","raceName":"Australian Grand Prix","Circuit":{"circuitId":"albert_park","circuitName":"Albert Park","Location":{"locality":"Melbourne","country":"Australia"}},"date":"2007-03-18","time":"01:00:00Z","Results":[{"number":"2","position":"3","positionText":"3","points":"6","Driver":{"driverId":"hamilton","permanentNumber":"44","code":"HAM","givenName":"Lewis","familyName":"Hamilton","dateOfBirth":"1985-01-07","nationality":"British"},"Constructor":{"constructorId":"mclaren","name":"McLaren"},"grid":"2","laps":"58","status":"Finished"}]}]}}}
        """.trimIndent()
        val obj = Json.parseToJsonElement(sample).jsonObject
        val data = F1Json.races(obj)
        val total = F1Json.total(obj)
        val race = data.raceTable.Races.first()
        assertEquals("398", total)
        assertEquals(1, data.raceTable.Races.size)
        assertEquals(1, race.Results?.size)
        assertEquals("hamilton", race.Results?.first()?.Driver?.driverId)
    }
}
