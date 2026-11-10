package com.robotpaw.myhome.game.save

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.Missions
import com.robotpaw.myhome.game.PetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// хранилище в памяти для тестов
private class InMemoryStorage : KeyValueStorage {
    val strings = mutableMapOf<String, String>()
    val sets = mutableMapOf<String, Set<String>>()

    override fun getString(key: String): String? = strings[key]

    override fun putString(key: String, value: String?) {
        if (value == null) strings.remove(key) else strings[key] = value
    }

    override fun getStringSet(key: String): Set<String> = sets[key] ?: emptySet()

    override fun putStringSet(key: String, value: Set<String>) {
        sets[key] = value
    }
}

class ProgressRepositoryTest {

    @Test
    fun emptyStorage_givesEmptyProgress() {
        val progress = ProgressRepository(InMemoryStorage()).load()
        assertTrue(progress.completedMissions.isEmpty())
        assertNull(progress.pet)
    }

    @Test
    fun saveAndLoad_roundTrip() {
        val storage = InMemoryStorage()
        val saved = GameProgress()
            .withCompleted(Missions.TUTORIAL[0])
            .withCompleted(Missions.LIVING_ROOM[1])
            .withPet(PetKind.PIG)
        ProgressRepository(storage).save(saved)

        val loaded = ProgressRepository(storage).load()
        assertEquals(saved, loaded)
    }

    @Test
    fun unknownMissionsAndPet_areDropped() {
        val storage = InMemoryStorage()
        storage.sets[ProgressRepository.KEY_COMPLETED] = setOf(Missions.TUTORIAL[0].id, "removed_mission")
        storage.strings[ProgressRepository.KEY_PET] = "DRAGON"

        val loaded = ProgressRepository(storage).load()
        assertEquals(setOf(Missions.TUTORIAL[0].id), loaded.completedMissions)
        assertNull(loaded.pet)
    }

    @Test
    fun reset_overwritesOldSave() {
        val storage = InMemoryStorage()
        val repository = ProgressRepository(storage)
        repository.save(GameProgress().withCompleted(Missions.TUTORIAL[0]).withPet(PetKind.CAT))
        repository.save(GameProgress())

        val loaded = repository.load()
        assertTrue(loaded.completedMissions.isEmpty())
        assertNull(loaded.pet)
    }
}
