package com.robotpaw.myhome.developer

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.adapt.Adaptation
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.Room
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.game.Stars

// Читы для отладочного меню (только debug-сборка). Меняют только прогресс, механики не трогают

/**
 * Отметить задание выполненным или снять отметку
 * @param done - true выполнено (если звёзд не было, ставится одна), false - задание как новое: без звёзд и рекорда
 */
fun GameProgress.withMissionDone(mission: Mission, done: Boolean): GameProgress =
    if (done) {
        withCompleted(mission).copy(stars = stars + (mission.id to starsOf(mission).coerceAtLeast(1)))
    } else {
        copy(
            completedMissions = completedMissions - mission.id,
            stars = stars - mission.id,
            records = records - mission.id
        )
    }

/**
 * Поставить заданию нужное число звёзд
 * @param count - 0..Stars.MAX, при 0 звёзды убираются, отметка «выполнено» остаётся
 */
fun GameProgress.withStarsOf(mission: Mission, count: Int): GameProgress {
    val n = count.coerceIn(0, Stars.MAX)
    if (n == 0) return copy(stars = stars - mission.id)
    return withCompleted(mission).copy(stars = stars + (mission.id to n))
}

// следующее число звёзд по кругу: 0 → 1 → 2 → 3 → 0
fun GameProgress.withNextStarsOf(mission: Mission): GameProgress =
    withStarsOf(mission, (starsOf(mission) + 1) % (Stars.MAX + 1))

/**
 * Все задания всех комнат выполнены на максимум звёзд
 */
fun GameProgress.withAllStars(): GameProgress {
    var result = this
    Rooms.ALL.forEach { room -> room.missions.forEach { result = result.withStarsOf(it, Stars.MAX) } }
    return result
}

// убрать все звёзды, отметки «выполнено» остаются
fun GameProgress.withoutStars(): GameProgress = copy(stars = emptyMap())

/**
 * Комната как новая: её задания не выполнены, звёзд и рекордов нет
 */
fun GameProgress.withRoomReset(room: Room): GameProgress {
    var result = this
    room.missions.forEach { result = result.withMissionDone(it, false) }
    return result
}

fun GameProgress.withoutPet(): GameProgress = copy(pet = null)

// уровень сложности задания по кругу: 0 → +1 → +2 → -2 → -1 → 0
fun GameProgress.withNextLevel(mission: Mission): GameProgress {
    val next = levelOf(mission) + 1
    return withLevel(mission, if (next > Adaptation.MAX) Adaptation.MIN else next)
}

// всем заданиям один уровень сложности
fun GameProgress.withAllLevels(level: Int): GameProgress =
    copy(levels = if (level == 0) emptyMap() else Rooms.ALL.flatMap { it.missions }.associate { it.id to level })