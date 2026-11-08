package com.robotpaw.myhome.mission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.MissionCheckpoint
import com.robotpaw.myhome.game.MissionResult
import com.robotpaw.myhome.menu.MenuItem
import com.robotpaw.myhome.menu.MenuList
import com.robotpaw.myhome.menu.rememberMenuControl
import com.robotpaw.myhome.mission.mechanics.Mechanics
import com.robotpaw.myhome.mission.mechanics.MissionMechanic
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType
import kotlinx.coroutines.delay

// сколько ждать на паузе, прежде чем можно выбрать
const val PAUSE_SECONDS = 15

@Composable
fun InstructionScreen(
    source: SignalSource,
    profile: SignalProfile,
    mission: Mission,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(MenuItem("Понятно, начать")),
        backTitle = "Назад в комнату",
        onBack = onBack,
        onSelect = { onStart() }
    )

    HomeScreen(
        title = mission.title,
        overline = "Инструкция к заданию",
        backProgress = control.backProgress
    ) {
        HomeCard {
            mission.instruction.forEachIndexed { i, line ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("${i + 1}", fontSize = HomeType.Body, fontWeight = FontWeight.ExtraBold, color = HomeColors.Accent)
                    Text(line, fontSize = HomeType.Body, color = HomeColors.Ink)
                }
            }
        }
        MenuList(control)
    }
}

/**
 * Экран задания: если у вида задания есть механика, запускает её, иначе показывает заглушку
 * @param checkpoint - откуда продолжаем после паузы
 * @param onPause - окно поддержки; чекпоинт вернётся в этот экран, если игрок выберет «Продолжить»
 */
@Composable
fun MissionScreen(
    source: SignalSource,
    profile: SignalProfile,
    roomTitle: String,
    mission: Mission,
    checkpoint: MissionCheckpoint,
    onDone: (MissionResult) -> Unit,
    onPause: (MissionCheckpoint) -> Unit
) {
    if (Mechanics.has(mission.kind)) {
        MissionMechanic(
            source = source,
            profile = profile,
            roomTitle = roomTitle,
            mission = mission,
            checkpoint = checkpoint,
            onDone = { log -> onDone(MissionResult(mission, log)) },
            onPause = onPause
        )
    } else {
        StubMission(source, profile, roomTitle, mission, onDone) { onPause(checkpoint) }
    }
}

/**
 * Заглушка для заданий без механики: два пункта меню
 */
@Composable
private fun StubMission(
    source: SignalSource,
    profile: SignalProfile,
    roomTitle: String,
    mission: Mission,
    onDone: (MissionResult) -> Unit,
    onPause: () -> Unit
) {
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(
            MenuItem("Выполнить задание", "заглушка: сразу к результату"),
            MenuItem("Пауза")
        ),
        onSelect = { if (it == 0) onDone(MissionResult(mission)) else onPause() }
    )

    HomeScreen(
        title = mission.title,
        overline = "$roomTitle · задание"
    ) {
        MissionObject(mission.kind, progress = 0f, modifier = Modifier.weight(1f))
        HomeCard {
            Text("Механика задания в разработке", fontSize = HomeType.Body, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
            Text("Пока задание можно только завершить из меню.", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
        }
        MenuList(control)
    }
}

enum class PauseChoice { CONTINUE, MENU }

/**
 * Пауза, пока идёт таймер выбор выключен
 */
@Composable
fun PauseScreen(
    source: SignalSource,
    profile: SignalProfile,
    onChoice: (PauseChoice) -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(PAUSE_SECONDS) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(MenuItem("Продолжить задание"), MenuItem("В главное меню")),
        enabled = secondsLeft == 0,
        onSelect = { onChoice(PauseChoice.entries[it]) }
    )

    HomeScreen(
        title = "Сделаем паузу",
        overline = "Окно поддержки",
        subtitle = "Мышцы устают — это нормально.",
        bottom = {
            if (secondsLeft > 0) {
                Text("Пока идёт таймер, выбор выключен", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
            }
        }
    ) {
        HomeCard {
            Text(
                "0:${secondsLeft.toString().padStart(2, '0')}",
                fontSize = HomeType.Display, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink
            )
            Text("Расслабьте руку и подышите", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
        }
        MenuList(control)
    }
}

enum class ResultChoice { ROOM, AGAIN }

@Composable
fun ResultScreen(
    source: SignalSource,
    profile: SignalProfile,
    result: MissionResult,
    onChoice: (ResultChoice) -> Unit
) {
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(MenuItem("В комнату"), MenuItem("Ещё раз")),
        onSelect = { onChoice(ResultChoice.entries[it]) }
    )

    HomeScreen(
        title = result.mission.doneTitle,
        overline = "Результат задания"
    ) {
        HomeCard {
            Text("Здесь будет статистика задания", fontSize = HomeType.Body, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
            Text("Чистота сигнала, удержание, встречные сигналы", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
        }
        MenuList(control)
    }
}

/**
 * Экран «Отдохни» после комнаты (заглушка)
 */
@Composable
fun RestScreen(
    source: SignalSource,
    profile: SignalProfile,
    roomTitle: String,
    nextRoomTitle: String?,
    onNext: () -> Unit
) {
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(MenuItem(if (nextRoomTitle != null) "Дальше: $nextRoomTitle" else "В меню")),
        onSelect = { onNext() }
    )

    HomeScreen(
        title = "Отдохните",
        overline = "$roomTitle пройдена",
        subtitle = "Расслабьте руку, прежде чем идти дальше."
    ) {
        HomeCard {
            Text("Здесь будет экран отдыха", fontSize = HomeType.Body, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
        }
        Spacer(Modifier.weight(1f))
        MenuList(control)
    }
}
