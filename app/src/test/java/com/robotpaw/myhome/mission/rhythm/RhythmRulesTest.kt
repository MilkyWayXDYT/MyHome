package com.robotpaw.myhome.mission.mechanics.rhythm

import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalState
import com.robotpaw.myhome.signal.calculateSignalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmRulesTest {

    private val flex = calculateSignalState(80, 0)
    private val ext = calculateSignalState(0, 80)
    private val rest = calculateSignalState(0, 0)

    private val beats = listOf(Channel.FLEXOR, Channel.EXTENSOR, Channel.FLEXOR, Channel.EXTENSOR)
    private val framesPerBeat = 50                            // интервал 1000 мс, кадры по 20 мс

    private fun rules(maxMisses: Int = 1, countdownMs: Long = 0L) =
        RhythmRules(beats, intervalMs = 1000L, maxMisses = maxMisses, countdownMs = countdownMs)

    private fun stateFor(channel: Channel): SignalState = if (channel == Channel.FLEXOR) flex else ext

    // проходит один удар: нужный сигнал или покой (промах)
    private fun playBeat(rules: RhythmRules, channel: Channel, hit: Boolean) {
        repeat(framesPerBeat) { rules.update(if (hit) stateFor(channel) else rest, 20L) }
    }

    @Test
    fun hittingEveryBeat_succeedsWithFullStreak() {
        val rules = rules()
        beats.forEach { playBeat(rules, it, hit = true) }
        assertTrue(rules.isFinished)
        assertTrue(rules.isSuccess)
        assertEquals(4, rules.hits)
        assertEquals(0, rules.misses)
        assertEquals(4, rules.bestStreak)
        assertEquals(1f, rules.progress, 0f)
    }

    @Test
    fun missingEveryBeat_failsForExperienced() {
        val rules = rules(maxMisses = 1)
        beats.forEach { playBeat(rules, it, hit = false) }
        assertTrue(rules.isFinished)
        assertFalse(rules.isSuccess)
        assertEquals(4, rules.misses)
    }

    @Test
    fun missingEveryBeat_stillSucceedsWhenMissesAreUnlimited() {
        val rules = rules(maxMisses = Int.MAX_VALUE)
        beats.forEach { playBeat(rules, it, hit = false) }
        assertTrue(rules.isSuccess)
    }

    @Test
    fun miss_resetsStreak_butKeepsBestStreak() {
        val rules = rules(maxMisses = 2)
        playBeat(rules, beats[0], hit = true)
        playBeat(rules, beats[1], hit = true)
        playBeat(rules, beats[2], hit = false)
        playBeat(rules, beats[3], hit = true)
        assertEquals(1, rules.streak)
        assertEquals(2, rules.bestStreak)
        assertTrue(rules.isSuccess)
    }

    @Test
    fun wrongChannel_isNotAHit() {
        val rules = rules()
        repeat(framesPerBeat) { rules.update(ext, 20L) }       // первый удар ждёт сжатия
        assertEquals(0, rules.hits)
        assertEquals(1, rules.misses)
    }

    @Test
    fun shortTwitch_isNotAHit() {
        val rules = rules()
        repeat(3) { rules.update(flex, 20L) }                  // 60 мс, меньше 120 мс
        repeat(framesPerBeat - 3) { rules.update(rest, 20L) }
        assertEquals(0, rules.hits)
    }

    @Test
    fun countdown_delaysFirstBeat() {
        val rules = rules(countdownMs = 500L)
        repeat(25) { rules.update(flex, 20L) }                 // 500 мс подготовки, сигнал не засчитывается
        assertEquals(0, rules.hits)
        assertEquals(0, rules.beatIndex)
        repeat(10) { rules.update(flex, 20L) }                 // дальше идёт первый удар
        assertEquals(1, rules.hits)
    }

    @Test
    fun progress_neverGoesBack() {
        val rules = rules(maxMisses = 4)
        var last = 0f
        beats.forEach { channel ->
            repeat(framesPerBeat) {
                rules.update(rest, 20L)
                assertTrue(rules.progress >= last)
                last = rules.progress
            }
        }
    }
}
