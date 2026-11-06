
package com.robotpaw.myhome.signal

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Обработка сигнала на стороне игры.
 * @param smoothing - экпоненциальное сглаживание (0..1; 1 - без сглаживания)
 * @param maxStepPerFrame - максимальное изменнеие силы за один кадр в процентах
 */
class SignalProcessor(
    profile: SignalProfile = SignalProfile(),
    private val smoothing: Float = 0.5f,
    private val maxStepPerFrame: Float = 15f
) {
    var profile: SignalProfile = profile
        set(value) {
            field = value
            reset()
        }

    private var previous: SignalState? = null
    private var flexorStrength = 0f
    private var extensorStrength = 0f

    fun reset(){
        previous = null
        flexorStrength = 0f
        extensorStrength = 0f
    }

    fun process(data: SignalData): SignalState {

        val state = calculateSignalState(
            flexor = data.flexor,
            extensor = data.extensor,
            flexorSettings = profile.flexor,
            extensorSettings = profile.extensor,
            previous = previous,
            timestampMs = data.timestampMs
        )

        flexorStrength = smooth(flexorStrength, state.flexorStrength.toFloat())
        extensorStrength = smooth(extensorStrength, state.extensorStrength.toFloat())

        val smoothed = state.copy(
            flexorSignal = flexorStrength.roundToInt(),
            extensorSignal = extensorStrength.roundToInt()
        )

        previous = smoothed
        return smoothed
    }

    private fun smooth(old: Float, target: Float): Float {
        val wanted = old + (target - old) * smoothing
        val delta = wanted - old
        val limited = if (abs(delta) > maxStepPerFrame) old + sign(delta) * maxStepPerFrame
                      else wanted

        return limited.coerceIn(0f, 100f)
    }
}
