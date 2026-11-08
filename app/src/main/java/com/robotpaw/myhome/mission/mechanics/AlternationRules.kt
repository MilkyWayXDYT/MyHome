package com.robotpaw.myhome.mission.mechanics

import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalState

/**
 * Один шаг чередования: напрячь нужный канал и держать
 * @param channel - какой канал напрягать (сгибатель - сжать, разгибатель - разжать)
 * @param holdMs - сколько держать, чтобы шаг засчитался
 * @param text - подсказка игроку
 * @param zone - допустимая сила канала в процентах; null - любая. Выше зоны - перебор, ниже - не засчитывается
 */
data class AlternationStep(
    val channel: Channel,
    val holdMs: Long,
    val text: String,
    val zone: IntRange? = null
)

/**
 * Что произошло на кадре
 * @param stepCompleted - шаг засчитан
 * @param overshoot - игрок нажал сильнее зоны (один раз за каждое превышение)
 */
data class AlternationEvent(val stepCompleted: Boolean = false, val overshoot: Boolean = false)

/**
 * Правила заданий на чередование сгибания и разгибания (картина, подушки)
 * Игрок проходит шаги по порядку. Шаг идёт, пока напряжён только нужный канал (и сила в зоне, если она задана)
 * Чистая логика без Compose, чтобы проверять юнит-тестами
 * @param decayRate - во сколько раз прогресс шага убывает медленнее, чем растёт, когда игрок расслабился
 */
class AlternationRules(
    val steps: List<AlternationStep>,
    private val decayRate: Float = 0.5f
) {
    var stepIndex: Int = 0
        private set

    // прогресс текущего шага (0..1)
    var stepProgress: Float = 0f
        private set

    // игрок сейчас давит сильнее зоны
    var isOvershooting: Boolean = false
        private set

    val isDone: Boolean get() = stepIndex >= steps.size
    val current: AlternationStep? get() = steps.getOrNull(stepIndex)

    // общий прогресс по всем шагам (0..1)
    val progress: Float
        get() = if (isDone) 1f else ((stepIndex + stepProgress) / steps.size).coerceIn(0f, 1f)

    /**
     * Продолжить с общего прогресса (после окна поддержки)
     */
    fun restore(total: Float) {
        val scaled = total.coerceIn(0f, 1f) * steps.size
        stepIndex = scaled.toInt().coerceAtMost(steps.size - 1)
        stepProgress = (scaled - stepIndex).coerceIn(0f, 1f)
    }

    fun update(state: SignalState, dtMs: Long): AlternationEvent {
        val step = current ?: return AlternationEvent()
        val delta = dtMs.toFloat() / step.holdMs

        val onlyRequired = when (step.channel) {
            Channel.FLEXOR -> state.code == "10"
            Channel.EXTENSOR -> state.code == "01"
        }
        val strength = when (step.channel) {
            Channel.FLEXOR -> state.flexorStrength
            Channel.EXTENSOR -> state.extensorStrength
        }
        val zone = step.zone
        val tooStrong = onlyRequired && zone != null && strength > zone.last
        val inZone = onlyRequired && (zone == null || strength in zone)

        val overshoot = tooStrong && !isOvershooting
        isOvershooting = tooStrong

        stepProgress += when {
            inZone -> delta
            tooStrong || state.isCoContraction -> -delta       // перебор или встречный сигнал: падает так же быстро, как растёт
            else -> -delta * decayRate                         // расслабился или напряжён другой канал
        }
        stepProgress = stepProgress.coerceIn(0f, 1f)

        if (stepProgress >= 1f) {
            stepIndex++
            stepProgress = 0f
            isOvershooting = false
            return AlternationEvent(stepCompleted = true, overshoot = overshoot)
        }
        return AlternationEvent(overshoot = overshoot)
    }
}
