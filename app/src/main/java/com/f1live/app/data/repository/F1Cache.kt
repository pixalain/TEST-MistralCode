package com.f1live.app.data.repository

import android.content.Context
import androidx.core.content.edit

/**
 * Simple SharedPreferences-backed JSON cache so the app still shows the last
 * known data when the network / API is unavailable.
 */
class F1Cache(context: Context) {

    private val prefs = context.getSharedPreferences("f1_cache", Context.MODE_PRIVATE)

    fun save(key: String, text: String) {
        prefs.edit {
            putString("json_$key", text)
            putLong("ts_$key", System.currentTimeMillis())
        }
    }

    fun load(key: String): Pair<String, Long>? {
        val raw = prefs.getString("json_$key", null) ?: return null
        return raw to prefs.getLong("ts_$key", 0L)
    }

    fun timestamp(key: String): Long = prefs.getLong("ts_$key", 0L)

    fun clear() = prefs.edit { clear() }

    companion object {
        const val KEY_STANDINGS = "standings"
        const val KEY_CONSTRUCTORS = "constructors"
        const val KEY_SCHEDULE = "schedule"
        fun raceKey(round: String) = "race_$round"
    }
}
