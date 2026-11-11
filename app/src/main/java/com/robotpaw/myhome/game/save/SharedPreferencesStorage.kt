package com.robotpaw.myhome.game.save

import android.content.SharedPreferences

/**
 * Хранилище на SharedPreferences (данные остаются между запусками приложения)
 */
class SharedPreferencesStorage(private val prefs: SharedPreferences) : KeyValueStorage {

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    // возвращаем копию: набор из SharedPreferences менять нельзя
    override fun getStringSet(key: String): Set<String> =
        prefs.getStringSet(key, emptySet())?.toSet() ?: emptySet()

    override fun putStringSet(key: String, value: Set<String>) {
        prefs.edit().putStringSet(key, value.toSet()).apply()
    }
}
