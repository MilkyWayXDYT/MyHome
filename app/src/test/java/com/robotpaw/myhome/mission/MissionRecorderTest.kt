package com.robotpaw.myhome.mission.mechanics.core

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

    @Test
    fun recordStreak_keepsBestOnly() {
        val recorder = MissionRecorder()
        recorder.recordStreak(3)
        recorder.recordStreak(1)
        recorder.recordStreak(5)
        assertEquals(5, recorder.snapshot().bestStreak)
    }

    private val ext = calculateSignalState(0, 80)

    @Test
    fun interruption_isCounted_whenRequiredChannelDropsLongerThanGrace() {
        val recorder = MissionRecorder()
        repeat(10) { recorder.onFrame(flex, 20L, Channel.FLEXOR) }
        repeat(15) { recorder.onFrame(rest, 20L, Channel.FLEXOR) }      // 300 мс без сигнала
        repeat(10) { recorder.onFrame(flex, 20L, Channel.FLEXOR) }
        val log = recorder.snapshot()
        assertEquals(1, log.interruptions)
        assertEquals(300L, log.offTargetMs)
        assertTrue(log.cleanPercent < 100)
        assertEquals(1, log.mistakes)
    }

    @Test
    fun shortFlicker_isNotInterruption_butReducesCleanPercent() {
        val recorder = MissionRecorder()
        repeat(10) { recorder.onFrame(flex, 20L, Channel.FLEXOR) }
        repeat(5) { recorder.onFrame(rest, 20L, Channel.FLEXOR) }       // 100 мс
        repeat(10) { recorder.onFrame(flex, 20L, Channel.FLEXOR) }
        val log = recorder.snapshot()
        assertEquals(0, log.interruptions)
        assertEquals(100L, log.offTargetMs)
    }

    @Test
    fun waitingBeforeFirstPress_isNotCounted() {
        val recorder = MissionRecorder()
        repeat(50) { recorder.onFrame(rest, 20L, Channel.FLEXOR) }
        repeat(10) { recorder.onFrame(flex, 20L, Channel.FLEXOR) }
        val log = recorder.snapshot()
        assertEquals(0, log.interruptions)
        assertEquals(0L, log.offTargetMs)
    }

    @Test
    fun switchingRequiredChannel_resetsTracking() {
        val recorder = MissionRecorder()
        repeat(10) { recorder.onFrame(flex, 20L, Channel.FLEXOR) }
        repeat(30) { recorder.onFrame(rest, 20L, Channel.EXTENSOR) }    // новый шаг, пока ничего не нажато
        assertEquals(0, recorder.snapshot().interruptions)
    }

    @Test
    fun wrongChannel_isCounted_afterGracePeriod() {
        val late = MissionRecorder()
        repeat(40) { late.onFrame(ext, 20L, Channel.FLEXOR) }           // 800 мс не той мышцей
        assertEquals(1, late.snapshot().wrongChannelEvents)

        val shortOne = MissionRecorder()
        repeat(20) { shortOne.onFrame(ext, 20L, Channel.FLEXOR) }       // 400 мс: игрок ещё отпускает прежнюю мышцу
        assertEquals(0, shortOne.snapshot().wrongChannelEvents)
    }

    @Test
    fun trackStabilityOff_countsNoInterruptionsOrWrongChannel() {
        val recorder = MissionRecorder()
        repeat(10) { recorder.onFrame(flex, 20L, Channel.FLEXOR, trackStability = false) }
        repeat(40) { recorder.onFrame(rest, 20L, Channel.FLEXOR, trackStability = false) }
        repeat(40) { recorder.onFrame(ext, 20L, Channel.FLEXOR, trackStability = false) }
        val log = recorder.snapshot()
        assertEquals(0, log.interruptions)
        assertEquals(0, log.wrongChannelEvents)
        assertEquals(0L, log.offTargetMs)
    }

    @Test
    fun steadiness_isFullForSteadyPress_andLowForJumpingStrength() {
        val steady = MissionRecorder()
        repeat(50) { steady.onFrame(flex, 20L, Channel.FLEXOR) }
        assertEquals(100, steady.snapshot().steadinessPercent)

        val strong = calculateSignalState(100, 0)
        val shaky = MissionRecorder()
        repeat(50) { shaky.onFrame(if (it % 2 == 0) flex else strong, 20L, Channel.FLEXOR) }   // сила скачет на ~20% каждый кадр
        assertTrue(shaky.snapshot().steadinessPercent < 20)
    }

    @Test
    fun steadiness_ignoresJumpsAcrossGaps() {
        val recorder = MissionRecorder()
        repeat(10) { recorder.onFrame(flex, 20L, Channel.FLEXOR) }
        repeat(10) { recorder.onFrame(rest, 20L, Channel.FLEXOR) }
        repeat(10) { recorder.onFrame(calculateSignalState(100, 0), 20L, Channel.FLEXOR) }
        assertEquals(100, recorder.snapshot().steadinessPercent)       // между нажатиями скачок не меряется
    }
}
