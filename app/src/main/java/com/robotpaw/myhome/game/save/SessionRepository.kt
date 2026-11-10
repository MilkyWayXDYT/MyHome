package com.robotpaw.myhome.game.save

import com.robotpaw.myhome.game.session.Session

/**
 * Сохраняет тренировку за день, чтобы план не начинался заново при каждом запуске
 * Формат строки: v1|день|заданий|план|сверх плана|звёзд|время в заданиях
 * Усталость не сохраняется: после перезапуска считаем, что рука отдохнула
 */
class SessionRepository(private val storage: KeyValueStorage) {

    /**
     * @param today - сегодняшняя дата; если сохранена другая, начинается новый день
     */
    fun load(today: String): Session {
        val parts = storage.getString(KEY_SESSION)?.split("|") ?: return Session(today)
        if (parts.size != 7 || parts[0] != VERSION || parts[1] != today) return Session(today)
        val done = parts[2].toIntOrNull() ?: return Session(today)
        val plan = parts[3].toIntOrNull() ?: return Session(today)
        val extra = parts[4].toIntOrNull() ?: return Session(today)
        val stars = parts[5].toIntOrNull() ?: return Session(today)
        val activeMs = parts[6].toLongOrNull() ?: return Session(today)
        return Session(today, done = done, extra = extra, starsGained = stars, activeMs = activeMs).withPlan(plan)
    }

    fun save(session: Session) {
        storage.putString(
            KEY_SESSION,
            listOf(
                VERSION,
                session.day,
                session.done.toString(),
                session.plan.toString(),
                session.extra.toString(),
                session.starsGained.toString(),
                session.activeMs.toString()
            ).joinToString("|")
        )
    }

    companion object {
        const val KEY_SESSION = "session_day"
        private const val VERSION = "v1"
    }
}