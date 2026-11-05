package com.robotpaw.myhome.signal

enum class Channel { FLEXOR, EXTENSOR }

/**
 * настройки канала, значения (0..100)
 * @param rest - уровень сигнала в покое
 * @param max - уровень максимального сокращения
 * @param threshold - порог активации
 * @param hysteresis - канал выключается тлько ниже разницы между порогом активации и гистерезисом ( threshold - hysteresis)
 * @param deadZone - все что ниже этого значение считается нулем
 */
data class ChannelSettings (
    val rest: Int = 5,
    val max: Int = 100,
    val threshold: Int = 30,
    val hysteresis: Int = 5,
    val deadZone: Int = 3
)

// уровень пользователя, определяется по чистоте сигнала на калибровке
enum class UserLevel { NOVICE, EXPERIENCED }

/**
 * Профиль управления пользователем
 * @param minPulseMs - защита от случайного срабатывания (в меню)
 * @param holdToSelectMs - время удержания для выбора пункта меню
 */
data class SignalProfile (
    val flexor: ChannelSettings = ChannelSettings(),
    val extensor: ChannelSettings = ChannelSettings(),
    val level: UserLevel = UserLevel.NOVICE,
    val minPulseMs: Long = MIN_PULSE_MS,
    val holdToSelectMs: Long = HOLD_NOVICE_MS
) {
    fun settings(channel: Channel): ChannelSettings = when (channel){
        Channel.FLEXOR -> flexor
        Channel.EXTENSOR -> extensor
    }

    companion object {
        const val HOLD_NOVICE_MS = 2000L
        const val HOLD_EXPERIENCED_MS = 1500L
        const val MIN_PULSE_MS = 300L
    }
}