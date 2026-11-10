package com.robotpaw.myhome.mission.mechanics.rhythm

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.robotpaw.myhome.signal.UserLevel
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.components.ProgressStrip
import com.robotpaw.myhome.ui.components.SignalBars
import com.robotpaw.myhome.ui.components.WarningLine
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

private fun beatName(channel: Channel?): String = when (channel) {
    Channel.FLEXOR -> "СЖАТЬ"
    Channel.EXTENSOR -> "РАЗЖАТЬ"
    null -> ""
}

/**
 * Механика ритм-заданий (нарезка салата)
 * Новичку удары идут медленнее и задание проходится при любом числе промахов; опытному нужно уложиться в допустимое число промахов
 * Если не уложился, открывается окно поддержки и задание начинается заново
 * @param checkpoint - лог до паузы (прогресс заново, ритм не ставится на паузу посередине)
 */
@Composable
fun RhythmMechanic(
    source: SignalSource,
    profile: SignalProfile,
    roomTitle: String,
    mission: Mission,
    config: RhythmConfig,
    checkpoint: MissionCheckpoint,
    onDone: (MissionLog) -> Unit,
    onPause: (MissionCheckpoint) -> Unit
) {
    val novice = profile.level == UserLevel.NOVICE
    val rules = remember {
        RhythmRules(
            beats = config.beats,
            intervalMs = if (novice) config.noviceIntervalMs else config.experiencedIntervalMs,
            maxMisses = if (novice) Int.MAX_VALUE else config.experiencedMaxMisses,
            hitMs = config.hitMs,
            countdownMs = config.countdownMs
        )
    }
    val recorder = remember { MissionRecorder(checkpoint.log) }
    val attempt = remember { MechanicAttempt() }

    var progress by remember { mutableFloatStateOf(0f) }
    var beatFraction by remember { mutableFloatStateOf(0f) }
    var beatIndex by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var countdownLeft by remember { mutableLongStateOf(config.countdownMs) }
    var lastResult by remember { mutableStateOf("") }
    var lastHit by remember { mutableStateOf(false) }

    val input = rememberMechanicInput(source, profile) { state, dt ->
        if (attempt.finished) return@rememberMechanicInput

        // в ритме паузы между ударами нормальны, ошибки считаются промахами
        recorder.onFrame(state, dt, rules.currentChannel, trackStability = false)
        val event = rules.update(state, dt)
        if (event.hit) {
            lastResult = "Попал!"
            lastHit = true
        }
        if (event.miss) {
            recorder.addError()
            lastResult = "Мимо"
            lastHit = false
        }
        recorder.recordStreak(rules.bestStreak)

        progress = rules.progress
        beatFraction = rules.beatFraction
        beatIndex = rules.beatIndex
        streak = rules.streak
        countdownLeft = rules.countdownLeftMs

        if (rules.isFinished) {
            attempt.finished = true
            if (rules.isSuccess) {
                onDone(recorder.snapshot())
            } else {
                recorder.addPause()
                onPause(MissionCheckpoint(progress = 0f, log = recorder.snapshot()))
            }
        }
    }
    val state = input.state
    val current = config.beats.getOrNull(beatIndex)
    val next = config.beats.getOrNull(beatIndex + 1)

    HomeScreen(
        title = mission.title,
        overline = "$roomTitle · задание"
    ) {
        MissionObject(mission.kind, progress = progress, modifier = Modifier.weight(1f))
        HomeCard {
            Text(
                "Удар ${(beatIndex + 1).coerceAtMost(config.beats.size)} из ${config.beats.size} · серия $streak",
                fontSize = HomeType.Label, color = HomeColors.InkSecondary
            )
            Text(
                when {
                    !input.armed -> "Расслабьте руку"
                    countdownLeft > 0L -> "Приготовьтесь"
                    else -> beatName(current)
                },
                fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink
            )
            if (input.armed && countdownLeft <= 0L && next != null) {
                Text("Следом: ${beatName(next)}", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
            }
        }
        ProgressStrip(
            beatFraction,
            if (current == Channel.EXTENSOR) HomeColors.Extensor else HomeColors.Flexor,
            height = 16.dp
        )
        // попадание - зелёным, промах и встречный сигнал - красным
        WarningLine(
            when {
                !input.armed -> null
                state.isCoContraction -> "Напряжены оба канала. Оставьте только нужный"
                lastResult.isNotEmpty() -> lastResult
                else -> null
            },
            positive = input.armed && !state.isCoContraction && lastHit
        )
        SignalBars(state)
    }
}