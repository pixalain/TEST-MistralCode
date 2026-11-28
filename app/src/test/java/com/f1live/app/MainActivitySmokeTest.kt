package com.f1live.app

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivitySmokeTest {

    @Test
    fun appStartsAndRendersContent() {
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(
            androidx.test.core.app.ApplicationProvider.getApplicationContext()
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity: ComponentActivity ->
                // force a frame
                val decor = activity.window.decorView
                decor.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(2400, android.view.View.MeasureSpec.EXACTLY)
                )
                decor.layout(0, 0, 1080, 2400)
                val content = activity.findViewById<ViewGroup>(android.R.id.content)
                assertTrue(
                    "L'écran d'accueil est vide (écran blanc) !",
                    content.childCount > 0 && content.getChildAt(0).height > 0
                )
            }
        }
    }
}
