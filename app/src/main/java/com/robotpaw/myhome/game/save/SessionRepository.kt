package com.robotpaw.myhome.game.save

import com.robotpaw.myhome.game.goals.DailyGoal
import com.robotpaw.myhome.game.goals.GoalKind
import com.robotpaw.myhome.game.session.Session

/**
 * Сохраняет тренировку за день, чтобы план не начинался заново при каждом запуске
 * Формат строки: v2|день|заданий|план|сверх плана|звёзд|время в заданиях|цели,
 * цель - МИССИЯ.уровень.челлендж.выполнена через запятую, «-» - целей нет
 * Усталость не сохраняется: после перезапуска считаем, что рука отдохнула
 */
class SessionRepository(private val storage: KeyValueStorage) {

    /**
     * @param today - сегодняшняя дата; если сохранена другая, начинается новый день
     */
    fun load(today: String): Session {
        val parts = storage.getString(KEY_SESSION)?.split("|") ?: return Session(today)
        if (parts.size != 8 || parts[0] != VERSION || parts[1] != today) return Session(today)
        val done = parts[2].toIntOrNull() ?: return Session(today)
        val plan = parts[3].toIntOrNull() ?: return Session(today)
        val extra = parts[4].toIntOrNull() ?: return Session(today)
        val stars = parts[5].toIntOrNull() ?: return Session(today)
        val activeMs = parts[6].toLongOrNull() ?: return Session(today)
        val goals = parseGoals(parts[7]) ?: return Session(today)
        return Session(today, done = done, extra = extra, starsGained = stars, activeMs = activeMs, goals = goals).withPlan(plan)
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
                session.activeMs.toString(),
                session.goals.joinToString(",") { "${it.kind.name}.${it.tier}.${flag(it.challenge)}.${flag(it.done)}" }.ifEmpty { "-" }
            ).joinToString("|")
        )
    }

    private fun flag(value: Boolean) = if (value) "1" else "0"

    // null - строка не читается
    private fun parseGoals(text: String): List<DailyGoal>? {
        if (text == "-") return emptyList()
        return text.split(",").map { item ->
            val p = item.split(".")
            if (p.size != 4) return null
            val kind = GoalKind.entries.firstOrNull { it.name == p[0] } ?: return null
            val tier = p[1].toIntOrNull() ?: return null
            DailyGoal(kind, tier, challenge = p[2] == "1", done = p[3] == "1")
        }
    }

    companion object {
        const val KEY_SESSION = "session_day"
        private const val VERSION = "v2"
    }
}