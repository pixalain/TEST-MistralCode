package com.tvlive.app

import com.tvlive.app.m3u.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    private val sample = buildString {
        append("#EXTM3U\r\n")
        append("#EXTINF:-1 tvg-id=\"Arte.fr\" tvg-logo=\"http://logo/arte.png\" group-title=\"Service public\",Arte\r\n")
        append("https://example.com/arte.m3u8\r\n")
        append("#EXTINF:-1 group-title=\"Infos\" radio=\"true\",Radio Info\n")
        append("http://radio.example.com/stream\n")
        append("#EXTINF:-1,Chaîne sans groupe\n")
        append("https://example.com/chaine.mp4\n")
        append("https://example.com/directe.m3u8\n")
    }

    @Test
    fun parsesAttributes() {
        val channels = M3uParser.parse(sample)
        assertEquals(4, channels.size)
        val arte = channels[0]
        assertEquals("Arte", arte.name)
        assertEquals("https://example.com/arte.m3u8", arte.url)
        assertEquals("Service public", arte.group)
        assertEquals("Arte.fr", arte.tvgId)
        assertEquals("http://logo/arte.png", arte.logo)
        assertFalse(arte.radio)
        assertTrue(arte.isPlaylist)
    }

    @Test
    fun parsesRadioAndDefaults() {
        val channels = M3uParser.parse(sample)
        assertTrue(channels[1].radio)
        assertEquals("Infos", channels[1].group)
        assertFalse(channels[1].isPlaylist)
        assertEquals("", channels[2].group)
        assertEquals(channels[3].url, channels[3].name) // ligne sans #EXTINF
    }

    @Test
    fun ignoresUnknownDirectives() {
        val parsed = M3uParser.parse("#EXTM3U\n#EXT-X-VERSION:3\n#EXTINF:-1,A\nhttp://a/b.m3u8\n")
        assertEquals(listOf("A"), parsed.map { it.name })
    }

    @Test
    fun groupsAreSortedCaseInsensitive() {
        val groups = M3uParser.group(M3uParser.parse(sample))
        assertEquals(setOf("Service public", "Infos", "Divers"), groups.keys)
        assertEquals(2, groups["Divers"]!!.size)
        assertEquals(
            groups.keys.toList(),
            groups.keys.toList().sortedWith(String.CASE_INSENSITIVE_ORDER),
        )
    }

    @Test
    fun searchMatchesNameGroupAndUrl() {
        val channels = M3uParser.parse(sample)
        assertEquals(1, M3uParser.search(channels, "arte").size)
        assertEquals(1, M3uParser.search(channels, "RADIO").size)
        assertEquals(4, M3uParser.search(channels, "example.com").size)
        assertEquals(4, M3uParser.search(channels, "").size)
        assertTrue(M3uParser.search(channels, "inexistant").isEmpty())
    }
}
