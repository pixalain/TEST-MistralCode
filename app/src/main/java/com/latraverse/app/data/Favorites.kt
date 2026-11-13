package com.latraverse.app.data

import android.content.Context

object Favorites {

    private const val PREFS = "latraverse_prefs"
    private const val KEY = "favorite_concerts"

    fun key(concert: Concert): String = concert.date.toString() + "|" + concert.artist

    fun load(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())
            ?.toSet()
            ?: emptySet()

    fun save(context: Context, favorites: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY, favorites)
            .apply()
    }

    fun toggle(context: Context, favorites: Set<String>, concert: Concert): Set<String> {
        val key = key(concert)
        val next = if (key in favorites) favorites - key else favorites + key
        save(context, next)
        return next
    }
}
