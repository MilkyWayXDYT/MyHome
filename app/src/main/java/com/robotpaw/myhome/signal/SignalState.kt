package com.robotpaw.myhome.signal

data class SignalState(
    val flexor: Int,
    val extensor: Int,
    val isFlexorActive: Boolean,
    val isExtensorActive: Boolean,
    val code: String,
    val description: String
)

fun calculateSignalState(
    flexor: Int,
    extensor: Int,
    threshold: Int = 30
): SignalState {
    val safeFlexor = flexor.coerceIn(0, 100)
    val safeExtensor = extensor.coerceIn(0, 100)

    val flexorActive = safeFlexor >= threshold
    val extensorActive = safeExtensor >= threshold

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
        flexor = safeFlexor,
        extensor = safeExtensor,
        isFlexorActive = flexorActive,
        isExtensorActive = extensorActive,
        code = code,
        description = description
    )
}
