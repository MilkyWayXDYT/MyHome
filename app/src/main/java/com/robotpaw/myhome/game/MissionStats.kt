package com.robotpaw.myhome.game

/**
 * Статистика задания для экрана результата, считается из лога выполнения
 * @param mistakes - ошибки задания вместе со встречными сигналами
 * @param averageStrength - средняя сила нужного канала, пока он был активен (0..100)
 * @param bestStreak - лучшая серия попаданий подряд (только ритм-задания)
 */
data class MissionStats(
    val durationMs: Long,
    val cleanPercent: Int,
    val mistakes: Int,
    val averageStrength: Int,
    val peakStrength: Int,
    val bestStreak: Int,
    val pauses: Int
) {
    // у заданий-заглушек лог пустой, статистики у них нет
    val hasData: Boolean get() = durationMs > 0L

    val timeText: String get() = formatTime(durationMs)
}

fun MissionLog.toStats() = MissionStats(
    durationMs = durationMs,
    cleanPercent = cleanPercent,
    mistakes = errors + coContractionEvents,
    averageStrength = averageStrength,
    peakStrength = peakStrength,
    bestStreak = bestStreak,
    pauses = pauses
)

/**
 * Время для экрана: «4,2 с», «35 с», «1 мин 05 с»
 */
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return when {
        ms < 10_000L -> {
            val tenths = ms / 100
            "${tenths / 10},${tenths % 10} с"
        }
        totalSeconds < 60 -> "$totalSeconds с"
        else -> "${totalSeconds / 60} мин ${(totalSeconds % 60).toString().padStart(2, '0')} с"
    }
}
