package com.robotpaw.myhome.mission.mechanics.core

import com.robotpaw.myhome.game.MissionLog
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalState

/**
 * Собирает MissionLog по кадрам сигнала
 * Механике достаточно вызывать onFrame на каждом кадре, а ошибки своей игры добавлять через addError
 * @param start - лог, с которого продолжаем (после паузы)
 */
class MissionRecorder(start: MissionLog = MissionLog()) {
    private var log = start
    private var wasCoContraction = false

    /**
     * @param state - обработанное состояние сигнала
     * @param dtMs - сколько прошло с прошлого кадра
     * @param channel - канал, с которым работает задание (для силы), null - не считать силу
     */
    fun onFrame(state: SignalState, dtMs: Long, channel: Channel? = null) {
        val co = state.isCoContraction
        val strength = when (channel) {
            Channel.FLEXOR -> state.flexorStrength
            Channel.EXTENSOR -> state.extensorStrength
            null -> 0
        }
        val channelActive = channel != null && state.isActive(channel)

        log = log.copy(
            durationMs = log.durationMs + dtMs,
            activeMs = if (channelActive) log.activeMs + dtMs else log.activeMs,
            coContractionMs = if (co) log.coContractionMs + dtMs else log.coContractionMs,
            coContractionEvents = if (co && !wasCoContraction) log.coContractionEvents + 1 else log.coContractionEvents,
            peakStrength = if (channelActive) maxOf(log.peakStrength, strength) else log.peakStrength,
            strengthSum = if (channelActive) log.strengthSum + strength else log.strengthSum,
            strengthFrames = if (channelActive) log.strengthFrames + 1 else log.strengthFrames
        )
        wasCoContraction = co
    }

    fun addError() {
        log = log.copy(errors = log.errors + 1)
    }

    // запоминает лучшую серию попаданий подряд
    fun recordStreak(streak: Int) {
        if (streak > log.bestStreak) log = log.copy(bestStreak = streak)
    }

    fun addPause() {
        log = log.copy(pauses = log.pauses + 1)
    }

    fun snapshot(): MissionLog = log
}
