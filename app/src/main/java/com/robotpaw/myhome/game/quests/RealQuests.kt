package com.robotpaw.myhome.game.quests

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.MissionKind
import com.robotpaw.myhome.game.MissionKind.CLOTHESPINS
import com.robotpaw.myhome.game.MissionKind.CUBES
import com.robotpaw.myhome.game.MissionKind.DOOR
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
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.game.goals.GoalKind
import kotlin.random.Random

/**
 * Квест в реальной жизни: навык из ТЗ на настоящих предметах. Открывается, когда в игре пройдено похожее задание
 * @param skill - навык из ТЗ
 * @param steps - что сделать, по шагам
 * @param practice - похожие задания игры: открывают квест и в них можно потренироваться
 * @param goal - миссия того же навыка: «Не вышло» ставит её первой в целях на следующий день
 */
enum class RealQuest(
    val title: String,
    val skill: String,
    val steps: List<String>,
    val practice: List<MissionKind>,
    val goal: GoalKind
) {
    DOOR_OPEN(
        "Откройте дверь протезом", "Сжать и удерживать",
        listOf("Возьмитесь протезом за ручку двери", "Держите и откройте дверь"),
        listOf(DOOR), GoalKind.IRON_GRIP
    ),
    SPOONS(
        "Перенесите две ложки", "Взять, перенести, отпустить",
        listOf("Возьмите ложку протезом", "Перенесите на другое место и отпустите", "Повторите со второй ложкой"),
        listOf(CUBES), GoalKind.BULLSEYE
    ),
    CUP(
        "Поставьте кружку на стол", "Точность",
        listOf("Возьмите кружку протезом", "Аккуратно поставьте её на стол"),
        listOf(TEA), GoalKind.NO_SPILL
    ),
    TOWEL_HOOK(
        "Повесьте полотенце на крючок", "Точность при отпускании",
        listOf("Возьмите полотенце", "Повесьте на крючок и отпустите, чтобы оно не упало"),
        listOf(TOWEL), GoalKind.BULLSEYE
    ),
    BOTTLE(
        "Подержите бутылку воды 10 секунд", "Сила и время удержания",
        listOf("Возьмите закрытую бутылку воды", "Держите 10 секунд, не уронив"),
        listOf(TRASH, VACUUM), GoalKind.IRON_GRIP
    ),
    PAPER_CUP(
        "Возьмите бумажный стаканчик", "Контроль силы, не пережимать",
        listOf("Возьмите пустой бумажный стаканчик", "Поднимите и поставьте, не смяв его"),
        listOf(CLOTHESPINS, PANCAKES), GoalKind.GENTLE
    ),
    TEMPO(
        "Сожмите и разожмите кисть 5 раз", "Быстрая смена мышц",
        listOf("Сожмите и разожмите кисть 5 раз подряд", "Старайтесь держать один темп"),
        listOf(SALAD), GoalKind.RHYTHM
    ),
    CLOTHESPIN(
        "Застегните прищепку на ткани", "Точная слабая сила",
        listOf("Возьмите прищепку протезом", "Застегните её на полотенце или футболке"),
        listOf(CLOTHESPINS), GoalKind.GENTLE
    ),
    TABLE_SET(
        "Разложите на столе три предмета", "Смена напряжённых мышц",
        listOf("Разложите тарелку, вилку и салфетку", "Каждую вещь возьмите и положите протезом"),
        listOf(TABLE), GoalKind.QUICK
    ),
    STILL_SPOON(
        "Подержите ложку неподвижно 5 секунд", "Удержание покоя",
        listOf("Возьмите ложку протезом", "Расслабьте руку и не двигайте кистью 5 секунд"),
        listOf(CUBES, PICTURE, PILLOWS, TABLE, TOWEL), GoalKind.FREEZE
    ),
    TOOTHPASTE(
        "Выдавите пасту на щётку", "Мягкое дозированное усилие",
        listOf("Возьмите тюбик пасты протезом", "Выдавите немного на зубную щётку"),
        listOf(TOOTHBRUSH), GoalKind.GENTLE
    ),
    WIPE(
        "Протрите зеркало или стол", "Сила с постоянным давлением",
        listOf("Возьмите салфетку протезом", "Протрите зеркало или стол, прижимая ровно"),
        listOf(MIRROR), GoalKind.IRON_GRIP
    );

    // «Дверь», «Мусор, Пылесос» - для экрана квеста
    val like: String get() = practice.joinToString(", ") { it.label }

    companion object {
        // общий совет по безопасности для всех квестов
        const val SAFETY = "Если неудобно или больно - остановитесь. Можно попросить кого-нибудь побыть рядом."
    }
}

/**
 * Ответ на «Как получилось?»
 */
enum class QuestFeeling(val title: String) {
    EASY("Легко"),
    HARD("С трудом"),
    NOT_YET("Не вышло")
}

/**
 * Ответ игрока после квеста
 */
data class Reflection(val feeling: QuestFeeling) {
    // с трудом или не вышло - предлагаем потренироваться в похожем задании
    val needsPractice: Boolean get() = feeling != QuestFeeling.EASY
}

object RealQuests {
    const val REWARD = 1   // звёзды за ответ, как прошёл квест (за честный ответ, не за успех)

    // квесты, похожее задание которых в игре уже пройдено
    fun unlocked(progress: GameProgress): List<RealQuest> {
        val doneKinds = Rooms.ALL.flatMap { it.missions }.filter { progress.isCompleted(it) }.map { it.kind }.toSet()
        return RealQuest.entries.filter { quest -> quest.practice.any { it in doneKinds } }
    }

    /**
     * Квест на сегодня: сначала новые, потом те, что шли с трудом или не вышли, иначе любой открытый
     * @param except - текущий квест, чтобы не повторять подряд
     */
    fun pick(progress: GameProgress, except: RealQuest? = null, random: Random = Random.Default): RealQuest? {
        val open = unlocked(progress)
        val pool = open.filter { it != except }.ifEmpty { open }
        if (pool.isEmpty()) return null
        val fresh = pool.filter { progress.reflectionOf(it) == null }
        if (fresh.isNotEmpty()) return fresh.first()
        val hard = pool.filter { progress.reflectionOf(it)?.feeling != QuestFeeling.EASY }
        return (hard.ifEmpty { pool }).random(random)
    }

    /**
     * Похожее задание игры, где потренироваться (первое из открытых)
     */
    fun practiceFor(quest: RealQuest, progress: GameProgress): Mission? {
        val open = Rooms.ALL.filter { progress.isUnlocked(it) }.flatMap { it.missions }
        return quest.practice.firstNotNullOfOrNull { kind -> open.firstOrNull { it.kind == kind } }
    }

    // навыки, где квест в жизни «не вышел»: им место первыми в целях
    fun failedSkills(progress: GameProgress): Set<GoalKind> =
        progress.reflections.filterValues { it.feeling == QuestFeeling.NOT_YET }.keys.map { it.goal }.toSet()
}