package com.robotpaw.myhome.game

/**
 * TUTORIAL - обучение, ROOM - обычная комната (в конце экран «Отдохни»), FINAL - питомец
 */
enum class RoomKind { TUTORIAL, ROOM, FINAL }

/**
 * Уровень игры
 * @param missions - задания в порядке стен комнаты
 */
data class Room(
    val id: String,
    val title: String,
    val kind: RoomKind,
    val missions: List<Mission>
) {
    // задания, без которых комната не считается пройденной
    val requiredMissions: List<Mission> get() = missions.filter { !it.optional }
}

object Rooms {
    val TUTORIAL = Room("tutorial", "Обучение", RoomKind.TUTORIAL, Missions.TUTORIAL)
    val LIVING_ROOM = Room("living", "Гостиная", RoomKind.ROOM, Missions.LIVING_ROOM)

    // в порядке прохождения
    val ALL: List<Room> = listOf(TUTORIAL, LIVING_ROOM)

    fun next(room: Room): Room? = ALL.getOrNull(ALL.indexOf(room) + 1)
}