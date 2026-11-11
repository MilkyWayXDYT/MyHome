package com.robotpaw.myhome.game.chores

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.MissionKind
import com.robotpaw.myhome.game.RoomKind
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.developer.withStarsOf
import com.robotpaw.myhome.game.quests.QuestFeeling
import com.robotpaw.myhome.game.quests.RealQuest
import com.robotpaw.myhome.game.quests.Reflection
import com.robotpaw.myhome.game.save.KeyValueStorage
import com.robotpaw.myhome.game.save.ProgressRepository
import com.robotpaw.myhome.game.save.SessionRepository
import com.robotpaw.myhome.game.session.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

// хранилище в памяти для теста дел
private class ChoresMemoryStorage : KeyValueStorage {
    val strings = mutableMapOf<String, String>()
    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String?) {
        if (value == null) strings.remove(key) else strings[key] = value
    }
    override fun getStringSet(key: String): Set<String> = emptySet()
    override fun putStringSet(key: String, value: Set<String>) = Unit
}

class ChoresTest {
    private val all = Rooms.ALL.fold(GameProgress()) { p, r -> r.missions.fold(p) { q, m -> q.withCompleted(m) } }
    private val missions = Rooms.ALL.flatMap { it.missions }.associateBy { it.id }

    @Test
    fun storyDone() {
        assertFalse(GameProgress().isStoryDone)
        assertTrue(all.isStoryDone)
    }

    @Test
    fun noTutorialAndNoPetChoice() {
        val ids = Chores.candidates(all).map { it.id }.toSet()
        assertTrue(Rooms.TUTORIAL.missions.none { it.id in ids })
        assertTrue(Chores.candidates(all).none { it.kind == MissionKind.PET_CHOICE })
        assertTrue(Chores.candidates(all).none { Rooms.of(it).kind == RoomKind.TUTORIAL })
    }

    @Test
    fun alwaysThreeWeakest() {
        // у всех по 3 звезды, у пылесоса и чая меньше - они слабее
        var p = all
        Chores.candidates(all).forEach { p = p.withStarsOf(it, 3) }
        p = p.withStarsOf(missions.getValue("living_vacuum"), 1)
        p = p.copy(stars = p.stars + ("kitchen_tea" to 2))
        val chores = Chores.topUp(p, Random(3))
        assertEquals(Chores.COUNT, chores.size)
        assertEquals("living_vacuum", chores[0])
        assertEquals("kitchen_tea", chores[1])
    }

    @Test
    fun failedQuestFirst() {
        val p = all.withReflection(RealQuest.TEMPO, Reflection(QuestFeeling.NOT_YET))
        assertEquals(MissionKind.SALAD, missions.getValue(Chores.topUp(p).first()).kind)
    }

    @Test
    fun unfinishedCarryOver() {
        val first = Chores.topUp(all, Random(1))
        // сделали одно дело - на следующий день два остаются, третье новое
        val next = Chores.topUp(all.withChores(first).withChoreDone(first[0]), Random(2))
        assertEquals(Chores.COUNT, next.size)
        assertEquals(first.drop(1), next.take(2))
    }

    @Test
    fun doneOnceWithStar() {
        val s = Session("d").withChores(listOf("living_flower", "living_pillows"))
        assertEquals(setOf("living_flower", "living_pillows"), s.pendingChores)
        val (after, chore) = s.withChoreDone("living_flower")
        assertTrue(chore)
        assertEquals(Chores.REWARD, after.starsGained)
        assertEquals(setOf("living_pillows"), after.pendingChores)
        assertFalse(after.withChoreDone("living_flower").second)
        assertFalse(after.withChoreDone("living_vacuum").second)
        assertTrue(after.withChoreDone("living_pillows").first.isChoresDone)
    }

    @Test
    fun saved() {
        val storage = ChoresMemoryStorage()
        val s = Session("2026-11-11").withChores(listOf("living_flower", "living_pillows")).withChoreDone("living_flower").first
        SessionRepository(storage).save(s)
        assertEquals(s, SessionRepository(storage).load("2026-11-11"))
        // новый день - дела заново
        assertTrue(SessionRepository(storage).load("2026-11-12").chores.isEmpty())
        // несделанные дела хранятся в прогрессе и переживают новый день
        val sets = mutableMapOf<String, Set<String>>()
        val progressStorage = object : KeyValueStorage by storage {
            override fun getStringSet(key: String): Set<String> = sets[key] ?: emptySet()
            override fun putStringSet(key: String, value: Set<String>) { sets[key] = value }
        }
        ProgressRepository(progressStorage).save(all.withChores(listOf("living_pillows", "kitchen_tea")))
        assertEquals(listOf("living_pillows", "kitchen_tea"), ProgressRepository(progressStorage).load().chores)
    }
}