package com.robotpaw.myhome.mission.mechanics.alternation

import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalState
import com.robotpaw.myhome.signal.calculateSignalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternationConfigsTest {

    // сырой сигнал, при котором сила канала примерно равна нужной (при стандартной калибровке rest=5, max=100)
    private fun stateWithStrength(channel: Channel, strength: Int): SignalState {
        val raw = (strength * 95 / 100.0 + 5).toInt()
        return when (channel) {
            Channel.FLEXOR -> calculateSignalState(raw, 0)
            Channel.EXTENSOR -> calculateSignalState(0, raw)
        }
    }

    @Test
    fun everyAlternationTask_canBeCompleted() {
        AlternationConfigs.ALL.forEach { config ->
            val rules = AlternationRules(config.steps)
            config.steps.forEach { step ->
                val strength = step.zone?.let { (it.first + it.last) / 2 } ?: 60
                val state = stateWithStrength(step.channel, strength)

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
    fun cubes_haveSixSteps_andTableTenSteps() {
        assertEquals(6, AlternationConfigs.CUBES.steps.size)
        assertEquals(10, AlternationConfigs.TABLE.steps.size)
    }
}
