package com.f1live.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.f1live.app.MainActivity
import com.f1live.app.R
import com.f1live.app.data.model.Race
import com.f1live.app.data.repository.F1Cache
import com.f1live.app.data.repository.F1Json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

data class WidgetData(
    val nextRaceName: String?,
    val nextRaceWhen: String?,
    val top3: List<String>
)

fun loadWidgetData(context: Context): WidgetData {
    val cache = F1Cache(context)
    var nextRaceName: String? = null
    var nextRaceWhen: String? = null
    var top3: List<String> = emptyList()
    try {
        cache.load(F1Cache.KEY_SCHEDULE)?.let { (raw, _) ->
            val data = F1Json.races(Json.parseToJsonElement(raw).jsonObject)
            val now = System.currentTimeMillis()
            val next: Race? = data.raceTable.Races
                .filter { r ->
                    runCatching {
                        val t = r.time?.removeSuffix("Z")?.let { LocalTime.parse(it) } ?: LocalTime.NOON
                        LocalDateTime.of(LocalDate.parse(r.date), t).toInstant(ZoneOffset.UTC).toEpochMilli()
                    }.getOrDefault(0L) > now
                }
                .minByOrNull { r ->
                    runCatching {
                        val t = r.time?.removeSuffix("Z")?.let { LocalTime.parse(it) } ?: LocalTime.NOON
                        LocalDateTime.of(LocalDate.parse(r.date), t).toInstant(ZoneOffset.UTC).toEpochMilli()
                    }.getOrDefault(Long.MAX_VALUE)
                }
            if (next != null) {
                nextRaceName = "${next.raceName} · ${next.circuitName}"
                val t = next.time?.removeSuffix("Z")?.let { LocalTime.parse(it) } ?: LocalTime.NOON
                val at = LocalDateTime.of(LocalDate.parse(next.date), t).toInstant(ZoneOffset.UTC).toEpochMilli()
                nextRaceWhen = SimpleDateFormat("EEE d MMM · HH:mm", Locale.getDefault()).format(Date(at))
            }
        }
    } catch (e: Exception) { }
    try {
        cache.load(F1Cache.KEY_STANDINGS)?.let { (raw, _) ->
            val data = F1Json.driverStandings(Json.parseToJsonElement(raw).jsonObject)
            top3 = data.standingsTable.StandingsLists.firstOrNull()?.DriverStandings.orEmpty()
                .sortedBy { it.position?.toIntOrNull() ?: 99 }
                .take(3)
                .map { "${it.position}. ${it.Driver.code ?: it.Driver.familyName} — ${it.points} pts" }
        }
    } catch (e: Exception) { }
    return WidgetData(nextRaceName, nextRaceWhen, top3)
}

class F1Widget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent {
            WidgetContent(context, data)
        }
    }

    @Composable
    private fun WidgetContent(context: Context, data: WidgetData) {
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF101418))
                .clickable { context.startActivity(openApp) }
                .padding(12.dp)
        ) {
            Text(
                text = "F1 LIVE",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFE10600)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            if (data.nextRaceName == null && data.top3.isEmpty()) {
                Text(
                    text = context.getString(R.string.widget_no_data),
                    style = TextStyle(color = ColorProvider(Color(0xFFCCCCCC)), fontSize = 12.sp)
                )
            } else {
                Text(
                    text = context.getString(R.string.widget_next_gp),
                    style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 11.sp)
                )
                Text(
                    text = data.nextRaceName ?: "—",
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
                data.nextRaceWhen?.let {
                    Text(
                        text = it,
                        style = TextStyle(color = ColorProvider(Color(0xFFAAAAAA)), fontSize = 12.sp)
                    )
                }
                if (data.top3.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        text = context.getString(R.string.widget_top3),
                        style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 11.sp)
                    )
                    data.top3.forEach { line ->
                        Text(
                            text = line,
                            style = TextStyle(color = ColorProvider(Color(0xFFDDDDDD)), fontSize = 12.sp)
                        )
                    }
                }
            }
        }
    }
}
