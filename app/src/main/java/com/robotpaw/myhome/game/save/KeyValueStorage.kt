package com.robotpaw.myhome.game.save

/**
 * Простое хранилище «ключ - значение» для сохранений
 * Отдельный интерфейс, чтобы логику сохранения можно было проверять юнит-тестами без Android
 */
interface KeyValueStorage {
    fun getString(key: String): String?

    // null удаляет значение
    fun putString(key: String, value: String?)

    fun getStringSet(key: String): Set<String>

    fun putStringSet(key: String, value: Set<String>)
}
