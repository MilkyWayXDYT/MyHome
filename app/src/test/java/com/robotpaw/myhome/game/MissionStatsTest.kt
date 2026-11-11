package com.robotpaw.myhome.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionStatsTest {

    private val door = Missions.TUTORIAL[0]

    private fun result(durationMs: Long, errors: Int = 0, coEvents: Int = 0, coMs: Long = 0L) =
        MissionResult(
            door,
            MissionLog(durationMs = durationMs, errors = errors, coContractionEvents = coEvents, coContractionMs = coMs)
        )

    @Test
    fun formatTime_shortMiddleAndLong() {
        assertEquals("4,2 с", formatTime(4200L))
        assertEquals("9,9 с", formatTime(9999L))
        assertEquals("35 с", formatTime(35_000L))
        assertEquals("1 мин 05 с", formatTime(65_000L))
    }

    @Test
    fun stats_sumErrorsAndCoContractions() {
        val stats = MissionLog(durationMs = 10_000L, errors = 1, coContractionEvents = 2, coContractionMs = 1000L).toStats()
        assertEquals(3, stats.mistakes)
        assertEquals(90, stats.cleanPercent)
        assertTrue(stats.hasData)
    }

    @Test
    fun emptyLog_hasNoStats() {
        assertFalse(MissionLog().toStats().hasData)
    }

    @Test
    fun record_fewerMistakesWins_thenSpeed() {
        val slowClean = MissionRecord(20_000L, 0, 100)
        val fastDirty = MissionRecord(5_000L, 2, 80)
        val fastClean = MissionRecord(10_000L, 0, 100)
        assertTrue(slowClean.isBetterThan(fastDirty))
        assertTrue(fastClean.isBetterThan(slowClean))
        assertFalse(slowClean.isBetterThan(slowClean))
    }

    @Test
    fun outcome_noData_first_new_notBeaten() {
        assertEquals(RecordStatus.NONE, RecordOutcome.of(MissionResult(door), null).status)

        val first = RecordOutcome.of(result(10_000L), null)
        assertEquals(RecordStatus.FIRST, first.status)

        val previous = first.best
        assertEquals(RecordStatus.NEW, RecordOutcome.of(result(8_000L), previous).status)
        assertEquals(RecordStatus.NOT_BEATEN, RecordOutcome.of(result(12_000L), previous).status)
        assertEquals(previous, RecordOutcome.of(result(12_000L), previous).best)
    }

    @Test
    fun withResult_keepsBestRecord_andCompletesMission() {
        val p1 = GameProgress().withResult(result(10_000L))
        assertTrue(p1.isCompleted(door))
        assertEquals(10_000L, p1.recordOf(door)?.durationMs)

        val worse = p1.withResult(result(15_000L))
        assertEquals(10_000L, worse.recordOf(door)?.durationMs)

        val better = worse.withResult(result(7_000L))
        assertEquals(7_000L, better.recordOf(door)?.durationMs)
    }

    @Test
    fun withResult_stubWithoutLog_completesButHasNoRecord() {
        val progress = GameProgress().withResult(MissionResult(door))
        assertTrue(progress.isCompleted(door))
        assertNull(progress.recordOf(door))
    }

    @Test
    fun summary_sumsBestResultsOfRoom() {
        val cubes = Missions.TUTORIAL[1]
        val progress = GameProgress()
            .withResult(result(10_000L, errors = 1, coMs = 0L))
            .withResult(MissionResult(cubes, MissionLog(durationMs = 20_000L, coContractionEvents = 1, coContractionMs = 2000L)))

        val summary = progress.summaryOf(Rooms.TUTORIAL)
        assertEquals(2, summary.done)
        assertEquals(2, summary.total)
        assertEquals(30_000L, summary.totalMs)
        assertEquals(2, summary.mistakes)
        assertEquals(95, summary.cleanPercent)            // среднее из 100 и 90
    }

    @Test
    fun stats_includeInterruptionsAndWrongChannel_inMistakesAndCleanPercent() {
        val stats = MissionLog(
            durationMs = 10_000L, interruptions = 2, wrongChannelEvents = 1, offTargetMs = 3000L
        ).toStats()
        assertEquals(3, stats.mistakes)
        assertEquals(2, stats.interruptions)
        assertEquals(1, stats.wrongChannel)
        assertEquals(70, stats.cleanPercent)
    }

    @Test
    fun unstableSignal_doesNotGiveThreeStars() {
        val stats = MissionLog(durationMs = 10_000L, interruptions = 3, offTargetMs = 4000L).toStats()
        assertEquals(1, Stars.forStats(stats))
    }

    @Test
    fun steadiness_reflectsJitter() {
        assertEquals(100, MissionLog().steadinessPercent)
        val log = MissionLog(durationMs = 1000L, jitterSum = 100L, jitterFrames = 50)    // средний скачок 2 -> 100 - 24
        assertEquals(76, log.steadinessPercent)
        assertEquals(76, log.toStats().steadyPercent)
    }
}
