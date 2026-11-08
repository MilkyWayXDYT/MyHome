package com.robotpaw.myhome.mission

import com.robotpaw.myhome.mission.mechanics.AlternationRules
import com.robotpaw.myhome.mission.mechanics.AlternationStep
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.calculateSignalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternationRulesTest {

    private val flex = calculateSignalState(80, 0)
    private val ext = calculateSignalState(0, 80)
    private val rest = calculateSignalState(0, 0)
    private val both = calculateSignalState(80, 80)

    private fun twoSteps() = AlternationRules(
        listOf(
            AlternationStep(Channel.FLEXOR, 1000L, "сжать"),
            AlternationStep(Channel.EXTENSOR, 1000L, "разжать")
        )
    )

    @Test
    fun correctAlternation_completesAllSteps() {
        val rules = twoSteps()
        repeat(52) { rules.update(flex, 20L) }              // чуть больше 1 с (запас на округление float)
        assertEquals(1, rules.stepIndex)
        repeat(52) { rules.update(ext, 20L) }               // чуть больше 1 с
        assertTrue(rules.isDone)
        assertEquals(1f, rules.progress, 0f)
    }

    @Test
    fun wrongChannel_doesNotAdvance() {
        val rules = twoSteps()
        repeat(100) { rules.update(ext, 20L) }              // первый шаг ждёт сжатия
        assertEquals(0, rules.stepIndex)
        assertEquals(0f, rules.stepProgress, 0f)
    }

    @Test
    fun stepProgress_decaysWhenReleased_butNeverGoesBackToPreviousStep() {
        val rules = twoSteps()
        repeat(52) { rules.update(flex, 20L) }              // первый шаг пройден
        repeat(25) { rules.update(ext, 20L) }               // половина второго
        repeat(500) { rules.update(rest, 20L) }             // долгий покой
        assertEquals(1, rules.stepIndex)
        assertEquals(0f, rules.stepProgress, 0f)
    }

    @Test
    fun coContraction_dropsStepProgressFasterThanRest() {
        val a = twoSteps()
        val b = twoSteps()
        repeat(25) { a.update(flex, 20L); b.update(flex, 20L) }   // половина шага
        repeat(10) { a.update(both, 20L); b.update(rest, 20L) }
        assertTrue(a.stepProgress < b.stepProgress)
    }

    @Test
    fun overshoot_isReportedOncePerExcursion_andBlocksProgress() {
        val soft = calculateSignalState(0, 50)
        val hard = calculateSignalState(0, 100)
        val zone = (soft.extensorStrength - 5)..(soft.extensorStrength + 5)
        val rules = AlternationRules(listOf(AlternationStep(Channel.EXTENSOR, 1000L, "мягко", zone)))

        var overshoots = 0
        repeat(10) { if (rules.update(hard, 20L).overshoot) overshoots++ }
        assertEquals(1, overshoots)
        assertEquals(0f, rules.stepProgress, 0f)

        repeat(5) { rules.update(soft, 20L) }               // вернулись в зону
        repeat(5) { if (rules.update(hard, 20L).overshoot) overshoots++ }
        assertEquals(2, overshoots)
    }

    @Test
    fun strengthInZone_completesStep() {
        val soft = calculateSignalState(0, 50)
        val zone = (soft.extensorStrength - 5)..(soft.extensorStrength + 5)
        val rules = AlternationRules(listOf(AlternationStep(Channel.EXTENSOR, 500L, "мягко", zone)))
        repeat(27) { rules.update(soft, 20L) }
        assertTrue(rules.isDone)
    }

    @Test
    fun restore_continuesFromSavedProgress() {
        val rules = twoSteps()
        rules.restore(0.75f)                                 // второй шаг, на половине
        assertEquals(1, rules.stepIndex)
        assertEquals(0.5f, rules.stepProgress, 0.01f)
        assertFalse(rules.isDone)
    }
}
