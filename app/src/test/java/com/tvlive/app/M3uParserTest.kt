package com.tvlive.app

import com.tvlive.app.data.m3u.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    private val sample = "#EXTM3U\r\n" +
        "#EXTINF:-1 tvg-id=\"Arte.fr\" tvg-logo=\"http://logo/arte.png\" group-title=\"Service public\",Arte\r\n" +
        "https://example.com/arte.m3u8\r\n" +
        "#EXTINF:-1 group-title=\"Infos\" radio=\"true\",Radio Info\n" +
        "http://radio.example.com/stream\n" +
        "#EXTINF:-1,Chaîne sans groupe\n" +
        "https://example.com/chaine.mp4\n" +
        "https://example.com/directe.m3u8\n"

    @Test
    fun parseBasicAttributes() {
        val channels = M3uParser.parse(sample)
        assertEquals(4, channels.size)
        val arte = channels[0]
        assertEquals("Arte", arte.name)
        assertEquals("https://example.com/arte.m3u8", arte.url)
        assertEquals("Service public", arte.group)
        assertEquals("Arte.fr", arte.tvgId)
        assertEquals("http://logo/arte.png", arte.logo)
        assertFalse(arte.radio)
    }

    @Test
    fun parseRadioAndDefaults() {
        val channels = M3uParser.parse(sample)
        assertTrue(channels[1].radio)
        assertEquals("Infos", channels[1].group)
        assertEquals("", channels[2].group)
        assertEquals("https://example.com/directe.m3u8", channels[3].name)
        assertEquals(channels[3].url, channels[3].name)
    }

    @Test
    fun ignoresOtherDirectives() {
        val text = "#EXTM3U\n#EXT-X-VERSION:3\n#EXTINF:-1,A\nhttp://a/b.m3u8\n"
        assertEquals(listOf("A"), M3uParser.parse(text).map { it.name })
    }

    @Test
    fun groupsSortedCaseInsensitive() {
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

    @Test
    fun playlistDetection() {
        val ch = M3uParser.parse(sample)
        assertTrue(ch[0].isPlaylist)
        assertFalse(ch[2].isPlaylist)
    }
}
