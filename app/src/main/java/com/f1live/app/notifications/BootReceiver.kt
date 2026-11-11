package com.f1live.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.f1live.app.data.model.Race
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.f1live.app.data.repository.F1Cache
import com.f1live.app.data.repository.F1Json
import com.f1live.app.widget.F1Widget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Re-schedules session reminders after a device reboot, using the cached schedule.
 */
private suspend fun updateWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    val ids = manager.getGlanceIds(F1Widget::class.java)
    val widget = F1Widget()
    for (id in ids) widget.update(context, id)
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        com.f1live.app.F1App.scheduleWidgetRefresh(context)
        val cached = F1Cache(context).load(F1Cache.KEY_SCHEDULE + "_current")?.first
        if (cached == null) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { updateWidgets(context) }
            }
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = F1Json.races(Json.parseToJsonElement(cached).jsonObject)
                scheduleSessionReminders(context, data.raceTable.Races)
                updateWidgets(context)
            } catch (e: Exception) {
                // ignore — reminders will be re-scheduled on next app start
            }
        }
    }
}
