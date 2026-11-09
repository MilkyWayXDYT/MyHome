package com.robotpaw.myhome.mission.mechanics.mixed

import com.robotpaw.myhome.mission.mechanics.alternation.AlternationRules
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalState
import com.robotpaw.myhome.signal.calculateSignalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixedConfigsTest {

    // сырой сигнал, при котором сила канала примерно равна нужной (при стандартной калибровке rest=5, max=100)
    private fun stateWithStrength(channel: Channel, strength: Int): SignalState {
        val raw = (strength * 95 / 100.0 + 5).toInt()
        return when (channel) {
            Channel.FLEXOR -> calculateSignalState(raw, 0)
            Channel.EXTENSOR -> calculateSignalState(0, raw)
        }
    }

    @Test
    fun everyMixedTask_canBeCompleted() {
        MixedConfigs.ALL.forEach { config ->
            val rules = AlternationRules(config.steps)
            config.steps.forEach { step ->
                val state = when {
                    step.rest -> calculateSignalState(0, 0)
                    else -> stateWithStrength(step.channel, step.zone?.let { (it.first + it.last) / 2 } ?: 60)
                }
                val before = rules.stepIndex
                var frames = 0
                while (rules.stepIndex == before && frames < step.holdMs / 20 + 20) {
                    rules.update(state, 20L)
                    frames++
                }
                assertEquals("шаг не пройден: " + step.text, before + 1, rules.stepIndex)
            }
            assertTrue(rules.isDone)
        }
    }

    @Test
    fun pancakes_haveRestSteps_andLaundryHasStrongPress() {
        assertEquals(2, MixedConfigs.PANCAKES.steps.count { it.rest })
        assertTrue(MixedConfigs.LAUNDRY.steps.any { (it.zone?.first ?: 0) >= 60 })
    }

    @Test
    fun pancakes_startWithMixing_beforeFrying() {
        val steps = MixedConfigs.PANCAKES.steps
        val firstRest = steps.indexOfFirst { it.rest }
        val mixingSteps = steps.take(firstRest).count { it.text.contains("Замешивайте") }
        assertEquals(6, mixingSteps)
        assertTrue(steps.first().text.contains("венчик"))
    }
}
