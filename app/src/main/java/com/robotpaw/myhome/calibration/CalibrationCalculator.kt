package com.robotpaw.myhome.calibration

import com.robotpaw.myhome.signal.ChannelSettings
import com.robotpaw.myhome.signal.SignalData
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.UserLevel
import kotlin.math.roundToInt


/**
 * Итог калибровки
 *
 * @param profile готовый профиль или null, если сигнал слишком слабый
 * @param flexorPurity доля времени сжатия, когда разгибатель не перешел свой порог (0..1)
 * @param extensorPurity доля времени сжатия, когда сгибатель не перешел свой порог (0..1)
 * @param problems список проблем
 */
data class CalibrationResult (
    val profile: SignalProfile?,
    val flexorPurity: Float,
    val extensorPurity: Float,
    val problems: List<String>
) {
    val isSuccess: Boolean get() = profile != null
    val purity: Float get() = minOf(flexorPurity, extensorPurity)
}


/**
 * Считает пороги по трем записям: покой, максимальное сжатие, максимальное раскрытие (0..100)
 */
object CalibrationCalculator {
    const val MIN_RANGE = 15
    const val THRESHOLD_FRACTION = 0.30f
    const val HYSTERESIS_FRACTION = 0.10f
    const val EXPERIENCED_PURITY = 0.85f

    private fun channel(rest: Int, max: Int): ChannelSettings? {
        val range = max - rest
        if (range < MIN_RANGE) return null

        val threshold = rest + (THRESHOLD_FRACTION * range).roundToInt()
        val hysteresis = (HYSTERESIS_FRACTION * range).roundToInt().coerceAtLeast(1)
        return ChannelSettings(
            rest = rest,
            max = max,
            threshold = threshold,
            hysteresis = hysteresis,
            deadZone = minOf(rest, threshold-hysteresis).coerceAtLeast(0)
        )
    }

    private inline fun purity(
        samples: List<SignalData>,
        target: (SignalData) -> Int,
        targetThreshold: Int,
        other: (SignalData) -> Int,
        otherThreshold: Int
    ): Float {
        val active = samples.filter { target(it) >= targetThreshold }
        if (active.isEmpty()) return 0f
        val clean = active.count {other(it) < otherThreshold}
        return clean.toFloat() / active.size
    }

    internal fun percentile(values: List<Int>, p: Float): Int {
        if (values.isEmpty()) return 0
        val sorted = values.sorted()
        val index = ((sorted.size - 1) * p).roundToInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }

    private fun percent(value: Float): String = "${(value * 100).roundToInt()}%"

    fun calculate(
        rest: List<SignalData>,
        flexorStep: List<SignalData>,
        extensorStep: List<SignalData>
    ): CalibrationResult {
        val problems = mutableListOf<String>()

        val flexor = channel(
            rest = percentile(rest.map {it.flexor}, 0.9f),
            max = percentile(flexorStep.map {it.flexor}, 0.9f)
        )
        val extensor = channel(
            rest = percentile(rest.map {it.extensor}, 0.9f),
            max = percentile(extensorStep.map {it.extensor}, 0.9f)
        )

        if (flexor == null) problems += "Сигнал сгибателя слишком слабый - сожмите кисть сильнее"
        if (extensor == null)problems += "Сигнал разгибателя слишком слабый - раскройте кисть сильнее"

        if (flexor == null || extensor == null) {
            return CalibrationResult(null, 0f, 0f, problems)
        }

        val flexorPurity = purity(
            samples = flexorStep,
            target = {it.flexor}, targetThreshold = flexor.threshold,
            other = { it.extensor}, otherThreshold = extensor.threshold
        )
        val extensorPurity = purity(
            samples = extensorStep,
            target = {it.extensor}, targetThreshold = extensor.threshold,
            other = {it.flexor}, otherThreshold = flexor.threshold
        )

        if (flexorPurity < EXPERIENCED_PURITY) {
            problems += "При сжатии напрягается и разгибатель (${percent(1f - flexorPurity)} времени)"
        }

        if (extensorPurity < EXPERIENCED_PURITY) {
            problems += "При раскрытии напрягается и сгибатель (${percent(1f - extensorPurity)} времени)"
        }

        val level = if (minOf(flexorPurity, extensorPurity) >= EXPERIENCED_PURITY){
            UserLevel.EXPERIENCED
        } else{
            UserLevel.NOVICE
        }

        val profile = SignalProfile(
            flexor = flexor,
            extensor = extensor,
            level = level,
            holdToSelectMs = when (level) {
                UserLevel.NOVICE -> SignalProfile.HOLD_NOVICE_MS
                UserLevel.EXPERIENCED -> SignalProfile.HOLD_EXPERIENCED_MS
            }
        )

        return CalibrationResult(profile, flexorPurity, extensorPurity, problems)
    }
}

