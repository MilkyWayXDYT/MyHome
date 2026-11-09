package com.robotpaw.myhome.mission.mechanics.door

import com.robotpaw.myhome.signal.SignalState

/**
 * Правила задания «Дверь»: сжать кисть и удерживать, пока дверь не откроется
 * Чистая логика без Compose, чтобы проверять юнит-тестами
 * @param holdMs - сколько нужно удерживать сжатие подряд, чтобы открыть дверь
 * @param decayRate - во сколько раз прогресс убывает медленнее, чем растёт, когда сжатие отпущено
 * @param keepProgress - прогресс не откатывается назад (для новичка): отпустил или напряг оба канала, полоса остаётся
 */
class DoorRules(
    private val holdMs: Long = 3000L,
    private val decayRate: Float = 0.5f,
    private val keepProgress: Boolean = false
) {
    var progress: Float = 0f
        private set

    val isDone: Boolean get() = progress >= 1f

    fun restore(value: Float) {
        progress = value.coerceIn(0f, 1f)
    }

    /**
     * @return true, если на этом кадре дверь открылась
     */
    fun update(state: SignalState, dtMs: Long): Boolean {
        if (isDone) return false
        val step = dtMs.toFloat() / holdMs
        progress = when (state.code) {
            "10" -> progress + step
            "11" -> if (keepProgress) progress else progress - step      // встречный сигнал: прогресс падает так же быстро, как растёт
            else -> if (keepProgress) progress else progress - step * decayRate
        }.coerceIn(0f, 1f)
        return isDone
    }
}