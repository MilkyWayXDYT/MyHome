package com.robotpaw.myhome.mission

import com.robotpaw.myhome.mission.mechanics.strength.StrengthHoldConfig
import com.robotpaw.myhome.mission.mechanics.strength.StrengthHoldRules
import com.robotpaw.myhome.signal.calculateSignalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthHoldRulesTest {

    private val strong = calculateSignalState(90, 0)
    private val weak = calculateSignalState(35, 0)      // активен, но слабый
    private val rest = calculateSignalState(0, 0)
    private val both = calculateSignalState(80, 80)

    // порог между слабым и сильным сигналом, чтобы тест не зависел от точных чисел калибровки
    private val middle = (weak.flexorStrength + strong.flexorStrength) / 2

    private fun config(
        minStrength: Int = middle,
        proportional: Boolean = false,
        dropIsError: Boolean = true
    ) = StrengthHoldConfig(
        text = "тест",
        holdMs = 1000L,
        minStrength = minStrength,
        proportional = proportional,
        dropIsError = dropIsError
    )

    @Test
    fun strongHold_completesTask() {
        val rules = StrengthHoldRules(config())
        var done = false
        repeat(55) { if (rules.update(strong, 20L).done) done = true }   // чуть больше 1 с
        assertTrue(done)
        assertTrue(rules.isDone)
    }

    @Test
    fun weakSignal_doesNotAdvance() {
        val rules = StrengthHoldRules(config())
        repeat(200) { rules.update(weak, 20L) }
        assertEquals(0f, rules.progress, 0f)
    }

    @Test
    fun drop_isReportedOncePerGrip() {
        val rules = StrengthHoldRules(config())
        repeat(10) { rules.update(strong, 20L) }
        var drops = 0
        repeat(60) { if (rules.update(rest, 20L).drop) drops++ }          // долго не держит
        assertEquals(1, drops)

        repeat(5) { rules.update(strong, 20L) }                           // взялся снова
        repeat(30) { if (rules.update(rest, 20L).drop) drops++ }
        assertEquals(2, drops)
    }

    @Test
    fun shortRelease_withinGrace_isNotDrop() {
        val rules = StrengthHoldRules(config())
        repeat(10) { rules.update(strong, 20L) }
        var drops = 0
        repeat(5) { if (rules.update(rest, 20L).drop) drops++ }           // 100 мс, меньше допуска 300 мс
        repeat(10) { rules.update(strong, 20L) }
        assertEquals(0, drops)
    }

    @Test
    fun dropIsNotError_whenConfigSaysSo() {
        val rules = StrengthHoldRules(config(dropIsError = false))
        repeat(10) { rules.update(strong, 20L) }
        var drops = 0
        repeat(60) { if (rules.update(rest, 20L).drop) drops++ }
        assertEquals(0, drops)
    }

    @Test
    fun proportional_strongerPressMakesFasterProgress() {
        val soft = calculateSignalState(55, 0)
        val hard = calculateSignalState(100, 0)
        val a = StrengthHoldRules(config(minStrength = 0, proportional = true))
        val b = StrengthHoldRules(config(minStrength = 0, proportional = true))
        repeat(20) { a.update(soft, 20L); b.update(hard, 20L) }
        assertTrue(b.progress > a.progress)
        assertTrue(a.progress > 0f)
    }

    @Test
    fun coContraction_dropsProgressFasterThanRest() {
        val a = StrengthHoldRules(config())
        val b = StrengthHoldRules(config())
        repeat(25) { a.update(strong, 20L); b.update(strong, 20L) }
        repeat(10) { a.update(both, 20L); b.update(rest, 20L) }
        assertTrue(a.progress < b.progress)
    }

    @Test
    fun restore_continuesFromSavedProgress() {
        val rules = StrengthHoldRules(config())
        rules.restore(0.9f)
        repeat(10) { rules.update(strong, 20L) }                          // ещё 0,2 с при нужных 0,1 с
        assertTrue(rules.isDone)
        assertFalse(rules.progress < 1f)
    }
}
