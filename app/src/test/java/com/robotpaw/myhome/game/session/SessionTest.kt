package com.robotpaw.myhome.game.session

import com.robotpaw.myhome.game.MissionStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    // обычное задание: чистый ровный сигнал
    private fun stats(
        clean: Int = 95,
        steady: Int = 90,
        peak: Int = 80,
        interruptions: Int = 0,
        pauses: Int = 0,
        durationMs: Long = 30_000L
    ) = MissionStats(
        durationMs = durationMs, cleanPercent = clean, mistakes = 0, interruptions = interruptions,
        wrongChannel = 0, averageStrength = 60, peakStrength = peak, bestStreak = 0, pauses = pauses, steadyPercent = steady
    )

    private fun play(vararg list: MissionStats): Session =
        list.fold(Session("2026-11-11")) { s, st -> s.withResult(st, 1) }

    @Test
    fun freshHandContinues() {
        val s = play(stats(), stats())
        assertEquals(Fatigue.NONE, s.fatigue)
        assertEquals(SessionAdvice.CONTINUE, s.advice())
    }

    @Test
    fun planReachedSaysEnough() {
        val s = play(stats(), stats(), stats())
        assertTrue(s.isPlanDone)
        assertEquals(SessionAdvice.ENOUGH, s.advice())
        // лимит мягкий: взял ещё одно - можно играть
        assertEquals(SessionAdvice.CONTINUE, s.withExtra().advice())
        assertEquals(SessionAdvice.ENOUGH, s.withExtra().withResult(stats(), 0).advice())
    }

    @Test
    fun oneSignIsMildRest() {
        val s = play(stats(), stats(steady = 70))
        assertEquals(setOf(FatigueSign.STEADY_DROP), s.signs)
        assertEquals(Fatigue.MILD, s.fatigue)
        assertEquals(SessionAdvice.REST, s.advice())
    }

    @Test
    fun twoSignsIsTiredEnough() {
        val s = play(stats(), stats(clean = 75, peak = 50))
        assertEquals(setOf(FatigueSign.CLEAN_DROP, FatigueSign.STRENGTH_DROP), s.signs)
        assertEquals(Fatigue.TIRED, s.fatigue)
        assertEquals(SessionAdvice.ENOUGH, s.advice())
    }

    @Test
    fun supportWindowAndInterruptions() {
        assertEquals(setOf(FatigueSign.PAUSES), play(stats(pauses = 1)).signs)
        assertEquals(setOf(FatigueSign.INTERRUPTIONS), play(stats(), stats(interruptions = 3)).signs)
    }

    @Test
    fun longPlayNeedsRestAndRestClearsIt() {
        val s = play(*Array(2) { stats(durationMs = 4 * 60_000L) })
        assertEquals(setOf(FatigueSign.LONG_PLAY), s.signs)
        assertEquals(Fatigue.NONE, s.withRest().fatigue)
    }

    @Test
    fun tutorialAndStubs() {
        val s = Session("d").withResult(stats(), 1, countsForPlan = false)
            .withResult(stats(durationMs = 0L), 1)
        assertEquals(1, s.done)
        assertEquals(1, s.history.size)
        assertEquals(2, s.starsGained)
    }

    @Test
    fun forcedFatigueUntilRest() {
        val s = Session("d").copy(forcedFatigue = Fatigue.TIRED)
        assertEquals(SessionAdvice.ENOUGH, s.advice())
        assertEquals(Fatigue.NONE, s.withRest().fatigue)
    }
}