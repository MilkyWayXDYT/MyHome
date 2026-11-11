package com.robotpaw.myhome.developer

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.MissionRecord
import com.robotpaw.myhome.game.PetKind
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.game.Stars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugCheatsTest {
    private val room = Rooms.ALL.first()
    private val mission = room.missions.first()

    @Test
    fun doneGivesOneStar() {
        val p = GameProgress().withMissionDone(mission, true)
        assertTrue(p.isCompleted(mission))
        assertEquals(1, p.starsOf(mission))
    }

    @Test
    fun undoneClearsStarsAndRecord() {
        val p = GameProgress()
            .withStarsOf(mission, 3)
            .copy(records = mapOf(mission.id to MissionRecord(1000L, 0, 100)))
            .withMissionDone(mission, false)
        assertFalse(p.isCompleted(mission))
        assertEquals(0, p.starsOf(mission))
        assertNull(p.recordOf(mission))
    }

    @Test
    fun starsCycle() {
        var p = GameProgress()
        val seen = (1..4).map { p = p.withNextStarsOf(mission); p.starsOf(mission) }
        assertEquals(listOf(1, 2, 3, 0), seen)
        assertTrue(p.isCompleted(mission))
    }

    @Test
    fun allStarsAndClear() {
        val all = GameProgress().withAllStars()
        assertEquals(Rooms.ALL.sumOf { it.missions.size } * Stars.MAX, all.totalStars)
        assertTrue(Rooms.ALL.all { all.isDone(it) })
        assertEquals(0, all.withoutStars().totalStars)
    }

    @Test
    fun roomReset() {
        val p = GameProgress().withAllStars().withRoomReset(room)
        assertEquals(0, p.completedCount(room))
        assertEquals(0, p.summaryOf(room).stars)
        assertTrue(p.isDone(Rooms.ALL[1]))
    }

    @Test
    fun pet() {
        val p = GameProgress().withPet(PetKind.DOG).withoutPet()
        assertNull(p.pet)
    }
}