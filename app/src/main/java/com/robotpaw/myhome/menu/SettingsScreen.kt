package com.robotpaw.myhome.menu

import androidx.compose.runtime.Composable
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.signal.UserLevel
import com.robotpaw.myhome.ui.components.HomeScreen

enum class SettingsChoice { MUSIC, EFFECTS, SIGNAL_CHECK, RECALIBRATE }

/**
 * Настройки: звук, проверка сигнала, калибровка
 * Музыка и эффекты переключаются выбором пункта, подпись показывает, что сейчас
 * @param musicOn - фоновая музыка включена
 * @param effectsOn - звуки действий и меню включены
 */
@Composable
fun SettingsScreen(
    source: SignalSource,
    profile: SignalProfile,
    musicOn: Boolean,
    effectsOn: Boolean,
    onChoice: (SettingsChoice) -> Unit,
    onBack: () -> Unit
) {
    val levelText = if (profile.level == UserLevel.NOVICE) "новичок" else "опытный"
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(
            MenuItem("Музыка", if (musicOn) "включена · выбрать, чтобы выключить" else "выключена · выбрать, чтобы включить"),
            MenuItem("Звуки", if (effectsOn) "включены · выбрать, чтобы выключить" else "выключены · выбрать, чтобы включить"),
            MenuItem("Проверка сигнала", "код состояния и команды меню"),
            MenuItem("Калибровка заново", "сейчас режим: $levelText")
        ),
        backTitle = "В меню",
        onBack = onBack,
        onSelect = { onChoice(SettingsChoice.entries[it]) }
    )

    HomeScreen(
        title = "Настройки",
        backProgress = control.backProgress
    ) {
        MenuList(control)
    }
}