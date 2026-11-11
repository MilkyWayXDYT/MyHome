package com.robotpaw.myhome.game.chores

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.RoomKind
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.game.quests.QuestFeeling
import kotlin.random.Random

/**
 * Дела на сегодня - бесконечная игра после сюжета
 * Дел всегда COUNT, это задания, которые даются хуже всего. Их предметы в комнатах снова «до»
 * Несделанные дела переходят на следующий день, пул добирается новыми до COUNT
 */
object Chores {
    const val COUNT = 3
    const val REWARD = 1   // звёзды за каждое дело

    // из каких заданий берутся дела: пройденные и повторяемые, без обучения
    fun candidates(progress: GameProgress): List<Mission> =
        Rooms.ALL.filter { it.kind != RoomKind.TUTORIAL }
            .flatMap { it.missions }
            .filter { it.repeatable && progress.isCompleted(it) }

    /**
     * Насколько задание даётся: меньше - хуже. Звёзды важнее, потом чистота сигнала в лучшем результате
     * Задания, похожие на квест в жизни с ответом «Не вышло», идут первыми
     */
    fun score(mission: Mission, progress: GameProgress): Int {
        val failedQuest = progress.reflections.any { (quest, r) -> r.feeling == QuestFeeling.NOT_YET && mission.kind in quest.practice }
        if (failedQuest) return -1
        return progress.starsOf(mission) * 100 + (progress.recordOf(mission)?.cleanPercent ?: 100)
    }

    /**
     * Дела на новый день: несделанные остаются, остальное - самые слабые задания до COUNT
     * @return id заданий
     */
    fun topUp(progress: GameProgress, random: Random = Random.Default): List<String> {
        val pool = candidates(progress)
        val ids = pool.map { it.id }.toSet()
        val kept = progress.chores.filter { it in ids }.distinct().take(COUNT)
        // перемешиваем до сортировки: равные по слабости задания идут в случайном порядке
        val weakest = pool.filter { it.id !in kept }.shuffled(random).sortedBy { score(it, progress) }
        return kept + weakest.take(COUNT - kept.size).map { it.id }
    }
}