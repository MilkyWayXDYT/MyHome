package com.robotpaw.myhome.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalState
import com.robotpaw.myhome.signal.UserLevel
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType


fun channelColor(channel: Channel): Color =
    if (channel == Channel.FLEXOR) HomeColors.Flexor else HomeColors.Extensor

fun channelLetter(channel: Channel): String =
    if (channel == Channel.FLEXOR) "С" else "Р"

/**
 * Каркас экрана
 * @param bottom - закреплено внизу (обычно подсказки управления)
 */
@Composable
fun HomeScreen(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
    bottom: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeColors.Background)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (overline != null) {
                    Text(overline, fontSize = HomeType.Label, fontWeight = FontWeight.Bold, color = HomeColors.InkSecondary)
                }
                Text(title, fontSize = HomeType.Title, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink, lineHeight = HomeType.TitleLine)
                if (subtitle != null) {
                    Text(subtitle, fontSize = HomeType.Body, lineHeight = HomeType.BodyLine, color = HomeColors.InkSecondary)
                }
            }
            trailing()
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
        bottom()
    }
}

@Composable
fun HomeCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HomeColors.Card)
            .then(if (highlighted) Modifier.border(3.dp, HomeColors.Accent, shape) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
fun ChannelBadge(channel: Channel, size: Dp = 40.dp, active: Boolean = true) {
    val color = channelColor(channel)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) color else color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            channelLetter(channel),
            fontSize = (size.value * 0.45f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (active && channel == Channel.FLEXOR) Color.White else HomeColors.Ink
        )
    }
}

/**
 * Кольцо удержания
 * @param progress - заполнение (0..1)
 */
@Composable
fun HoldRing(progress: Float, color: Color, size: Dp = 48.dp) {
    Canvas(Modifier.size(size)) {
        val stroke = (this.size.minDimension * 0.15f).coerceAtLeast(3.dp.toPx())
        val inset = stroke / 2
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(
            color = HomeColors.Divider, startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke)
        )
        drawArc(
            color = color, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
    }
}

/**
 * Пункт меню, у выбранного - рамка и кольцо удержания
 * @param ringColor - цвет мышцы, которую сейчас держат
 */
@Composable
fun MenuItemCard(
    title: String,
    selected: Boolean,
    holdProgress: Float,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    ringColor: Color = HomeColors.Flexor,
    done: Boolean = false
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HomeColors.Card)
            .then(if (selected) Modifier.border(3.dp, HomeColors.Accent, shape) else Modifier)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = HomeType.Heading, lineHeight = HomeType.HeadingLine, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
            if (subtitle != null) Text(subtitle, fontSize = HomeType.Label, lineHeight = HomeType.LabelLine, color = HomeColors.InkSecondary)
        }
        if (done) Text("✓", fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold, color = HomeColors.Accent)
        // место под кольцо есть всегда, чтобы пункт не менял высоту при выборе
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (selected) HoldRing(holdProgress, ringColor)
        }
    }
}

/**
 * Подсказки управления меню, свои для новичка и опытного
 * @param backProgress - заполнение «назад» удержанием Р (только у опытного)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlHints(level: UserLevel, hasBack: Boolean, backProgress: Float = 0f) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (level == UserLevel.NOVICE) {
            HintChip("короткий сигнал", "следующий пункт")
            HintChip("удержание", "выбрать")
        } else {
            HintChip("С", "вниз", Channel.FLEXOR)
            HintChip("Р", "вверх", Channel.EXTENSOR)
            HintChip("держать С", "выбрать", Channel.FLEXOR)
            if (hasBack) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HintChip("держать Р", "назад", Channel.EXTENSOR)
                    if (backProgress > 0f) HoldRing(backProgress, HomeColors.Extensor, 28.dp)
                }
            }
        }
    }
}

/**
 * Блок с инструкцией по управлению меню
 */
@Composable
fun ControlGuide(level: UserLevel, hasBack: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Управление", fontSize = HomeType.Label, fontWeight = FontWeight.Bold, color = HomeColors.InkSecondary)
        if (level == UserLevel.NOVICE) {
            GuideRow(null, "Коротко", "следующий пункт")
            GuideRow(null, "Удержать", "выбрать")
            Text("любой мышцей", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
        } else {
            GuideRow(Channel.FLEXOR, "Коротко", "вниз")
            GuideRow(Channel.EXTENSOR, "Коротко", "вверх")
            GuideRow(Channel.FLEXOR, "Удержать", "выбрать")
            if (hasBack) GuideRow(Channel.EXTENSOR, "Удержать", "назад")
        }
    }
}

// channel = null - любая мышца
@Composable
private fun GuideRow(channel: Channel?, key: String, action: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (channel != null) {
            ChannelBadge(channel, size = 32.dp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ChannelBadge(Channel.FLEXOR, size = 32.dp)
                ChannelBadge(Channel.EXTENSOR, size = 32.dp)
            }
        }
        Text(key, fontSize = HomeType.Body, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
        Text("— $action", fontSize = HomeType.Body, color = HomeColors.InkSecondary)
    }
}

@Composable
private fun HintChip(key: String, action: String, channel: Channel? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (channel != null) Box(Modifier.size(14.dp).clip(CircleShape).background(channelColor(channel)))
        Text(key, fontSize = HomeType.Label, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
        Text("— $action", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
    }
}

@Composable
fun ProgressStrip(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(HomeColors.Divider)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

/**
 * Столбики силы С и Р (strength, 0..100)
 */
@Composable
fun SignalBars(state: SignalState, modifier: Modifier = Modifier, barHeight: Dp = 72.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
        SignalBar(Channel.FLEXOR, state.flexorStrength, state.isFlexorActive, barHeight)
        SignalBar(Channel.EXTENSOR, state.extensorStrength, state.isExtensorActive, barHeight)
    }
}

@Composable
private fun SignalBar(channel: Channel, strength: Int, active: Boolean, barHeight: Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .width(28.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(HomeColors.Track),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(strength.coerceIn(0, 100) / 100f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(channelColor(channel))
            )
        }
        ChannelBadge(channel, size = 32.dp, active = active)
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(HomeColors.Card)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, fontSize = HomeType.Caption, color = HomeColors.InkSecondary)
        Text(value, fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
    }
}

/**
 * Строка предупреждения. При text = null место остаётся, чтобы экран не прыгал
 */
@Composable
fun WarningLine(text: String?) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (text != null) HomeColors.WarningSoft else Color.Transparent)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (text != null) Text(text, fontSize = HomeType.Label, lineHeight = HomeType.LabelLine, fontWeight = FontWeight.Bold, color = HomeColors.Warning)
    }
}

@Composable
fun StarsBadge(stars: Int) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(HomeColors.Card)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("★", fontSize = HomeType.Body, color = HomeColors.Extensor)
        Text("$stars", fontSize = HomeType.Body, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
    }
}

// затемнение под окнами
val OverlayScrim = Color.Black.copy(alpha = 0.7f)

/**
 * Карточка по центру экрана поверх затемнения
 * @param dimmed - false, если затемнение рисуется отдельно (для плавной смены окон)
 */
@Composable
fun HomeOverlay(
    modifier: Modifier = Modifier,
    dimmed: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .then(if (dimmed) Modifier.background(OverlayScrim) else Modifier)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(HomeColors.Card)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

/**
 * Окно с одной кнопкой (кнопка - удержание любой мышцы)
 * @param holdProgress - заполнение кольца (0..1)
 * @param ringColor - цвет мышцы, которую сейчас держат
 */
@Composable
fun HoldPopup(
    title: String,
    text: String,
    buttonTitle: String,
    holdProgress: Float,
    ringColor: Color,
    modifier: Modifier = Modifier,
    hint: String = "удерживайте любую мышцу",
    dimmed: Boolean = true
) {
    HomeOverlay(modifier, dimmed) {
        Text(title, fontSize = HomeType.Title, lineHeight = HomeType.TitleLine, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
        Text(text, fontSize = HomeType.Body, lineHeight = HomeType.BodyLine, color = HomeColors.Ink)
        Gap(4.dp)
        MenuItemCard(
            title = buttonTitle,
            subtitle = hint,
            selected = true,
            holdProgress = holdProgress,
            ringColor = ringColor
        )
    }
}

@Composable
fun Gap(height: Dp) = Spacer(Modifier.height(height))