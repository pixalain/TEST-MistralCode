package com.f1live.app

import androidx.test.core.app.ApplicationProvider
import com.f1live.app.data.repository.F1Cache
import com.f1live.app.data.repository.F1Repository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
