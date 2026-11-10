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
import com.robotpaw.myhome.ui.components.HomeScreen
import kotlinx.coroutines.delay

/**
 * Короткая сцена после задания: в комнате предмет меняется на «после», потом сам открывается экран результата
 * Нажимать ничего не нужно
 * @param isDone - выполнены ли остальные задания комнаты (их предметы показываются как есть)
 */
@Composable
fun SceneChangeScreen(
    mission: Mission,
    roomTitle: String,
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

    HomeScreen(title = mission.doneTitle, overline = roomTitle) {
        if (art != null) {
            RoomSceneView(
                art = art,
                selected = mission.kind,
                isDone = isDone,
                change = change.value,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// сначала видно комнату как была, потом предмет меняется, потом есть время рассмотреть
private const val PAUSE_BEFORE_MS = 500L
private const val CHANGE_MS = 1400
private const val PAUSE_AFTER_MS = 1300L
