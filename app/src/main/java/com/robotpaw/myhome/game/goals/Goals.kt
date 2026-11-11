package com.robotpaw.myhome.game.goals

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.MissionKind
import com.robotpaw.myhome.game.MissionKind.CLOTHESPINS
import com.robotpaw.myhome.game.MissionKind.CUBES
import com.robotpaw.myhome.game.MissionKind.FLOWER
import com.robotpaw.myhome.game.MissionKind.MIRROR
import com.robotpaw.myhome.game.MissionKind.PANCAKES
import com.robotpaw.myhome.game.MissionKind.PICTURE
import com.robotpaw.myhome.game.MissionKind.PILLOWS
import com.robotpaw.myhome.game.MissionKind.SALAD
import com.robotpaw.myhome.game.MissionKind.TABLE
import com.robotpaw.myhome.game.MissionKind.TEA
import com.robotpaw.myhome.game.MissionKind.TOOTHBRUSH
import com.robotpaw.myhome.game.MissionKind.TOWEL
import com.robotpaw.myhome.game.MissionKind.TRASH
import com.robotpaw.myhome.game.MissionKind.VACUUM
import com.robotpaw.myhome.game.MissionLog
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.game.quests.RealQuests
import kotlin.random.Random

/**
 * Миссия из документа команды: задание + условие. Проверяется по логу задания, новой графики не нужно
 * @param skill - навык из ТЗ, который тренирует миссия
 * @param missions - на каких заданиях можно выполнить; null - на любых
 * @param tiers - уровни сложности условия (секунды или число движений), пусто - уровней нет
 * @param ready - false: для миссии пока нет задания, в цели дня она не попадает
 */
enum class GoalKind(
    val title: String,
    val skill: String,
    val missions: Set<MissionKind>?,
    val tiers: List<Int> = emptyList(),
    val ready: Boolean = true
) {
    CLEAN("Чистая работа", "изолированное сокращение", null),
    NO_SPILL("Ни капли", "меньше совместных напряжений", setOf(TEA, FLOWER)),
    IRON_GRIP("Железная хватка", "длительность сокращения", setOf(VACUUM, TRASH, MIRROR)),
    GENTLE("Нежное касание", "контроль силы", setOf(PANCAKES, CLOTHESPINS)),
    BULLSEYE("В яблочко", "точность", setOf(TOWEL, PICTURE, TOOTHBRUSH)),
    QUICK("Быстрая рука", "скорость реакции", setOf(TABLE, PILLOWS), tiers = listOf(20, 15, 11)),
    RHYTHM("В ритм", "быстрое переключение каналов", setOf(SALAD), tiers = listOf(4, 6, 8)),
    FREEZE("Замри", "удержание расслабления", setOf(CUBES, PICTURE, PILLOWS, TABLE, TOWEL), tiers = listOf(3, 5, 8)),
    // нужен шаг «напрячь оба канала по команде» в механике, пока такого задания нет
    BOTH("Обе руки", "действие «оба канала»", emptySet(), ready = false),
    FLAWLESS("Без ошибок", "устойчивость навыка", null, tiers = listOf(3)),
    RECORD("Новый рекорд", "прогресс", null),
    WARM_UP("Разминка", "связь «мышца - действие»", emptySet());

    /**
     * Условие для игрока
     * @param value - значение уровня из tiers (0, если уровней нет)
     */
    fun condition(value: Int): String = when (this) {
        CLEAN -> "Ни одного встречного сигнала за задание"
        NO_SPILL -> "Не пролить: без перебора силы и встречного сигнала"
        IRON_GRIP -> "Не разжать кисть, пока несёте"
        GENTLE -> "Ни разу не нажать сильнее нужного"
        BULLSEYE -> "Отпустить точно в цель с первого раза"
        QUICK -> "Уложиться в $value с"
        RHYTHM -> "$value движений подряд без сбоя"
        FREEZE -> "Посреди задания замереть: обе мышцы в покое $value с"
        BOTH -> "Напрячь обе мышцы по команде"
        FLAWLESS -> "$value задания подряд без ошибок"
        RECORD -> "Побить свой лучший результат"
        WARM_UP -> "Пройти разминку целиком"
    }

    // где выполнять: «Чай, Цветочек»; null - подходит любое задание
    val where: String?
        get() = when {
            this == WARM_UP -> "Разминка в начале игры"
            this == RECORD -> "Любое пройденное задание"
            missions == null -> null
            else -> missions.joinToString(", ") { it.label }
        }

    // подходит ли задание для проверки миссии
    fun appliesTo(kind: MissionKind): Boolean = this != WARM_UP && ready && (missions == null || kind in missions)
}

/**
 * Что известно о задании, когда оно пройдено
 * @param newRecord - побит прежний рекорд (первое прохождение не считается)
 * @param flawlessStreak - сколько заданий подряд без ошибок, включая это
 */
data class GoalContext(
    val kind: MissionKind,
    val log: MissionLog,
    val newRecord: Boolean = false,
    val flawlessStreak: Int = 0
)

/**
 * Выполнена ли миссия этим заданием
 * @param value - значение уровня (секунды, движения), для миссий без уровней не важно
 */
fun GoalKind.isMet(ctx: GoalContext, value: Int): Boolean {
    if (!appliesTo(ctx.kind)) return false
    val log = ctx.log
    val hasData = log.durationMs > 0L
    return when (this) {
        GoalKind.CLEAN -> hasData && log.coContractionEvents == 0
        GoalKind.NO_SPILL -> hasData && log.errors == 0 && log.coContractionEvents == 0
        GoalKind.IRON_GRIP -> hasData && log.errors == 0 && log.interruptions == 0
        GoalKind.GENTLE -> hasData && log.errors == 0
        GoalKind.BULLSEYE -> hasData && log.errors == 0 && log.interruptions == 0 && log.wrongChannelEvents == 0
        GoalKind.QUICK -> hasData && log.durationMs <= value * 1000L
        GoalKind.RHYTHM -> log.bestStreak >= value
        GoalKind.FREEZE -> log.longestRestMs >= value * 1000L
        GoalKind.FLAWLESS -> ctx.flawlessStreak >= value
        GoalKind.RECORD -> ctx.newRecord
        GoalKind.BOTH, GoalKind.WARM_UP -> false
    }
}

/**
 * Сколько раз миссию пробовали и выполнили (по всем заданиям, где она подходит) - по этому ищем слабый навык
 */
data class GoalTries(val tries: Int = 0, val wins: Int = 0) {
    val rate: Float get() = if (tries == 0) 0f else wins.toFloat() / tries
    fun with(won: Boolean) = GoalTries(tries + 1, if (won) wins + 1 else wins)
}

/**
 * Цель на сегодня
 * @param tier - номер уровня в kind.tiers
 * @param challenge - необязательный челлендж: сложнее и звёзд больше
 */
data class DailyGoal(
    val kind: GoalKind,
    val tier: Int = 0,
    val challenge: Boolean = false,
    val done: Boolean = false
) {
    val value: Int get() = kind.tiers.getOrElse(tier) { 0 }
    val condition: String get() = kind.condition(value)
    val reward: Int get() = if (challenge) CHALLENGE_REWARD else REWARD

    companion object {
        const val REWARD = 1
        const val CHALLENGE_REWARD = 2
    }
}

/**
 * Подбор целей на день по правилам команды:
 * одна - под самый слабый навык (первым идёт навык, где квест в жизни «не вышел»),
 * одна - случайная из открытых, третья - необязательный челлендж.
 * Цели берутся только из открытых комнат, при усталости - одна цель без челленджа
 */
object GoalPicker {
    const val WINS_PER_TIER = 3   // столько раз выполнить миссию, чтобы её уровень вырос

    /**
     * Миссии, которые можно выполнить сейчас
     * @param canWarmUp - калибровка сохранена, значит разминка есть
     */
    fun available(progress: GameProgress, canWarmUp: Boolean): List<GoalKind> {
        val open = openMissions(progress)
        val kinds = open.map { it.kind }.toSet()
        val hasRecords = open.any { progress.recordOf(it) != null }
        return GoalKind.entries.filter { goal ->
            goal.ready && when (goal) {
                GoalKind.WARM_UP -> canWarmUp
                GoalKind.RECORD -> hasRecords
                else -> goal.missions == null && kinds.isNotEmpty() || goal.missions.orEmpty().any { it in kinds }
            }
        }
    }

    /**
     * @param tired - признаки усталости: одна цель и без челленджа
     */
    fun pick(progress: GameProgress, canWarmUp: Boolean, tired: Boolean = false, random: Random = Random.Default): List<DailyGoal> {
        val pool = available(progress, canWarmUp).toMutableList()
        if (pool.isEmpty()) return emptyList()
        val result = mutableListOf<DailyGoal>()

        // слабый навык: сначала тот, где квест в жизни «не вышел», потом - меньше всего удачных попыток,
        // если ещё ничего не пробовали - случайная
        val failed = RealQuests.failedSkills(progress)
        val tried = pool.filter { progress.goalTriesOf(it).tries > 0 }
        val weakest = pool.firstOrNull { it in failed }
            ?: tried.minByOrNull { progress.goalTriesOf(it).rate }
            ?: pool.random(random)
        result += DailyGoal(weakest, tierOf(weakest, progress))
        pool -= weakest
        if (tired || pool.isEmpty()) return result

        val other = pool.random(random)
        result += DailyGoal(other, tierOf(other, progress))
        pool -= other
        if (pool.isEmpty()) return result

        // челлендж: лучше миссия с уровнями, тогда уровень на ступень выше
        val withTiers = pool.filter { it.tiers.size > 1 }
        val challenge = if (withTiers.isNotEmpty()) withTiers.random(random) else pool.random(random)
        val tier = (tierOf(challenge, progress) + 1).coerceAtMost((challenge.tiers.size - 1).coerceAtLeast(0))
        result += DailyGoal(challenge, tier, challenge = true)
        return result
    }

    // уровень растёт с числом выполнений миссии
    fun tierOf(kind: GoalKind, progress: GameProgress): Int =
        (progress.goalTriesOf(kind).wins / WINS_PER_TIER).coerceAtMost((kind.tiers.size - 1).coerceAtLeast(0))

    /**
     * Задание, куда отправить игрока за целью. null - подходит любое (или это разминка)
     */
    fun missionFor(goal: GoalKind, progress: GameProgress): Mission? {
        val open = openMissions(progress)
        return when (goal) {
            GoalKind.WARM_UP -> null
            GoalKind.RECORD -> open.firstOrNull { progress.recordOf(it) != null }
            else -> goal.missions?.let { set -> open.firstOrNull { it.kind in set } }
        }
    }

    // задания открытых комнат, которые можно запустить
    private fun openMissions(progress: GameProgress): List<Mission> =
        Rooms.ALL.filter { progress.isUnlocked(it) }
            .flatMap { it.missions }
            .filter { it.repeatable || !progress.isCompleted(it) }
}