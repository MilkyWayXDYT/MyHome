package com.robotpaw.myhome.game.adapt

import com.robotpaw.myhome.game.MissionStats
import com.robotpaw.myhome.game.session.Fatigue
import kotlin.math.roundToLong

/**
 * Адаптивная сложность заданий: у каждого задания свой уровень от MIN (легче) до MAX (сложнее)
 * После каждого прохождения уровень сдвигается на шаг: чисто и без ошибок - сложнее, с трудом - легче
 * Механики не меняются, подстраиваются только числа в их настройках (см. mission/mechanics/AdaptiveConfigs)
 */
object Adaptation {
    const val MIN = -2
    const val MAX = 2

    // «чисто»: без ошибок, без окна поддержки и с чистым сигналом
    const val GOOD_CLEAN = 85
    // «с трудом»: окно поддержки, грязный сигнал или много ошибок
    const val BAD_CLEAN = 60
    const val BAD_MISTAKES = 4

    // на сколько меняются числа за один уровень
    const val TIME_STEP = 0.15f        // время удержания: +15% за уровень сложнее
    const val LIMIT_STEP = 0.10f       // время на попытку до окна поддержки: -10% за уровень сложнее
    const val STRENGTH_STEP = 5        // минимальная сила: +5% за уровень сложнее
    const val ZONE_STEP = 5            // зона силы: уже на 5% с каждой стороны за уровень сложнее
    const val TEMPO_STEP = 0.10f       // ритм: интервал короче на 10% за уровень сложнее
    const val MIN_ZONE_WIDTH = 15      // уже этого зона не становится

    /**
     * Новый уровень задания после прохождения
     * @return уровень, сдвинутый не больше чем на шаг; у заглушек без статистики не меняется
     */
    fun next(level: Int, stats: MissionStats): Int {
        if (!stats.hasData) return level
        val good = stats.mistakes == 0 && stats.pauses == 0 && stats.cleanPercent >= GOOD_CLEAN
        val bad = stats.pauses > 0 || stats.cleanPercent < BAD_CLEAN || stats.mistakes >= BAD_MISTAKES
        val step = when {
            good -> 1
            bad -> -1
            else -> 0
        }
        return (level + step).coerceIn(MIN, MAX)
    }

    /**
     * С каким уровнем запускать задание сейчас: при усталости на ступень легче
     */
    fun effective(level: Int, fatigue: Fatigue): Int =
        (level - if (fatigue == Fatigue.NONE) 0 else 1).coerceIn(MIN, MAX)

    // время удержания шага
    fun holdMs(base: Long, level: Int): Long = (base * (1f + TIME_STEP * level)).roundToLong()

    // время на попытку до окна поддержки: сложнее - меньше
    fun limitMs(base: Long, level: Int): Long = (base * (1f - LIMIT_STEP * level)).roundToLong()

    // минимальная сила в процентах
    fun minStrength(base: Int, level: Int): Int = (base + STRENGTH_STEP * level).coerceIn(10, 90)

    /**
     * Зона силы: сложнее - уже, легче - шире, но не уже MIN_ZONE_WIDTH и в пределах 0..100
     */
    fun zone(base: IntRange, level: Int): IntRange {
        val shift = ZONE_STEP * level
        var from = (base.first + shift).coerceIn(0, 100)
        var to = (base.last - shift).coerceIn(0, 100)
        if (to - from < MIN_ZONE_WIDTH) {
            val middle = (base.first + base.last) / 2
            from = (middle - MIN_ZONE_WIDTH / 2).coerceAtLeast(0)
            to = (from + MIN_ZONE_WIDTH).coerceAtMost(100)
        }
        return from..to
    }

    // интервал между ударами ритма
    fun intervalMs(base: Long, level: Int): Long = (base * (1f - TEMPO_STEP * level)).roundToLong()

    // сколько промахов можно в ритме: легче - больше
    fun maxMisses(base: Int, level: Int): Int = (base - level).coerceAtLeast(0)

    // для игрока: «чуть легче», «как обычно», «сложнее»
    fun title(level: Int): String = when {
        level <= -2 -> "легче"
        level == -1 -> "чуть легче"
        level == 0 -> "как обычно"
        level == 1 -> "чуть сложнее"
        else -> "сложнее"
    }
}