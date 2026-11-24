package com.tvlive.app.m3u

import com.tvlive.app.model.Channel

/**
 * Parseur de playlists IPTV (M3U / M3U8) : gère #EXTM3U, #EXTINF avec attributs
 * tvg-id / tvg-logo / group-title / radio, les lignes vides et les autres directives.
 */
object M3uParser {

    private val extinfRegex = Regex("""^#EXTINF:([^,]*)(?:,(.*))?$""")
    private val attrRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")

    fun parse(text: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var pendingAttrs: Map<String, String> = emptyMap()
        var pendingName = ""
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF") -> {
                    val match = extinfRegex.find(line)
                    pendingAttrs = match?.let {
                        attrRegex.findAll(it.groupValues[1])
                            .associate { m -> m.groupValues[1] to m.groupValues[2] }
                    } ?: emptyMap()
                    pendingName = match?.groupValues?.get(2)?.trim() ?: ""
                }
                line.startsWith("#") -> Unit // autres directives ignorées
                else -> {
                    channels += Channel(
                        name = pendingName.ifEmpty { line },
                        url = line,
                        group = pendingAttrs["group-title"].orEmpty(),
                        logo = pendingAttrs["tvg-logo"].orEmpty(),
                        tvgId = pendingAttrs["tvg-id"].orEmpty(),
                        radio = pendingAttrs["radio"].equals("true", ignoreCase = true),
                    )
                    pendingAttrs = emptyMap()
                    pendingName = ""
                }
            }
        }
        return channels
    }

    /** Regroupe par group-title (tri insensible à la casse, « Divers » si vide). */
    fun group(channels: List<Channel>): Map<String, List<Channel>> =
        channels
            .groupBy { it.group.ifEmpty { "Divers" } }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    /** Filtre par nom, groupe ou URL (insensible à la casse). */
    fun search(channels: List<Channel>, query: String): List<Channel> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return channels
        return channels.filter {
            q in it.name.lowercase() || q in it.group.lowercase() || q in it.url.lowercase()
        }
    }
}
