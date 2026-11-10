package com.robotpaw.myhome.game

/**
 * Пройденные задания, выбранный питомец и лучшие результаты
 */
data class GameProgress(
    val completedMissions: Set<String> = emptySet(),
    val pet: PetKind? = null,
    val records: Map<String, MissionRecord> = emptyMap()
) {

    fun withPet(pet: PetKind) = copy(pet = pet)

    fun recordOf(mission: Mission): MissionRecord? = records[mission.id]

    /**
     * Засчитывает задание как пройденное и обновляет рекорд, если результат лучше прежнего
     */
    fun withResult(result: MissionResult): GameProgress {
        val done = withCompleted(result.mission)
        val stats = result.log.toStats()
        if (!stats.hasData) return done
        val now = MissionRecord.of(stats)
        val old = records[result.mission.id]
        return if (old == null || now.isBetterThan(old)) done.copy(records = records + (result.mission.id to now)) else done
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
            cleanPercent = if (roomRecords.isEmpty()) 100 else roomRecords.sumOf { it.cleanPercent } / roomRecords.size
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

    // первая непройденная комната, если пройдено всё - последняя
    fun currentRoom(): Room = Rooms.ALL.firstOrNull { !isDone(it) } ?: Rooms.ALL.last()
}
