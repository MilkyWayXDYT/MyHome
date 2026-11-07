package com.robotpaw.myhome.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotpaw.myhome.emulator.EmulatorSignalSource
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.ui.theme.HomeColors
import kotlin.math.roundToInt

/**
 * Компактные кнопки эмулятора у правого края экрана (только debug).
 *
 * Зажали кнопку — мышца напряжена, отпустили — расслаблена.
 * Сила зависит от того, где держите: выше на кнопке — сильнее (низ ≈ 0%, верх = 100%).
 * Можно вести пальцем / мышью вверх-вниз, не отпуская.
 *
 * @param onOpenSettings открыть большую панель с шумами и наборами помех.
 */
@Composable
fun EmulatorPads(
    emulator: EmulatorSignalSource,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val raw by emulator.raw.collectAsState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HomeColors.Card.copy(alpha = 0.9f))
                .border(1.dp, HomeColors.Divider, CircleShape)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Text("⚙", fontSize = 18.sp, color = HomeColors.Ink)
        }

        Pad(label = "С", value = raw.flexor, color = HomeColors.Flexor) {
            emulator.setChannel(Channel.FLEXOR, it)
        }
        Pad(label = "Р", value = raw.extensor, color = HomeColors.Extensor) {
            emulator.setChannel(Channel.EXTENSOR, it)
        }
        Pad(label = "С+Р", value = minOf(raw.flexor, raw.extensor), color = HomeColors.Warning) {
            emulator.setRaw(it, it)
        }
    }
}

@Composable
private fun Pad(
    label: String,
    value: Int,
    color: Color,
    onValue: (Int) -> Unit
) {
    val currentOnValue by rememberUpdatedState(onValue)
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 120.dp)
            .clip(shape)
            .background(HomeColors.Card.copy(alpha = 0.9f))
            .border(2.dp, color, shape)
            .pointerInput(Unit) {
                fun level(y: Float) = ((1f - y / size.height) * 100).roundToInt().coerceIn(0, 100)

                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    currentOnValue(level(down.position.y))

                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        currentOnValue(level(change.position.y))
                        change.consume()
                    }
                    currentOnValue(0)
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        // Заливка снизу — текущая сила.
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(value.coerceIn(0, 100) / 100f)
                .background(color.copy(alpha = 0.35f))
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
            Text("$value", fontSize = 12.sp, color = HomeColors.InkSecondary)
        }
    }
}