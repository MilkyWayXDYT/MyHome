package com.robotpaw.myhome.calibration

import com.robotpaw.myhome.signal.SignalData

/** Шаги калибровки по порядку. */
enum class CalibrationStep { REST, FLEXOR, EXTENSOR }

/** Что сейчас показывает экран калибровки. */
sealed interface CalibrationPhase {
    val stepIndex: Int

    /** Отсчёт перед записью. */
    data class Preparing(
        val step: CalibrationStep,
        override val stepIndex: Int,
        val remainingMs: Long
    ) : CalibrationPhase

    /**
     * Идёт запись шага.
     *
     * @param isHolding человек делает то, что просят (держит сжатие / раскрытие / покой).
     * Если нет — запись на паузе, прогресс стоит на месте.
     * @param waitingMs сколько уже ждём нужный сигнал без перерыва (для подсказки «проверьте протез»).
     */
    data class Recording(
        val step: CalibrationStep,
        override val stepIndex: Int,
        val progress: Float,
        val remainingMs: Long,
        val isHolding: Boolean = true,
        val waitingMs: Long = 0L
    ) : CalibrationPhase

    /** Калибровка закончена (успешно или нет — смотри result.isSuccess). */
    data class Finished(val result: CalibrationResult) : CalibrationPhase {
        override val stepIndex: Int get() = CalibrationController.STEPS.size
    }
}

/**
 * Пошаговый сценарий калибровки. Ведётся только кадрами сигнала, без таймеров и касаний:
 * время берётся из timestampMs кадра, поэтому сценарий легко тестировать.
 *
 * Каждый шаг: отсчёт [prepareMs] → запись [recordMs].
 *
 * Запись идёт, только пока человек делает то, что просят:
 * - покой — оба канала ниже [REST_MAX_LEVEL];
 * - сжатие / раскрытие — нужный канал выше уровня покоя на [CalibrationCalculator.MIN_RANGE].
 *
 * Прогресс растёт только вместе с записанными кадрами: первые [reactionMs] каждого удержания
 * не пишутся и прогресс не двигают. Поэтому одиночные скачки и шумы шаг не «проматывают».
 * Короткие провалы сигнала (до [graceMs]) удержание не прерывают — так ведёт себя
 * настоящий ЭМГ, когда электрод на мгновение теряет контакт.
 *
 * Если за [stepTimeoutMs] шаг так и не записался, калибровка сразу заканчивается
 * с понятной причиной. Шаг никогда не засчитывается «с тем, что есть».
 *
 * Пауза между кадрами учитывается не больше [maxFrameGapMs]:
 * если телефон подтормозил, шаг не проскочит сам собой.
 */
class CalibrationController(
    private val prepareMs: Long = 2000L,
    private val recordMs: Long = 3000L,
    private val reactionMs: Long = 500L,
    private val graceMs: Long = 150L,
    private val stepTimeoutMs: Long = 20_000L,
    private val maxFrameGapMs: Long = 100L
) {
    private var stepIndex = 0
    private var recording = false
    private var lastFrameMs: Long? = null
    private var prepareLeftMs = prepareMs
    private var recordedMs = 0L    // сколько записано в этом шаге
    private var holdMs = 0L        // сколько держат без перерыва
    private var missMs = 0L        // сколько длится текущий провал сигнала
    private var stepTotalMs = 0L   // сколько длится запись шага вместе с паузами
    private var restFlexor = 0
    private var restExtensor = 0
    private val samples = mutableMapOf<CalibrationStep, MutableList<SignalData>>()

    var phase: CalibrationPhase = CalibrationPhase.Preparing(STEPS.first(), 0, prepareMs)
        private set

    fun restart() {
        stepIndex = 0
        restFlexor = 0
        restExtensor = 0
        samples.clear()
        lastFrameMs = null
        resetStep()
        phase = CalibrationPhase.Preparing(STEPS.first(), 0, prepareMs)
    }

    fun onFrame(frame: SignalData): CalibrationPhase {
        if (phase is CalibrationPhase.Finished) return phase

        val dt = lastFrameMs?.let { (frame.timestampMs - it).coerceIn(0L, maxFrameGapMs) } ?: 0L
        lastFrameMs = frame.timestampMs
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
        // Короткий провал сигнала удержание не прерывает.
        val holding = holdingNow || (holdMs > 0L && missMs <= graceMs)

        // Пишем и двигаем прогресс только по настоящим кадрам удержания, после времени реакции.
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
        return CalibrationPhase.Preparing(STEPS[stepIndex], stepIndex, prepareMs)
    }

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

        /** В покое оба канала должны быть ниже этого уровня, иначе запись покоя на паузе. */
        const val REST_MAX_LEVEL = 30

        /** Через сколько ожидания показать подсказку про посадку протеза. */
        const val FIT_HINT_AFTER_MS = 5000L
    }
}