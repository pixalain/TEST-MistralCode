package com.tvlive.app.model

/** Une entrée de playlist IPTV : chaîne TV, radio ou playlist distante (.m3u/.m3u8). */
data class Channel(
    val name: String,
    val url: String,
    val group: String = "",
    val logo: String = "",
    val tvgId: String = "",
    val radio: Boolean = false,
) {
    /** true si l'URL pointe vers une playlist plutôt que vers un flux direct. */
    val isPlaylist: Boolean
        get() {
            val path = url.substringBefore('?').lowercase()
            return path.endsWith(".m3u") || path.endsWith(".m3u8")
        }
}
