package com.robotpaw.myhome.mission.mechanics.strength

import com.robotpaw.myhome.signal.SignalState

/**
 * Настройки задания на силу и удержание
 * @param text - подсказка игроку
 * @param holdMs - сколько нужно держать (при proportional - сколько при силе 100%)
 * @param minStrength - минимальная сила сгибателя в процентах, ниже этого прогресс не идёт
 * @param proportional - скорость прогресса зависит от силы (сильнее жмёт - быстрее идёт)
 * @param dropIsError - если отпустить раньше времени, это ошибка (миссия «Железная хватка»)
 * @param dropGraceMs - сколько можно не держать, пока это не считается «уронил»
 * @param decayRate - во сколько раз прогресс убывает медленнее, чем растёт, когда игрок не держит
 * @param attemptLimitMs - если за это время задание не выполнено, показываем окно поддержки
 */
data class StrengthHoldConfig(
    val text: String,
    val holdMs: Long,
    val minStrength: Int,
    val proportional: Boolean = false,
    val dropIsError: Boolean = false,
    val dropGraceMs: Long = 300L,
    val decayRate: Float = 0.5f,
    val attemptLimitMs: Long = 30_000L
)

/**
 * Что произошло на кадре
 * @param done - задание выполнено
 * @param drop - игрок отпустил раньше времени (один раз за каждый «хват»)
 */
data class StrengthHoldEvent(val done: Boolean = false, val drop: Boolean = false)

/**
 * Правила заданий на силу и удержание (мусор, пылесос, зеркало)
 * Прогресс идёт, пока напряжён только сгибатель и его сила не ниже minStrength
 * Чистая логика без Compose, чтобы проверять юнит-тестами
 */
class StrengthHoldRules(private val config: StrengthHoldConfig) {

    var progress: Float = 0f
        private set

    val isDone: Boolean get() = progress >= 1f

    private var gripping = false      // сейчас «держит» или ещё не прошёл допуск на отпускание
    private var notHoldingMs = 0L

    fun restore(value: Float) {
        progress = value.coerceIn(0f, 1f)
    }

    fun update(state: SignalState, dtMs: Long): StrengthHoldEvent {
        if (isDone) return StrengthHoldEvent()

        val step = dtMs.toFloat() / config.holdMs
        val holding = state.code == "10" && state.flexorStrength >= config.minStrength

        var drop = false
        if (holding) {
            gripping = true
            notHoldingMs = 0L
            val rate = if (config.proportional) state.flexorStrength / 100f else 1f
            progress += step * rate
        } else {
            if (gripping) {
                notHoldingMs += dtMs
                if (notHoldingMs >= config.dropGraceMs) {
                    gripping = false
                    drop = config.dropIsError
                }
            }
            progress -= if (state.isCoContraction) step else step * config.decayRate
        }
        progress = progress.coerceIn(0f, 1f)
        return StrengthHoldEvent(done = isDone, drop = drop)
    }
}
