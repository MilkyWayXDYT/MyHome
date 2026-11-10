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
 * Разминка перед игрой, она же проверка, что калибровка с прошлого раза подходит
 * На каждом шаге нужно продержать нужное состояние holdMs подряд, на шаг даётся не больше timeoutMs
 * @param holdMs - сколько держать состояние без перерыва
 * @param timeoutMs - сколько ждать на шаге, потом разминка не пройдена
 * @param graceMs - короткий провал сигнала не сбрасывает удержание
 */
class QuickCheck(
    private val holdMs: Long = 1200L,
    private val timeoutMs: Long = 10_000L,
    private val graceMs: Long = 150L
) {
    var stepIndex = 0
        private set
    var heldMs = 0L
        private set
    var failed = false
        private set
    private var stepMs = 0L
    private var missMs = 0L

    val step: QuickCheckStep? get() = QuickCheckStep.entries.getOrNull(stepIndex)
    val isPassed: Boolean get() = stepIndex >= QuickCheckStep.entries.size
    val holdProgress: Float get() = (heldMs.toFloat() / holdMs).coerceIn(0f, 1f)

    /**
     * @param code - код состояния каналов из SignalState
     */
    fun update(code: String, dtMs: Long) {
        val current = step ?: return
        if (failed) return
        stepMs += dtMs
        if (code == current.code) {
            heldMs += dtMs
            missMs = 0L
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
    }
}