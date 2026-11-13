package com.f1live.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.f1live.app.data.repository.F1Cache
import com.f1live.app.data.repository.F1Repository

/**
 * Periodically refreshes the offline cache (schedule + standings) and updates
 * all home-screen widgets. Runs on WorkManager's 15-min periodic schedule.
 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cache = F1Cache(applicationContext)
        val repo = F1Repository(cache = cache)
        return try {
            repo.seasonSchedule()
            repo.driverStandings()
            F1Widget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            // still update widgets from cache so they show last known data
            runCatching { F1Widget().updateAll(applicationContext) }
            Result.success()
        }
    }
}
