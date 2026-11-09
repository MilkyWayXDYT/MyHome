package com.robotpaw.myhome.mission.mechanics.door

import com.robotpaw.myhome.mission.mechanics.core.rememberMechanicInput
import com.robotpaw.myhome.mission.mechanics.core.MechanicAttempt
import com.robotpaw.myhome.mission.mechanics.core.MissionRecorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.MissionCheckpoint
import com.robotpaw.myhome.game.MissionLog
import com.robotpaw.myhome.mission.MissionObject
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.signal.UserLevel
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.components.ProgressStrip
import com.robotpaw.myhome.ui.components.SignalBars
import com.robotpaw.myhome.ui.components.WarningLine
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

// если за это время дверь не открылась - показываем окно поддержки
private const val ATTEMPT_LIMIT_MS = 20_000L

/**
 * Задание «Дверь» (обучение): сжать кисть и держать
 * @param checkpoint - откуда продолжаем (после окна поддержки)
 * @param onDone - дверь открыта, лог для экрана результата
 * @param onPause - нужна пауза; в первый раз прогресс сохраняется, потом начинается заново
 */
@Composable
fun DoorMechanic(
    source: SignalSource,
    profile: SignalProfile,
    roomTitle: String,
    mission: Mission,
    checkpoint: MissionCheckpoint,
    onDone: (MissionLog) -> Unit,
    onPause: (MissionCheckpoint) -> Unit
) {
    val rules = remember { DoorRules(keepProgress = profile.level == UserLevel.NOVICE).apply { restore(checkpoint.progress) } }
    val recorder = remember { MissionRecorder(checkpoint.log) }
    var progress by remember { mutableFloatStateOf(checkpoint.progress) }
    val attempt = remember { MechanicAttempt() }

    val input = rememberMechanicInput(source, profile) { state, dt ->
        if (attempt.finished) return@rememberMechanicInput

        recorder.onFrame(state, dt, Channel.FLEXOR)
        val opened = rules.update(state, dt)
        progress = rules.progress
        attempt.ms += dt

        if (opened) {
            attempt.finished = true
            onDone(recorder.snapshot())
        } else if (attempt.ms >= ATTEMPT_LIMIT_MS) {
            attempt.finished = true
            val firstTime = recorder.snapshot().pauses == 0
            recorder.addPause()
            onPause(MissionCheckpoint(progress = if (firstTime) rules.progress else 0f, log = recorder.snapshot()))
        }
    }
    val state = input.state

    HomeScreen(
        title = mission.title,
        overline = "$roomTitle · задание"
    ) {
        MissionObject(mission.kind, progress = progress, modifier = Modifier.weight(1f))
        ProgressStrip(progress, HomeColors.Flexor, height = 16.dp)
        HomeCard {
            Text(
                when {
                    !input.armed -> "Расслабьте руку"
                    state.isFlexorActive && !state.isExtensorActive -> "Так, держите"
                    else -> "Сожмите кисть и держите"
                },
                fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink
            )
        }
        WarningLine(if (input.armed && state.isCoContraction) "Напряжены оба канала. Отпустите разгибатель" else null)
        SignalBars(state)
    }
}
