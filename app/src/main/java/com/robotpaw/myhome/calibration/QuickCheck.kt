package com.robotpaw.myhome.calibration

/**
 * Шаг разминки
 * @param code - какое состояние каналов нужно держать
 */
enum class QuickCheckStep(val code: String, val text: String) {
    SQUEEZE("10", "Сожмите кисть и держите"),
    OPEN("01", "Разожмите кисть и держите"),
    REST("00", "Расслабьте руку")
}

/**
 * Сила сигнала на разминке заметно отличается от калибровки, хотя коды правильные
 */
enum class StrengthDrift(val title: String, val text: String) {
    TOO_STRONG("Сигнал стал сильнее", "Сила почти всё время упирается в максимум: задания на силу будут слишком лёгкими."),
    TOO_WEAK("Сигнал стал слабее", "Сжатие едва доходит до порога: задания на силу будут слишком трудными.")
}

/**
 * Разминка перед игрой, она же проверка, что калибровка с прошлого раза подходит
 * На каждом шаге нужно продержать нужное состояние holdMs подряд, на шаг даётся не больше timeoutMs
 * @param holdMs - сколько держать состояние без перерыва
 * @param timeoutMs - сколько ждать на шаге, потом разминка не пройдена
 * @param graceMs - короткий провал сигнала не сбрасывает удержание
 * Заодно на шагах «сожмите» и «разожмите» смотрим силу нужного канала: если она почти всё время на максимуме
 * или не поднимается выше weakPeak, калибровка устарела, даже если коды верные (см. drift)
 * @param saturated - с этой силы (0..100) считаем, что сигнал упёрся в максимум
 *   (100 - выше максимума калибровки, а он взят как 90-й перцентиль сжатия)
 * @param saturatedShare - какая доля удержания на максимуме - уже «слишком сильно»
 * @param weakPeak - если пик силы ниже, сигнал «слишком слабый»
 */
class QuickCheck(
    private val holdMs: Long = 1200L,
    private val timeoutMs: Long = 10_000L,
    private val graceMs: Long = 150L,
    private val saturated: Int = 100,
    private val saturatedShare: Float = 0.8f,
    private val weakPeak: Int = 30
) {
    var stepIndex = 0
        private set
    var heldMs = 0L
        private set
    var failed = false
        private set
    private var stepMs = 0L
    private var missMs = 0L

    // сила нужного канала на шагах «сожмите» и «разожмите»: время в нужном состоянии, из них на максимуме, пик
    private val matchedMs = LongArray(2)
    private val saturatedMs = LongArray(2)
    private val peak = IntArray(2)

    val step: QuickCheckStep? get() = QuickCheckStep.entries.getOrNull(stepIndex)
    val isPassed: Boolean get() = stepIndex >= QuickCheckStep.entries.size
    val holdProgress: Float get() = (heldMs.toFloat() / holdMs).coerceIn(0f, 1f)

    /**
     * Насколько сила отличается от калибровки (null - всё в порядке или разминка ещё не пройдена)
     */
    val drift: StrengthDrift?
        get() {
            if (!isPassed) return null
            val strong = (0..1).any { matchedMs[it] > 0 && saturatedMs[it].toFloat() / matchedMs[it] >= saturatedShare }
            return when {
                strong -> StrengthDrift.TOO_STRONG
                peak.any { it < weakPeak } -> StrengthDrift.TOO_WEAK
                else -> null
            }
        }

    /**
     * @param code - код состояния каналов из SignalState
     * @param flexor - сила сгибателя 0..100 (для проверки силы на шаге «сожмите»)
     * @param extensor - сила разгибателя 0..100 (на шаге «разожмите»)
     */
    fun update(code: String, dtMs: Long, flexor: Int = 0, extensor: Int = 0) {
        val current = step ?: return
        if (failed) return
        stepMs += dtMs
        if (code == current.code) {
            heldMs += dtMs
            missMs = 0L
            val channel = when (current) {
                QuickCheckStep.SQUEEZE -> 0
                QuickCheckStep.OPEN -> 1
                QuickCheckStep.REST -> -1
            }
            if (channel >= 0) {
                val strength = if (channel == 0) flexor else extensor
                matchedMs[channel] += dtMs
                if (strength >= saturated) saturatedMs[channel] += dtMs
                peak[channel] = maxOf(peak[channel], strength)
            }
        } else {
            missMs += dtMs
            if (missMs > graceMs) heldMs = 0L
        }
        when {
            heldMs >= holdMs -> {
                stepIndex++
                heldMs = 0L
                stepMs = 0L
                missMs = 0L
            }
            stepMs >= timeoutMs -> failed = true
        }
    }

    fun restart() {
        stepIndex = 0
        heldMs = 0L
        stepMs = 0L
        missMs = 0L
        failed = false
        matchedMs.fill(0L)
        saturatedMs.fill(0L)
        peak.fill(0)
    }
}