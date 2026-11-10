package com.robotpaw.myhome.game.session

import com.robotpaw.myhome.game.MissionStats

/**
 * Насколько устала рука
 * @param restSeconds - сколько отдыхать на передышке (считается только время, пока рука расслаблена)
 */
enum class Fatigue(val title: String, val restSeconds: Int) {
    NONE("нет", 30),
    MILD("лёгкая", 45),
    TIRED("сильная", 90)
}

/**
 * Признак усталости, его видит игрок на передышке
 */
enum class FatigueSign(val text: String) {
    CLEAN_DROP("сигнал стал менее чистым"),
    STEADY_DROP("сила нажатия стала скакать"),
    STRENGTH_DROP("сила сжатия стала меньше"),
    INTERRUPTIONS("сигнал чаще срывается"),
    PAUSES("понадобилось окно поддержки"),
    LONG_PLAY("давно не было отдыха")
}

/**
 * Что делать после задания
 */
enum class SessionAdvice {
    CONTINUE,   // играть дальше
    REST,       // сначала передышка
    ENOUGH      // план на сегодня выполнен или рука устала: предложить закончить (лимит мягкий, можно продолжить)
}

/**
 * Пороги усталости. Последнее задание сравнивается с началом сессии (первые задания со статистикой),
 * задания разные, поэтому пороги с запасом
 */
object FatigueRules {
    const val BASELINE_SIZE = 2          // сколько первых заданий берём за норму
    const val CLEAN_DROP = 15            // чистота сигнала упала на столько процентов
    const val STEADY_DROP = 15           // ровность нажатия упала на столько процентов
    const val STRENGTH_LEFT = 0.75f      // пиковая сила меньше 75% от нормы
    const val MIN_BASE_STRENGTH = 20     // слабее этого силу не сравниваем
    const val INTERRUPTIONS_MORE = 3     // срывов на столько больше нормы
    const val LONG_PLAY_MS = 8 * 60_000L // столько в заданиях без передышки
    const val TIRED_SIGNS = 2            // с этого числа признаков - сильная усталость
}

/**
 * Признаки усталости по последнему заданию
 * @param history - статистика заданий сессии по порядку, последнее - только что пройденное (без заглушек)
 * @param sinceRestMs - время в заданиях после последнего отдыха
 */
fun fatigueSigns(history: List<MissionStats>, sinceRestMs: Long): Set<FatigueSign> {
    val signs = mutableSetOf<FatigueSign>()
    if (sinceRestMs >= FatigueRules.LONG_PLAY_MS) signs += FatigueSign.LONG_PLAY
    val last = history.lastOrNull() ?: return signs
    if (last.pauses > 0) signs += FatigueSign.PAUSES

    // сравнивать есть с чем, только если до последнего были задания
    val base = history.dropLast(1).take(FatigueRules.BASELINE_SIZE)
    if (base.isEmpty()) return signs
    val clean = base.map { it.cleanPercent }.average()
    val interruptions = base.map { it.interruptions }.average()
    if (clean - last.cleanPercent >= FatigueRules.CLEAN_DROP) signs += FatigueSign.CLEAN_DROP
    if (last.interruptions - interruptions >= FatigueRules.INTERRUPTIONS_MORE) signs += FatigueSign.INTERRUPTIONS

    // ровность и силу считаем только у заданий, где нужный канал был активен
    val pressed = base.filter { it.averageStrength > 0 }
    if (pressed.isNotEmpty() && last.averageStrength > 0) {
        val steady = pressed.map { it.steadyPercent }.average()
        val peak = pressed.map { it.peakStrength }.average()
        if (steady - last.steadyPercent >= FatigueRules.STEADY_DROP) signs += FatigueSign.STEADY_DROP
        if (peak >= FatigueRules.MIN_BASE_STRENGTH && last.peakStrength < peak * FatigueRules.STRENGTH_LEFT) {
            signs += FatigueSign.STRENGTH_DROP
        }
    }
    return signs
}

/**
 * Тренировка за день: сколько заданий сделано, есть ли усталость, что советовать дальше
 * Лимит мягкий: после плана игроку предлагают закончить, но он может взять ещё задание
 * @param day - дата «2026-11-11», с новым днём сессия начинается заново
 * @param done - заданий за день (обучение не считается)
 * @param plan - сколько заданий на сегодня, 1..3
 * @param extra - сколько заданий игрок взял сверх плана
 * @param starsGained - звёзд получено за день
 * @param activeMs - время в заданиях за день
 * @param sinceRestMs - время в заданиях после последнего отдыха (не сохраняется: новый запуск - рука отдохнула)
 * @param history - статистика заданий этого запуска для поиска усталости (не сохраняется)
 * @param forcedFatigue - усталость из отладочного меню, null - считается по заданиям
 */
data class Session(
    val day: String,
    val done: Int = 0,
    val plan: Int = DEFAULT_PLAN,
    val extra: Int = 0,
    val starsGained: Int = 0,
    val activeMs: Long = 0L,
    val sinceRestMs: Long = 0L,
    val history: List<MissionStats> = emptyList(),
    val forcedFatigue: Fatigue? = null
) {
    val signs: Set<FatigueSign> get() = fatigueSigns(history, sinceRestMs)

    val fatigue: Fatigue
        get() = forcedFatigue ?: when {
            signs.size >= FatigueRules.TIRED_SIGNS -> Fatigue.TIRED
            signs.isNotEmpty() -> Fatigue.MILD
            else -> Fatigue.NONE
        }

    val isPlanDone: Boolean get() = done >= plan

    /**
     * Задание пройдено
     * @param stars - сколько новых звёзд получено
     * @param countsForPlan - false для обучения: оно не тратит план на день
     */
    fun withResult(stats: MissionStats, stars: Int, countsForPlan: Boolean = true) = copy(
        done = if (countsForPlan) done + 1 else done,
        starsGained = starsGained + stars,
        activeMs = activeMs + stats.durationMs,
        sinceRestMs = sinceRestMs + stats.durationMs,
        history = if (stats.hasData) history + stats else history
    )

    /**
     * Игрок отдохнул: время без отдыха обнуляется, норма сигнала остаётся прежней
     */
    fun withRest() = copy(sinceRestMs = 0L, forcedFatigue = null)

    // игрок решил сделать ещё одно задание сверх плана
    fun withExtra() = copy(extra = extra + 1)

    fun withPlan(count: Int) = copy(plan = count.coerceIn(1, MAX_PLAN))

    /**
     * Совет после задания. При сильной усталости - закончить сразу (по правилам команды при усталости одна миссия)
     */
    fun advice(): SessionAdvice = when {
        fatigue == Fatigue.TIRED -> SessionAdvice.ENOUGH
        done >= plan + extra -> SessionAdvice.ENOUGH
        fatigue == Fatigue.MILD -> SessionAdvice.REST
        else -> SessionAdvice.CONTINUE
    }

    companion object {
        const val DEFAULT_PLAN = 3
        const val MAX_PLAN = 3
    }
}