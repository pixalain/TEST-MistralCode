package com.tvlive.app.data.model

data class Channel(
    val name: String,
    val url: String,
    val group: String = "",
    val logo: String = "",
    val tvgId: String = "",
    val radio: Boolean = false,
) {
    val isPlaylist: Boolean
        get() {
            val path = url.split("?", limit = 2)[0].lowercase()
            return path.endsWith(".m3u") || path.endsWith(".m3u8")
        }
}
