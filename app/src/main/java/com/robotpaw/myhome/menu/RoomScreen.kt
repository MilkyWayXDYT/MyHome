package com.robotpaw.myhome.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.Room
import com.robotpaw.myhome.mission.EmptyWall
import com.robotpaw.myhome.mission.MissionObject
import com.robotpaw.myhome.mission.RoomSceneView
import com.robotpaw.myhome.mission.RoomScenes
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.ui.components.HoldRing
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

/**
 * Комната, каждое задание - пункт меню (у новичка последний пункт - выход)
 * @param initialWall - с какого задания начать
 */
@Composable
fun RoomScreen(
    source: SignalSource,
    profile: SignalProfile,
    room: Room,
    progress: GameProgress,
    initialWall: Int,
    onStart: (Mission) -> Unit,
    onBack: () -> Unit
) {
    val missions = room.missions
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = missions.map { MenuItem(it.wallTitle, subtitleOf(it), done = progress.isCompleted(it)) },
        backTitle = "Выйти в меню",
        initialIndex = initialWall,
        onBack = onBack,
        onSelect = { onStart(missions[it]) }
    )
    val index = control.selected
    val mission = missions.getOrNull(index)   // null - пункт «Выйти в меню»
    val item = control.items[index]

    HomeScreen(
        title = room.title,
        overline = "${index + 1} из ${control.items.size}",
        backProgress = control.backProgress
    ) {
        val scene = mission?.let { RoomScenes.of(it.kind) }
        if (mission != null && scene != null) {
            RoomSceneView(
                art = scene,
                selected = mission.kind,
                isDone = { kind -> missions.any { it.kind == kind && progress.isCompleted(it) } },
                modifier = Modifier.weight(1f)
            )
        } else if (mission != null) {
            MissionObject(
                kind = mission.kind,
                progress = if (item.done) 1f else 0f,
                modifier = Modifier.weight(1f)
            )
        } else {
            EmptyWall(Modifier.weight(1f)) {
                Text("Выход в главное меню", fontSize = HomeType.Heading, fontWeight = FontWeight.Bold, color = HomeColors.InkSecondary)
            }
        }

        WallDots(count = control.items.size, selected = index)

        WallCard(item, control.selectProgress, control.ringColor)
    }
}

/**
 * Карточка стены под сценой (её же показывает экран смены предмета, чтобы сцена была того же размера)
 */
@Composable
internal fun WallCard(item: MenuItem, ringProgress: Float, ringColor: Color) {
    HomeCard(highlighted = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
                if (item.subtitle != null) {
                    Text(item.subtitle, fontSize = HomeType.Label, color = HomeColors.InkSecondary)
                }
                // строка есть всегда, у невыполненного она прозрачная: высота карточки и размер сцены не прыгают
                Text(
                    "выполнено",
                    fontSize = HomeType.Label,
                    fontWeight = FontWeight.Bold,
                    color = if (item.done) HomeColors.Accent else Color.Transparent,
                    modifier = if (item.done) Modifier else Modifier.clearAndSetSemantics {}
                )
            }
            HoldRing(ringProgress, ringColor)
        }
    }
}

@Composable
internal fun WallDots(count: Int, selected: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(if (i == selected) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (i == selected) HomeColors.Accent else HomeColors.Divider)
            )
        }
    }
}

internal fun subtitleOf(mission: Mission): String = listOfNotNull(
    mission.title,
    mission.difficulty.label.ifEmpty { null },
    if (mission.optional) "доп. сложность" else null
).joinToString(" · ")