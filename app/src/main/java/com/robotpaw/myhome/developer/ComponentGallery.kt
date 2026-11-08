package com.robotpaw.myhome.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.HoldDetector
import com.robotpaw.myhome.signal.SignalProcessor
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.signal.SignalState
import com.robotpaw.myhome.signal.UserLevel
import com.robotpaw.myhome.ui.components.ChannelBadge
import com.robotpaw.myhome.ui.components.ControlHints
import com.robotpaw.myhome.ui.components.HoldPopup
import com.robotpaw.myhome.ui.components.HoldRing
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.MenuItemCard
import com.robotpaw.myhome.ui.components.ProgressStrip
import com.robotpaw.myhome.ui.components.SignalBars
import com.robotpaw.myhome.ui.components.StarsBadge
import com.robotpaw.myhome.ui.components.StatTile
import com.robotpaw.myhome.ui.components.WarningLine
import com.robotpaw.myhome.ui.components.channelColor
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

/**
 * Витрина компонентов — только для разработки (debug).
 *
 * Все детали интерфейса на одном экране. Сверху — ползунки и переключатели,
 * чтобы посмотреть каждый компонент в разных состояниях и подобрать размеры и цвета.
 * «Живой сигнал» берёт силу и кольцо удержания из эмулятора: зажмите С / Р справа.
 *
 * Управляется касанием — это инструмент разработчика, а не экран игры.
 *
 * @param source источник сигнала; null — только ручные ползунки (так работает превью в Android Studio).
 */
@Composable
fun ComponentGallery(
    source: SignalSource?,
    profile: SignalProfile,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0.6f) }
    var flexor by remember { mutableFloatStateOf(70f) }
    var extensor by remember { mutableFloatStateOf(10f) }
    var selected by remember { mutableStateOf(true) }
    var done by remember { mutableStateOf(false) }
    var experienced by remember { mutableStateOf(false) }
    var warning by remember { mutableStateOf(false) }
    var live by remember { mutableStateOf(false) }

    // Живой сигнал из эмулятора
    var liveState by remember { mutableStateOf(SignalState.EMPTY) }
    var liveHold by remember { mutableFloatStateOf(0f) }
    var liveHoldChannel by remember { mutableStateOf<Channel?>(null) }
    if (source != null && live) {
        val processor = remember(profile) { SignalProcessor(profile) }
        val detector = remember(profile) { HoldDetector(profile.minPulseMs, profile.holdToSelectMs) }
        LaunchedEffect(source, processor) {
            source.frames.collect { frame ->
                val s = processor.process(frame)
                liveState = s
                detector.update(s)
                liveHold = detector.progress
                liveHoldChannel = detector.progressChannel
            }
        }
    }

    val state = if (live && source != null) liveState else fakeState(flexor.toInt(), extensor.toInt())
    val ringProgress = if (live && source != null) liveHold else progress
    val ringColor = channelColor(if (live) liveHoldChannel ?: Channel.FLEXOR else Channel.FLEXOR)
    val level = if (experienced) UserLevel.EXPERIENCED else UserLevel.NOVICE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeColors.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Витрина компонентов", fontSize = HomeType.Title, fontWeight = FontWeight.ExtraBold,
                color = HomeColors.Ink, modifier = Modifier.weight(1f)
            )
            Button(onClick = onBack) { Text("Назад") }
        }

        // ---- Управление ----
        HomeCard {
            SectionTitle("Параметры")
            LabeledSlider("Прогресс (кольцо, полоса)", progress) { progress = it }
            LabeledSlider("Сила С", flexor / 100f) { flexor = it * 100f }
            LabeledSlider("Сила Р", extensor / 100f) { extensor = it * 100f }
            LabeledSwitch("Пункт выбран", selected) { selected = it }
            LabeledSwitch("Пункт выполнен", done) { done = it }
            LabeledSwitch("Режим опытного", experienced) { experienced = it }
            LabeledSwitch("Показать предупреждение", warning) { warning = it }
            if (source != null) LabeledSwitch("Живой сигнал из эмулятора", live) { live = it }
        }

        // ---- Компоненты ----
        Section("MenuItemCard — пункт меню") {
            MenuItemCard(
                title = "Продолжить тренировку",
                subtitle = "Гостиная · 1 из 3 заданий",
                selected = selected,
                holdProgress = ringProgress,
                ringColor = ringColor,
                done = done
            )
            MenuItemCard(title = "Пункт без подписи", selected = false, holdProgress = 0f)
        }

        Section("HoldRing — кольцо удержания") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                HoldRing(ringProgress, HomeColors.Flexor, 24.dp)
                HoldRing(ringProgress, HomeColors.Flexor, 40.dp)
                HoldRing(ringProgress, HomeColors.Extensor, 56.dp)
                HoldRing(ringProgress, HomeColors.Accent, 72.dp)
            }
        }

        Section("ChannelBadge — кружок мышцы") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ChannelBadge(Channel.FLEXOR, 24.dp, active = state.isFlexorActive)
                ChannelBadge(Channel.FLEXOR, 32.dp, active = state.isFlexorActive)
                ChannelBadge(Channel.FLEXOR, 48.dp, active = state.isFlexorActive)
                ChannelBadge(Channel.EXTENSOR, 24.dp, active = state.isExtensorActive)
                ChannelBadge(Channel.EXTENSOR, 32.dp, active = state.isExtensorActive)
                ChannelBadge(Channel.EXTENSOR, 48.dp, active = state.isExtensorActive)
            }
        }

        Section("SignalBars — столбики силы") {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Bottom) {
                SignalBars(state, barHeight = 48.dp)
                SignalBars(state, barHeight = 72.dp)
                SignalBars(state, barHeight = 120.dp)
            }
            Text("код ${state.code} · ${state.description}", fontSize = HomeType.Caption, color = HomeColors.InkSecondary)
        }

        Section("ProgressStrip — полоса прогресса") {
            ProgressStrip(progress, HomeColors.Accent)
            ProgressStrip(progress, HomeColors.Flexor, height = 6.dp)
            ProgressStrip(progress, HomeColors.Extensor, height = 16.dp)
        }

        Section("ControlHints — подсказки управления") {
            ControlHints(level, hasBack = true, backProgress = if (experienced) ringProgress else 0f)
        }

        Section("WarningLine — предупреждение") {
            WarningLine(if (warning) "Обе мышцы сразу — предмет застрял" else null)
            Text("пустая строка держит место, чтобы вёрстка не прыгала", fontSize = HomeType.Caption, color = HomeColors.InkSecondary)
        }

        Section("StatTile, StarsBadge") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Чистота сигнала", "${(progress * 100).toInt()}%", Modifier.weight(1f))
                StatTile("Удержание", "3,2 с", Modifier.weight(1f))
            }
            StarsBadge(12)
        }

        Section("HomeCard — карточка") {
            HomeCard(highlighted = selected) {
                Text("Заголовок карточки", fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
                Text("Подпись, вторичный текст", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
            }
        }

        Section("HoldPopup — окно с одной кнопкой") {
            Box(Modifier.fillMaxWidth().height(560.dp)) {
                HoldPopup(
                    title = "Шаг 2 из 3: сжатие",
                    text = "Когда будете готовы, сожмите кисть изо всех сил и держите 3 секунды. Вторую мышцу постарайтесь не напрягать.",
                    buttonTitle = "Я готов",
                    holdProgress = ringProgress,
                    ringColor = ringColor
                )
            }
        }

        Section("Палитра") {
            listOf(
                "Background" to HomeColors.Background, "Card" to HomeColors.Card,
                "Track" to HomeColors.Track, "Divider" to HomeColors.Divider,
                "Ink" to HomeColors.Ink, "InkSecondary" to HomeColors.InkSecondary,
                "Accent" to HomeColors.Accent, "AccentSoft" to HomeColors.AccentSoft,
                "Flexor" to HomeColors.Flexor, "Extensor" to HomeColors.Extensor,
                "Warning" to HomeColors.Warning, "WarningSoft" to HomeColors.WarningSoft
            ).forEach { (name, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .height(28.dp)
                            .fillMaxWidth(0.25f)
                            .background(color)
                    )
                    Text(name, fontSize = HomeType.Label, color = HomeColors.Ink)
                }
            }
        }
    }
}

/** Состояние сигнала из ползунков: порог 30, как стандартный. */
private fun fakeState(flexor: Int, extensor: Int): SignalState {
    val f = flexor >= 30
    val e = extensor >= 30
    val code = when {
        f && e -> "11"
        f -> "10"
        e -> "01"
        else -> "00"
    }
    return SignalState(
        flexorSignal = flexor,
        extensorSignal = extensor,
        isFlexorActive = f,
        isExtensorActive = e,
        code = code,
        description = "из ползунков",
        timestampMs = 0L,
        flexorStrength = flexor,
        extensorStrength = extensor
    )
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = HomeType.Label, fontWeight = FontWeight.ExtraBold, color = HomeColors.Accent)
}

@Composable
private fun LabeledSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Text("$label: ${(value * 100).toInt()}%", fontSize = HomeType.Label, color = HomeColors.Ink)
        Slider(value = value, onValueChange = onChange)
    }
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = HomeType.Label, color = HomeColors.Ink, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Превью в Android Studio: откройте файл и включите Split — витрина появится без запуска. */
@Preview(widthDp = 390, heightDp = 3200, showBackground = true)
@Composable
private fun ComponentGalleryPreview() {
    ComponentGallery(source = null, profile = SignalProfile(), onBack = {})
}