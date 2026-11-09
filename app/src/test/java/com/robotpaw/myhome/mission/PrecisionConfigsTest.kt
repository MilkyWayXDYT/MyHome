package com.robotpaw.myhome.mission.mechanics.precision

import com.robotpaw.myhome.mission.mechanics.alternation.AlternationConfig
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationRules
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationStep
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalState
import com.robotpaw.myhome.signal.calculateSignalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrecisionConfigsTest {

    // сырой сигнал, при котором сила канала примерно равна нужной (при стандартной калибровке rest=5, max=100)
    private fun stateWithStrength(channel: Channel, strength: Int): SignalState {
        val raw = (strength * 95 / 100.0 + 5).toInt()
        return when (channel) {
            Channel.FLEXOR -> calculateSignalState(raw, 0)
            Channel.EXTENSOR -> calculateSignalState(0, raw)
        }
    }

    private fun targetStrength(step: AlternationStep): Int =
        step.zone?.let { (it.first + it.last) / 2 } ?: 60

    @Test
    fun zones_areValid() {
        PrecisionConfigs.ALL.forEach { config ->
            config.steps.forEach { step ->
                val zone = step.zone ?: return@forEach
                assertTrue("зона " + zone, zone.first >= 30 && zone.last <= 100 && zone.first < zone.last)
            }
        }
    }

    @Test
    fun channels_alternateOnEveryStep() {
        PrecisionConfigs.ALL.forEach { config ->
            config.steps.zipWithNext().forEach { (a, b) ->
                assertTrue(a.channel != b.channel)
            }
        }
    }

    @Test
    fun everyTask_canBeCompletedInsideZones() {
        PrecisionConfigs.ALL.forEach { config ->
            val rules = AlternationRules(config.steps)
            config.steps.forEach { step ->
                val state = stateWithStrength(step.channel, targetStrength(step))
                // проверяем, что наш тестовый сигнал действительно попал в зону
                val strength = if (step.channel == Channel.FLEXOR) state.flexorStrength else state.extensorStrength
                step.zone?.let { assertTrue("сила $strength вне зоны $it", strength in it) }

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
    fun overshootInZoneStep_isError() {
        PrecisionConfigs.ALL.forEach { config ->
            val index = config.steps.indexOfFirst { it.zone != null }
            val step = config.steps[index]
            val rules = AlternationRules(config.steps)
            rules.restore((index + 0.01f) / config.steps.size)       // встали на этот шаг (запас от округления float)

            val hard = stateWithStrength(step.channel, 100)
            var overshoots = 0
            repeat(10) { if (rules.update(hard, 20L).overshoot) overshoots++ }
            assertEquals(1, overshoots)
        }
    }
}
