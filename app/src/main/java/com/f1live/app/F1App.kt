package com.f1live.app

import android.app.Application
import android.content.Context
import com.f1live.app.notifications.ensureNotificationChannel

class F1App : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel(this)
        scheduleWidgetRefresh(this)
    }

    companion object {
        fun scheduleWidgetRefresh(context: Context) {
            val request = androidx.work.PeriodicWorkRequestBuilder<com.f1live.app.widget.WidgetRefreshWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES
            ).build()
            androidx.work.WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "widget_refresh",
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
