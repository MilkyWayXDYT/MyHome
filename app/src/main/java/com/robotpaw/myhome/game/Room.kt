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

    // после комнаты показывается экран «Отдохни»
    val restAfter: Boolean get() = kind == RoomKind.ROOM
}

object Rooms {
    val TUTORIAL = Room("tutorial", "Обучение", RoomKind.TUTORIAL, Missions.TUTORIAL)
    val LIVING_ROOM = Room("living", "Гостиная", RoomKind.ROOM, Missions.LIVING_ROOM)
    val BATHROOM = Room("bathroom", "Ванная", RoomKind.ROOM, Missions.BATHROOM)
    val KITCHEN = Room("kitchen", "Кухня", RoomKind.ROOM, Missions.KITCHEN)
    val PET = Room("pet", "Питомец", RoomKind.FINAL, Missions.PET)

    // в порядке прохождения
    val ALL: List<Room> = listOf(TUTORIAL, LIVING_ROOM, BATHROOM, KITCHEN, PET)

    fun next(room: Room): Room? = ALL.getOrNull(ALL.indexOf(room) + 1)

    fun byId(id: String): Room = ALL.first { it.id == id }

    // комната, в которой лежит задание
    fun of(mission: Mission): Room = ALL.first { mission in it.missions }
}