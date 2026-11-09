package com.robotpaw.myhome.mission.mechanics.pet

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.PetKind
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationRules
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalState
import com.robotpaw.myhome.signal.calculateSignalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetConfigsTest {

    // сырой сигнал, при котором сила канала примерно равна нужной (при стандартной калибровке rest=5, max=100)
    private fun stateWithStrength(channel: Channel, strength: Int): SignalState {
        val raw = (strength * 95 / 100.0 + 5).toInt()
        return when (channel) {
            Channel.FLEXOR -> calculateSignalState(raw, 0)
            Channel.EXTENSOR -> calculateSignalState(0, raw)
        }
    }

    @Test
    fun everyPetTask_canBeCompleted_forEveryPet() {
        PetKind.entries.forEach { pet ->
            PetConfigs.all(pet).forEach { config ->
                val rules = AlternationRules(config.steps)
                config.steps.forEach { step ->
                    val state = stateWithStrength(step.channel, step.zone?.let { (it.first + it.last) / 2 } ?: 60)
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
    }

    @Test
    fun texts_dependOnPet() {
        assertTrue(PetConfigs.toy(PetKind.CAT).steps.first().text.contains("мышку"))
        assertTrue(PetConfigs.toy(PetKind.DOG).steps.first().text.contains("мячик"))
        assertTrue(PetConfigs.food(PetKind.DOG).steps.first().text.contains("собаки"))
    }

    @Test
    fun progress_remembersPet() {
        assertNull(GameProgress().pet)
        assertEquals(PetKind.DOG, GameProgress().withPet(PetKind.DOG).pet)
    }
}
