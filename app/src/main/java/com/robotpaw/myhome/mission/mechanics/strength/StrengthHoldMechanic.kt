package com.robotpaw.myhome.mission.mechanics.strength

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
import com.robotpaw.myhome.mission.mechanics.core.MechanicAttempt
import com.robotpaw.myhome.mission.mechanics.core.MissionRecorder
import com.robotpaw.myhome.mission.mechanics.core.rememberMechanicInput
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.components.ProgressStrip
import com.robotpaw.myhome.ui.components.SignalBars
import com.robotpaw.myhome.ui.components.WarningLine
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

/**
 * Общая механика для заданий на силу и удержание (мусор, пылесос, зеркало)
 * Правила и тексты берутся из config, поэтому для нового задания такого типа достаточно нового config
 * @param checkpoint - откуда продолжаем (после окна поддержки)
 * @param onDone - задание выполнено, лог для экрана результата
 * @param onPause - нужна пауза; в первый раз прогресс сохраняется, потом начинается заново
 */
@Composable
fun StrengthHoldMechanic(
    source: SignalSource,
    profile: SignalProfile,
    roomTitle: String,
    mission: Mission,
    config: StrengthHoldConfig,
    checkpoint: MissionCheckpoint,
    onDone: (MissionLog) -> Unit,
    onPause: (MissionCheckpoint) -> Unit
) {
    val rules = remember { StrengthHoldRules(config).apply { restore(checkpoint.progress) } }
    val recorder = remember { MissionRecorder(checkpoint.log) }
    val attempt = remember { MechanicAttempt() }
    var progress by remember { mutableFloatStateOf(checkpoint.progress) }

    val input = rememberMechanicInput(source, profile) { state, dt ->
        if (attempt.finished) return@rememberMechanicInput

        recorder.onFrame(state, dt, Channel.FLEXOR)
        val event = rules.update(state, dt)
        if (event.drop) recorder.addError()
        progress = rules.progress
        attempt.ms += dt

        if (event.done) {
            attempt.finished = true
            onDone(recorder.snapshot())
        } else if (attempt.ms >= config.attemptLimitMs) {
            attempt.finished = true
            val firstTime = recorder.snapshot().pauses == 0
            recorder.addPause()
            onPause(
                MissionCheckpoint(
                    progress = if (firstTime) rules.progress else 0f,
                    log = recorder.snapshot()
                )
            )
        }
    }
    val state = input.state
    val tooWeak = state.code == "10" && state.flexorStrength < config.minStrength

    HomeScreen(
        title = mission.title,
        overline = "$roomTitle · задание"
    ) {
        MissionObject(mission.kind, progress = progress, modifier = Modifier.weight(1f))
        HomeCard {
            Text(
                "Сила не ниже ${config.minStrength}%",
                fontSize = HomeType.Label, color = HomeColors.InkSecondary
            )
            Text(
                when {
                    !input.armed -> "Расслабьте руку"
                    tooWeak -> "Сожмите сильнее"
                    state.code == "10" -> "Так, держите"
                    else -> config.text
                },
                fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink
            )
        }
        ProgressStrip(progress, HomeColors.Flexor, height = 16.dp)
        WarningLine(if (input.armed && state.isCoContraction) "Напряжены оба канала. Отпустите разгибатель" else null)
        SignalBars(state)
    }
}
