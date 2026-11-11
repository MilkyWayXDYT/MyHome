package com.robotpaw.myhome.game.save

import com.robotpaw.myhome.game.history.DayStats
import com.robotpaw.myhome.game.history.History

/**
 * Сохраняет историю тренировок по дням для экрана «Мой прогресс»
 * Формат: v1|день;заданий;чистота;встречные;ошибки;время;ровность;заданий с ровностью|...
 */
class HistoryRepository(private val storage: KeyValueStorage) {

    // нечитаемые дни пропускаются, остальная история остаётся
    fun load(): History {
        val parts = storage.getString(KEY_HISTORY)?.split("|") ?: return History()
        if (parts.firstOrNull() != VERSION) return History()
        return History(parts.drop(1).mapNotNull { parseDay(it) })
    }

    fun save(history: History) {
        storage.putString(
            KEY_HISTORY,
            if (history.days.isEmpty()) null
            else (listOf(VERSION) + history.days.map { encodeDay(it) }).joinToString("|")
        )
    }

    fun clear() = storage.putString(KEY_HISTORY, null)

    private fun encodeDay(d: DayStats) =
        listOf(d.day, d.tasks, d.cleanSum, d.coContractions, d.mistakes, d.activeMs, d.steadySum, d.steadyTasks).joinToString(";")

    private fun parseDay(text: String): DayStats? {
        val p = text.split(";")
        if (p.size != 8) return null
        return DayStats(
            day = p[0],
            tasks = p[1].toIntOrNull() ?: return null,
            cleanSum = p[2].toIntOrNull() ?: return null,
            coContractions = p[3].toIntOrNull() ?: return null,
            mistakes = p[4].toIntOrNull() ?: return null,
            activeMs = p[5].toLongOrNull() ?: return null,
            steadySum = p[6].toIntOrNull() ?: return null,
            steadyTasks = p[7].toIntOrNull() ?: return null
        )
    }

    companion object {
        const val KEY_HISTORY = "history"
        private const val VERSION = "v1"
    }
}