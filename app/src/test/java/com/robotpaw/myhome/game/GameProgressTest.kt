package com.robotpaw.myhome.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameProgressTest {

    private fun complete(progress: GameProgress, room: Room) =
        room.requiredMissions.fold(progress) { p, m -> p.withCompleted(m) }

    @Test
    fun firstRoom_isOpen_restAreLocked() {
        val p = GameProgress()
        assertTrue(p.isUnlocked(Rooms.ALL.first()))
        Rooms.ALL.drop(1).forEach { assertFalse(p.isUnlocked(it)) }
        assertEquals(Rooms.ALL.first(), p.currentRoom())
    }

    @Test
    fun finishedRoom_opensNext() {
        val p = complete(GameProgress(), Rooms.TUTORIAL)
        assertTrue(p.isDone(Rooms.TUTORIAL))
        assertTrue(p.isUnlocked(Rooms.LIVING_ROOM))
        assertEquals(Rooms.LIVING_ROOM, p.currentRoom())
    }

    @Test
    fun partlyFinishedRoom_keepsNextLocked() {
        val p = GameProgress().withCompleted(Rooms.TUTORIAL.missions.first())
        assertEquals(1, p.completedCount(Rooms.TUTORIAL))
        assertFalse(p.isDone(Rooms.TUTORIAL))
        assertFalse(p.isUnlocked(Rooms.LIVING_ROOM))
    }

    @Test
    fun everythingDone_currentIsLast() {
        val p = Rooms.ALL.fold(GameProgress()) { acc, room -> complete(acc, room) }
        assertEquals(Rooms.ALL.last(), p.currentRoom())
    }

    @Test
    fun extraMission_isNotRequired() {
        val p = complete(GameProgress(), Rooms.BATHROOM)
        assertFalse(p.isCompleted(Rooms.BATHROOM.missions.first { it.optional }))
        assertTrue(p.isDone(Rooms.BATHROOM))
    }
}