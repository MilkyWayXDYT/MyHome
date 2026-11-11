package com.robotpaw.myhome.game.feedback

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.game.goals.DailyGoal
import com.robotpaw.myhome.game.goals.GoalKind
import com.robotpaw.myhome.game.history.History
import com.robotpaw.myhome.game.quests.QuestFeeling
import com.robotpaw.myhome.game.quests.RealQuest
import com.robotpaw.myhome.game.quests.Reflection
import com.robotpaw.myhome.game.save.KeyValueStorage
import com.robotpaw.myhome.game.save.SurveyRepository
import com.robotpaw.myhome.game.session.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// хранилище в памяти для теста опроса
private class SurveyMemoryStorage : KeyValueStorage {
    val strings = mutableMapOf<String, String>()
    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String?) {
        if (value == null) strings.remove(key) else strings[key] = value
    }
    override fun getStringSet(key: String): Set<String> = emptySet()
    override fun putStringSet(key: String, value: Set<String>) = Unit
}

class FeedbackTest {
    @Test
    fun answerText() {
        assertEquals(
            "понятно - да, всё понятно; рука - немного устала; интересно - скучно",
            SurveyAnswer("2026-11-11", listOf(0, 1, 2)).text
        )
    }

    @Test
    fun oneAnswerPerDay() {
        var list = SurveyRepository.withAnswer(emptyList(), SurveyAnswer("2026-11-10", listOf(0, 0, 0)))
        list = SurveyRepository.withAnswer(list, SurveyAnswer("2026-11-11", listOf(1, 1, 1)))
        list = SurveyRepository.withAnswer(list, SurveyAnswer("2026-11-11", listOf(2, 2, 2)))
        assertEquals(listOf("2026-11-10", "2026-11-11"), list.map { it.day })
        assertEquals(listOf(2, 2, 2), list.last().answers)
    }

    @Test
    fun saved() {
        val storage = SurveyMemoryStorage()
        val list = listOf(SurveyAnswer("2026-11-10", listOf(0, 1, 2)), SurveyAnswer("2026-11-11", listOf(1, 0, 0)))
        SurveyRepository(storage).save(list)
        assertEquals(list, SurveyRepository(storage).load())
    }

    @Test
    fun reportHasEverything() {
        val door = Rooms.TUTORIAL.missions.first()
        val progress = GameProgress().withCompleted(door).withLevel(door, 1)
            .withReflection(RealQuest.DOOR_OPEN, Reflection(QuestFeeling.NOT_YET))
        val session = Session("2026-11-11").withGoals(listOf(DailyGoal(GoalKind.FREEZE, done = true)))
            .withQuest(RealQuest.DOOR_OPEN).withQuestDone()
        val text = Report.build(
            "2026-11-11", "новичок", progress, session,
            History.sample("2026-11-11"), listOf(SurveyAnswer("2026-11-11", listOf(0, 1, 0)))
        )
        listOf(
            "Режим управления: новичок",
            "2026-11-11: понятно - да, всё понятно; рука - немного устала",
            "чистота", "Чистота сигнала с начала: +",
            "Цель «Замри»: выполнена",
            "Квест в жизни: Откройте дверь протезом - Не вышло",
            "Откройте дверь: +1"
        ).forEach { assertTrue(it, it in text) }
    }

    @Test
    fun emptyReport() {
        val text = Report.build("2026-11-11", "опытный", GameProgress(), Session("2026-11-11"), History(), emptyList())
        assertTrue("ответов пока нет" in text)
        assertTrue("тренировок не было" in text)
    }
}