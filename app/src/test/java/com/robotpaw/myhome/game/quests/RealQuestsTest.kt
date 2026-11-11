package com.robotpaw.myhome.game.quests

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.MissionKind
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.game.save.KeyValueStorage
import com.robotpaw.myhome.game.save.ProgressRepository
import com.robotpaw.myhome.game.save.SessionRepository
import com.robotpaw.myhome.game.session.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// хранилище в памяти для тестов квестов
private class QuestMemoryStorage : KeyValueStorage {
    val strings = mutableMapOf<String, String>()
    val sets = mutableMapOf<String, Set<String>>()
    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String?) {
        if (value == null) strings.remove(key) else strings[key] = value
    }
    override fun getStringSet(key: String): Set<String> = sets[key] ?: emptySet()
    override fun putStringSet(key: String, value: Set<String>) { sets[key] = value }
}

class RealQuestsTest {
    private val door = Rooms.TUTORIAL.missions.first { it.kind == MissionKind.DOOR }
    private val all = Rooms.ALL.fold(GameProgress()) { p, r -> r.missions.fold(p) { q, m -> q.withCompleted(m) } }

    @Test
    fun unlockedAfterGameTask() {
        assertNull(RealQuests.pick(GameProgress()))
        assertEquals(RealQuest.DOOR_OPEN, RealQuests.pick(GameProgress().withCompleted(door)))
        assertEquals(12, RealQuests.unlocked(all).size)
    }

    @Test
    fun freshFirstThenHard() {
        // все прошли легко, кроме ложек - они и выпадут
        var p = all
        RealQuest.entries.forEach { p = p.withReflection(it, Reflection(QuestFeeling.EASY)) }
        p = p.withReflection(RealQuest.SPOONS, Reflection(QuestFeeling.NOT_YET))
        assertEquals(RealQuest.SPOONS, RealQuests.pick(p))
        // новый квест важнее трудного
        assertEquals(RealQuest.DOOR_OPEN, RealQuests.pick(p.copy(reflections = p.reflections - RealQuest.DOOR_OPEN)))
    }

    @Test
    fun notTheSameTwice() {
        assertEquals(RealQuest.SPOONS, RealQuests.pick(all, except = RealQuest.DOOR_OPEN))
        // если открыт только один - можно и его
        assertEquals(RealQuest.DOOR_OPEN, RealQuests.pick(GameProgress().withCompleted(door), except = RealQuest.DOOR_OPEN))
    }

    @Test
    fun practiceInOpenRoom() {
        assertEquals(MissionKind.TRASH, RealQuests.practiceFor(RealQuest.BOTTLE, all)?.kind)
        // кухня закрыта - тренироваться негде
        assertNull(RealQuests.practiceFor(RealQuest.BOTTLE, GameProgress()))
        assertTrue(Reflection(QuestFeeling.HARD).needsPractice)
        assertFalse(Reflection(QuestFeeling.EASY).needsPractice)
    }

    @Test
    fun notYetRaisesSkillInGoals() {
        val p = all.withReflection(RealQuest.PAPER_CUP, Reflection(QuestFeeling.NOT_YET))
            .withGoalTry(com.robotpaw.myhome.game.goals.GoalKind.CLEAN, false)
        val goals = com.robotpaw.myhome.game.goals.GoalPicker.pick(p, canWarmUp = true)
        assertEquals(com.robotpaw.myhome.game.goals.GoalKind.GENTLE, goals.first().kind)
    }

    @Test
    fun rewardOnlyOnce() {
        val s = Session("d").withQuest(RealQuest.DOOR_OPEN).withQuestDone()
        assertEquals(RealQuests.REWARD, s.starsGained)
        assertEquals(RealQuests.REWARD, s.withQuestDone().starsGained)
    }

    @Test
    fun saved() {
        val storage = QuestMemoryStorage()
        val p = GameProgress().withReflection(RealQuest.BOTTLE, Reflection(QuestFeeling.HARD))
        ProgressRepository(storage).save(p)
        assertEquals(p.reflections, ProgressRepository(storage).load().reflections)

        val s = Session("2026-11-11").withQuest(RealQuest.CUP).withQuestDone()
        SessionRepository(storage).save(s)
        assertEquals(s, SessionRepository(storage).load("2026-11-11"))
        val none = Session("2026-11-12")
        SessionRepository(storage).save(none)
        assertEquals(none, SessionRepository(storage).load("2026-11-12"))
    }
}