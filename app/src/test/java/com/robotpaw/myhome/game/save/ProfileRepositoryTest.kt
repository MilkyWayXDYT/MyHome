package com.robotpaw.myhome.game.save

import com.robotpaw.myhome.signal.ChannelSettings
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.UserLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// хранилище в памяти для тестов профиля
private class ProfileMemoryStorage : KeyValueStorage {
    val strings = mutableMapOf<String, String>()
    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String?) {
        if (value == null) strings.remove(key) else strings[key] = value
    }
    override fun getStringSet(key: String): Set<String> = emptySet()
    override fun putStringSet(key: String, value: Set<String>) = Unit
}

class ProfileRepositoryTest {

    @Test
    fun noCalibration_givesNull() {
        assertNull(ProfileRepository(ProfileMemoryStorage()).load())
    }

    @Test
    fun saveAndLoad_roundTrip() {
        val storage = ProfileMemoryStorage()
        val profile = SignalProfile(
            flexor = ChannelSettings(rest = 7, max = 88, threshold = 31, hysteresis = 4, deadZone = 2),
            extensor = ChannelSettings(rest = 9, max = 76, threshold = 27, hysteresis = 6, deadZone = 3),
            level = UserLevel.EXPERIENCED,
            holdToSelectMs = SignalProfile.HOLD_EXPERIENCED_MS
        )
        ProfileRepository(storage).save(profile)
        assertEquals(profile, ProfileRepository(storage).load())
    }

    @Test
    fun brokenSave_givesNull() {
        val storage = ProfileMemoryStorage()
        storage.putString(ProfileRepository.KEY_PROFILE, "v1|мусор")
        assertNull(ProfileRepository(storage).load())
    }

    @Test
    fun clear_removesProfile() {
        val storage = ProfileMemoryStorage()
        val repo = ProfileRepository(storage)
        repo.save(SignalProfile())
        repo.clear()
        assertNull(repo.load())
    }
}