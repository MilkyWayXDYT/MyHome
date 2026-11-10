package com.robotpaw.myhome.game.save

import com.robotpaw.myhome.signal.ChannelSettings
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.UserLevel

/**
 * Сохраняет профиль сигнала после калибровки, чтобы при следующем запуске её можно было пропустить
 * Формат строки: v1|сгибатель|разгибатель|режим|защита от срабатываний|удержание,
 * канал - rest;max;threshold;hysteresis;deadZone
 */
class ProfileRepository(private val storage: KeyValueStorage) {

    // null - калибровки ещё не было или сохранение не читается
    fun load(): SignalProfile? {
        val parts = storage.getString(KEY_PROFILE)?.split("|") ?: return null
        if (parts.size != 6 || parts[0] != VERSION) return null
        val flexor = parseChannel(parts[1]) ?: return null
        val extensor = parseChannel(parts[2]) ?: return null
        val level = UserLevel.entries.firstOrNull { it.name == parts[3] } ?: return null
        val minPulse = parts[4].toLongOrNull() ?: return null
        val hold = parts[5].toLongOrNull() ?: return null
        return SignalProfile(flexor, extensor, level, minPulse, hold)
    }

    fun save(profile: SignalProfile) {
        storage.putString(
            KEY_PROFILE,
            listOf(
                VERSION,
                encodeChannel(profile.flexor),
                encodeChannel(profile.extensor),
                profile.level.name,
                profile.minPulseMs.toString(),
                profile.holdToSelectMs.toString()
            ).joinToString("|")
        )
    }

    fun clear() = storage.putString(KEY_PROFILE, null)

    private fun encodeChannel(c: ChannelSettings) = "${c.rest};${c.max};${c.threshold};${c.hysteresis};${c.deadZone}"

    private fun parseChannel(text: String): ChannelSettings? {
        val n = text.split(";").map { it.toIntOrNull() ?: return null }
        if (n.size != 5) return null
        return ChannelSettings(rest = n[0], max = n[1], threshold = n[2], hysteresis = n[3], deadZone = n[4])
    }

    companion object {
        const val KEY_PROFILE = "signal_profile"
        private const val VERSION = "v1"
    }
}