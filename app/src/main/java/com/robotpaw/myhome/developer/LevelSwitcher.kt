package com.robotpaw.myhome.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.Room
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.ui.theme.HomeColors

/**
 * Проходит все обязательные задания комнат, которые идут до этой комнаты (чтобы она открылась)
 */
fun GameProgress.withCompletedBefore(room: Room): GameProgress {
    val index = Rooms.ALL.indexOf(room)
    var result = this
    Rooms.ALL.take(index.coerceAtLeast(0)).forEach { previous ->
        previous.requiredMissions.forEach { result = result.withCompleted(it) }
    }
    return result
}

/**
 * Проходит все обязательные задания во всех комнатах
 */
fun GameProgress.withAllCompleted(): GameProgress {
    var result = this
    Rooms.ALL.forEach { room ->
        room.requiredMissions.forEach { result = result.withCompleted(it) }
    }
    return result
}

/**
 * Панель разработчика для быстрого перехода между комнатами и заданиями, чтобы не проходить игру с начала
 * Показывать только в debug-сборке
 * @param progress - текущий прогресс (для отметок «пройдено»)
 * @param onJumpToMission - открыть задание сразу, минуя меню и инструкцию
 * @param onJumpToRoom - открыть комнату; предыдущие комнаты при этом считаются пройденными
 * @param onUnlockAll - считать все комнаты пройденными
 * @param onReset - сбросить прогресс
 */
@Composable
fun LevelSwitcher(
    progress: GameProgress,
    onJumpToMission: (Mission) -> Unit,
    onJumpToRoom: (Room) -> Unit,
    onUnlockAll: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    if (!expanded) {
        // та же круглая кнопка, что и ⚙ у панелей эмулятора
        Box(
            modifier = modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HomeColors.Card.copy(alpha = 0.9f))
                .border(1.dp, HomeColors.Divider, CircleShape)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Text("⏭", fontSize = 18.sp, color = HomeColors.Ink)
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Уровни (debug)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { expanded = false }) { Text("Свернуть") }
                }

                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onUnlockAll, modifier = Modifier.weight(1f)) {
                        Text("Открыть все")
                    }
                    OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                        Text("Сбросить")
                    }
                }

                Rooms.ALL.forEach { room ->
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${room.title} · ${progress.completedCount(room)}/${room.requiredMissions.size}",
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = {
                            expanded = false
                            onJumpToRoom(room)
                        }) { Text("В комнату") }
                    }

                    room.missions.forEach { mission ->
                        TextButton(
                            onClick = {
                                expanded = false
                                onJumpToMission(mission)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val mark = if (progress.isCompleted(mission)) "✓ " else ""
                            val extra = if (mission.optional) " (доп.)" else ""
                            Text("$mark${mission.title}$extra", modifier = Modifier.weight(1f))
                            Text(mission.difficulty.label, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
