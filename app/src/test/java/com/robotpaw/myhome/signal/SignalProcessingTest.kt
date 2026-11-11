package com.robotpaw.myhome.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalStateTest {

    @Test
    fun oldCall_stillWorksWithThreshold30() {
        assertEquals("00", calculateSignalState(10, 10).code)
        assertEquals("10", calculateSignalState(60, 10).code)
        assertEquals("11", calculateSignalState(60, 60).code)
        assertEquals("01", calculateSignalState(10, 60).code)
    }
}

class SignalProcessorTest {

    private val settings = ChannelSettings(rest = 5, max = 85, threshold = 30, hysteresis = 8, deadZone = 5)
    private val profile = SignalProfile(flexor = settings, extensor = settings)

    @Test
    fun hysteresis_keepsChannelOnJustBelowThreshold() {
        val p = SignalProcessor(profile)
        assertTrue(p.process(SignalData(0, 35, 0)).isFlexorActive)
        assertTrue(p.process(SignalData(20, 25, 0)).isFlexorActive)   // выше 30 - 8
        assertFalse(p.process(SignalData(40, 20, 0)).isFlexorActive)  // ниже — выключился
        assertFalse(p.process(SignalData(60, 25, 0)).isFlexorActive)  // обратно включится только с 30
    }

    @Test
    fun deadZone_zeroesSignal() {
        val p = SignalProcessor(profile, smoothing = 1f, maxStepPerFrame = 100f)
        val s = p.process(SignalData(0, 4, 0))
        assertEquals(0, s.flexorSignal)
        assertEquals(0, s.flexorStrength)
    }

    @Test
    fun smoothing_limitsJumps() {
        val p = SignalProcessor(profile, smoothing = 1f, maxStepPerFrame = 15f)
        assertEquals(15, p.process(SignalData(0, 85, 0)).flexorStrength)
    }
}

class HoldDetectorTest {

    private fun state(t: Long, f: Boolean, e: Boolean) =
        calculateSignalState(if (f) 60 else 0, if (e) 60 else 0, timestampMs = t)

    private fun ready(d: HoldDetector) {
        d.update(state(0, f = false, e = false))  // после старта ждём расслабления
    }

    @Test
    fun shortSignal_isPulse() {
        val d = HoldDetector(minPulseMs = 300, holdMs = 1000)
        ready(d)
        d.update(state(100, f = true, e = false))
        assertEquals(ControlEvent.Pulse(Channel.FLEXOR), d.update(state(600, f = false, e = false)))
    }

    @Test
    fun tooShortSignal_isIgnored() {
        val d = HoldDetector(minPulseMs = 300, holdMs = 1000)
        ready(d)
        d.update(state(100, f = true, e = false))
        assertNull(d.update(state(200, f = false, e = false)))
    }

    @Test
    fun longSignal_isHoldOnce() {
        val d = HoldDetector(minPulseMs = 300, holdMs = 1000)
        ready(d)
        d.update(state(100, f = false, e = true))
        assertNull(d.update(state(600, f = false, e = true)))
        assertEquals(ControlEvent.Hold(Channel.EXTENSOR), d.update(state(1100, f = false, e = true)))
        assertNull(d.update(state(1500, f = false, e = true)))
        assertNull(d.update(state(1600, f = false, e = false)))  // после удержания отпускание — не короткий
    }

    @Test
    fun coContraction_cancelsAndWaitsForRelease() {
        val d = HoldDetector(minPulseMs = 300, holdMs = 1000)
        ready(d)
        d.update(state(100, f = true, e = false))
        assertNull(d.update(state(500, f = true, e = true)))
        assertNull(d.update(state(1500, f = true, e = false)))  // не засчитываем, пока не расслабится
        assertNull(d.update(state(1600, f = false, e = false)))
    }

    @Test
    fun startsOnlyAfterRelease() {
        val d = HoldDetector(minPulseMs = 300, holdMs = 1000)
        // Канал уже был зажат на прошлом экране — удержание не должно сработать.
        d.update(state(0, f = true, e = false))
        assertNull(d.update(state(1500, f = true, e = false)))
    }
}