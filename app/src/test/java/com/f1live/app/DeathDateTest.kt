package com.f1live.app

import com.f1live.app.data.repository.parseWikidataDeathDate
import com.f1live.app.data.repository.parseWikidataEntityId
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeathDateTest {

    private fun parse(s: String): JsonObject =
        Json.parseToJsonElement(s) as JsonObject

    @Test
    fun parseWikidataEntityIdWorks() {
        val json = parse("""{"search":[{"id":"Q10490","label":"Ayrton Senna"}]}""")
        assertEquals("Q10490", parseWikidataEntityId(json))
    }

    @Test
    fun parseWikidataDeathDateWorks() {
        val json = parse(
            """{"entities":{"Q10490":{"claims":{"P570":[{"mainsnak":{"datavalue":{"value":{"time":"+1994-05-01T00:00:00Z"}}}}]}}}}"""
        )
        assertEquals("1994-05-01", parseWikidataDeathDate(json))
    }

    @Test
    fun parseWikidataDeathDateNullWhenAlive() {
        val json = parse("""{"entities":{"Q2239218":{"claims":{}}}}""")
        assertNull(parseWikidataDeathDate(json))
    }

    @Test
    fun liveDeathDateSennaAndVerstappen() {
        runBlocking {
            val repo = com.f1live.app.data.repository.F1Repository()
            val senna = com.f1live.app.data.model.Driver(
                driverId = "senna", code = "SEN", givenName = "Ayrton", familyName = "Senna",
                dateOfBirth = "1960-03-21", nationality = "Brazilian"
            )
            val max = com.f1live.app.data.model.Driver(
                driverId = "max_verstappen", code = "VER", givenName = "Max", familyName = "Verstappen",
                dateOfBirth = "1997-09-30", nationality = "Dutch"
            )
            val dSenna = repo.driverDeathDate(senna)
            val dMax = repo.driverDeathDate(max)
            println("SENNA_DEATH=$dSenna | VER_DEATH=$dMax")
            assertEquals("1994-05-01", dSenna)
            assertNull(dMax)
        }
    }
}
