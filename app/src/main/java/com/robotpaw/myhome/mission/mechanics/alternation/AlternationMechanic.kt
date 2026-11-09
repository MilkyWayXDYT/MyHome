package com.robotpaw.myhome.mission.mechanics.alternation

import com.robotpaw.myhome.mission.mechanics.core.rememberMechanicInput
import com.robotpaw.myhome.mission.mechanics.core.MechanicAttempt
import com.robotpaw.myhome.mission.mechanics.core.MissionRecorder
import com.robotpaw.myhome.mission.mechanics.core.ZoneGauge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.components.ProgressStrip
import com.robotpaw.myhome.ui.components.SignalBars
import com.robotpaw.myhome.ui.components.WarningLine
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

/**
 * Общая механика для заданий на чередование сгибания и разгибания (картина, подушки)
 * Правила и тексты берутся из config, поэтому для нового задания такого типа достаточно нового config
 * @param checkpoint - откуда продолжаем (после окна поддержки)
 * @param onDone - задание выполнено, лог для экрана результата
 * @param onPause - нужна пауза; в первый раз прогресс сохраняется, потом начинается заново
 */
@Composable
fun AlternationMechanic(
    source: SignalSource,
    profile: SignalProfile,
    roomTitle: String,
    mission: Mission,
    config: AlternationConfig,
    checkpoint: MissionCheckpoint,
    onDone: (MissionLog) -> Unit,
    onPause: (MissionCheckpoint) -> Unit
) {
    val rules = remember { AlternationRules(config.steps).apply { restore(checkpoint.progress) } }
    val recorder = remember { MissionRecorder(checkpoint.log) }
    val attempt = remember { MechanicAttempt() }

    var total by remember { mutableFloatStateOf(rules.progress) }
    var stepProgress by remember { mutableFloatStateOf(rules.stepProgress) }
    var stepIndex by remember { mutableIntStateOf(rules.stepIndex) }

    val input = rememberMechanicInput(source, profile) { state, dt ->
        if (attempt.finished) return@rememberMechanicInput

        recorder.onFrame(state, dt, rules.current?.channel)
        val event = rules.update(state, dt)
        if (event.overshoot) recorder.addError()

        total = rules.progress
        stepProgress = rules.stepProgress
        stepIndex = rules.stepIndex
        attempt.ms += dt

        if (rules.isDone) {
            attempt.finished = true
            onDone(recorder.snapshot())
        } else if (attempt.ms >= config.attemptLimitMs) {
            attempt.finished = true
            val firstTime = recorder.snapshot().pauses == 0
            recorder.addPause()
            onPause(MissionCheckpoint(progress = if (firstTime) rules.progress else 0f, log = recorder.snapshot()))
        }
    }
    val state = input.state
    val step = config.steps.getOrNull(stepIndex)
    val zone = step?.zone
    val stepColor = if (step?.channel == Channel.EXTENSOR) HomeColors.Extensor else HomeColors.Flexor
    val strengthNow = if (step?.channel == Channel.EXTENSOR) state.extensorStrength else state.flexorStrength
    val onlyRequired = step != null && state.code == (if (step.channel == Channel.EXTENSOR) "01" else "10")
    val tooWeak = onlyRequired && zone != null && strengthNow < zone.first

    HomeScreen(
        title = mission.title,
        overline = "$roomTitle · задание"
    ) {
        MissionObject(mission.kind, progress = total, modifier = Modifier.weight(1f))
        HomeCard {
            Text(
                "Шаг ${(stepIndex + 1).coerceAtMost(config.steps.size)} из ${config.steps.size}" +
                        if (zone != null) " · держите силу ${zone.first}–${zone.last}%" else "",
                fontSize = HomeType.Label, color = HomeColors.InkSecondary
            )
            Text(
                if (!input.armed) "Расслабьте руку" else step?.text ?: "Готово",
                fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink
            )
        }
        if (zone != null) ZoneGauge(strengthNow, zone, stepColor)
        ProgressStrip(stepProgress, stepColor, height = 16.dp)
        WarningLine(
            when {
                !input.armed -> null
                state.isCoContraction -> "Напряжены оба канала. Оставьте только нужный"
                rules.isOvershooting -> "Слишком сильно. Ослабьте нажим"
                tooWeak -> "Слишком слабо. Нажмите сильнее"
                else -> null
            }
        )
        SignalBars(state)
    }
}
