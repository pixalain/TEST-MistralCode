package com.tvlive.app.data.m3u

import com.tvlive.app.data.model.Channel

object M3uParser {
    private val attrRe = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")
    private val extinfRe = Regex("""^#EXTINF:([^,]*)(?:,(.*))?$""")

    fun parse(text: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var attrs: Map<String, String> = emptyMap()
        var pendingName = ""
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF")) {
                val m = extinfRe.find(line)
                attrs = m?.let {
                    attrRe.findAll(it.groupValues[1])
                        .associate { match -> match.groupValues[1] to match.groupValues[2] }
                } ?: emptyMap()
                pendingName = m?.groupValues?.get(2)?.trim() ?: ""
            } else if (line.startsWith("#")) {
                continue
            } else {
                channels += Channel(
                    name = pendingName.ifEmpty { line },
                    url = line,
                    group = attrs["group-title"].orEmpty(),
                    logo = attrs["tvg-logo"].orEmpty(),
                    tvgId = attrs["tvg-id"].orEmpty(),
                    radio = attrs["radio"]?.lowercase() == "true",
                )
                attrs = emptyMap()
                pendingName = ""
            }
        }
        return channels
    }

    fun group(channels: List<Channel>): Map<String, List<Channel>> =
        channels
            .groupBy { it.group.ifEmpty { "Divers" } }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    fun search(channels: List<Channel>, query: String): List<Channel> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return channels
        return channels.filter {
            q in it.name.lowercase() || q in it.group.lowercase() || q in it.url.lowercase()
        }
    }
}
