package com.robotpaw.myhome.game.history

import com.robotpaw.myhome.game.MissionLog
import java.time.LocalDate

/**
 * Итоги одного дня тренировок для экрана «Мой прогресс»
 * Хранятся суммы, средние считаются при показе
 * @param day - дата «2026-11-11»
 * @param tasks - заданий со статистикой за день
 * @param cleanSum - сумма чистоты сигнала по заданиям
 * @param coContractions - встречные сигналы (обе мышцы сразу) за день
 * @param mistakes - все ошибки за день
 * @param activeMs - время в заданиях
 * @param steadySum - сумма ровности нажатия по заданиям, где нужный канал был активен
 * @param steadyTasks - сколько таких заданий
 */
data class DayStats(
    val day: String,
    val tasks: Int = 0,
    val cleanSum: Int = 0,
    val coContractions: Int = 0,
    val mistakes: Int = 0,
    val activeMs: Long = 0L,
    val steadySum: Int = 0,
    val steadyTasks: Int = 0
) {
    val clean: Int get() = if (tasks == 0) 0 else cleanSum / tasks
    val steady: Int get() = if (steadyTasks == 0) 0 else steadySum / steadyTasks

    // встречных сигналов в среднем на задание, с одним знаком после запятой
    val coPerTask: Float get() = if (tasks == 0) 0f else coContractions.toFloat() / tasks

    fun with(log: MissionLog): DayStats {
        val pressed = log.strengthFrames > 0
        return copy(
            tasks = tasks + 1,
            cleanSum = cleanSum + log.cleanPercent,
            coContractions = coContractions + log.coContractionEvents,
            mistakes = mistakes + log.mistakes,
            activeMs = activeMs + log.durationMs,
            steadySum = if (pressed) steadySum + log.steadinessPercent else steadySum,
            steadyTasks = if (pressed) steadyTasks + 1 else steadyTasks
        )
    }
}

/**
 * История тренировок по дням (последние KEEP_DAYS дней, в которые были задания)
 */
data class History(val days: List<DayStats> = emptyList()) {

    /**
     * Задание пройдено: добавить в итоги дня. Заглушки без статистики не считаются
     */
    fun withResult(day: String, log: MissionLog): History {
        if (log.durationMs <= 0L) return this
        val today = days.lastOrNull()?.takeIf { it.day == day } ?: DayStats(day)
        val others = days.filter { it.day != day }
        return History((others + today.with(log)).sortedBy { it.day }.takeLast(KEEP_DAYS))
    }

    /**
     * Последние count дней по календарю, заканчивая today; дни без тренировок - пустые
     */
    fun lastDays(today: String, count: Int = WEEK): List<DayStats> {
        val end = LocalDate.parse(today)
        val byDay = days.associateBy { it.day }
        return (count - 1 downTo 0).map { back ->
            val day = end.minusDays(back.toLong()).toString()
            byDay[day] ?: DayStats(day)
        }
    }

    // дней подряд с тренировкой, включая сегодня (или вчера, если сегодня ещё не занимались)
    fun streak(today: String): Int {
        val trained = days.filter { it.tasks > 0 }.map { it.day }.toSet()
        var day = LocalDate.parse(today)
        if (day.toString() !in trained) day = day.minusDays(1)
        var count = 0
        while (day.toString() in trained) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    val totalTasks: Int get() = days.sumOf { it.tasks }

    /**
     * Насколько изменилась чистота сигнала: последние дни против первых (null - мало данных)
     * Сравниваем средние по первым и последним COMPARE_DAYS дням с заданиями
     */
    fun cleanChange(): Int? {
        val trained = days.filter { it.tasks > 0 }
        if (trained.size < COMPARE_DAYS * 2) return null
        val first = trained.take(COMPARE_DAYS).let { d -> d.sumOf { it.cleanSum } / d.sumOf { it.tasks } }
        val last = trained.takeLast(COMPARE_DAYS).let { d -> d.sumOf { it.cleanSum } / d.sumOf { it.tasks } }
        return last - first
    }

    companion object {
        const val KEEP_DAYS = 30
        const val WEEK = 7
        const val COMPARE_DAYS = 2

        /**
         * Пример истории для отладки и показа на защите: неделя, сигнал чище с каждым днём
         */
        fun sample(today: String): History {
            val end = LocalDate.parse(today)
            return History((6 downTo 0).map { back ->
                val i = 6 - back
                val tasks = 2 + i % 2
                DayStats(
                    day = end.minusDays(back.toLong()).toString(),
                    tasks = tasks,
                    cleanSum = (62 + i * 5) * tasks,
                    coContractions = (6 - i).coerceAtLeast(0) * tasks / 2,
                    mistakes = (7 - i) * tasks / 2,
                    activeMs = tasks * 45_000L,
                    steadySum = (60 + i * 4) * tasks,
                    steadyTasks = tasks
                )
            })
        }
    }
}