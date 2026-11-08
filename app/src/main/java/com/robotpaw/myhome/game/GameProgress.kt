package com.robotpaw.myhome.game

/**
 * Пройденные задания (только в памяти)
 */
data class GameProgress(val completedMissions: Set<String> = emptySet()) {
    fun withCompleted(mission: Mission) = copy(completedMissions = completedMissions + mission.id)
}