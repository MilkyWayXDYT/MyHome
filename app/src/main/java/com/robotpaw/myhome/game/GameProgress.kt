package com.robotpaw.myhome.game

/**
 * Пройденные задания (только в памяти)
 */
data class GameProgress(val completedMissions: Set<String> = emptySet()) {

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