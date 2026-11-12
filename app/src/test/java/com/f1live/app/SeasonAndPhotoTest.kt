package com.f1live.app

import com.f1live.app.data.repository.parseWikipediaPhoto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonAndPhotoTest {

    @Test
    fun seasonsGoBackTo1950() {
        val current = java.time.Year.now().value
        val seasons = (current downTo 1950).map { it.toString() }
        assertEquals("1950", seasons.last())
        assertEquals(current.toString(), seasons.first())
        assertEquals(current - 1950 + 1, seasons.size)
    }

    @Test
    fun parseWikipediaPhotoExtractsUrl() {
        val json = Json.parseToJsonElement("""
            {"query":{"pages":{"123":{"pageid":123,"title":"Max Verstappen","original":{"source":"https://upload.wikimedia.org/x.jpg","width":600,"height":400}}}}}
        """.trimIndent()).let { it as kotlinx.serialization.json.JsonObject }
        assertEquals("https://upload.wikimedia.org/x.jpg", parseWikipediaPhoto(json))
    }

    @Test
    fun parseWikipediaPhotoNullWhenMissing() {
        val json = Json.parseToJsonElement("""{"query":{"pages":{"-1":{"missing":""}}}}""")
            .let { it as kotlinx.serialization.json.JsonObject }
        assertNull(parseWikipediaPhoto(json))
    }

    @Test
    fun livePhotoForVerstappen() {
        runBlocking {
            val repo = com.f1live.app.data.repository.F1Repository()
            val driver = com.f1live.app.data.model.Driver(
                driverId = "max_verstappen", code = "VER",
                givenName = "Max", familyName = "Verstappen", dateOfBirth = "1997-09-30",
                nationality = "Dutch"
            )
            val url = repo.driverPhotoUrl(driver)
            println("PHOTO=$url")
            assertTrue("photo url expected", url != null && url.startsWith("https://upload.wikimedia.org/"))
        }
    }
}
