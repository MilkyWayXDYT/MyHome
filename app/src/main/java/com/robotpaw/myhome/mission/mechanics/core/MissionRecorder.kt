package com.robotpaw.myhome.mission.mechanics.core

import com.robotpaw.myhome.game.MissionLog
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalState

// нужный канал пропал дольше, чем на это время, - считаем срывом сигнала
private const val INTERRUPTION_GRACE_MS = 200L

// после смены нужного канала игроку даётся время отпустить прежний канал
private const val WRONG_CHANNEL_GRACE_MS = 500L

/**
 * Собирает MissionLog по кадрам сигнала
 * Механике достаточно вызывать onFrame на каждом кадре, а ошибки своей игры добавлять через addError
 * @param start - лог, с которого продолжаем (после паузы)
 */
class MissionRecorder(start: MissionLog = MissionLog()) {
    private var log = start
    private var wasCoContraction = false

    // слежение за стабильностью сигнала в рамках одного нужного канала
    private var trackedChannel: Channel? = null
    private var engaged = false          // нужный канал уже включали в этом шаге
    private var gapMs = 0L               // сколько нужный канал уже отсутствует
    private var gapCounted = false       // срыв из этого перерыва уже засчитан
    private var stepAgeMs = 0L           // сколько нужный канал не менялся
    private var wasWrongChannel = false
    private var previousStrength: Int? = null   // сила в прошлом кадре, пока нужный канал был активен
    private var restMs = 0L              // сколько длится текущий покой посреди задания

    /**
     * @param state - обработанное состояние сигнала
     * @param dtMs - сколько прошло с прошлого кадра
     * @param channel - канал, с которым работает задание (для силы и стабильности), null - не считать
     * @param trackStability - считать срывы сигнала и не тот канал; выключают механики со своими правилами ошибок
     */
    fun onFrame(state: SignalState, dtMs: Long, channel: Channel? = null, trackStability: Boolean = true) {
        val co = state.isCoContraction
        val strength = when (channel) {
            Channel.FLEXOR -> state.flexorStrength
            Channel.EXTENSOR -> state.extensorStrength
            null -> 0
        }
        val channelActive = channel != null && state.isActive(channel)

        var interruptions = 0
        var wrongEvents = 0
        var offTargetMs = 0L

        if (channel != trackedChannel) {
            // нужный канал сменился (новый шаг): слежение начинается заново
            trackedChannel = channel
            engaged = false
            gapMs = 0L
            gapCounted = false
            stepAgeMs = 0L
            wasWrongChannel = false
            previousStrength = null
        } else {
            stepAgeMs += dtMs
        }

        if (trackStability && channel != null) {
            val other = if (channel == Channel.FLEXOR) Channel.EXTENSOR else Channel.FLEXOR
            val wrong = !channelActive && state.isActive(other) && stepAgeMs > WRONG_CHANNEL_GRACE_MS
            val lost = !channelActive && engaged

            if (channelActive) {
                engaged = true
                gapMs = 0L
                gapCounted = false
            } else if (engaged) {
                gapMs += dtMs
                if (!gapCounted && gapMs >= INTERRUPTION_GRACE_MS) {
                    interruptions = 1
                    gapCounted = true
                }
            }
            if (wrong && !wasWrongChannel) wrongEvents = 1
            wasWrongChannel = wrong
            if (lost || wrong) offTargetMs = dtMs
        }

        // скачки силы между соседними кадрами: шум и дрожание нажатия
        var jitter = 0L
        var jitterFrame = 0
        if (channelActive) {
            previousStrength?.let { jitter = kotlin.math.abs(strength - it).toLong(); jitterFrame = 1 }
            previousStrength = strength
        } else {
            previousStrength = null
        }

        // покой считается только после первого напряжения: замереть посреди задания, а не до начала
        val resting = !state.isActive(Channel.FLEXOR) && !state.isActive(Channel.EXTENSOR) && log.activeMs > 0L
        restMs = if (resting) restMs + dtMs else 0L

        log = log.copy(
            longestRestMs = maxOf(log.longestRestMs, restMs),
            durationMs = log.durationMs + dtMs,
            activeMs = if (channelActive) log.activeMs + dtMs else log.activeMs,
            coContractionMs = if (co) log.coContractionMs + dtMs else log.coContractionMs,
            coContractionEvents = if (co && !wasCoContraction) log.coContractionEvents + 1 else log.coContractionEvents,
            peakStrength = if (channelActive) maxOf(log.peakStrength, strength) else log.peakStrength,
            strengthSum = if (channelActive) log.strengthSum + strength else log.strengthSum,
            strengthFrames = if (channelActive) log.strengthFrames + 1 else log.strengthFrames,
            interruptions = log.interruptions + interruptions,
            wrongChannelEvents = log.wrongChannelEvents + wrongEvents,
            offTargetMs = log.offTargetMs + offTargetMs,
            jitterSum = log.jitterSum + jitter,
            jitterFrames = log.jitterFrames + jitterFrame
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