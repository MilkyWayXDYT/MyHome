package com.robotpaw.myhome.game

/**
 * Лучший результат игрока в задании
 * Лучше тот, у кого меньше ошибок, а при равенстве - быстрее
 */
data class MissionRecord(
    val durationMs: Long,
    val mistakes: Int,
    val cleanPercent: Int
) {
    fun isBetterThan(other: MissionRecord): Boolean =
        mistakes < other.mistakes || (mistakes == other.mistakes && durationMs < other.durationMs)

    companion object {
        fun of(stats: MissionStats) = MissionRecord(stats.durationMs, stats.mistakes, stats.cleanPercent)
    }
}

/**
 * NONE - у задания нет статистики (заглушка), FIRST - первое прохождение,
 * NEW - побит прежний рекорд, NOT_BEATEN - рекорд остался прежним
 */
enum class RecordStatus { NONE, FIRST, NEW, NOT_BEATEN }

/**
 * Что показать на экране результата про рекорд
 * @param best - рекорд игрока после этого прохождения
 */
data class RecordOutcome(val status: RecordStatus, val best: MissionRecord? = null) {
    companion object {
        /**
         * @param previous - рекорд игрока до этого прохождения
         */
        fun of(result: MissionResult, previous: MissionRecord?): RecordOutcome {
            val stats = result.log.toStats()
            if (!stats.hasData) return RecordOutcome(RecordStatus.NONE)
            val now = MissionRecord.of(stats)
            return when {
                previous == null -> RecordOutcome(RecordStatus.FIRST, now)
                now.isBetterThan(previous) -> RecordOutcome(RecordStatus.NEW, now)
                else -> RecordOutcome(RecordStatus.NOT_BEATEN, previous)
            }
        }
    }
}

/**
 * Итоги комнаты по лучшим результатам её заданий
 * @param done - сколько обязательных заданий пройдено
 * @param total - сколько обязательных заданий в комнате
 * @param totalMs - суммарное время лучших результатов
 * @param mistakes - суммарные ошибки в лучших результатах
 * @param cleanPercent - средняя чистота сигнала (0..100)
 * @param stars - звёзды, заработанные в комнате
 * @param maxStars - сколько звёзд можно получить в комнате
 */
data class RoomSummary(
    val done: Int,
    val total: Int,
    val totalMs: Long,
    val mistakes: Int,
    val cleanPercent: Int,
    val stars: Int = 0,
    val maxStars: Int = 0
) {
    val hasData: Boolean get() = totalMs > 0L
}
