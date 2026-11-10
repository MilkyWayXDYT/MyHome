package com.robotpaw.myhome.calibration

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.menu.MenuItem
import com.robotpaw.myhome.menu.MenuList
import com.robotpaw.myhome.menu.rememberMenuControl
import com.robotpaw.myhome.mission.mechanics.core.rememberMechanicInput
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.components.ProgressStrip
import com.robotpaw.myhome.ui.components.SignalBars
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

private enum class WelcomePhase { MENU, CHECK, FAILED }

/**
 * Повторный запуск: калибровку можно пропустить, вместо неё короткая разминка
 * Если на разминке сигнал не похож на прошлый раз, предлагаем пройти калибровку
 * @param profile - сохранённый профиль с прошлой калибровки
 * @param onReady - разминка пройдена или игрок решил продолжить без неё
 */
@Composable
fun WelcomeBackScreen(
    source: SignalSource,
    profile: SignalProfile,
    onReady: () -> Unit,
    onRecalibrate: () -> Unit
) {
    var phase by remember { mutableStateOf(WelcomePhase.MENU) }
    when (phase) {
        WelcomePhase.MENU -> WelcomeMenu(source, profile, onCheck = { phase = WelcomePhase.CHECK }, onRecalibrate = onRecalibrate)
        WelcomePhase.CHECK -> WarmUp(source, profile, onPassed = onReady, onFailed = { phase = WelcomePhase.FAILED })
        WelcomePhase.FAILED -> CheckFailed(source, profile, onRecalibrate = onRecalibrate, onContinue = onReady)
    }
}

@Composable
private fun WelcomeMenu(source: SignalSource, profile: SignalProfile, onCheck: () -> Unit, onRecalibrate: () -> Unit) {
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(
            MenuItem("Продолжить", "короткая разминка вместо калибровки"),
            MenuItem("Пройти калибровку заново", "если электроды стоят иначе, чем в прошлый раз")
        ),
        onSelect = { if (it == 0) onCheck() else onRecalibrate() }
    )
    HomeScreen(title = "С возвращением!", overline = "Мой дом", subtitle = "Настройки сигнала сохранились с прошлого раза.") {
        MenuList(control)
    }
}

@Composable
private fun WarmUp(source: SignalSource, profile: SignalProfile, onPassed: () -> Unit, onFailed: () -> Unit) {
    val check = remember { QuickCheck() }
    var stepIndex by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var finished by remember { mutableStateOf(false) }

    val input = rememberMechanicInput(source, profile) { state, dt ->
        if (finished) return@rememberMechanicInput
        check.update(state.code, dt)
        stepIndex = check.stepIndex
        progress = check.holdProgress
        if (check.isPassed || check.failed) finished = true
    }
    LaunchedEffect(finished) {
        if (finished) if (check.isPassed) onPassed() else onFailed()
    }

    val step = QuickCheckStep.entries.getOrNull(stepIndex) ?: QuickCheckStep.REST
    HomeScreen(
        title = "Разминка",
        overline = "Шаг ${(stepIndex + 1).coerceAtMost(QuickCheckStep.entries.size)} из ${QuickCheckStep.entries.size}",
        subtitle = "Проверим, что настройки с прошлого раза подходят."
    ) {
        HomeCard {
            Text(
                if (input.armed) step.text else "Расслабьте руку, чтобы начать",
                fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink
            )
        }
        ProgressStrip(
            progress,
            when (step) {
                QuickCheckStep.SQUEEZE -> HomeColors.Flexor
                QuickCheckStep.OPEN -> HomeColors.Extensor
                QuickCheckStep.REST -> HomeColors.Accent
            },
            height = 16.dp
        )
        SignalBars(input.state)
    }
}

@Composable
private fun CheckFailed(source: SignalSource, profile: SignalProfile, onRecalibrate: () -> Unit, onContinue: () -> Unit) {
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(MenuItem("Пройти калибровку"), MenuItem("Всё равно продолжить")),
        onSelect = { if (it == 0) onRecalibrate() else onContinue() }
    )
    HomeScreen(
        title = "Сигнал изменился",
        overline = "Разминка",
        subtitle = "Похоже, электроды стоят иначе, чем в прошлый раз. Лучше пройти калибровку заново."
    ) {
        MenuList(control)
    }
}