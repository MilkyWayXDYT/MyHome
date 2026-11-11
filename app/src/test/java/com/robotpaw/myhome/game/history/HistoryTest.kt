package com.robotpaw.myhome.game.history

import com.robotpaw.myhome.game.MissionLog
import com.robotpaw.myhome.game.save.HistoryRepository
import com.robotpaw.myhome.game.save.KeyValueStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// хранилище в памяти для теста истории
private class HistoryMemoryStorage : KeyValueStorage {
    val strings = mutableMapOf<String, String>()
    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String?) {
        if (value == null) strings.remove(key) else strings[key] = value
    }
    override fun getStringSet(key: String): Set<String> = emptySet()
    override fun putStringSet(key: String, value: Set<String>) = Unit
}

class HistoryTest {
    // 20 с, из них 4 с встречного сигнала (чистота 80%), два встречных сигнала
    private val log = MissionLog(durationMs = 20_000L, coContractionMs = 4_000L, coContractionEvents = 2, strengthFrames = 10, strengthSum = 500)

    @Test
    fun sumsPerDay() {
        val h = History().withResult("2026-11-10", log).withResult("2026-11-11", log).withResult("2026-11-11", log.copy(coContractionMs = 0L, coContractionEvents = 0))
        assertEquals(2, h.days.size)
        val today = h.days.last()
        assertEquals(2, today.tasks)
        assertEquals(90, today.clean)
        assertEquals(1f, today.coPerTask)
        assertEquals(3, h.totalTasks)
        // заглушка без статистики не считается
        assertEquals(h, h.withResult("2026-11-11", MissionLog()))
    }

    @Test
    fun weekWithEmptyDays() {
        val h = History().withResult("2026-11-05", log).withResult("2026-11-11", log)
        val week = h.lastDays("2026-11-11")
        assertEquals(7, week.size)
        assertEquals("2026-11-05", week.first().day)
        assertEquals(listOf(1, 0, 0, 0, 0, 0, 1), week.map { it.tasks })
    }

    @Test
    fun streak() {
        var h = History()
        listOf("2026-11-08", "2026-11-09", "2026-11-10").forEach { h = h.withResult(it, log) }
        // сегодня ещё не занимались - серия с вчерашнего дня не прервалась
        assertEquals(3, h.streak("2026-11-11"))
        assertEquals(4, h.withResult("2026-11-11", log).streak("2026-11-11"))
        assertEquals(0, h.streak("2026-11-13"))
    }

    @Test
    fun keepsMonthAndChange() {
        var h = History()
        val start = java.time.LocalDate.parse("2026-10-01")
        repeat(40) { h = h.withResult(start.plusDays(it.toLong()).toString(), log) }
        assertEquals(History.KEEP_DAYS, h.days.size)
        assertEquals(0, h.cleanChange())
        assertNull(History().withResult("2026-11-11", log).cleanChange())
        val sample = History.sample("2026-11-11")
        assertTrue(sample.cleanChange()!! > 0)
        assertEquals(7, sample.streak("2026-11-11"))
    }

    @Test
    fun saved() {
        val storage = HistoryMemoryStorage()
        val h = History.sample("2026-11-11")
        HistoryRepository(storage).save(h)
        assertEquals(h, HistoryRepository(storage).load())
        HistoryRepository(storage).save(History())
        assertEquals(History(), HistoryRepository(storage).load())
    }
}