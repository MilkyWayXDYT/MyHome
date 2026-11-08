package com.robotpaw.myhome.mission

import com.robotpaw.myhome.mission.mechanics.door.DoorRules
import com.robotpaw.myhome.signal.SignalState
import com.robotpaw.myhome.signal.calculateSignalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoorRulesTest {

    private fun state(flexor: Int, extensor: Int): SignalState = calculateSignalState(flexor, extensor)

    private val flex = state(80, 0)
    private val rest = state(0, 0)
    private val both = state(80, 80)

    @Test
    fun holdingFlexor_opensDoorAfterHoldTime() {
        val rules = DoorRules(holdMs = 3000L)
        var opened = false
        repeat(155) { if (rules.update(flex, 20L)) opened = true }   // чуть больше 3 с, запас на округление float
        assertTrue(opened)
        assertTrue(rules.isDone)
    }

    @Test
    fun shortHold_doesNotOpenDoor() {
        val rules = DoorRules(holdMs = 3000L)
        repeat(100) { rules.update(flex, 20L) }             // 2 с
        assertFalse(rules.isDone)
        assertEquals(2f / 3f, rules.progress, 0.01f)
    }

    @Test
    fun release_decaysProgressSlowerThanItGrows() {
        val rules = DoorRules(holdMs = 3000L, decayRate = 0.5f)
        repeat(75) { rules.update(flex, 20L) }              // 1,5 с, прогресс 0,5
        repeat(50) { rules.update(rest, 20L) }              // 1 с покоя
        assertEquals(0.5f - (1000f / 3000f) * 0.5f, rules.progress, 0.01f)
    }

    @Test
    fun coContraction_dropsProgressAtFullSpeed() {
        val rules = DoorRules(holdMs = 3000L)
        repeat(75) { rules.update(flex, 20L) }              // 0,5
        repeat(25) { rules.update(both, 20L) }              // 0,5 с встречного сигнала
        assertEquals(0.5f - 500f / 3000f, rules.progress, 0.01f)
    }

    @Test
    fun progress_neverGoesBelowZero() {
        val rules = DoorRules()
        repeat(500) { rules.update(both, 20L) }
        assertEquals(0f, rules.progress, 0f)
    }

    @Test
    fun restore_continuesFromSavedProgress() {
        val rules = DoorRules(holdMs = 3000L)
        rules.restore(0.9f)
        repeat(20) { rules.update(flex, 20L) }              // ещё 0,4 с, нужно только 0,3 с
        assertTrue(rules.isDone)
    }
}
