package com.robotpaw.myhome.game

/**
 * Награды-звёзды за задания
 * 1 звезда - задание пройдено, 2 - чистота сигнала не ниже 80%, 3 - ещё и без ошибок
 * У заданий без статистики (заглушки) всегда одна звезда
 */
object Stars {
    const val MAX = 3
    const val CLEAN_PERCENT_FOR_SECOND = 80

    fun forStats(stats: MissionStats): Int {
        if (!stats.hasData) return 1
        var stars = 1
        if (stats.cleanPercent >= CLEAN_PERCENT_FOR_SECOND) stars++
        if (stats.mistakes == 0) stars++
        return stars
    }

    // «1 звезда», «2 звезды», «5 звёзд»
    fun word(count: Int): String {
        val lastTwo = count % 100
        val last = count % 10
        return when {
            lastTwo in 11..14 -> "звёзд"
            last == 1 -> "звезда"
            last in 2..4 -> "звезды"
            else -> "звёзд"
        }
    }
}

/**
 * Что показать про звёзды на экране результата
 * @param earned - сколько звёзд заработано в этом прохождении (0..3)
 * @param gained - сколько из них новые: за ту же задачу звёзды не начисляются повторно, только прибавка к лучшему
 * @param total - всего звёзд у игрока после этого прохождения
 */
data class StarsOutcome(val earned: Int, val gained: Int, val total: Int)
