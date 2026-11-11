package com.robotpaw.myhome.game.save

import com.robotpaw.myhome.game.session.Session
import org.junit.Assert.assertEquals
import org.junit.Test

// хранилище в памяти для тестов сессии
private class SessionMemoryStorage : KeyValueStorage {
    val strings = mutableMapOf<String, String>()
    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String?) {
        if (value == null) strings.remove(key) else strings[key] = value
    }
    override fun getStringSet(key: String): Set<String> = emptySet()
    override fun putStringSet(key: String, value: Set<String>) = Unit
}

class SessionRepositoryTest {
    private val day = "2026-11-11"

    @Test
    fun emptyIsNewDay() {
        assertEquals(Session(day), SessionRepository(SessionMemoryStorage()).load(day))
    }

    @Test
    fun saveAndLoadSameDay() {
        val storage = SessionMemoryStorage()
        val saved = Session(day, done = 2, plan = 2, extra = 1, starsGained = 5, activeMs = 90_000L)
        SessionRepository(storage).save(saved)
        assertEquals(saved, SessionRepository(storage).load(day))
    }

    @Test
    fun nextDayStartsOver() {
        val storage = SessionMemoryStorage()
        SessionRepository(storage).save(Session(day, done = 3))
        assertEquals(Session("2026-11-12"), SessionRepository(storage).load("2026-11-12"))
    }

    @Test
    fun brokenIsNewDay() {
        val storage = SessionMemoryStorage()
        storage.putString(SessionRepository.KEY_SESSION, "v1|$day|мусор")
        assertEquals(Session(day), SessionRepository(storage).load(day))
    }
}

class SessionGoalsSaveTest {
    @Test
    fun goalsSurviveRestart() {
        val storage = SessionMemoryStorage()
        val goals = listOf(
            com.robotpaw.myhome.game.goals.DailyGoal(com.robotpaw.myhome.game.goals.GoalKind.FREEZE, tier = 1, challenge = true, done = true),
            com.robotpaw.myhome.game.goals.DailyGoal(com.robotpaw.myhome.game.goals.GoalKind.CLEAN)
        )
        val saved = Session("2026-11-11").withGoals(goals)
        SessionRepository(storage).save(saved)
        assertEquals(saved, SessionRepository(storage).load("2026-11-11"))
    }
}