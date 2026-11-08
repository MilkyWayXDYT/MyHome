package com.robotpaw.myhome.calibration

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.robotpaw.myhome.ui.components.ControlGuide
import com.robotpaw.myhome.ui.components.HoldPopup
import com.robotpaw.myhome.ui.components.HomeOverlay
import com.robotpaw.myhome.ui.components.OverlayScrim
import com.robotpaw.myhome.ui.components.MenuItemCard
import com.robotpaw.myhome.ui.components.ProgressStrip
import com.robotpaw.myhome.ui.components.WarningLine
import com.robotpaw.myhome.ui.components.channelColor
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val RESULT_HOLD_MS = 1500L

private const val OVERLAY_FADE_MS = 300

// порогов ещё нет, «лишняя» мышца подсвечивается выше этого уровня
private const val CALIBRATION_WARNING_LEVEL = 30

/**
 * Экран калибровки
 */
@Composable
fun CalibrationScreen(
    source: SignalSource,
    onFinished: (SignalProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = remember { CalibrationController() }
    val detector = remember { HoldDetector(holdMs = RESULT_HOLD_MS) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    var phase by remember { mutableStateOf(controller.phase) }
    var frame by remember { mutableStateOf(source.frames.value) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var holdChannel by remember { mutableStateOf<Channel?>(null) }
    var selectedOption by remember { mutableIntStateOf(0) }
    var resultLevel by remember { mutableStateOf(UserLevel.NOVICE) }
    var showThresholds by remember { mutableStateOf(false) }

    LaunchedEffect(source) {
        // пока порогов нет, окна работают со стандартными
        val defaultProcessor = SignalProcessor(SignalProfile())
        var introIndex: Int? = null
        var resultProcessor: SignalProcessor? = null
        var navigator: MenuNavigator? = null
        var navigatorCount = 0

        source.frames.collect { f ->
            frame = f
            val p = controller.onFrame(f)
            phase = p

            when (p) {
                is CalibrationPhase.Intro -> {
                    if (introIndex != p.stepIndex) {
                        introIndex = p.stepIndex
                        detector.reset()
                    }
                    val event = detector.update(defaultProcessor.process(f))
                    holdProgress = detector.progress
                    holdChannel = detector.progressChannel
                    if (event is ControlEvent.Hold) {
                        controller.confirm()
                        phase = controller.phase
                        holdProgress = 0f
                        holdChannel = null
                    }
                }

                is CalibrationPhase.Finished -> {
                    introIndex = null
                    val profile = p.result.profile
                    val processor = resultProcessor ?: SignalProcessor(profile ?: SignalProfile()).also {
                        resultProcessor = it
                        detector.reset()
                    }
                    // если не получилось, управляем как новичок
                    val level = profile?.level ?: UserLevel.NOVICE
                    val itemCount = if (profile == null || showThresholds) 1 else 3
                    val nav = navigator?.takeIf { navigatorCount == itemCount }
                        ?: MenuNavigator(itemCount = itemCount, level = level).also {
                            navigator = it
                            navigatorCount = itemCount
                            resultLevel = level
                            selectedOption = 0
                        }

                    val event = detector.update(processor.process(f))
                    holdProgress = detector.progress
                    holdChannel = detector.progressChannel
                    if (event == null) return@collect

                    val action = nav.onEvent(event)
                    selectedOption = nav.selected
                    val index = when (action) {
                        is MenuAction.Select -> action.index
                        MenuAction.Back -> if (showThresholds) 0 else return@collect
                        null -> return@collect
                    }

                    when {
                        profile == null -> controller.restart()
                        showThresholds -> showThresholds = false           // «Назад к результату»
                        index == 0 -> currentOnFinished(profile)           // «Продолжить»
                        index == 1 -> showThresholds = true                // «Проверить пороги»
                        else -> controller.restart()                       // «Пройти ещё раз»
                    }
                    if (controller.phase !is CalibrationPhase.Finished) {
                        showThresholds = false
                        resultProcessor = null
                    }
                    phase = controller.phase
                    navigator = null
                    detector.reset()
                    holdProgress = 0f
                    holdChannel = null
                }

                else -> {
                    introIndex = null
                    holdProgress = 0f
                    holdChannel = null
                }
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeColors.Background)
                .statusBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StepHeader(phase)
            when (val p = phase) {
                is CalibrationPhase.Finished ->
                    FinishedContent(p.result, frame, holdChannel, holdProgress, resultLevel, selectedOption, showThresholds)
                // под окнами столбики стоят на нуле
                is CalibrationPhase.Recording -> StepContent(p, frame)
                else -> StepContent(p, SignalData(frame.timestampMs, 0, 0))
            }
        }

        // затемнение отдельно от окон, чтобы при смене окна оно не мигало
        AnimatedVisibility(
            visible = overlayKey(phase) != null,
            enter = fadeIn(tween(OVERLAY_FADE_MS)),
            exit = fadeOut(tween(OVERLAY_FADE_MS))
        ) {
            Box(Modifier.fillMaxSize().background(OverlayScrim))
        }

        AnimatedContent(
            targetState = phase,
            contentKey = { overlayKey(it) },
            transitionSpec = {
                (fadeIn(tween(OVERLAY_FADE_MS)) + scaleIn(tween(OVERLAY_FADE_MS), initialScale = 0.94f)) togetherWith
                        fadeOut(tween(OVERLAY_FADE_MS / 2))
            },
            modifier = Modifier.fillMaxSize(),
            label = "calibration overlay"
        ) { p ->
            when (p) {
                is CalibrationPhase.Intro -> {
                    val (title, text) = introTexts(p.step)
                    HoldPopup(
                        title = title,
                        text = text,
                        buttonTitle = "Я готов",
                        holdProgress = holdProgress,
                        ringColor = channelColor(holdChannel ?: Channel.FLEXOR),
                        dimmed = false
                    )
                }
                is CalibrationPhase.Preparing -> CountdownOverlay(p)
                is CalibrationPhase.StepDone -> StepDoneOverlay(p)
                else -> Box(Modifier.fillMaxSize())
            }
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
            text = if (done) "Настройка завершена" else "Шаг ${phase.stepIndex + 1} из $total",
            fontSize = HomeType.Label,
            fontWeight = FontWeight.Bold,
            color = HomeColors.InkSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .size(width = 28.dp, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (i <= phase.stepIndex || done) HomeColors.Accent else HomeColors.Divider)
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.StepContent(phase: CalibrationPhase, frame: SignalData) {
    val step = when (phase) {
        is CalibrationPhase.Intro -> phase.step
        is CalibrationPhase.Preparing -> phase.step
        is CalibrationPhase.Recording -> phase.step
        is CalibrationPhase.StepDone -> phase.step
        is CalibrationPhase.Finished -> return
    }

    Text(
        stepTitle(step, phase),
        fontSize = HomeType.Title, lineHeight = HomeType.TitleLine,
        fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink
    )

    val target = when (step) {
        CalibrationStep.REST -> null
        CalibrationStep.FLEXOR -> Channel.FLEXOR
        CalibrationStep.EXTENSOR -> Channel.EXTENSOR
    }
    val active = phase is CalibrationPhase.Recording
    val flexorWrong = active && target != Channel.FLEXOR && frame.flexor >= CALIBRATION_WARNING_LEVEL
    val extensorWrong = active && target != Channel.EXTENSOR && frame.extensor >= CALIBRATION_WARNING_LEVEL

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
    WarningLine(
        when {
            step == CalibrationStep.REST && (flexorWrong || extensorWrong) -> "Расслабьте руку полностью"
            longWait && step != CalibrationStep.REST -> "Сигнала нет — проверьте протез"
            flexorWrong -> "Сгибатель тоже напряжён"
            extensorWrong -> "Разгибатель тоже напряжён"
            else -> null
        }
    )

    when (phase) {
        is CalibrationPhase.Intro, is CalibrationPhase.Preparing -> BottomProgress("", "", 0f)
        is CalibrationPhase.StepDone -> BottomProgress("Записано", "", 1f)
        is CalibrationPhase.Recording -> BottomProgress(
            left = when {
                phase.isHolding -> if (step == CalibrationStep.REST) "Не двигайтесь…" else "Держите…"
                step == CalibrationStep.REST -> "Пауза — расслабьте руку"
                step == CalibrationStep.FLEXOR -> "Пауза — сожмите сильнее"
                else -> "Пауза — раскройте сильнее"
            },
            right = "${seconds(phase.remainingMs)} с",
            progress = phase.progress
        )
        is CalibrationPhase.Finished -> Unit
    }
}

@Composable
private fun ColumnScope.FinishedContent(
    result: CalibrationResult,
    frame: SignalData,
    holdChannel: Channel?,
    holdProgress: Float,
    level: UserLevel,
    selectedOption: Int,
    showThresholds: Boolean
) {
    val profile = result.profile
    val ringColor = channelColor(holdChannel ?: Channel.FLEXOR)

    @Composable
    fun Options(vararg items: Pair<String, String?>) {
        items.forEachIndexed { index, (title, subtitle) ->
            val selected = index == selectedOption
            MenuItemCard(
                title = title,
                subtitle = subtitle,
                selected = selected,
                holdProgress = if (selected && holdChannel != null) holdProgress else 0f,
                ringColor = ringColor
            )
        }
    }

    // не получилось
    if (profile == null) {
        Title("Не получилось", result.problems.joinToString("\n"))
        Spacer(Modifier.weight(1f))
        MenuItemCard(
            title = "Попробовать ещё раз",
            subtitle = "удерживайте любую мышцу",
            selected = true,
            holdProgress = if (holdChannel != null) holdProgress else 0f,
            ringColor = ringColor
        )
        return
    }

    // проверка порогов
    if (showThresholds) {
        Title("Проверка порогов", "Напрягите мышцу: когда столбик пересечёт пунктир, кружок загорится.")
        BarsCard(
            frame = frame,
            flexorThreshold = profile.flexor.threshold,
            extensorThreshold = profile.extensor.threshold,
            flexorDimmed = false,
            extensorDimmed = false,
            flexorWarning = false,
            extensorWarning = false,
            modifier = Modifier.weight(1f)
        )
        Options("Назад к результату" to "долгое удержание")
        return
    }

    Title("Готово!", "Управление настроено под вас.")
    ResultSummary(result, profile, level)
    Spacer(Modifier.weight(1f))
    Options(
        "Продолжить" to null,
        "Проверить пороги" to null,
        "Пройти ещё раз" to null
    )
}

@Composable
private fun Title(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title, fontSize = HomeType.Display, lineHeight = HomeType.DisplayLine,
            fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink
        )
        Text(text, fontSize = HomeType.Body, lineHeight = HomeType.BodyLine, color = HomeColors.InkSecondary)
    }
}

@Composable
private fun ResultSummary(result: CalibrationResult, profile: SignalProfile, level: UserLevel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HomeColors.Card)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Чистота сигнала", fontSize = HomeType.Body, color = HomeColors.Ink, modifier = Modifier.weight(1f))
            Text(percent(result.purity), fontSize = HomeType.Title, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Режим", fontSize = HomeType.Body, color = HomeColors.Ink, modifier = Modifier.weight(1f))
            Text(
                if (profile.level == UserLevel.NOVICE) "новичок" else "опытный",
                fontSize = HomeType.Body, fontWeight = FontWeight.ExtraBold, color = HomeColors.Accent
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(HomeColors.Divider))
        ControlGuide(level, hasBack = false)
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
            .padding(20.dp),
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
        Text("${value.coerceIn(0, 100)}%", fontSize = HomeType.Body, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
        Canvas(
            modifier = Modifier
                .width(72.dp)
                .weight(1f)
        ) {
            val radius = CornerRadius(22.dp.toPx())
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
                    strokeWidth = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))
                )
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (active) color else color.copy(alpha = if (threshold == null) 1f else 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                letter, fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold,
                color = if (color == HomeColors.Flexor && (active || threshold == null)) Color.White else HomeColors.Ink
            )
        }
        Text(name, fontSize = HomeType.Caption, color = HomeColors.InkSecondary)
    }
}

@Composable
private fun BottomProgress(left: String, right: String, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                left, fontSize = HomeType.Body, fontWeight = FontWeight.ExtraBold,
                color = HomeColors.Ink, modifier = Modifier.weight(1f)
            )
            Text(right, fontSize = HomeType.Body, color = HomeColors.InkSecondary)
        }
        ProgressStrip(progress, HomeColors.Accent, height = 12.dp)
    }
}

private fun introTexts(step: CalibrationStep): Pair<String, String> = when (step) {
    CalibrationStep.REST -> "Шаг 1: покой" to
            "Сядьте удобно. Чтобы начать, удержите любую мышцу, а потом полностью расслабьте руку. " +
            "После отсчёта 3 секунды не двигайтесь — так мы запомним сигнал покоя."
    CalibrationStep.FLEXOR -> "Шаг 2: сжатие" to
            "После отсчёта сожмите кисть изо всех сил и держите 3 секунды. " +
            "Разгибатель постарайтесь не напрягать."
    CalibrationStep.EXTENSOR -> "Шаг 3: раскрытие" to
            "После отсчёта раскройте кисть изо всех сил и держите 3 секунды. " +
            "Сгибатель постарайтесь не напрягать."
}

private fun stepTitle(step: CalibrationStep, phase: CalibrationPhase): String {
    if (phase is CalibrationPhase.StepDone) return "Готово"
    if (phase !is CalibrationPhase.Recording) return when (step) {
        CalibrationStep.REST -> "Приготовьтесь расслабить руку"
        CalibrationStep.FLEXOR -> "Приготовьтесь сжать кисть"
        CalibrationStep.EXTENSOR -> "Приготовьтесь раскрыть кисть"
    }
    return when (step) {
        CalibrationStep.REST -> "Не напрягайте мышцы"
        CalibrationStep.FLEXOR -> "Сожмите кисть изо всех сил"
        CalibrationStep.EXTENSOR -> "Раскройте кисть изо всех сил"
    }
}

@Composable
private fun CountdownOverlay(phase: CalibrationPhase.Preparing) {
    val action = when (phase.step) {
        CalibrationStep.REST -> "Расслабьте руку через"
        CalibrationStep.FLEXOR -> "Сожмите кисть через"
        CalibrationStep.EXTENSOR -> "Раскройте кисть через"
    }
    HomeOverlay(dimmed = false) {
        Text(
            action, fontSize = HomeType.Title, lineHeight = HomeType.TitleLine,
            fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        AnimatedContent(
            targetState = ceil(phase.remainingMs / 1000.0).toInt(),
            transitionSpec = {
                (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 1.3f)) togetherWith fadeOut(tween(150))
            },
            modifier = Modifier.fillMaxWidth(),
            label = "countdown"
        ) { secondsLeft ->
            Text(
                "$secondsLeft", fontSize = 120.sp, lineHeight = 130.sp,
                fontWeight = FontWeight.ExtraBold, color = HomeColors.Accent,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepDoneOverlay(phase: CalibrationPhase.StepDone) {
    val title = when (phase.step) {
        CalibrationStep.REST -> "Покой записан"
        CalibrationStep.FLEXOR -> "Сжатие записано"
        CalibrationStep.EXTENSOR -> "Раскрытие записано"
    }
    HomeOverlay(dimmed = false) {
        Text(
            "✓", fontSize = 96.sp, lineHeight = 104.sp, fontWeight = FontWeight.ExtraBold,
            color = HomeColors.Accent, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Text(
            title, fontSize = HomeType.Title, lineHeight = HomeType.TitleLine, fontWeight = FontWeight.ExtraBold,
            color = HomeColors.Ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Можно расслабить руку", fontSize = HomeType.Body, color = HomeColors.InkSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
    }
}

// у окон одного шага один ключ, чтобы смена цифр отсчёта не запускала анимацию окна
private fun overlayKey(phase: CalibrationPhase): String? = when (phase) {
    is CalibrationPhase.Intro -> "intro${phase.stepIndex}"
    is CalibrationPhase.Preparing -> "count${phase.stepIndex}"
    is CalibrationPhase.StepDone -> "done${phase.stepIndex}"
    else -> null
}

private fun percent(value: Float): String = "${(value.coerceIn(0f, 1f) * 100).roundToInt()}%"

private fun seconds(ms: Long): String = String.format(Locale.forLanguageTag("ru"), "%.1f", ms / 1000f)