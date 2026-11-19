package com.tvlive.app.data.sources

import android.content.Context
import android.net.Uri
import com.tvlive.app.data.m3u.M3uParser
import com.tvlive.app.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SourceEntry(val kind: String, val path: String, val name: String)

class SourcesRepository(private val context: Context) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun builtinStreams(): List<Channel> = readCatalog("streams", "Chaînes intégrées")

    fun builtinPlaylists(): List<Channel> = readCatalog("playlists", "Playlists publiques")

    private fun readCatalog(key: String, defaultGroup: String): List<Channel> {
        val json = context.assets.open("sources_builtin.json")
            .bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val arr = root.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val e = arr.getJSONObject(i)
            Channel(
                name = e.getString("name"),
                url = e.getString("url"),
                group = e.optString("group", defaultGroup),
                logo = e.optString("logo"),
            )
        }
    }

    suspend fun fetchPlaylist(url: String): List<Channel> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "TVLive/1.0 (Android)")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code} pour $url")
            }
            M3uParser.parse(resp.body!!.string())
        }
    }

    suspend fun readLocalFile(uri: Uri): List<Channel> = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Impossible de lire le fichier sélectionné")
        M3uParser.parse(text)
    }

    fun savedSources(): List<SourceEntry> {
        val raw = prefs.getString(KEY_SOURCES, null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val e = arr.getJSONObject(i)
            SourceEntry(e.getString("kind"), e.getString("path"), e.optString("name"))
        }
    }

    fun addSource(kind: String, path: String, name: String) {
        val list = savedSources().filterNot { it.path == path } +
            SourceEntry(kind, path, name)
        save(list)
    }

    fun removeSource(path: String) {
        save(savedSources().filterNot { it.path == path })
    }

    private fun save(list: List<SourceEntry>) {
        val arr = JSONArray()
        for (s in list) {
            arr.put(JSONObject().put("kind", s.kind).put("path", s.path).put("name", s.name))
        }
        prefs.edit().putString(KEY_SOURCES, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "tvlive_sources"
        private const val KEY_SOURCES = "sources"
    }
}
