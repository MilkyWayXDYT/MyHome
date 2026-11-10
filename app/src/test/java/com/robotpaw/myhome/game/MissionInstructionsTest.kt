package com.robotpaw.myhome.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionInstructionsTest {

    private val allMissions = Rooms.ALL.flatMap { it.missions }

    @Test
    fun everyMission_hasRealInstruction() {
        val placeholder = "Здесь будет инструкция к заданию"
        allMissions.forEach { mission ->
            assertTrue("нет инструкции: " + mission.id, mission.instruction.isNotEmpty() && mission.instruction.first() != placeholder)
            assertTrue("слишком длинная инструкция: " + mission.id, mission.instruction.size in 2..4)
        }
    }

    @Test
    fun missionIds_areUnique() {
        assertEquals(allMissions.size, allMissions.map { it.id }.toSet().size)
    }
}
