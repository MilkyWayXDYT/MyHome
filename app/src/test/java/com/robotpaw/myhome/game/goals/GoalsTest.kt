package com.robotpaw.myhome.game.goals

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.MissionKind
import com.robotpaw.myhome.game.MissionLog
import com.robotpaw.myhome.game.MissionRecord
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.game.session.Fatigue
import com.robotpaw.myhome.game.session.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GoalsTest {
    private val clean = MissionLog(durationMs = 20_000L, activeMs = 10_000L)

    private fun ctx(kind: MissionKind, log: MissionLog = clean, record: Boolean = false, streak: Int = 0) =
        GoalContext(kind, log, newRecord = record, flawlessStreak = streak)

    @Test
    fun checksOverLog() {
        assertTrue(GoalKind.CLEAN.isMet(ctx(MissionKind.DOOR), 0))
        assertFalse(GoalKind.CLEAN.isMet(ctx(MissionKind.DOOR, clean.copy(coContractionEvents = 1)), 0))
        assertTrue(GoalKind.NO_SPILL.isMet(ctx(MissionKind.TEA), 0))
        assertFalse(GoalKind.NO_SPILL.isMet(ctx(MissionKind.TEA, clean.copy(errors = 1)), 0))
        // не то задание - не считается
        assertFalse(GoalKind.NO_SPILL.isMet(ctx(MissionKind.DOOR), 0))
        assertFalse(GoalKind.IRON_GRIP.isMet(ctx(MissionKind.VACUUM, clean.copy(interruptions = 1)), 0))
        assertTrue(GoalKind.QUICK.isMet(ctx(MissionKind.TABLE), 20))
        assertFalse(GoalKind.QUICK.isMet(ctx(MissionKind.TABLE), 15))
        assertTrue(GoalKind.RHYTHM.isMet(ctx(MissionKind.SALAD, clean.copy(bestStreak = 6)), 6))
        assertTrue(GoalKind.FREEZE.isMet(ctx(MissionKind.CUBES, clean.copy(longestRestMs = 3_200L)), 3))
        assertFalse(GoalKind.FREEZE.isMet(ctx(MissionKind.CUBES, clean.copy(longestRestMs = 3_200L)), 5))
        assertTrue(GoalKind.FLAWLESS.isMet(ctx(MissionKind.DOOR, streak = 3), 3))
        assertTrue(GoalKind.RECORD.isMet(ctx(MissionKind.DOOR, record = true), 0))
        // заглушка без статистики не выполняет «Чистую работу»
        assertFalse(GoalKind.CLEAN.isMet(ctx(MissionKind.DOOR, MissionLog()), 0))
    }

    @Test
    fun firstDayOnlyTutorialGoals() {
        val pool = GoalPicker.available(GameProgress(), canWarmUp = false)
        assertEquals(setOf(GoalKind.CLEAN, GoalKind.FREEZE, GoalKind.FLAWLESS), pool.toSet())
        val goals = GoalPicker.pick(GameProgress(), canWarmUp = false, random = Random(1))
        assertEquals(3, goals.size)
        assertEquals(1, goals.count { it.challenge })
        assertEquals(3, goals.map { it.kind }.toSet().size)
    }

    @Test
    fun weakestSkillFirst() {
        var p = GameProgress().withGoalTry(GoalKind.CLEAN, true).withGoalTry(GoalKind.FREEZE, false)
        p = p.withGoalTry(GoalKind.FLAWLESS, true)
        val goals = GoalPicker.pick(p, canWarmUp = true, random = Random(7))
        assertEquals(GoalKind.FREEZE, goals.first().kind)
    }

    @Test
    fun tiredOnlyOneGoal() {
        val goals = GoalPicker.pick(GameProgress(), canWarmUp = true, tired = true)
        assertEquals(1, goals.size)
        assertFalse(goals.first().challenge)
    }

    @Test
    fun openRoomsAndRecords() {
        val all = Rooms.ALL.fold(GameProgress()) { p, r -> r.missions.fold(p) { q, m -> q.withCompleted(m) } }
        val pool = GoalPicker.available(all, canWarmUp = true)
        assertTrue(GoalKind.WARM_UP in pool)
        assertFalse(GoalKind.RECORD in pool)
        assertFalse(GoalKind.BOTH in pool)
        val withRecord = all.copy(records = mapOf("tutorial_door" to MissionRecord(5000L, 0, 100)))
        assertTrue(GoalKind.RECORD in GoalPicker.available(withRecord, canWarmUp = false))
        assertEquals(MissionKind.FLOWER, GoalPicker.missionFor(GoalKind.NO_SPILL, all)?.kind)
        assertNull(GoalPicker.missionFor(GoalKind.NO_SPILL, GameProgress()))
    }

    @Test
    fun tierGrowsWithWins() {
        var p = GameProgress()
        repeat(GoalPicker.WINS_PER_TIER) { p = p.withGoalTry(GoalKind.FREEZE, true) }
        assertEquals(1, GoalPicker.tierOf(GoalKind.FREEZE, p))
        repeat(20) { p = p.withGoalTry(GoalKind.FREEZE, true) }
        assertEquals(2, GoalPicker.tierOf(GoalKind.FREEZE, p))
    }

    @Test
    fun sessionMarksGoalsAndStars() {
        val s = Session("d").withGoals(
            listOf(DailyGoal(GoalKind.CLEAN), DailyGoal(GoalKind.FREEZE, challenge = true), DailyGoal(GoalKind.WARM_UP))
        )
        assertEquals(3, s.plan)
        val (after, newly) = s.withGoalsChecked(ctx(MissionKind.CUBES, clean.copy(longestRestMs = 9_000L)))
        assertEquals(listOf(GoalKind.CLEAN, GoalKind.FREEZE), newly.map { it.kind })
        assertEquals(3, after.starsGained)
        // повторно не засчитывается
        assertTrue(after.withGoalsChecked(ctx(MissionKind.CUBES)).second.isEmpty())
        val (warm, w) = after.withWarmUpDone()
        assertEquals(1, w.size)
        assertTrue(warm.isGoalsDone)
    }

    @Test
    fun challengePostponedWhenTired() {
        val s = Session("d", forcedFatigue = Fatigue.MILD).withGoals(listOf(DailyGoal(GoalKind.CLEAN, challenge = true)))
        assertTrue(s.withGoalsChecked(ctx(MissionKind.DOOR)).second.isEmpty())
    }
}

// хранилище в памяти для теста сохранения целей
private class GoalsMemoryStorage : com.robotpaw.myhome.game.save.KeyValueStorage {
    val strings = mutableMapOf<String, String>()
    val sets = mutableMapOf<String, Set<String>>()
    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String?) {
        if (value == null) strings.remove(key) else strings[key] = value
    }
    override fun getStringSet(key: String): Set<String> = sets[key] ?: emptySet()
    override fun putStringSet(key: String, value: Set<String>) { sets[key] = value }
}

class GoalsSaveTest {
    @Test
    fun bonusStarsAndTriesSaved() {
        val storage = GoalsMemoryStorage()
        val p = GameProgress().withBonusStars(4).withGoalTry(GoalKind.CLEAN, true).withGoalTry(GoalKind.CLEAN, false)
        com.robotpaw.myhome.game.save.ProgressRepository(storage).save(p)
        val loaded = com.robotpaw.myhome.game.save.ProgressRepository(storage).load()
        assertEquals(4, loaded.bonusStars)
        assertEquals(4, loaded.totalStars)
        assertEquals(GoalTries(2, 1), loaded.goalTriesOf(GoalKind.CLEAN))
        // сброс прогресса стирает и это
        com.robotpaw.myhome.game.save.ProgressRepository(storage).save(GameProgress())
        assertEquals(GameProgress(), com.robotpaw.myhome.game.save.ProgressRepository(storage).load())
    }
}