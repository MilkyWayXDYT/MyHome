package com.robotpaw.myhome.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.robotpaw.myhome.game.PetKind
import com.robotpaw.myhome.game.Room
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.game.quests.RealQuests
import com.robotpaw.myhome.game.session.Fatigue
import com.robotpaw.myhome.game.session.Session
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
 * Куда можно сразу перейти из отладочного меню
 */
enum class DebugJump(val title: String) {
    MAIN_MENU("Меню"),
    WELCOME_BACK("Разминка"),
    CALIBRATION("Калибровка"),
    REST("Отдых"),
    GOALS("Цели"),
    SIGNAL_CHECK("Сигнал"),
    BREAK("Передышка"),
    SESSION_END("Итог дня"),
    CHORES("Дела")
}

private enum class DebugTab(val title: String) { ROOMS("Комнаты"), CHEATS("Читы"), SCREENS("Экраны") }

/**
 * Отладочное меню: быстрый переход между комнатами и заданиями и читы, чтобы не проходить игру с начала
 * Показывать только в debug-сборке
 * @param progress - текущий прогресс (для отметок «пройдено» и звёзд)
 * @param onProgressChange - новый прогресс после чита
 * @param session - тренировка за день (план и усталость)
 * @param onSessionChange - новая сессия после чита
 * @param onNewGoals - подобрать цели на сегодня заново
 * @param onNewDay - начать день заново: план, цели, квест и дела с нуля
 * @param onNewChores - подобрать дела на сегодня заново (после сюжета)
 * @param onJumpToMission - открыть задание сразу, минуя меню и инструкцию
 * @param onWinMission - засчитать задание без игры: смена предмета и экран результата, как после настоящего прохождения
 * @param onJumpToRoom - открыть комнату; предыдущие комнаты при этом считаются пройденными
 * @param onJump - открыть экран
 * @param onResetCalibration - забыть сохранённую калибровку и пройти её заново
 */
@Composable
fun LevelSwitcher(
    progress: GameProgress,
    onProgressChange: (GameProgress) -> Unit,
    session: Session,
    onSessionChange: (Session) -> Unit,
    onNewGoals: () -> Unit,
    onNewDay: () -> Unit,
    onNewChores: () -> Unit,
    onJumpToMission: (Mission) -> Unit,
    onWinMission: (Mission) -> Unit,
    onJumpToRoom: (Room) -> Unit,
    onJump: (DebugJump) -> Unit,
    onResetCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(DebugTab.ROOMS) }

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
            Text("🛠", fontSize = 18.sp, color = HomeColors.Ink)
        }
        return
    }

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
                Text("Отладка · ★ ${progress.totalStars}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { expanded = false }) { Text("Свернуть") }
            }

            Row(Modifier.fillMaxWidth()) {
                DebugTab.entries.forEach { t ->
                    TextButton(onClick = { tab = t }, modifier = Modifier.weight(1f)) {
                        Text(
                            t.title,
                            fontWeight = if (t == tab) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (t == tab) HomeColors.Ink else HomeColors.InkSecondary
                        )
                    }
                }
            }

            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            when (tab) {
                DebugTab.ROOMS -> RoomsTab(
                    progress = progress,
                    onProgressChange = onProgressChange,
                    onJumpToMission = { expanded = false; onJumpToMission(it) },
                    onWinMission = { expanded = false; onWinMission(it) },
                    onJumpToRoom = { expanded = false; onJumpToRoom(it) }
                )
                DebugTab.CHEATS -> CheatsTab(
                    progress = progress,
                    onProgressChange = onProgressChange,
                    session = session,
                    onSessionChange = onSessionChange,
                    onNewGoals = onNewGoals,
                    onNewDay = onNewDay,
                    onNewChores = onNewChores,
                    onResetCalibration = { expanded = false; onResetCalibration() }
                )
                DebugTab.SCREENS -> DebugJump.entries.chunked(3).forEach { row ->
                    ButtonRow(row.map { jump -> jump.title to { expanded = false; onJump(jump) } })
                }
            }
        }
    }
}

/**
 * Комнаты и задания: ▶ играть, ✓ выполнить без игры (с анимацией и результатом),
 * ★ звёзды по кругу 0..3, ○/● отметка «выполнено» без анимации
 */
@Composable
private fun RoomsTab(
    progress: GameProgress,
    onProgressChange: (GameProgress) -> Unit,
    onJumpToMission: (Mission) -> Unit,
    onWinMission: (Mission) -> Unit,
    onJumpToRoom: (Room) -> Unit
) {
    Text("▶ играть · ✓ пройти сразу · ★ звёзды · ● отметка", fontSize = 12.sp, color = HomeColors.InkSecondary)
    Rooms.ALL.forEach { room ->
        Spacer(Modifier.height(12.dp))
        val summary = progress.summaryOf(room)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${room.title} · ${progress.completedCount(room)}/${room.requiredMissions.size} · ★${summary.stars}/${summary.maxStars}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onProgressChange(progress.withRoomReset(room)) }) { Text("Сброс") }
            TextButton(onClick = { onJumpToRoom(room) }) { Text("В комнату") }
        }

        room.missions.forEach { mission ->
            val done = progress.isCompleted(mission)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val extra = if (mission.optional) " (доп.)" else ""
                Text(
                    "${mission.title}$extra",
                    fontSize = 14.sp,
                    color = if (done) HomeColors.Accent else HomeColors.Ink,
                    modifier = Modifier.weight(1f)
                )
                SmallButton("▶") { onJumpToMission(mission) }
                SmallButton("✓") { onWinMission(mission) }
                SmallButton("★${progress.starsOf(mission)}") { onProgressChange(progress.withNextStarsOf(mission)) }
                SmallButton(if (done) "●" else "○") { onProgressChange(progress.withMissionDone(mission, !done)) }
            }
        }
    }
}

@Composable
private fun CheatsTab(
    progress: GameProgress,
    onProgressChange: (GameProgress) -> Unit,
    session: Session,
    onSessionChange: (Session) -> Unit,
    onNewGoals: () -> Unit,
    onNewDay: () -> Unit,
    onNewChores: () -> Unit,
    onResetCalibration: () -> Unit
) {
    Section("Прогресс")
    ButtonRow(
        listOf(
            "Открыть все" to { onProgressChange(progress.withAllCompleted()) },
            "Сбросить всё" to { onProgressChange(GameProgress()) }
        )
    )

    Section("Звёзды · сейчас ${progress.totalStars}")
    ButtonRow(
        listOf(
            "Все на ★★★" to { onProgressChange(progress.withAllStars()) },
            "Убрать звёзды" to { onProgressChange(progress.withoutStars()) }
        )
    )

    Section("Питомец · ${progress.pet?.title ?: "не выбран"}")
    ButtonRow(
        PetKind.entries.map { pet -> pet.title to { onProgressChange(progress.withPet(pet)) } } +
                listOf("Нет" to { onProgressChange(progress.withoutPet()) })
    )

    Section("Сегодня ${session.done} из ${session.plan} · усталость: ${session.fatigue.title}")
    ButtonRow((1..Session.MAX_PLAN).map { n -> "План $n" to { onSessionChange(session.withPlan(n)) } })
    // усталость: нет / лёгкая / сильная, держится до ближайшего отдыха
    ButtonRow(Fatigue.entries.map { f -> f.title to { onSessionChange(session.copy(forcedFatigue = f)) } })
    ButtonRow(
        listOf(
            "Новый день" to onNewDay,
            "+1 задание" to { onSessionChange(session.copy(done = session.done + 1)) }
        )
    )

    Section("Цели · выполнено ${session.goalsDone} из ${session.goals.size}")
    session.goals.forEach { goal ->
        val mark = if (goal.done) "✓ " else ""
        val extra = if (goal.challenge) " (челлендж)" else ""
        Text("$mark${goal.kind.title}$extra: ${goal.condition}", fontSize = 13.sp, color = HomeColors.InkSecondary)
    }
    ButtonRow(
        listOf(
            "Новые цели" to onNewGoals,
            "Выполнить все" to { onSessionChange(session.copy(goals = session.goals.map { it.copy(done = true) })) }
        )
    )

    Section("Дела · ${if (progress.isStoryDone) "сделано ${session.choresDone.size} из ${session.chores.size}" else "после сюжета"}")
    // дела подбираются только после сюжета: сначала «Открыть все»
    ButtonRow(
        listOf(
            "Новые дела" to onNewChores,
            "Все сделаны" to {
                onSessionChange(session.copy(choresDone = session.chores.toSet()))
                onProgressChange(progress.withChores(emptyList()))
            }
        )
    )

    Section("Квест в жизни · ${session.quest?.title ?: "нет"}${if (session.questDone) " ✓" else ""}")
    ButtonRow(
        listOf(
            "Другой квест" to { onSessionChange(session.withQuest(RealQuests.pick(progress, except = session.quest))) },
            "Забыть ответы" to { onProgressChange(progress.copy(reflections = emptyMap())) }
        )
    )

    Section("Калибровка")
    ButtonRow(listOf("Забыть калибровку" to onResetCalibration))
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(12.dp))
    Text(title, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ButtonRow(buttons: List<Pair<String, () -> Unit>>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { (title, onClick) ->
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(title, fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit) {
    // обычная кнопка шире 58dp, четыре в строку не помещаются
    Box(
        Modifier
            .size(width = 42.dp, height = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, color = HomeColors.Ink)
    }
}