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
import com.robotpaw.myhome.ui.components.StarsBadge
import com.robotpaw.myhome.ui.components.starsText
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

/**
 * Комната, каждое задание - пункт меню (у новичка последний пункт - выход)
 * @param initialWall - с какого задания начать
 * @param pending - дела на сегодня, ещё не сделанные: предмет снова «до», хоть задание и пройдено
 */
@Composable
fun RoomScreen(
    source: SignalSource,
    profile: SignalProfile,
    room: Room,
    progress: GameProgress,
    initialWall: Int,
    onStart: (Mission) -> Unit,
    onBack: () -> Unit,
    pending: Set<String> = emptySet()
) {
    val missions = room.missions
    // как выглядит предмет: дело на сегодня снова «до»
    fun looksDone(m: Mission) = progress.isCompleted(m) && m.id !in pending
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = missions.map {
            val locked = isLocked(it, progress)
            val subtitle = when {
                locked -> "Уже выполнено, повторить нельзя"
                it.id in pending -> "Дело на сегодня · ${subtitleOf(it)}"
                else -> subtitleOf(it)
            }
            MenuItem(it.wallTitle, subtitle, done = looksDone(it))
        },
        backTitle = "Выйти в меню",
        initialIndex = initialWall,
        onBack = onBack,
        onSelect = { if (!isLocked(missions[it], progress)) onStart(missions[it]) }
    )
    val index = control.selected
    val mission = missions.getOrNull(index)   // null - пункт «Выйти в меню»
    val item = control.items[index]
    val summary = progress.summaryOf(room)

    HomeScreen(
        title = room.title,
        overline = "${index + 1} из ${control.items.size}",
        backProgress = control.backProgress,
        trailing = { StarsBadge(summary.stars, summary.maxStars) }
    ) {
        val scene = mission?.let { RoomScenes.of(it.kind) }
        if (mission != null && scene != null) {
            RoomSceneView(
                art = scene,
                selected = mission.kind,
                isDone = { kind -> missions.any { it.kind == kind && looksDone(it) } },
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

        // у закрытого задания кольцо не заполняется: выбрать его нельзя
        val locked = mission != null && isLocked(mission, progress)
        WallCard(item, if (locked) 0f else control.selectProgress, control.ringColor, stars = mission?.let { progress.starsOf(it) })
    }
}

/**
 * Карточка стены под сценой (её же показывает экран смены предмета, чтобы сцена была того же размера)
 * @param stars - лучшие звёзды за задание, у выполненного показываются рядом с «выполнено»
 */
@Composable
internal fun WallCard(item: MenuItem, ringProgress: Float, ringColor: Color, stars: Int? = null) {
    HomeCard(highlighted = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
                if (item.subtitle != null) {
                    Text(item.subtitle, fontSize = HomeType.Label, color = HomeColors.InkSecondary)
                }
                // строка есть всегда, у невыполненного она прозрачная: высота карточки и размер сцены не прыгают
                Text(
                    if (stars != null && stars > 0) "выполнено · ${starsText(stars)}" else "выполнено",
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

// выполненное задание, которое нельзя повторить (выбор питомца)
private fun isLocked(mission: Mission, progress: GameProgress) = !mission.repeatable && progress.isCompleted(mission)

internal fun subtitleOf(mission: Mission): String = listOfNotNull(
    mission.title,
    mission.difficulty.label.ifEmpty { null },
    if (mission.optional) "доп. сложность" else null
).joinToString(" · ")