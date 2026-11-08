package com.robotpaw.myhome.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.HoldDetector
import com.robotpaw.myhome.signal.MenuAction
import com.robotpaw.myhome.signal.MenuNavigator
import com.robotpaw.myhome.signal.SignalProcessor
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.signal.UserLevel
import com.robotpaw.myhome.ui.components.channelColor

data class MenuItem(val title: String, val subtitle: String? = null, val done: Boolean = false)

/**
 * Состояние меню для отрисовки, создаётся через rememberMenuControl
 * @param items - пункты для отрисовки (у новичка в конце «Назад»)
 */
class MenuControl internal constructor(
    val items: List<MenuItem>,
    val level: UserLevel,
    val hasBack: Boolean,
    internal val navigator: MenuNavigator
) {
    var selected by mutableIntStateOf(navigator.selected)
        internal set

    internal var holdProgress by mutableFloatStateOf(0f)
    internal var holdChannel by mutableStateOf<Channel?>(null)

    val selectProgress: Float
        get() = holdChannel?.let { if (navigator.holdSelects(it)) holdProgress else 0f } ?: 0f

    // «назад» удержанием Р, только у опытного
    val backProgress: Float
        get() = holdChannel?.let { if (!navigator.holdSelects(it)) holdProgress else 0f } ?: 0f

    val ringColor: Color
        get() = channelColor(holdChannel ?: Channel.FLEXOR)
}

/**
 * Меню, которое управляется сигналом
 * @param items - пункты без «Назад»
 * @param backTitle - текст пункта «Назад» у новичка, null - назад нельзя
 * @param enabled - false, команды не принимаются (например, пока идёт таймер паузы)
 */
@Composable
fun rememberMenuControl(
    source: SignalSource,
    profile: SignalProfile,
    items: List<MenuItem>,
    backTitle: String? = null,
    enabled: Boolean = true,
    initialIndex: Int = 0,
    onBack: () -> Unit = {},
    onSelect: (Int) -> Unit
): MenuControl {
    val hasBack = backTitle != null
    val withBack = if (hasBack && profile.level == UserLevel.NOVICE) items + MenuItem(backTitle!!) else items

    val control = remember(profile, withBack) {
        MenuControl(withBack, profile.level, hasBack, MenuNavigator(withBack.size, profile.level, initialIndex))
    }
    val processor = remember(profile) { SignalProcessor(profile) }
    val detector = remember(profile) { HoldDetector(profile.minPulseMs, profile.holdToSelectMs) }

    val currentEnabled by rememberUpdatedState(enabled)
    // после включения ждём расслабления, иначе удержание, начатое во время таймера, сразу нажмёт пункт
    LaunchedEffect(enabled) {
        if (enabled) detector.reset()
    }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnBack by rememberUpdatedState(onBack)
    val baseCount = items.size

    LaunchedEffect(source, control) {
        source.frames.collect { frame ->
            val event = detector.update(processor.process(frame))
            control.holdChannel = detector.progressChannel
            control.holdProgress = if (currentEnabled) detector.progress else 0f

            if (event == null || !currentEnabled) return@collect
            when (val action = control.navigator.onEvent(event)) {
                is MenuAction.Select ->
                    if (action.index >= baseCount) currentOnBack() else currentOnSelect(action.index)
                MenuAction.Back -> if (hasBack) currentOnBack()
                null -> Unit
            }
            control.selected = control.navigator.selected
        }
    }
    return control
}