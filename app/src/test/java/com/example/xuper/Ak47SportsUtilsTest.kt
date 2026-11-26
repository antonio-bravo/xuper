package com.example.xuper

import com.example.xuper.data.Ak47SportsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ak47SportsUtilsTest {

    @Test
    fun `isSpanishTeamEvent detects spanish teams and ignores foreign matches`() {
        assertTrue(Ak47SportsManager.isSpanishTeamEvent("Real Madrid vs Arsenal"))
        assertTrue(Ak47SportsManager.isSpanishTeamEvent("FC Barcelona - Girona"))
        assertTrue(Ak47SportsManager.isSpanishTeamEvent("Unicaja Málaga vs Baskonia"))
        assertFalse(Ak47SportsManager.isSpanishTeamEvent("Liverpool vs Chelsea"))
        assertFalse(Ak47SportsManager.isSpanishTeamEvent(""))
    }

    @Test
    fun `isGenericOptionLabel distinguishes generic labels from match names`() {
        assertTrue(Ak47SportsManager.isGenericOptionLabel("Opción 1"))
        assertTrue(Ak47SportsManager.isGenericOptionLabel("Server 2"))
        assertTrue(Ak47SportsManager.isGenericOptionLabel("HD"))
        assertTrue(Ak47SportsManager.isGenericOptionLabel(""))
        assertFalse(Ak47SportsManager.isGenericOptionLabel("Atlético de Madrid vs Sevilla"))
    }

    @Test
    fun `normalizeCategoryName maps common sports categories`() {
        assertEquals("⚽ Fútbol", Ak47SportsManager.normalizeCategoryName("Live Football"))
        assertEquals("🏀 Baloncesto", Ak47SportsManager.normalizeCategoryName("NBA Basketball"))
        assertEquals("🏎️ Motor", Ak47SportsManager.normalizeCategoryName("F1 Grand Prix"))
        assertEquals("🎾 Tenis", Ak47SportsManager.normalizeCategoryName("ATP Tennis"))
        assertEquals("Ciclismo", Ak47SportsManager.normalizeCategoryName("  Ciclismo  "))
    }
}
