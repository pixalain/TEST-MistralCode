package com.latraverse.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
data class ArchiveEntry(
    val artist: String,
    val date: String,
    val time: String,
    val genre: String,
    val support: String? = null,
    val imageUrl: String? = null
)

object ArchiveRepository {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<Concert> = runCatching {
        val text = context.assets.open("archive.json").bufferedReader().use { it.readText() }
        json.decodeFromString<List<ArchiveEntry>>(text).mapNotNull { e ->
            val date = runCatching { LocalDate.parse(e.date) }.getOrNull() ?: return@mapNotNull null
            Concert(
                artist = e.artist,
                date = date,
                time = e.time,
                genre = e.genre,
                support = e.support,
                description = null,
                imageUrl = e.imageUrl
            )
        }
    }.getOrElse { emptyList() }
}
