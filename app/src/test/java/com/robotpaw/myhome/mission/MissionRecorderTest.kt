package com.robotpaw.myhome.mission

import com.robotpaw.myhome.mission.mechanics.MissionRecorder
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.calculateSignalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionRecorderTest {

    private val flex = calculateSignalState(80, 0)
    private val rest = calculateSignalState(0, 0)
    private val both = calculateSignalState(80, 80)

    @Test
    fun countsDurationAndActiveTime() {
        val recorder = MissionRecorder()
        repeat(10) { recorder.onFrame(flex, 20L, Channel.FLEXOR) }
        repeat(10) { recorder.onFrame(rest, 20L, Channel.FLEXOR) }
        val log = recorder.snapshot()
        assertEquals(400L, log.durationMs)
        assertEquals(200L, log.activeMs)
        assertTrue(log.peakStrength > 0)
    }

    @Test
    fun coContractionEvents_countOnsetsNotFrames() {
        val recorder = MissionRecorder()
        repeat(5) { recorder.onFrame(both, 20L) }           // один длинный встречный сигнал
        repeat(5) { recorder.onFrame(rest, 20L) }
        repeat(5) { recorder.onFrame(both, 20L) }           // второй
        val log = recorder.snapshot()
        assertEquals(2, log.coContractionEvents)
        assertEquals(200L, log.coContractionMs)
        assertEquals(false, log.isFlawless)
    }

    @Test
    fun cleanPercent_isShareOfTimeWithoutCoContraction() {
        val recorder = MissionRecorder()
        repeat(5) { recorder.onFrame(both, 20L) }
        repeat(15) { recorder.onFrame(flex, 20L, Channel.FLEXOR) }
        assertEquals(75, recorder.snapshot().cleanPercent)
    }

    @Test
    fun startingLog_isContinued() {
        val first = MissionRecorder()
        repeat(10) { first.onFrame(flex, 20L, Channel.FLEXOR) }
        first.addPause()

        val second = MissionRecorder(first.snapshot())
        repeat(10) { second.onFrame(flex, 20L, Channel.FLEXOR) }
        assertEquals(400L, second.snapshot().durationMs)
        assertEquals(1, second.snapshot().pauses)
    }
}
