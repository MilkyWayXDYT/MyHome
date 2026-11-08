package com.robotpaw.myhome

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.developer.ComponentGallery
import com.robotpaw.myhome.developer.EmulatorPads
import com.robotpaw.myhome.developer.SignalOverlay
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.lifecycleScope
import com.robotpaw.myhome.calibration.CalibrationScreen
import com.robotpaw.myhome.emulator.EmulatorSignalSource
import com.robotpaw.myhome.developer.SignalCheckScreen
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalProcessor
import com.robotpaw.myhome.signal.SignalProfile
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.robotpaw.myhome.signal.ControlEvent
import com.robotpaw.myhome.signal.HoldDetector
import com.robotpaw.myhome.signal.MenuAction
import com.robotpaw.myhome.signal.MenuNavigator
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.signal.UserLevel
import com.robotpaw.myhome.ui.theme.HomeColors
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val emulator by lazy { EmulatorSignalSource(lifecycleScope) }

    private var isDebug = false

    private var gamepadTriggerListener: ((Int, Int) -> Unit)? = null

    fun setGamepadTriggerListener(listener: ((Int, Int) -> Unit)?) {
        gamepadTriggerListener = listener
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val isGamepad = event.isFromSource(InputDevice.SOURCE_JOYSTICK) ||
                event.isFromSource(InputDevice.SOURCE_GAMEPAD)

        if (event.action == MotionEvent.ACTION_MOVE && isGamepad) {
            val flexorValue = getTriggerValue(event, MotionEvent.AXIS_LTRIGGER)
            val extensorValue = getTriggerValue(event, MotionEvent.AXIS_RTRIGGER)

            gamepadTriggerListener?.invoke(
                (flexorValue * 100).roundToInt(),
                (extensorValue * 100).roundToInt()
            )

            if (gamepadTriggerListener != null) return true
        }

        return super.dispatchGenericMotionEvent(event)
    }

    // Клавиатура (как и геймпад — только в debug): F — сгибатель, J — разгибатель, с Shift — сильнее.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!isDebug) return super.dispatchKeyEvent(event)

        val channel = when (event.keyCode) {
            KeyEvent.KEYCODE_F -> Channel.FLEXOR
            KeyEvent.KEYCODE_J -> Channel.EXTENSOR
            else -> return super.dispatchKeyEvent(event)
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> emulator.setChannel(channel, if (event.isShiftPressed) 100 else 60)
            KeyEvent.ACTION_UP -> emulator.setChannel(channel, 0)
        }
        return true
    }

    private fun getTriggerValue(event: MotionEvent, axis: Int): Float {
        val device = event.device ?: return 0f
        val range = device.getMotionRange(axis, event.source)
            ?: device.getMotionRange(axis)
            ?: return event.getAxisValue(axis).coerceIn(0f, 1f)

        val value = event.getAxisValue(axis)
        val span = range.max - range.min

        if (span <= 0f) return 0f

        return ((value - range.min) / span).coerceIn(0f, 1f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isDebug = (applicationInfo.flags and
                ApplicationInfo.FLAG_DEBUGGABLE) != 0

        emulator.start()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyHomeApp(emulator = emulator, isDebug = isDebug)
                }
            }
        }
    }
}

private enum class Screen { CALIBRATION, MENU_TEST, SIGNAL_CHECK, GALLERY }

@Composable
fun MyHomeApp(emulator: EmulatorSignalSource, isDebug: Boolean) {

    // Профиль пока живёт только в памяти, поэтому после поворота экрана начинаем с калибровки.
    var screen by remember { mutableStateOf(Screen.CALIBRATION) }
    var profile by remember { mutableStateOf(SignalProfile()) }

    var overlayExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    // Код состояния и задержка — для панели эмулятора.
    var stateCode by remember { mutableStateOf("00") }
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    val processor = remember(profile) { SignalProcessor(profile) }

    LaunchedEffect(emulator, processor) {
        emulator.frames.collect { frame ->
            stateCode = processor.process(frame).code
            withFrameNanos {
                latencyMs = System.nanoTime() / 1_000_000 - frame.timestampMs
            }
        }
    }

    val activity = LocalContext.current as? MainActivity

    SideEffect {
        if (isDebug) {
            activity?.setGamepadTriggerListener { l2, r2 ->
                emulator.setRaw(l2, r2)
            }
        } else {
            activity?.setGamepadTriggerListener(null)
        }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.setGamepadTriggerListener(null)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Экраны
        when (screen) {
            Screen.CALIBRATION -> CalibrationScreen(
                source = emulator,
                onFinished = {
                    profile = it
                    screen = Screen.MENU_TEST
                }
            )

            Screen.MENU_TEST -> MenuTestScreen(
                source = emulator,
                profile = profile,
                onOpen = { index ->
                    when (index) {
                        0 -> screen = Screen.SIGNAL_CHECK
                        1 -> screen = Screen.CALIBRATION
                        2 -> screen = Screen.GALLERY
                    }
                },
                onBack = { screen = Screen.CALIBRATION }
            )

            Screen.GALLERY -> ComponentGallery(
                source = emulator,
                profile = profile,
                onBack = { screen = Screen.MENU_TEST }
            )

            Screen.SIGNAL_CHECK -> SignalCheckScreen(
                source = emulator,
                profile = profile,
                onExit = { screen = Screen.MENU_TEST }
            )
        }

        // Эмулятор доступен только в debug-сборке.
        // Свёрнут — маленькие кнопки С / Р / С+Р у правого края, развёрнут — большая панель.
        if (isDebug && !overlayExpanded) {
            EmulatorPads(
                emulator = emulator,
                onOpenSettings = { overlayExpanded = true },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }

        if (isDebug && overlayExpanded) {

            SignalOverlay(
                expanded = overlayExpanded,

                onExpandedChange = {
                    overlayExpanded = it
                },

                emulator = emulator,
                stateCode = stateCode,
                latencyMs = latencyMs,

                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
            )
        }
    }
}

/**
 * Временный экран для проверки MenuNavigator (уберётся с настоящим меню)
 */
@Composable
private fun MenuTestScreen(
    source: SignalSource,
    profile: SignalProfile,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit
) {
    val items = listOf("Проверка сигнала", "Калибровка заново", "Витрина компонентов")
    val navigator = remember(profile) { MenuNavigator(items.size, profile.level) }
    val processor = remember(profile) { SignalProcessor(profile) }
    val detector = remember(profile) { HoldDetector(profile.minPulseMs, profile.holdToSelectMs) }
    val currentOnOpen by rememberUpdatedState(onOpen)
    val currentOnBack by rememberUpdatedState(onBack)

    var selected by remember { mutableIntStateOf(0) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var lastEvent by remember { mutableStateOf("пока ничего") }

    LaunchedEffect(source, navigator) {
        source.frames.collect { frame ->
            val event = detector.update(processor.process(frame))
            holdProgress = detector.progress
            if (event == null) return@collect

            val action = navigator.onEvent(event)
            selected = navigator.selected
            lastEvent = when (event) {
                is ControlEvent.Pulse -> "короткий ${if (event.channel == Channel.FLEXOR) "С" else "Р"}"
                is ControlEvent.Hold -> "удержание ${if (event.channel == Channel.FLEXOR) "С" else "Р"}"
            }
            when (action) {
                is MenuAction.Select -> {
                    lastEvent += " → выбран «${items[action.index]}»"
                    currentOnOpen(action.index)
                }
                MenuAction.Back -> currentOnBack()
                null -> Unit
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeColors.Background)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Проверка меню", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
        Text(
            if (profile.level == UserLevel.NOVICE)
                "Режим: новичок. Короткий сигнал любой мышцей — следующий пункт, удержание — выбрать."
            else
                "Режим: опытный. Короткий С — вниз, Р — вверх. Удержание С — выбрать, удержание Р — назад (к калибровке).",
            fontSize = 15.sp, color = HomeColors.InkSecondary
        )

        items.forEachIndexed { index, title ->
            val isSelected = index == selected
            val shape = RoundedCornerShape(18.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(HomeColors.Card)
                    .then(if (isSelected) Modifier.border(3.dp, HomeColors.Flexor, shape) else Modifier)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected && holdProgress > 0f) {
                    Text("${(holdProgress * 100).roundToInt()}%", fontSize = 15.sp, color = HomeColors.Flexor)
                }
            }
        }

        Text("Последнее событие: $lastEvent", fontSize = 15.sp, color = HomeColors.Ink)
    }
}