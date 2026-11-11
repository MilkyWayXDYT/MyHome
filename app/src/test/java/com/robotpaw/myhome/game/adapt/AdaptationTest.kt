package com.robotpaw.myhome.game.adapt

import com.robotpaw.myhome.game.MissionStats
import com.robotpaw.myhome.game.session.Fatigue
import com.robotpaw.myhome.mission.mechanics.adapted
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationConfigs
import com.robotpaw.myhome.mission.mechanics.precision.PrecisionConfigs
import com.robotpaw.myhome.mission.mechanics.rhythm.RhythmConfigs
import com.robotpaw.myhome.mission.mechanics.strength.StrengthHoldConfigs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptationTest {
    private fun stats(clean: Int = 95, mistakes: Int = 0, pauses: Int = 0, durationMs: Long = 20_000L) = MissionStats(
        durationMs = durationMs, cleanPercent = clean, mistakes = mistakes, interruptions = 0, wrongChannel = 0,
        averageStrength = 60, peakStrength = 80, bestStreak = 0, pauses = pauses, steadyPercent = 90
    )

    @Test
    fun levelMovesOneStep() {
        assertEquals(1, Adaptation.next(0, stats()))
        assertEquals(-1, Adaptation.next(0, stats(pauses = 1)))
        assertEquals(-1, Adaptation.next(0, stats(clean = 50)))
        assertEquals(-1, Adaptation.next(0, stats(mistakes = 4)))
        // средне - уровень не меняется
        assertEquals(0, Adaptation.next(0, stats(clean = 75, mistakes = 1)))
        assertEquals(Adaptation.MAX, Adaptation.next(Adaptation.MAX, stats()))
        assertEquals(Adaptation.MIN, Adaptation.next(Adaptation.MIN, stats(pauses = 2)))
        // заглушка без статистики
        assertEquals(1, Adaptation.next(1, stats(durationMs = 0L)))
    }

    @Test
    fun tiredIsEasier() {
        assertEquals(0, Adaptation.effective(1, Fatigue.MILD))
        assertEquals(1, Adaptation.effective(1, Fatigue.NONE))
        assertEquals(Adaptation.MIN, Adaptation.effective(Adaptation.MIN, Fatigue.TIRED))
    }

    @Test
    fun zoneStaysSane() {
        assertEquals(25..65, Adaptation.zone(20..70, 1))
        assertEquals(10..80, Adaptation.zone(20..70, -2))
        val narrow = Adaptation.zone(40..60, 2)
        assertTrue(narrow.last - narrow.first >= Adaptation.MIN_ZONE_WIDTH)
        assertTrue(Adaptation.zone(0..20, -2).first >= 0)
    }

    @Test
    fun configsScaled() {
        // уровень 0 - настройки как есть
        assertSame(AlternationConfigs.PICTURE, AlternationConfigs.PICTURE.adapted(0))
        val hard = AlternationConfigs.PICTURE.adapted(2)
        assertEquals(1560L, hard.steps[0].holdMs)
        assertEquals(30..60, hard.steps.last().zone)
        assertEquals(24_000L, hard.attemptLimitMs)
        val easy = StrengthHoldConfigs.VACUUM.adapted(-2)
        assertEquals(4200L, easy.holdMs)
        assertEquals(40, easy.minStrength)
        val rhythm = RhythmConfigs.SALAD.adapted(-1)
        assertEquals(1320L, rhythm.experiencedIntervalMs)
        assertEquals(3, rhythm.experiencedMaxMisses)
        // дверь: держать 3 с, окно поддержки через 20 с
        assertEquals(3900L, Adaptation.holdMs(3000L, 2))
        assertEquals(2100L, Adaptation.holdMs(3000L, -2))
        assertEquals(16_000L, Adaptation.limitMs(20_000L, 2))
        // кубики на +2
        val cubes = AlternationConfigs.CUBES.adapted(2)
        assertEquals(listOf(1300L, 2600L, 1300L), cubes.steps.take(3).map { it.holdMs })
        assertEquals(32_000L, cubes.attemptLimitMs)
        // тексты и каналы шагов не меняются
        val tea = PrecisionConfigs.TEA.adapted(1)
        assertEquals(PrecisionConfigs.TEA.steps.map { it.text to it.channel }, tea.steps.map { it.text to it.channel })
    }
}