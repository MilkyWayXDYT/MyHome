package com.robotpaw.myhome.calibration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.ControlEvent
import com.robotpaw.myhome.signal.HoldDetector
import com.robotpaw.myhome.signal.MenuAction
import com.robotpaw.myhome.signal.MenuNavigator
import com.robotpaw.myhome.signal.SignalData
import com.robotpaw.myhome.signal.SignalProcessor
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.signal.UserLevel
import com.robotpaw.myhome.ui.theme.HomeColors
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt


private const val CALIBRATION_WARNING_LEVEL = 30
private const val RESULT_HOLD_MS = 1500L

/**
 * Экран калибровки.
 */
@Composable
fun CalibrationScreen(
    source: SignalSource,
    onFinished: (SignalProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = remember { CalibrationController() }
    val holdDetector = remember { HoldDetector(holdMs = RESULT_HOLD_MS) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    var phase by remember { mutableStateOf(controller.phase) }
    var frame by remember { mutableStateOf(source.frames.value) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var holdChannel by remember { mutableStateOf<Channel?>(null) }
    // Итоговый экран: какой из двух пунктов подсвечен и как ими управлять (новичок / опытный).
    var selectedOption by remember { mutableIntStateOf(0) }
    var resultLevel by remember { mutableStateOf(UserLevel.NOVICE) }

    LaunchedEffect(source) {
        var processor: SignalProcessor? = null
        var navigator: MenuNavigator? = null
        source.frames.collect { f ->
            frame = f
            val p = controller.onFrame(f)
            phase = p

            if (p !is CalibrationPhase.Finished) {
                processor = null
                navigator = null
                return@collect
            }

            // На итоговом экране управляем уже по новым порогам (или стандартным, если не вышло).
            val profile = p.result.profile ?: SignalProfile()
            val interp = processor ?: SignalProcessor(profile).also {
                processor = it
                holdDetector.reset()
            }
            // Не вышло — управляем как новичок: так проще всего выбрать пункт.
            val level = p.result.profile?.level ?: UserLevel.NOVICE
            val nav = navigator ?: MenuNavigator(itemCount = 2, level = level).also {
                navigator = it
                resultLevel = level
                selectedOption = 0
            }

            val event = holdDetector.update(interp.process(f))
            holdProgress = holdDetector.progress
            holdChannel = holdDetector.progressChannel
            if (event == null) return@collect

            // Новичок: короткий сигнал — переключить пункт, удержание любой мышцей — выбрать подсвеченный.
            // Опытный: удержание С — первый пункт, удержание Р — второй, как раньше.
            val option = if (level == UserLevel.NOVICE) {
                (nav.onEvent(event) as? MenuAction.Select)?.index
            } else {
                (event as? ControlEvent.Hold)?.let { if (it.channel == Channel.FLEXOR) 0 else 1 }
            }
            selectedOption = nav.selected
            if (option == null) return@collect

            val success = p.result.profile
            when {
                success != null && option == 0 -> currentOnFinished(success)
                success == null && option == 1 -> currentOnFinished(SignalProfile())
                else -> {
                    controller.restart()
                    phase = controller.phase
                }
            }
            holdProgress = 0f
            holdChannel = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeColors.Background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        StepHeader(phase)
        when (val p = phase) {
            is CalibrationPhase.Finished -> FinishedContent(p.result, frame, holdChannel, holdProgress, resultLevel, selectedOption)
            else -> StepContent(p, frame)
        }
    }
}

@Composable
private fun StepHeader(phase: CalibrationPhase) {
    val total = CalibrationController.STEPS.size
    val done = phase is CalibrationPhase.Finished
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (done) "Настройка завершена" else "Настройка · шаг ${phase.stepIndex + 1} из $total",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = HomeColors.InkSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .size(width = 22.dp, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i <= phase.stepIndex || done) HomeColors.Flexor else HomeColors.Divider)
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.StepContent(
    phase: CalibrationPhase,
    frame: SignalData
) {
    val step = when (phase) {
        is CalibrationPhase.Preparing -> phase.step
        is CalibrationPhase.Recording -> phase.step
        is CalibrationPhase.Finished -> return
    }
    val recording = phase is CalibrationPhase.Recording

    val (title, subtitle) = stepTexts(step, recording)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
        Text(subtitle, fontSize = 16.sp, lineHeight = 22.sp, color = HomeColors.InkSecondary)
    }

    val target = when (step) {
        CalibrationStep.REST -> null
        CalibrationStep.FLEXOR -> Channel.FLEXOR
        CalibrationStep.EXTENSOR -> Channel.EXTENSOR
    }
    val flexorWrong = target != Channel.FLEXOR && frame.flexor >= CALIBRATION_WARNING_LEVEL
    val extensorWrong = target != Channel.EXTENSOR && frame.extensor >= CALIBRATION_WARNING_LEVEL

    BarsCard(
        frame = frame,
        flexorThreshold = null,
        extensorThreshold = null,
        flexorDimmed = target == Channel.EXTENSOR,
        extensorDimmed = target == Channel.FLEXOR,
        flexorWarning = flexorWrong,
        extensorWarning = extensorWrong,
        modifier = Modifier.weight(1f)
    )

    val longWait = phase is CalibrationPhase.Recording && !phase.isHolding &&
            phase.waitingMs >= CalibrationController.FIT_HINT_AFTER_MS
    val warning = when {
        step == CalibrationStep.REST && (flexorWrong || extensorWrong) -> "Расслабьте руку полностью"
        longWait && step != CalibrationStep.REST -> "Сигнала нет — проверьте, как сидит протез"
        flexorWrong -> "Сгибатель тоже напряжён — постарайтесь его отпустить"
        extensorWrong -> "Разгибатель тоже напряжён — постарайтесь его отпустить"
        else -> null
    }
    WarningLine(warning)

    when (phase) {
        is CalibrationPhase.Preparing -> BottomProgress(
            left = "Начинаем через ${ceil(phase.remainingMs / 1000.0).toInt()}…",
            right = "",
            progress = 0f
        )
        is CalibrationPhase.Recording -> BottomProgress(
            left = when {
                phase.isHolding -> if (step == CalibrationStep.REST) "Не двигайтесь…" else "Держите…"
                step == CalibrationStep.REST -> "Пауза — расслабьте руку"
                step == CalibrationStep.FLEXOR -> "Пауза — сожмите кисть сильнее"
                else -> "Пауза — раскройте кисть сильнее"
            },
            right = "осталось ${seconds(phase.remainingMs)} с",
            progress = phase.progress
        )
        is CalibrationPhase.Finished -> Unit
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.FinishedContent(
    result: CalibrationResult,
    frame: SignalData,
    holdChannel: Channel?,
    holdProgress: Float,
    level: UserLevel,
    selectedOption: Int
) {
    val profile = result.profile
    val novice = level == UserLevel.NOVICE
    // Новичок: подсвечен выбранный пункт, кольцо заполняется удержанием любой мышцы.
    // Опытный: у каждого пункта своя мышца, подсвечен основной.
    fun progressOf(index: Int, channel: Channel): Float = when {
        novice -> if (index == selectedOption && holdChannel != null) holdProgress else 0f
        else -> if (holdChannel == channel) holdProgress else 0f
    }
    fun highlightedOf(index: Int) = if (novice) index == selectedOption else index == 0
    // Цвет кольца: у новичка — цвет мышцы, которую сейчас держат.
    fun ringOf(channel: Channel) = if (novice) (holdChannel ?: Channel.FLEXOR) else channel
    fun hintOf(channel: Channel) = when {
        novice -> "короткий сигнал — переключить, удержание — выбрать"
        channel == Channel.FLEXOR -> "держите С"
        else -> "держите Р"
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = if (profile != null) "Готово!" else "Не получилось",
            fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink
        )
        Text(
            text = if (profile != null) {
                "Пороги настроены под вас. Проверьте: пересеките пунктир — канал загорится."
            } else {
                result.problems.joinToString("\n")
            },
            fontSize = 16.sp, lineHeight = 22.sp, color = HomeColors.InkSecondary
        )
    }

    BarsCard(
        frame = frame,
        flexorThreshold = profile?.flexor?.threshold,
        extensorThreshold = profile?.extensor?.threshold,
        flexorDimmed = false,
        extensorDimmed = false,
        flexorWarning = false,
        extensorWarning = false,
        modifier = Modifier.weight(1f)
    )

    if (profile != null) {
        ResultSummary(result, profile)
        HoldOption(
            channel = ringOf(Channel.FLEXOR),
            title = "Продолжить",
            hint = hintOf(Channel.FLEXOR),
            progress = progressOf(0, Channel.FLEXOR),
            highlighted = highlightedOf(0)
        )
        HoldOption(
            channel = ringOf(Channel.EXTENSOR),
            title = "Пройти ещё раз",
            hint = hintOf(Channel.EXTENSOR),
            progress = progressOf(1, Channel.EXTENSOR),
            highlighted = highlightedOf(1)
        )
    } else {
        HoldOption(
            channel = ringOf(Channel.FLEXOR),
            title = "Попробовать ещё раз",
            hint = hintOf(Channel.FLEXOR),
            progress = progressOf(0, Channel.FLEXOR),
            highlighted = highlightedOf(0)
        )
        HoldOption(
            channel = ringOf(Channel.EXTENSOR),
            title = "Пропустить, стандартные пороги",
            hint = hintOf(Channel.EXTENSOR),
            progress = progressOf(1, Channel.EXTENSOR),
            highlighted = highlightedOf(1)
        )
    }
}

@Composable
private fun ResultSummary(result: CalibrationResult, profile: SignalProfile) {
    val levelText = when (profile.level) {
        UserLevel.NOVICE -> "Режим: новичок — меню медленнее и терпимее к ошибкам"
        UserLevel.EXPERIENCED -> "Режим: опытный — быстрое меню"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HomeColors.Card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Чистота сигнала", fontSize = 15.sp, color = HomeColors.Ink)
            Text(percent(result.purity), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
        }
        Text(levelText, fontSize = 13.sp, color = HomeColors.InkSecondary)
    }
}

@Composable
private fun BarsCard(
    frame: SignalData,
    flexorThreshold: Int?,
    extensorThreshold: Int?,
    flexorDimmed: Boolean,
    extensorDimmed: Boolean,
    flexorWarning: Boolean,
    extensorWarning: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HomeColors.Card)
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        ChannelBar(
            letter = "С", name = "сгибатель", value = frame.flexor, color = HomeColors.Flexor,
            threshold = flexorThreshold, dimmed = flexorDimmed, warning = flexorWarning,
            modifier = Modifier.weight(1f, fill = false)
        )
        ChannelBar(
            letter = "Р", name = "разгибатель", value = frame.extensor, color = HomeColors.Extensor,
            threshold = extensorThreshold, dimmed = extensorDimmed, warning = extensorWarning,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun ChannelBar(
    letter: String,
    name: String,
    value: Int,
    color: Color,
    threshold: Int?,
    dimmed: Boolean,
    warning: Boolean,
    modifier: Modifier = Modifier
) {
    val active = threshold != null && value >= threshold
    val fillColor = when {
        warning -> HomeColors.Warning
        dimmed -> color.copy(alpha = 0.35f)
        else -> color
    }
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
    ) {
        Text("${value.coerceIn(0, 100)}%", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
        Canvas(
            modifier = Modifier
                .width(64.dp)
                .weight(1f)
        ) {
            val radius = CornerRadius(20.dp.toPx())
            drawRoundRect(color = HomeColors.Track, cornerRadius = radius)
            val h = size.height * value.coerceIn(0, 100) / 100f
            if (h > 0f) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(0f, size.height - h),
                    size = Size(size.width, h),
                    cornerRadius = radius
                )
            }
            if (threshold != null) {
                val y = size.height * (1f - threshold.coerceIn(0, 100) / 100f)
                drawLine(
                    color = HomeColors.Ink,
                    start = Offset(-6.dp.toPx(), y),
                    end = Offset(size.width + 6.dp.toPx(), y),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (active) color else color.copy(alpha = if (threshold == null) 1f else 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                letter, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                color = if (color == HomeColors.Flexor && (active || threshold == null)) Color.White else HomeColors.Ink
            )
        }
        Text(name, fontSize = 13.sp, color = HomeColors.InkSecondary)
    }
}

@Composable
private fun WarningLine(text: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (text != null) HomeColors.WarningSoft else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HomeColors.Warning)
        }
    }
}

@Composable
private fun BottomProgress(left: String, right: String, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(left, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
            Text(right, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(HomeColors.Divider)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(10.dp)
                    .background(HomeColors.Flexor)
            )
        }
    }
}

/** Пункт, который выбирается удержанием канала: кольцо заполняется, пока держишь. */
@Composable
private fun HoldOption(
    channel: Channel,
    title: String,
    hint: String,
    progress: Float,
    highlighted: Boolean
) {
    val color = if (channel == Channel.FLEXOR) HomeColors.Flexor else HomeColors.Extensor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HomeColors.Card)
            .then(
                if (highlighted) Modifier.border(3.dp, HomeColors.Flexor, RoundedCornerShape(18.dp))
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Canvas(Modifier.size(40.dp)) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
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
        Column {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
            Text(hint, fontSize = 13.sp, color = HomeColors.InkSecondary)
        }
        Spacer(Modifier.weight(1f))
    }
}

private fun stepTexts(step: CalibrationStep, recording: Boolean): Pair<String, String> = when (step) {
    CalibrationStep.REST ->
        if (recording) "Не напрягайте мышцы" to "Держите руку расслабленной, запоминаем покой."
        else "Расслабьте руку" to "Сначала запишем, как выглядит сигнал в покое."
    CalibrationStep.FLEXOR ->
        if (recording) "Сожмите кисть изо всех сил" to "Запись идёт, пока держите. Разгибатель постарайтесь не напрягать."
        else "Приготовьтесь сжать кисть" to "Через пару секунд сожмите изо всех сил и держите."
    CalibrationStep.EXTENSOR ->
        if (recording) "Раскройте кисть изо всех сил" to "Запись идёт, пока держите. Сгибатель постарайтесь не напрягать."
        else "Приготовьтесь раскрыть кисть" to "Через пару секунд раскройте изо всех сил и держите."
}

private fun percent(value: Float): String = "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%"

private fun seconds(ms: Long): String = String.format(Locale.forLanguageTag("ru"), "%.1f", ms / 1000f)