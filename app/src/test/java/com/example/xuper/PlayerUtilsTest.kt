package com.example.xuper

import com.example.xuper.util.PlayerUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerUtilsTest {

    private val validHash = "0123456789ABCDEF0123456789abcdef01234567"
    private val expectedLower = validHash.lowercase()

    @Test
    fun `getAceId extracts hash from acestream uri, proxy urls and raw id`() {
        assertEquals(expectedLower, PlayerUtils.getAceId("acestream://$validHash"))
        assertEquals(
            expectedLower,
            PlayerUtils.getAceId("http://127.0.0.1:6878/ace/manifest.m3u8?id=$validHash")
        )
        assertEquals(
            expectedLower,
            PlayerUtils.getAceId("http://125.125.125.125:6878/ace/getstream?id=$validHash&pid=1")
        )
        assertEquals(expectedLower, PlayerUtils.getAceId(validHash))
    }

    @Test
    fun `getAceId and isAceStreamUrl reject zicotv and generic iptv urls`() {
        val zicoUrl = "https://bp.zicotv.cc/live/stream.m3u8?token=$expectedLower"
        val iptvUrl = "https://cdn.example.com/live/channel1/index.m3u8"

        assertEquals("", PlayerUtils.getAceId(zicoUrl))
        assertEquals("", PlayerUtils.getAceId(iptvUrl))
        assertFalse(PlayerUtils.isAceStreamUrl(zicoUrl))
        assertFalse(PlayerUtils.isAceStreamUrl(iptvUrl))
        assertTrue(PlayerUtils.isAceStreamUrl("acestream://$validHash"))
    }

    @Test
    fun `formatAceStream urls build expected endpoints`() {
        assertEquals(
            "http://125.125.125.125:6878/ace/manifest.m3u8?id=$expectedLower",
            PlayerUtils.formatAceStreamHttpUrl("acestream://$validHash", "125.125.125.125", 6878)
        )
        assertEquals(
            "http://125.125.125.125:6879/ace/getstream?id=$expectedLower",
            PlayerUtils.formatAceStreamGetStreamUrl("acestream://$validHash", "125.125.125.125", 6879)
        )
    }
}
