package com.robotpaw.myhome.mission

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.MissionKind
import com.robotpaw.myhome.game.Room
import com.robotpaw.myhome.menu.MenuItem
import com.robotpaw.myhome.menu.WallCard
import com.robotpaw.myhome.menu.WallDots
import com.robotpaw.myhome.menu.subtitleOf
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.theme.HomeColors
import kotlinx.coroutines.delay

/**
 * Короткая сцена после задания: в комнате предмет меняется на «после», потом сам открывается экран результата
 * Нажимать ничего не нужно. Разметка как у экрана комнаты, чтобы сцена была того же размера
 * @param isDone - выполнены ли остальные задания комнаты (их предметы показываются как есть)
 */
@Composable
fun SceneChangeScreen(
    mission: Mission,
    room: Room,
    isDone: (MissionKind) -> Boolean,
    onFinished: () -> Unit
) {
    val art = RoomScenes.of(mission.kind)
    val change = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (art == null) {
            onFinished()
            return@LaunchedEffect
        }
        delay(PAUSE_BEFORE_MS)
        change.animateTo(1f, tween(CHANGE_MS, easing = FastOutSlowInEasing))
        delay(PAUSE_AFTER_MS)
        onFinished()
    }

    HomeScreen(title = room.title, overline = mission.doneTitle) {
        if (art != null) {
            RoomSceneView(
                art = art,
                selected = mission.kind,
                isDone = isDone,
                change = change.value,
                modifier = Modifier.weight(1f)
            )
        }
        WallDots(count = room.missions.size, selected = room.missions.indexOf(mission))
        WallCard(MenuItem(mission.wallTitle, subtitleOf(mission), done = true), ringProgress = 0f, ringColor = HomeColors.Accent)
    }
}

// сначала видно комнату как была, потом предмет меняется, потом есть время рассмотреть
private const val PAUSE_BEFORE_MS = 500L
private const val CHANGE_MS = 1400
private const val PAUSE_AFTER_MS = 1300L
