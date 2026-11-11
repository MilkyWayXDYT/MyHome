package com.robotpaw.myhome.signal

sealed interface ControlEvent {
    /**
     * Короткий сигнал канала, засчитывается в момент отпускания (от minPulseMs до holdMs)
     */
    data class Pulse(val channel: Channel) : ControlEvent

    /**
     * Канал считывающий удержание, засчитывается в момент заполнения шкалы, не дожидаясь отпускания (мб переделать на срабатывание после отпускания)
     */
    data class Hold(val channel: Channel) : ControlEvent
}

class HoldDetector (
    var minPulseMs: Long = SignalProfile.MIN_PULSE_MS,
    var holdMs: Long = SignalProfile.HOLD_NOVICE_MS
) {
    private var activeChannel: Channel? = null
    private var startMs = 0L
    private var holdFired = false
    private var waitingForRelease = true

    /**
     * Какой канал сейчас удерживается
     */
    val progressChannel: Channel? get() = activeChannel

    /**
     * Насколько заполнено удержание текущего канала (0..1)
     */
    var progress: Float = 0f
        private set

    fun reset(){
        activeChannel = null
        holdFired = false
        progress = 0f
        waitingForRelease = true
    }

    fun update(state: SignalState): ControlEvent? {
        val now = state.timestampMs

        if (state.isCoContraction) {
            activeChannel = null
            holdFired = false
            progress = 0f
            waitingForRelease = true
            return null
        }

        if (waitingForRelease){
            if (state.isRest) waitingForRelease = false
            return null
        }

        val current = when {
            state.isFlexorActive -> Channel.FLEXOR
            state.isExtensorActive -> Channel.EXTENSOR
            else -> null
        }

        if (current == activeChannel) {
            if(current != null) {
                val held = now - startMs
                progress = (held.toFloat() / holdMs).coerceIn(0f, 1f)
                if (!holdFired && held >= holdMs) {
                    holdFired = true
                    return ControlEvent.Hold(current)
                }
            }
            return null
        }

        val previous = activeChannel
        val previousHeld = now - startMs
        val previousFired = holdFired

        activeChannel = current
        startMs = now
        holdFired = false
        progress = 0f

        return if (previous != null && !previousFired && previousHeld >= minPulseMs) {
            ControlEvent.Pulse(previous)
        } else {
            null
        }
    }
}