package com.robotpaw.myhome.signal

import com.robotpaw.myhome.signal.SignalProfile
import kotlin.math.roundToInt


/**
 * @param timestampMs - время кадра
 * @param flexorStrength - сила сгибателя конкретного пользователя (0..100)
 * @param extensorStrength - сила разгибателя конкретного пользователя (0..100)
 */

data class SignalState(
    val flexorSignal: Int,
    val extensorSignal: Int,
    val isFlexorActive: Boolean,
    val isExtensorActive: Boolean,
    val code: String,
    val description: String,

    val timestampMs: Long,
    val flexorStrength: Int,
    val extensorStrength: Int
) {
    val isCoContraction: Boolean get() = isFlexorActive && isExtensorActive
    val isRest: Boolean get() = !isFlexorActive && !isExtensorActive

    fun isActive(channel:Channel): Boolean = when (channel) {
        Channel.FLEXOR -> isFlexorActive
        Channel.EXTENSOR -> isExtensorActive
    }

    companion object {
        val EMPTY = SignalState(0, 0,false,false, "00", "Покой", 0L, 0, 0)
    }
}


/**
 * Считает состояния по двум каналам
 */
fun calculateSignalState(
    flexor: Int,
    extensor: Int,
    threshold: Int = 30,
    flexorSettings: ChannelSettings = ChannelSettings(threshold = threshold),
    extensorSettings: ChannelSettings = ChannelSettings(threshold = threshold),
    previous: SignalState? = null,
    timestampMs: Long = 0L
): SignalState {
    val safeFlexor = applyDeadZone(flexor.coerceIn(0, 100), flexorSettings)
    val safeExtensor = applyDeadZone(extensor.coerceIn(0, 100), extensorSettings)

    val flexorActive = isChannelActive(safeFlexor, flexorSettings, previous?.isFlexorActive == true)
    val extensorActive = isChannelActive(safeExtensor, extensorSettings, previous?.isExtensorActive == true)

    val code = when {
        !flexorActive && !extensorActive -> "00"
        flexorActive && !extensorActive -> "10"
        !flexorActive && extensorActive -> "01"
        else -> "11"
    }

    val description = when (code) {
        "00" -> "Покой"
        "10" -> "Сгибание"
        "01" -> "Разгибание"
        "11" -> "Активны оба канала"
        else -> "Неизвестное состояние"
    }

    return SignalState(
        flexorSignal = safeFlexor,
        extensorSignal = safeExtensor,
        isFlexorActive = flexorActive,
        isExtensorActive = extensorActive,
        code = code,
        description = description,
        timestampMs= timestampMs,
        flexorStrength = strength(safeFlexor, flexorSettings),
        extensorStrength = strength(safeExtensor, extensorSettings)
    )
}

private fun applyDeadZone(value: Int, settings: ChannelSettings): Int =
    if (value < settings.deadZone) 0 else value

private fun isChannelActive(value: Int, settings: ChannelSettings, wasActive: Boolean): Boolean =
    if(wasActive) value >= settings.threshold - settings.hysteresis
    else value >= settings.threshold

/**
 * Процент от личного диапазона
 */
private fun strength(value: Int, settings: ChannelSettings): Int {
    val range = settings.max - settings.rest
    if (range <= 0) return 0
    return ((value - settings.rest) * 100f / range).roundToInt().coerceIn(0, 100)
}