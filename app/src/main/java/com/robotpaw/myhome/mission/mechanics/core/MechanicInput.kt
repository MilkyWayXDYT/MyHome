package com.robotpaw.myhome.mission.mechanics.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.robotpaw.myhome.signal.SignalProcessor
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.signal.SignalState

// кадр длиннее считаем разрывом (пауза, свёрнутое приложение), чтобы он не засчитался как время игры
private const val MAX_DT_MS = 100L

/**
 * Текущая попытка задания, общая для всех механик
 * @param ms - сколько времени идёт попытка (по кадрам)
 * @param finished - уже вызвали onDone или onPause, чтобы не вызвать повторно
 */
class MechanicAttempt {
    var ms = 0L
    var finished = false
}

/**
 * То, что механика показывает на экране
 * @param state - последнее обработанное состояние сигнала (для столбиков и подсказок)
 * @param armed - false, пока игрок не расслабил руку после выбора пункта меню
 */
class MechanicInput {
    var state by mutableStateOf(SignalState.EMPTY)
        internal set
    var armed by mutableStateOf(false)
        internal set
}

/**
 * Общий для всех механик подписчик на сигнал: один SignalProcessor на экран, колбэк через rememberUpdatedState
 * Пока рука не расслаблена, кадры в onFrame не отдаются: удержание, которым выбрали «Начать», не должно засчитаться в задании
 * @param onFrame - вызывается на каждом кадре; dtMs - время с прошлого кадра по timestampMs
 */
@Composable
fun rememberMechanicInput(
    source: SignalSource,
    profile: SignalProfile,
    onFrame: (state: SignalState, dtMs: Long) -> Unit
): MechanicInput {
    val input = remember { MechanicInput() }
    val processor = remember(profile) { SignalProcessor(profile) }
    val currentOnFrame by rememberUpdatedState(onFrame)

    LaunchedEffect(source, processor) {
        var lastTimestamp = -1L
        source.frames.collect { frame ->
            val state = processor.process(frame)
            input.state = state

            if (!input.armed) {
                if (state.isRest) input.armed = true
                lastTimestamp = frame.timestampMs
                return@collect
            }

            val dt = if (lastTimestamp < 0L) 0L else (frame.timestampMs - lastTimestamp).coerceIn(0L, MAX_DT_MS)
            lastTimestamp = frame.timestampMs
            currentOnFrame(state, dt)
        }
    }
    return input
}
