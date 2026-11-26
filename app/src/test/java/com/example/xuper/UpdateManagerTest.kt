package com.example.xuper

import com.example.xuper.util.UpdateManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun `isNewerVersion compares semantic and timestamped versions correctly`() {
        assertTrue(UpdateManager.isNewerVersion("v1.20261123.000000", "1.20261122.221553"))
        assertTrue(UpdateManager.isNewerVersion("2.0.0", "1.9.9"))
        assertFalse(UpdateManager.isNewerVersion("1.20261122.221553", "1.20261122.221553"))
        assertFalse(UpdateManager.isNewerVersion("1.20261121.120000", "1.20261122.221553"))
        assertFalse(UpdateManager.isNewerVersion("", "1.0.0"))
    }
}
