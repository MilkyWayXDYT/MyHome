package com.robotpaw.myhome.calibration

import com.robotpaw.myhome.signal.SignalData

enum class CalibrationStep { REST, FLEXOR, EXTENSOR }

/**
 * Фаза калибровки
 */
sealed interface CalibrationPhase {
    val stepIndex: Int

    /**
     * Окно перед шагом, ждём confirm()
     */
    data class Intro(
        val step: CalibrationStep,
        override val stepIndex: Int
    ) : CalibrationPhase

    data class Preparing(
        val step: CalibrationStep,
        override val stepIndex: Int,
        val remainingMs: Long
    ) : CalibrationPhase

    /**
     * Запись шага
     * @param isHolding - человек держит нужное состояние, иначе запись на паузе
     * @param waitingMs - сколько ждём нужный сигнал без перерыва
     */
    data class Recording(
        val step: CalibrationStep,
        override val stepIndex: Int,
        val progress: Float,
        val remainingMs: Long,
        val isHolding: Boolean = true,
        val waitingMs: Long = 0L
    ) : CalibrationPhase

    /**
     * Шаг записан, пауза перед следующим
     * @param step - шаг, который только что закончился
     */
    data class StepDone(
        val step: CalibrationStep,
        override val stepIndex: Int
    ) : CalibrationPhase

    data class Finished(val result: CalibrationResult) : CalibrationPhase {
        override val stepIndex: Int get() = CalibrationController.STEPS.size
    }
}

/**
 * Сценарий калибровки, время берётся из timestampMs кадров
 * Шаг: окно (если requireConfirm) -> отсчёт -> запись -> пауза stepDoneMs
 *
 * @param reactionMs - начало каждого удержания не пишется (защита от скачков)
 * @param graceMs - провал сигнала короче этого не прерывает удержание
 * @param stepTimeoutMs - если шаг не записался за это время, калибровка заканчивается неудачей
 * @param maxFrameGapMs - максимум времени между кадрами, чтобы шаг не проскочил при подтормаживании
 */
class CalibrationController(
    private val prepareMs: Long = 3000L,
    private val recordMs: Long = 3000L,
    private val reactionMs: Long = 500L,
    private val graceMs: Long = 150L,
    private val stepTimeoutMs: Long = 20_000L,
    private val maxFrameGapMs: Long = 100L,
    private val requireConfirm: Boolean = true,
    private val stepDoneMs: Long = 1500L
) {
    private var stepIndex = 0
    private var recording = false
    private var lastFrameMs: Long? = null
    private var prepareLeftMs = prepareMs
    private var doneLeftMs = 0L
    private var recordedMs = 0L    // сколько записано в этом шаге
    private var holdMs = 0L        // сколько держат без перерыва
    private var missMs = 0L        // сколько длится текущий провал сигнала
    private var stepTotalMs = 0L   // сколько длится запись шага вместе с паузами
    private var restFlexor = 0
    private var restExtensor = 0
    private val samples = mutableMapOf<CalibrationStep, MutableList<SignalData>>()

    var phase: CalibrationPhase = startOf(0)
        private set

    /**
     * Закрыть окно перед шагом и начать отсчёт
     */
    fun confirm() {
        val p = phase
        if (p is CalibrationPhase.Intro) {
            lastFrameMs = null
            phase = CalibrationPhase.Preparing(p.step, p.stepIndex, prepareMs)
        }
    }

    fun restart() {
        stepIndex = 0
        restFlexor = 0
        restExtensor = 0
        samples.clear()
        lastFrameMs = null
        resetStep()
        phase = startOf(0)
    }

    fun onFrame(frame: SignalData): CalibrationPhase {
        if (phase is CalibrationPhase.Finished || phase is CalibrationPhase.Intro) return phase

        val dt = lastFrameMs?.let { (frame.timestampMs - it).coerceIn(0L, maxFrameGapMs) } ?: 0L
        lastFrameMs = frame.timestampMs
        if (phase is CalibrationPhase.StepDone) {
            doneLeftMs -= dt
            if (doneLeftMs <= 0L) phase = startOf(stepIndex)
            return phase
        }

        val step = STEPS[stepIndex]

        if (!recording) {
            prepareLeftMs -= dt
            phase = if (prepareLeftMs <= 0L) {
                recording = true
                CalibrationPhase.Recording(step, stepIndex, 0f, recordMs, isHolding(step, frame))
            } else {
                CalibrationPhase.Preparing(step, stepIndex, prepareLeftMs)
            }
            return phase
        }

        stepTotalMs += dt
        val holdingNow = isHolding(step, frame)
        if (holdingNow) {
            holdMs += dt
            missMs = 0L
        } else {
            missMs += dt
            if (missMs > graceMs) holdMs = 0L
        }
        val holding = holdingNow || (holdMs > 0L && missMs <= graceMs)

        // прогресс двигается только вместе с записью
        if (holdingNow && holdMs >= reactionMs) {
            samples.getOrPut(step) { mutableListOf() }.add(frame)
            recordedMs += dt
        }

        phase = when {
            recordedMs >= recordMs -> nextStep()
            stepTotalMs >= stepTimeoutMs -> fail(step)
            else -> CalibrationPhase.Recording(
                step, stepIndex,
                progress = recordedMs.toFloat() / recordMs,
                remainingMs = recordMs - recordedMs,
                isHolding = holding,
                waitingMs = if (holding) 0L else missMs
            )
        }
        return phase
    }

    private fun isHolding(step: CalibrationStep, frame: SignalData): Boolean = when (step) {
        CalibrationStep.REST -> frame.flexor < REST_MAX_LEVEL && frame.extensor < REST_MAX_LEVEL
        CalibrationStep.FLEXOR -> frame.flexor >= restFlexor + CalibrationCalculator.MIN_RANGE
        CalibrationStep.EXTENSOR -> frame.extensor >= restExtensor + CalibrationCalculator.MIN_RANGE
    }

    private fun resetStep() {
        recording = false
        prepareLeftMs = prepareMs
        recordedMs = 0L
        holdMs = 0L
        missMs = 0L
        stepTotalMs = 0L
    }

    private fun nextStep(): CalibrationPhase {
        if (STEPS[stepIndex] == CalibrationStep.REST) {
            val rest = samples[CalibrationStep.REST].orEmpty()
            restFlexor = CalibrationCalculator.percentile(rest.map { it.flexor }, 0.9f)
            restExtensor = CalibrationCalculator.percentile(rest.map { it.extensor }, 0.9f)
        }

        val done = CalibrationPhase.StepDone(STEPS[stepIndex], stepIndex)
        stepIndex++
        resetStep()

        if (stepIndex >= STEPS.size) {
            return CalibrationPhase.Finished(
                CalibrationCalculator.calculate(
                    rest = samples[CalibrationStep.REST].orEmpty(),
                    flexorStep = samples[CalibrationStep.FLEXOR].orEmpty(),
                    extensorStep = samples[CalibrationStep.EXTENSOR].orEmpty()
                )
            )
        }
        if (stepDoneMs <= 0L) return startOf(stepIndex)
        doneLeftMs = stepDoneMs
        return done
    }

    private fun startOf(index: Int): CalibrationPhase =
        if (requireConfirm) CalibrationPhase.Intro(STEPS[index], index)
        else CalibrationPhase.Preparing(STEPS[index], index, prepareMs)

    /** Если шаг не записался за отведённое время  */
    private fun fail(step: CalibrationStep): CalibrationPhase {
        val reason = when (step) {
            CalibrationStep.REST -> "Не получилось записать покой: рука всё время была напряжена."
            CalibrationStep.FLEXOR -> "Не дождались сжатия кисти. Проверьте, как сидит протез."
            CalibrationStep.EXTENSOR -> "Не дождались раскрытия кисти. Проверьте, как сидит протез."
        }
        return CalibrationPhase.Finished(CalibrationResult(null, 0f, 0f, listOf(reason)))
    }

    companion object {
        val STEPS = listOf(CalibrationStep.REST, CalibrationStep.FLEXOR, CalibrationStep.EXTENSOR)

        // в покое оба канала должны быть ниже
        const val REST_MAX_LEVEL = 30

        // через сколько без сигнала показать подсказку про протез
        const val FIT_HINT_AFTER_MS = 5000L
    }
}