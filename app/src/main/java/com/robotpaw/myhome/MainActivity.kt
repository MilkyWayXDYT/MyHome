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
import com.robotpaw.myhome.home.SignalCheckScreen
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalProcessor
import com.robotpaw.myhome.signal.SignalProfile
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    /** Единственный источник сигнала для игры. Позже здесь будет драйвер протеза. */
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

private enum class Screen { CALIBRATION, SIGNAL_CHECK }

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
                    screen = Screen.SIGNAL_CHECK
                }
            )

            Screen.SIGNAL_CHECK -> SignalCheckScreen(
                source = emulator,
                profile = profile,
                onRecalibrate = { screen = Screen.CALIBRATION }
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