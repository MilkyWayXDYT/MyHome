package com.robotpaw.myhome.calibration

import com.robotpaw.myhome.signal.SignalData
import com.robotpaw.myhome.signal.UserLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationCalculatorTest {

    private fun frames(n: Int, flexor: Int, extensor: Int) =
        List(n) { SignalData(it * 20L, flexor, extensor) }

    @Test
    fun cleanSignal_givesExperiencedProfile() {
        val result = CalibrationCalculator.calculate(
            rest = frames(100, 5, 5),
            flexorStep = frames(100, 85, 5),
            extensorStep = frames(100, 5, 75)
        )
        assertNotNull(result.profile)
        val profile = result.profile!!
        // порог = покой + 30% диапазона
        assertEquals(5 + 24, profile.flexor.threshold)
        assertEquals(5 + 21, profile.extensor.threshold)
        assertEquals(1f, result.purity, 0.001f)
        assertEquals(UserLevel.EXPERIENCED, profile.level)
    }

    @Test
    fun crosstalk_givesNoviceProfile() {
        // При сжатии разгибатель тоже поднимается выше своего порога половину времени.
        val flexorStep = frames(50, 85, 5) + frames(50, 85, 50)
        val result = CalibrationCalculator.calculate(
            rest = frames(100, 5, 5),
            flexorStep = flexorStep,
            extensorStep = frames(100, 5, 75)
        )
        assertTrue(result.isSuccess)
        assertEquals(0.5f, result.flexorPurity, 0.01f)
        assertEquals(UserLevel.NOVICE, result.profile!!.level)
        assertTrue(result.problems.isNotEmpty())
    }

    @Test
    fun weakChannel_fails() {
        val result = CalibrationCalculator.calculate(
            rest = frames(100, 5, 5),
            flexorStep = frames(100, 12, 5),
            extensorStep = frames(100, 5, 75)
        )
        assertNull(result.profile)
        assertFalse(result.isSuccess)
    }
}

class CalibrationControllerTest {

    @Test
    fun walksAllStepsAndFinishes() {
        val controller = CalibrationController(prepareMs = 100, recordMs = 200, reactionMs = 0, requireConfirm = false, stepDoneMs = 0)
        var t = 0L
        fun feed(ms: Long, f: Int, e: Int) {
            val end = t + ms
            while (t < end) {
                controller.onFrame(SignalData(t, f, e))
                t += 20
            }
        }
        feed(320, 5, 5)   // покой: отсчёт + запись
        feed(320, 90, 5)  // сжатие
        feed(320, 5, 80)  // раскрытие
        controller.onFrame(SignalData(t, 0, 0))

        val phase = controller.phase
        assertTrue(phase is CalibrationPhase.Finished)
        assertTrue((phase as CalibrationPhase.Finished).result.isSuccess)
    }

    @Test
    fun recordingPauses_whenUserLetsGo() {
        val controller = CalibrationController(prepareMs = 100, recordMs = 1000, reactionMs = 0, requireConfirm = false, stepDoneMs = 0)
        var t = 0L
        fun feed(ms: Long, f: Int, e: Int) {
            val end = t + ms
            while (t < end) {
                controller.onFrame(SignalData(t, f, e))
                t += 20
            }
        }
        feed(1200, 5, 5)   // покой записан
        feed(500, 90, 5)   // сжимали ~0,4 с
        feed(3000, 5, 5)   // отпустили — запись должна стоять
        val paused = controller.phase as CalibrationPhase.Recording
        assertEquals(CalibrationStep.FLEXOR, paused.step)
        assertFalse(paused.isHolding)
        assertTrue(paused.progress < 0.5f)
    }

    @Test
    fun frameGap_doesNotSkipRecording() {
        val controller = CalibrationController(prepareMs = 100, recordMs = 3000, reactionMs = 0, requireConfirm = false, stepDoneMs = 0)
        controller.onFrame(SignalData(0, 5, 5))
        controller.onFrame(SignalData(100, 5, 5))      // отсчёт закончился
        controller.onFrame(SignalData(5_000, 5, 5))    // телефон «подвис» на 5 секунд
        val phase = controller.phase as CalibrationPhase.Recording
        assertEquals(CalibrationStep.REST, phase.step)
    }

    @Test
    fun weakSignal_finishesByTimeoutInsteadOfHanging() {
        val controller = CalibrationController(prepareMs = 100, recordMs = 500, reactionMs = 0, stepTimeoutMs = 2000, requireConfirm = false, stepDoneMs = 0)
        var t = 0L
        while (t < 10_000 && controller.phase !is CalibrationPhase.Finished) {
            controller.onFrame(SignalData(t, 5, 5))   // ни сжатия, ни раскрытия
            t += 20
        }
        val phase = controller.phase
        assertTrue(phase is CalibrationPhase.Finished)
        val result = (phase as CalibrationPhase.Finished).result
        assertFalse(result.isSuccess)
        assertTrue(result.problems.single().contains("сжатия"))   // застряли на шаге сжатия, а не проскочили его
    }

    @Test
    fun spikes_doNotAdvanceRecording() {
        val controller = CalibrationController(prepareMs = 100, recordMs = 1000, reactionMs = 300, requireConfirm = false, stepDoneMs = 0)
        var t = 0L
        fun frame(f: Int, e: Int) {
            controller.onFrame(SignalData(t, f, e))
            t += 20
        }
        repeat(80) { frame(5, 5) }          // покой записан, начался шаг сжатия
        repeat(100) {                       // 2 секунды: скачок 40 мс, затем тишина 260 мс
            if (it % 15 < 2) frame(90, 5) else frame(5, 5)
        }
        val phase = controller.phase as CalibrationPhase.Recording
        assertEquals(CalibrationStep.FLEXOR, phase.step)
        assertEquals(0f, phase.progress, 0.001f)
    }

    @Test
    fun shortDropout_doesNotBreakHold() {
        val controller = CalibrationController(prepareMs = 100, recordMs = 1000, reactionMs = 300, requireConfirm = false, stepDoneMs = 0)
        var t = 0L
        fun frame(f: Int, e: Int) {
            controller.onFrame(SignalData(t, f, e))
            t += 20
        }
        repeat(80) { frame(5, 5) }
        repeat(30) { frame(90, 5) }         // держим 0,6 с — реакция прошла, запись идёт
        val before = (controller.phase as CalibrationPhase.Recording).progress
        repeat(5) { frame(5, 5) }           // провал 100 мс — в пределах допуска
        repeat(5) { frame(90, 5) }
        val after = controller.phase as CalibrationPhase.Recording
        assertTrue(after.isHolding)
        assertTrue(after.progress > before) // запись продолжилась без новой паузы на реакцию
    }

    @Test
    fun intro_waitsForConfirm_beforeEachStep() {
        val controller = CalibrationController(prepareMs = 100, recordMs = 200, reactionMs = 0, stepDoneMs = 0)
        var t = 0L
        fun frame(f: Int, e: Int) = controller.onFrame(SignalData(t, f, e)).also { t += 20 }

        repeat(50) { frame(5, 5) }                          // секунда без подтверждения
        assertTrue(controller.phase is CalibrationPhase.Intro)

        controller.confirm()
        repeat(20) { frame(5, 5) }                          // отсчёт + запись покоя
        val next = controller.phase
        assertTrue(next is CalibrationPhase.Intro)          // перед сжатием — снова окно
        assertEquals(CalibrationStep.FLEXOR, (next as CalibrationPhase.Intro).step)
    }

    @Test
    fun stepDone_pausesBeforeNextStep() {
        val controller = CalibrationController(prepareMs = 100, recordMs = 200, reactionMs = 0, requireConfirm = false, stepDoneMs = 500)
        var t = 0L
        fun frame(f: Int, e: Int) = controller.onFrame(SignalData(t, f, e)).also { t += 20 }

        repeat(16) { frame(5, 5) }                          // отсчёт + запись покоя
        assertTrue(controller.phase is CalibrationPhase.StepDone)
        repeat(20) { frame(5, 5) }                          // 0,4 с - ещё пауза
        assertTrue(controller.phase is CalibrationPhase.StepDone)
        repeat(5) { frame(5, 5) }                           // пауза закончилась - отсчёт следующего шага
        assertTrue(controller.phase is CalibrationPhase.Preparing)
    }
}