
package com.robotpaw.myhome

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.developer.SignalOverlay
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
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

        val isDebug = (applicationInfo.flags and
                ApplicationInfo.FLAG_DEBUGGABLE) != 0

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyHomeApp(isDebug = isDebug)
                }
            }
        }
    }
}

@Composable
fun MyHomeApp(isDebug: Boolean) {

    var flexor by rememberSaveable {
        mutableIntStateOf(0)
    }

    var extensor by rememberSaveable {
        mutableIntStateOf(0)
    }

    var overlayExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    val activity = LocalContext.current as? MainActivity

    SideEffect {
        if (isDebug) {
            activity?.setGamepadTriggerListener { l2, r2 ->
                flexor = l2
                extensor = r2
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

        // Главная страница
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "МОЙ ДОМ",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Эмулятор доступен только после скрытого входа
        if (isDebug) {

            SignalOverlay(
                expanded = overlayExpanded,

                onExpandedChange = {
                    overlayExpanded = it
                },

                flexor = flexor,
                extensor = extensor,

                onFlexorChange = {
                    flexor = it
                },

                onExtensorChange = {
                    extensor = it
                },

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
