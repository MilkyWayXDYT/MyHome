package com.robotpaw.myhome.calibration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickCheckTest {

    // держать код n кадров по 50 мс
    private fun QuickCheck.hold(code: String, frames: Int) = repeat(frames) { update(code, 50L) }

    @Test
    fun allStepsHeld_passes() {
        val check = QuickCheck(holdMs = 1000L)
        check.hold("10", 20)
        check.hold("01", 20)
        check.hold("00", 20)
        assertTrue(check.isPassed)
        assertFalse(check.failed)
    }

    @Test
    fun wrongState_failsAfterTimeout() {
        val check = QuickCheck(holdMs = 1000L, timeoutMs = 3000L)
        check.hold("11", 60)
        assertTrue(check.failed)
        assertEquals(0, check.stepIndex)
    }

    @Test
    fun shortGap_keepsHold() {
        val check = QuickCheck(holdMs = 1000L, graceMs = 150L)
        check.hold("10", 10)
        check.hold("00", 2)   // 100 мс провала
        check.hold("10", 10)
        assertEquals(1, check.stepIndex)
    }

    @Test
    fun longGap_resetsHold() {
        val check = QuickCheck(holdMs = 1000L, graceMs = 150L)
        check.hold("10", 10)
        check.hold("00", 6)   // 300 мс провала
        check.hold("10", 10)
        assertEquals(0, check.stepIndex)
    }
}