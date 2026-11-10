package com.robotpaw.myhome.mission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.robotpaw.myhome.game.PetKind
import com.robotpaw.myhome.game.MissionStats
import com.robotpaw.myhome.game.RecordOutcome
import com.robotpaw.myhome.game.RecordStatus
import com.robotpaw.myhome.game.RoomSummary
import com.robotpaw.myhome.game.Stars
import com.robotpaw.myhome.game.StarsOutcome
import com.robotpaw.myhome.game.formatTime
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
 * @param pet - выбранный питомец (для заданий финала)
 * @param onPetChosen - игрок выбрал питомца в задании «Питомец»
 */
@Composable
fun MissionScreen(
    source: SignalSource,
    profile: SignalProfile,
    roomTitle: String,
    mission: Mission,
    checkpoint: MissionCheckpoint,
    onDone: (MissionResult) -> Unit,
    onPause: (MissionCheckpoint) -> Unit,
    pet: PetKind = PetKind.CAT,
    onPetChosen: (PetKind) -> Unit = {}
) {
    if (Mechanics.has(mission.kind)) {
        MissionMechanic(
            source = source,
            profile = profile,
            roomTitle = roomTitle,
            mission = mission,
            checkpoint = checkpoint,
            onDone = { log -> onDone(MissionResult(mission, log)) },
            onPause = onPause,
            pet = pet,
            onPetChosen = onPetChosen
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
    stats: MissionStats,
    record: RecordOutcome,
    stars: StarsOutcome,
    onChoice: (ResultChoice) -> Unit
) {
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = if (result.mission.repeatable) listOf(MenuItem("В комнату"), MenuItem("Ещё раз")) else listOf(MenuItem("В комнату")),
        onSelect = { onChoice(ResultChoice.entries[it]) }
    )

    HomeScreen(
        title = result.mission.doneTitle,
        overline = "Результат задания"
    ) {
        HomeCard {
            Text(
                "★".repeat(stars.earned) + "☆".repeat(Stars.MAX - stars.earned),
                fontSize = HomeType.Heading, color = HomeColors.Accent
            )
            if (stats.hasData) {
                StatRow("Время", stats.timeText)
                StatRow("Чистота сигнала", "${stats.cleanPercent}%")
                StatRow("Ошибки", stats.mistakes.toString())
                if (stats.interruptions > 0) StatRow("Срывы сигнала", stats.interruptions.toString())
                if (stats.wrongChannel > 0) StatRow("Не та мышца", stats.wrongChannel.toString())
                if (stats.averageStrength > 0) {
                    StatRow("Средняя сила", "${stats.averageStrength}%")
                    StatRow("Ровность нажатия", "${stats.steadyPercent}%")
                }
                if (stats.bestStreak > 0) StatRow("Лучшая серия", stats.bestStreak.toString())
                if (stats.pauses > 0) StatRow("Окна поддержки", stats.pauses.toString())
            } else {
                Text("Задание выполнено", fontSize = HomeType.Body, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
            }
            RecordLine(record)
            Text(
                if (stars.gained > 0) "+${stars.gained} ${Stars.word(stars.gained)}" else "Звёзды за это задание уже получены",
                fontSize = HomeType.Label, color = HomeColors.InkSecondary
            )
            StatRow("Всего звёзд", stars.total.toString())
        }
        MenuList(control)
    }
}

// строка статистики: подпись слева, значение справа
@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = HomeType.Label, color = HomeColors.InkSecondary)
        Text(value, fontSize = HomeType.Body, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
    }
}

// что показать про рекорд под статистикой
@Composable
private fun RecordLine(record: RecordOutcome) {
    val best = record.best
    when (record.status) {
        RecordStatus.NONE -> Unit
        RecordStatus.FIRST ->
            Text("Первое прохождение, результат сохранён", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
        RecordStatus.NEW ->
            Text("Новый рекорд!", fontSize = HomeType.Body, fontWeight = FontWeight.ExtraBold, color = HomeColors.Accent)
        RecordStatus.NOT_BEATEN -> if (best != null) {
            Text(
                "Ваш рекорд: ${formatTime(best.durationMs)}, ошибок: ${best.mistakes}",
                fontSize = HomeType.Label, color = HomeColors.InkSecondary
            )
        }
    }
}

/**
 * Экран «Отдохни» после комнаты: итоги комнаты и совет расслабить руку
 * @param summary - итоги по лучшим результатам заданий комнаты
 */
@Composable
fun RestScreen(
    source: SignalSource,
    profile: SignalProfile,
    roomTitle: String,
    nextRoomTitle: String?,
    summary: RoomSummary? = null,
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
        if (summary != null) {
            HomeCard {
                Text("Итоги комнаты", fontSize = HomeType.Body, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
                StatRow("Задания", "${summary.done} из ${summary.total}")
                if (summary.maxStars > 0) StatRow("Звёзды", "${summary.stars} из ${summary.maxStars}")
                if (summary.hasData) {
                    StatRow("Время", formatTime(summary.totalMs))
                    StatRow("Ошибки", summary.mistakes.toString())
                    StatRow("Чистота сигнала", "${summary.cleanPercent}%")
                }
            }
        }
        Spacer(Modifier.weight(1f))
        MenuList(control)
    }
}