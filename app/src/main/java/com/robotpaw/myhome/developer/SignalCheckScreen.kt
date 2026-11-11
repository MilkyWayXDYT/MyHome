package com.robotpaw.myhome.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.ControlEvent
import com.robotpaw.myhome.signal.HoldDetector
import com.robotpaw.myhome.signal.SignalProcessor
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.signal.SignalState
import com.robotpaw.myhome.signal.UserLevel
import com.robotpaw.myhome.ui.theme.HomeColors

/**
 * Временный экран после калибровки
 *
 * Показывает код состояния и ловит короткие сигналы / удержания так, как это будет в меню.
 * Удержание Р — вернуться в главное меню.
 */
@Composable
fun SignalCheckScreen(
    source: SignalSource,
    profile: SignalProfile,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val processor = remember(profile) { SignalProcessor(profile) }
    val detector = remember(profile) {
        HoldDetector(minPulseMs = profile.minPulseMs, holdMs = profile.holdToSelectMs)
    }
    val currentOnExit by rememberUpdatedState(onExit)

    var state by remember { mutableStateOf(SignalState.EMPTY) }
    val events = remember { mutableStateListOf<String>() }

    LaunchedEffect(source, profile) {
        source.frames.collect { frame ->
            val s = processor.process(frame)
            state = s
            when (val e = detector.update(s)) {
                is ControlEvent.Pulse -> events.add(0, "Короткий ${letter(e.channel)}")
                is ControlEvent.Hold -> {
                    events.add(0, "Удержание ${letter(e.channel)}")
                    if (e.channel == Channel.EXTENSOR) currentOnExit()
                }
                null -> Unit
            }
            while (events.size > 6) events.removeAt(events.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeColors.Background)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Проверка сигнала", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
        Text(
            "Временный экран. Короткий сигнал — шаг в меню, удержание — выбор. Удержание Р — в меню.",
            fontSize = 15.sp, color = HomeColors.InkSecondary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(HomeColors.Card)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Lamp("С", state.isFlexorActive, HomeColors.Flexor, state.flexorStrength)
            Text(state.code, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = codeColor(state))
            Lamp("Р", state.isExtensorActive, HomeColors.Extensor, state.extensorStrength)
        }

        Text(codeDescription(state), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HomeColors.Ink)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(HomeColors.Card)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("События меню", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HomeColors.InkSecondary)
            if (events.isEmpty()) Text("пока ничего", fontSize = 15.sp, color = HomeColors.InkSecondary)
            events.forEach { Text(it, fontSize = 15.sp, color = HomeColors.Ink) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(HomeColors.Card)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Профиль", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HomeColors.InkSecondary)
            Text(
                "Режим: ${if (profile.level == UserLevel.NOVICE) "новичок" else "опытный"} · удержание ${profile.holdToSelectMs} мс",
                fontSize = 15.sp, color = HomeColors.Ink
            )
            Text(
                "Порог С: ${profile.flexor.threshold}%  ·  покой ${profile.flexor.rest}%  ·  максимум ${profile.flexor.max}%",
                fontSize = 14.sp, color = HomeColors.Ink
            )
            Text(
                "Порог Р: ${profile.extensor.threshold}%  ·  покой ${profile.extensor.rest}%  ·  максимум ${profile.extensor.max}%",
                fontSize = 14.sp, color = HomeColors.Ink
            )
        }
    }
}

@Composable
private fun Lamp(letter: String, active: Boolean, color: Color, strength: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (active) color else color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(letter, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = if (active && color == HomeColors.Flexor) Color.White else HomeColors.Ink)
        }
        Box(
            Modifier
                .size(width = 56.dp, height = 6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(HomeColors.Track)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(strength.coerceIn(0, 100) / 100f)
                    .height(6.dp)
                    .background(color)
            )
        }
        Text("сила ${strength.coerceIn(0, 100)}%", fontSize = 12.sp, color = HomeColors.InkSecondary)
    }
}

private fun codeColor(state: SignalState): Color = when {
    state.isCoContraction -> HomeColors.Warning
    state.isFlexorActive -> HomeColors.Flexor
    state.isExtensorActive -> HomeColors.Extensor
    else -> HomeColors.InkSecondary
}

private fun codeDescription(state: SignalState): String = when {
    state.isCoContraction -> "Оба канала — встречный сигнал"
    state.isFlexorActive -> "Сгибание: кисть сжимается"
    state.isExtensorActive -> "Разгибание: кисть раскрывается"
    else -> "Покой"
}

private fun letter(channel: Channel) = if (channel == Channel.FLEXOR) "С" else "Р"