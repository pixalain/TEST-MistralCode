package com.tvlive.app.sources

import android.content.Context
import android.net.Uri
import com.tvlive.app.m3u.M3uParser
import com.tvlive.app.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Entrée de playlist enregistrée par l'utilisateur (fichier local ou URL). */
data class SourceEntry(val kind: String, val path: String, val name: String)

/**
 * Sources de chaînes : catalogue intégré (assets/sources_builtin.json, flux ouverts
 * uniquement) + playlists utilisateur persistées en SharedPreferences.
 */
class SourcesRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun builtinStreams(): List<Channel> = readCatalog("streams", "Chaînes intégrées")

    fun builtinPlaylists(): List<Channel> = readCatalog("playlists", "Playlists publiques")

    private fun readCatalog(key: String, defaultGroup: String): List<Channel> {
        val json = context.assets.open("sources_builtin.json")
            .bufferedReader().use { it.readText() }
        val array = JSONObject(json).optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).map { i ->
            val e = array.getJSONObject(i)
            Channel(
                name = e.getString("name"),
                url = e.getString("url"),
                group = e.optString("group", defaultGroup),
                logo = e.optString("logo"),
            )
        }
    }

    /** Télécharge et parse une playlist distante (M3U/M3U8). */
    suspend fun fetchPlaylist(url: String): List<Channel> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "TVLive/1.0 (Android)")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} pour $url")
            }
            M3uParser.parse(response.body!!.string())
        }
    }

    /** Lit et parse une playlist locale (URI SAF). */
    suspend fun readLocalPlaylist(uri: Uri): List<Channel> = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Impossible de lire le fichier sélectionné")
        M3uParser.parse(text)
    }

    fun savedSources(): List<SourceEntry> {
        val raw = prefs.getString(KEY_SOURCES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val e = array.getJSONObject(i)
                SourceEntry(e.getString("kind"), e.getString("path"), e.optString("name"))
            }
        }.getOrElse { emptyList() }
    }

    fun addSource(kind: String, path: String, name: String) =
        save(savedSources().filterNot { it.path == path } + SourceEntry(kind, path, name))

    fun removeSource(path: String) = save(savedSources().filterNot { it.path == path })

    private fun save(list: List<SourceEntry>) {
        val array = JSONArray()
        list.forEach {
            array.put(JSONObject().put("kind", it.kind).put("path", it.path).put("name", it.name))
        }
        prefs.edit().putString(KEY_SOURCES, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "tvlive_sources"
        private const val KEY_SOURCES = "sources"
    }
}
