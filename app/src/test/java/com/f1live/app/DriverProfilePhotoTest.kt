package com.f1live.app

import androidx.test.core.app.ApplicationProvider
import com.f1live.app.data.repository.F1Cache
import com.f1live.app.data.repository.F1Repository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = F1LiveApp::class)
class DriverProfilePhotoTest {

    @Test
    fun fullDriverProfileHasPhotoAndDeathDate() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = F1Repository(cache = F1Cache(app))
        val senna = repo.driverProfile("senna", null)
        println("SENNA photo=${senna.photoUrl} death=${senna.deathDate}")
        assertNotNull("photo Senna manquante", senna.photoUrl)
        assertNotNull("deathDate Senna manquante", senna.deathDate)

        val max = repo.driverProfile("max_verstappen", "Red Bull Racing")
        println("MAX photo=${max.photoUrl} death=${max.deathDate}")
        assertNotNull("photo Verstappen manquante", max.photoUrl)
        assertNull("Verstappen vivant: deathDate doit être null", max.deathDate)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = F1LiveApp::class)
class DriverPhotoDownloadTest {

    @Test
    fun photoIsDownloadedToLocalFile() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = com.f1live.app.data.repository.F1Repository(
            cache = com.f1live.app.data.repository.F1Cache(app)
        )
        val senna = com.f1live.app.data.model.Driver(
            driverId = "senna", code = "SEN", givenName = "Ayrton", familyName = "Senna",
            dateOfBirth = "1960-03-21", nationality = "Brazilian"
        )
        val file = repo.downloadDriverPhoto(senna)
        println("PHOTO_FILE=${file?.absolutePath} size=${file?.length()}")
        assertNotNull("photo file expected", file)
        assertTrue("photo file should be non-empty jpeg", file!!.length() > 10_000)
        val head = file.readBytes().toList(); assertTrue(head[0] == 0xFF.toByte() && head[1] == 0xD8.toByte()) // JPEG magic
    }
}
