package com.robotpaw.myhome.game

import org.junit.Assert.assertEquals
import org.junit.Test

class StarsTest {

    private val door = Missions.TUTORIAL[0]

    private fun result(errors: Int = 0, coEvents: Int = 0, coMs: Long = 0L, durationMs: Long = 10_000L) =
        MissionResult(
            door,
            MissionLog(durationMs = durationMs, errors = errors, coContractionEvents = coEvents, coContractionMs = coMs)
        )

    @Test
    fun threeStars_forFlawlessRun() {
        assertEquals(3, Stars.forStats(result().log.toStats()))
    }

    @Test
    fun twoStars_forCleanSignalWithMistakes() {
        // ошибка есть, но чистота 100%
        assertEquals(2, Stars.forStats(result(errors = 1).log.toStats()))
    }

    @Test
    fun oneStar_forDirtySignal() {
        // встречный сигнал полвремени: чистота 50%, есть ошибки
        assertEquals(1, Stars.forStats(result(coEvents = 2, coMs = 5000L).log.toStats()))
    }

    @Test
    fun stub_withoutStats_givesOneStar() {
        assertEquals(1, Stars.forStats(MissionLog().toStats()))
    }

    @Test
    fun repeatedRun_doesNotGiveStarsTwice_onlyImprovement() {
        val first = GameProgress().starsFor(result(coEvents = 2, coMs = 5000L))
        assertEquals(StarsOutcome(earned = 1, gained = 1, total = 1), first)

        val afterFirst = GameProgress().withResult(result(coEvents = 2, coMs = 5000L))
        val same = afterFirst.starsFor(result(coEvents = 2, coMs = 5000L))
        assertEquals(0, same.gained)
        assertEquals(1, same.total)

        val better = afterFirst.starsFor(result())
        assertEquals(StarsOutcome(earned = 3, gained = 2, total = 3), better)
    }

    @Test
    fun worseRun_doesNotLowerStars() {
        val progress = GameProgress().withResult(result()).withResult(result(coEvents = 2, coMs = 5000L))
        assertEquals(3, progress.starsOf(door))
        assertEquals(3, progress.totalStars)
    }

    @Test
    fun total_sumsBestOverMissions() {
        val cubes = Missions.TUTORIAL[1]
        val progress = GameProgress()
            .withResult(result())
            .withResult(MissionResult(cubes))
        assertEquals(4, progress.totalStars)               // 3 за дверь и 1 за кубики (заглушка)
    }

    @Test
    fun summary_countsRoomStars() {
        val progress = GameProgress().withResult(result())
        val summary = progress.summaryOf(Rooms.TUTORIAL)
        assertEquals(3, summary.stars)
        assertEquals(6, summary.maxStars)
    }

    @Test
    fun word_followsRussianRules() {
        assertEquals("звезда", Stars.word(1))
        assertEquals("звезды", Stars.word(2))
        assertEquals("звёзд", Stars.word(5))
        assertEquals("звёзд", Stars.word(11))
        assertEquals("звезда", Stars.word(21))
    }
}
