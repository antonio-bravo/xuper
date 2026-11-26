package com.example.xuper

import com.example.xuper.data.M3UParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3UParserTest {

    @Test
    fun `parse extracts name, logo, category and url from standard EXTINF`() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="laliga" tvg-logo="https://example.com/logo.png" group-title="Deportes;Fútbol",LaLiga TV
            acestream://0123456789abcdef0123456789abcdef01234567
        """.trimIndent()

        val channels = M3UParser.parse(m3u, "MiLista")

        assertEquals(1, channels.size)
        val channel = channels[0]
        assertEquals("LaLiga TV", channel.name)
        assertEquals("https://example.com/logo.png", channel.logo)
        assertEquals("Deportes", channel.category)
        assertEquals("acestream://0123456789abcdef0123456789abcdef01234567", channel.url)
        assertEquals("MiLista", channel.sourceListName)
    }

    @Test
    fun `parse falls back to tvg-name and EXTGRP when comma name is missing`() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-name="Canal Fallback",
            #EXTGRP:Documentales;Historia
            https://example.com/stream.m3u8
        """.trimIndent()

        val channels = M3UParser.parse(m3u, "TDT")

        assertEquals(1, channels.size)
        assertEquals("Canal Fallback", channels[0].name)
        assertEquals("Documentales", channels[0].category)
        assertNull(channels[0].logo)
        assertEquals("https://example.com/stream.m3u8", channels[0].url)
    }

    @Test
    fun `parse ignores oversized urls and empty content`() {
        val hugeUrl = "https://example.com/" + "a".repeat(2005)
        val m3u = """
            #EXTM3U
            #EXTINF:-1,Gigante
            $hugeUrl
        """.trimIndent()

        assertTrue(M3UParser.parse(m3u, "Test").isEmpty())
        assertTrue(M3UParser.parse("", "Test").isEmpty())
        assertTrue(M3UParser.parse("#EXTM3U\n# comentario", "Test").isEmpty())
    }
}
