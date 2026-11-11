package com.robotpaw.myhome.game

import com.robotpaw.myhome.game.goals.GoalKind
import com.robotpaw.myhome.game.goals.GoalTries
import com.robotpaw.myhome.game.quests.RealQuest
import com.robotpaw.myhome.game.quests.Reflection

/**
 * Пройденные задания, выбранный питомец, лучшие результаты и звёзды
 */
data class GameProgress(
    val completedMissions: Set<String> = emptySet(),
    val pet: PetKind? = null,
    val records: Map<String, MissionRecord> = emptyMap(),
    val stars: Map<String, Int> = emptyMap(),
    val bonusStars: Int = 0,
    val goalTries: Map<GoalKind, GoalTries> = emptyMap(),
    val reflections: Map<RealQuest, Reflection> = emptyMap(),
    val chores: List<String> = emptyList()
) {

    fun withPet(pet: PetKind) = copy(pet = pet)

    fun recordOf(mission: Mission): MissionRecord? = records[mission.id]

    // лучшее число звёзд за задание (0, если звёзд ещё нет)
    fun starsOf(mission: Mission): Int = stars[mission.id] ?: 0

    // всего звёзд у игрока: лучшие результаты по заданиям и звёзды за цели дня
    val totalStars: Int get() = stars.values.sum() + bonusStars

    // звёзды за цели на сегодня
    fun withBonusStars(count: Int) = copy(bonusStars = bonusStars + count)

    fun goalTriesOf(kind: GoalKind): GoalTries = goalTries[kind] ?: GoalTries()

    // последний ответ игрока после квеста в жизни (null - квест ещё не проходили)
    fun reflectionOf(quest: RealQuest): Reflection? = reflections[quest]

    fun withReflection(quest: RealQuest, reflection: Reflection) = copy(reflections = reflections + (quest to reflection))

    /**
     * Запоминает попытку миссии (для поиска слабого навыка)
     * @param won - миссия выполнена
     */
    fun withGoalTry(kind: GoalKind, won: Boolean) = copy(goalTries = goalTries + (kind to goalTriesOf(kind).with(won)))

    /**
     * Сколько звёзд даст это прохождение. Считать нужно до withResult
     */
    fun starsFor(result: MissionResult): StarsOutcome {
        val earned = Stars.forStats(result.log.toStats())
        val gained = (earned - starsOf(result.mission)).coerceAtLeast(0)
        return StarsOutcome(earned = earned, gained = gained, total = totalStars + gained)
    }

    /**
     * Засчитывает задание как пройденное, обновляет звёзды и рекорд, если результат лучше прежнего
     */
    fun withResult(result: MissionResult): GameProgress {
        val stats = result.log.toStats()
        val earned = Stars.forStats(stats)
        var updated = withCompleted(result.mission)
        if (earned > starsOf(result.mission)) updated = updated.copy(stars = stars + (result.mission.id to earned))
        if (!stats.hasData) return updated
        val now = MissionRecord.of(stats)
        val old = records[result.mission.id]
        return if (old == null || now.isBetterThan(old)) updated.copy(records = records + (result.mission.id to now)) else updated
    }

    /**
     * Итоги комнаты по лучшим результатам её заданий
     */
    fun summaryOf(room: Room): RoomSummary {
        val roomRecords = room.missions.mapNotNull { records[it.id] }
        return RoomSummary(
            done = completedCount(room),
            total = room.requiredMissions.size,
            totalMs = roomRecords.sumOf { it.durationMs },
            mistakes = roomRecords.sumOf { it.mistakes },
            cleanPercent = if (roomRecords.isEmpty()) 100 else roomRecords.sumOf { it.cleanPercent } / roomRecords.size,
            stars = room.missions.sumOf { starsOf(it) },
            maxStars = room.missions.size * Stars.MAX
        )
    }

    fun withCompleted(mission: Mission) = copy(completedMissions = completedMissions + mission.id)

    fun isCompleted(mission: Mission) = mission.id in completedMissions

    // сколько обязательных заданий комнаты пройдено
    fun completedCount(room: Room) = room.requiredMissions.count { isCompleted(it) }

    fun isDone(room: Room) = completedCount(room) == room.requiredMissions.size

    // комната открыта, если пройдена предыдущая
    fun isUnlocked(room: Room): Boolean {
        val index = Rooms.ALL.indexOf(room)
        return index <= 0 || isDone(Rooms.ALL[index - 1])
    }

    // несделанные дела после сюжета: переходят на следующий день
    fun withChores(ids: List<String>) = copy(chores = ids)

    fun withChoreDone(missionId: String) = copy(chores = chores - missionId)

    // сюжет пройден: дальше бесконечная игра, «Дела на сегодня»
    val isStoryDone: Boolean get() = Rooms.ALL.all { isDone(it) }

    // первая непройденная комната, если пройдено всё - последняя
    fun currentRoom(): Room = Rooms.ALL.firstOrNull { !isDone(it) } ?: Rooms.ALL.last()
}