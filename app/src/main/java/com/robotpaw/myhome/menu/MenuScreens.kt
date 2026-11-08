package com.robotpaw.myhome.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.Room
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.signal.UserLevel
import com.robotpaw.myhome.ui.components.ControlHints
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.components.MenuItemCard
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

@Composable
fun MenuList(control: MenuControl, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        control.items.forEachIndexed { index, item ->
            MenuItemCard(
                title = item.title,
                subtitle = item.subtitle,
                selected = index == control.selected,
                holdProgress = if (index == control.selected) control.selectProgress else 0f,
                ringColor = control.ringColor,
                done = item.done
            )
        }
    }
}

enum class MainMenuChoice { CONTINUE, ROOMS, GOALS, SIGNAL_CHECK, RECALIBRATE, GALLERY }

/**
 * Главное меню
 * @param showDeveloperItems - пункт «Витрина компонентов» (только debug)
 */
@Composable
fun MainMenuScreen(
    source: SignalSource,
    profile: SignalProfile,
    progress: GameProgress,
    showDeveloperItems: Boolean = false,
    onChoice: (MainMenuChoice) -> Unit
) {
    val room = progress.currentRoom()
    val unlocked = Rooms.ALL.count { progress.isUnlocked(it) }
    val levelText = if (profile.level == UserLevel.NOVICE) "новичок" else "опытный"

    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(
            MenuItem("Продолжить тренировку", "${room.title} · ${progress.completedCount(room)} из ${room.requiredMissions.size}"),
            MenuItem("Все комнаты", "открыто $unlocked из ${Rooms.ALL.size}"),
            MenuItem("Цели на сегодня", "что даёт звёзды"),
            MenuItem("Проверка сигнала", "код состояния и команды меню"),
            MenuItem("Калибровка заново", "сейчас режим: $levelText")
        ) + if (showDeveloperItems) listOf(MenuItem("Витрина компонентов", "только в debug-сборке")) else emptyList(),
        onSelect = { onChoice(MainMenuChoice.entries[it]) }
    )

    HomeScreen(
        title = "Мой дом",
        overline = "Добрый день!",
        bottom = { ControlHints(control.level, hasBack = false) }
    ) {
        MenuList(control)
    }
}

/**
 * Список комнат, закрытые выбрать нельзя
 */
@Composable
fun RoomsScreen(
    source: SignalSource,
    profile: SignalProfile,
    progress: GameProgress,
    onOpen: (Room) -> Unit,
    onBack: () -> Unit
) {
    val rooms = Rooms.ALL
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = rooms.mapIndexed { i, room ->
            val subtitle = when {
                !progress.isUnlocked(room) -> "закрыто, сначала: ${rooms[i - 1].title}"
                progress.isDone(room) -> "пройдено"
                else -> "${progress.completedCount(room)} из ${room.requiredMissions.size}"
            }
            MenuItem(room.title, subtitle, done = progress.isDone(room))
        },
        backTitle = "В меню",
        onBack = onBack,
        onSelect = { if (progress.isUnlocked(rooms[it])) onOpen(rooms[it]) }
    )

    HomeScreen(
        title = "Комнаты",
        bottom = { ControlHints(control.level, hasBack = true, backProgress = control.backProgress) }
    ) {
        MenuList(control)
    }
}

/**
 * Цели на сегодня (заглушка, только вёрстка)
 */
@Composable
fun GoalsScreen(
    source: SignalSource,
    profile: SignalProfile,
    onBack: () -> Unit
) {
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(MenuItem("В меню")),
        onSelect = { onBack() }
    )

    HomeScreen(
        title = "Цели на сегодня",
        overline = "Подобрано для вас",
        bottom = { ControlHints(control.level, hasBack = false) }
    ) {
        listOf("Цель 1", "Цель 2", "Цель 3").forEach { title ->
            HomeCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title, fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold,
                        color = HomeColors.Ink, modifier = Modifier.weight(1f)
                    )
                    Text("+★", fontSize = HomeType.Body, fontWeight = FontWeight.Bold, color = HomeColors.Extensor)
                }
                Text("Описание цели", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
            }
        }
        MenuList(control)
    }
}