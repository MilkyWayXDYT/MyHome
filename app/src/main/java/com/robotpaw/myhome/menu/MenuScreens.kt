package com.robotpaw.myhome.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.Room
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.game.chores.Chores
import com.robotpaw.myhome.game.goals.DailyGoal
import com.robotpaw.myhome.game.quests.RealQuests
import com.robotpaw.myhome.game.session.Session
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.signal.UserLevel
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.components.MenuItemCard
import com.robotpaw.myhome.ui.components.StarsBadge

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

enum class MainMenuChoice { CONTINUE, ROOMS, GOALS, PROGRESS, SIGNAL_CHECK, RECALIBRATE, GALLERY }

/**
 * Главное меню
 * @param session - тренировка за день, для строки «выполнено 1 из 3» у целей
 * @param showDeveloperItems - пункт «Витрина компонентов» (только debug)
 */
@Composable
fun MainMenuScreen(
    source: SignalSource,
    profile: SignalProfile,
    progress: GameProgress,
    session: Session? = null,
    showDeveloperItems: Boolean = false,
    onChoice: (MainMenuChoice) -> Unit
) {
    val room = progress.currentRoom()
    val unlocked = Rooms.ALL.count { progress.isUnlocked(it) }
    val levelText = if (profile.level == UserLevel.NOVICE) "новичок" else "опытный"
    val roomStars = progress.summaryOf(room)
    // после сюжета вместо «Продолжить» - дела на сегодня
    val continueItem = if (progress.isStoryDone) {
        MenuItem(
            "Дела на сегодня",
            when {
                session == null || session.chores.isEmpty() -> "новые дела по дому"
                session.isChoresDone -> "всё сделано · ${session.chores.size} из ${session.chores.size}"
                else -> "сделано ${session.choresDone.size} из ${session.chores.size}"
            },
            done = session?.isChoresDone == true
        )
    } else {
        MenuItem("Продолжить тренировку", "${room.title} · ${progress.completedCount(room)} из ${room.requiredMissions.size} · ★ ${roomStars.stars} из ${roomStars.maxStars}")
    }

    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(
            continueItem,
            MenuItem("Все комнаты", "открыто $unlocked из ${Rooms.ALL.size}"),
            MenuItem(
                "Цели на сегодня",
                when {
                    session == null || session.goals.isEmpty() -> "что даёт звёзды"
                    session.isGoalsDone -> "всё выполнено · ${session.goalsDone} из ${session.goals.size}"
                    else -> "выполнено ${session.goalsDone} из ${session.goals.size}"
                },
                done = session?.isGoalsDone == true
            ),
            MenuItem("Мой прогресс", "чистота сигнала по дням"),
            MenuItem("Проверка сигнала", "код состояния и команды меню"),
            MenuItem("Калибровка заново", "сейчас режим: $levelText")
        ) + if (showDeveloperItems) listOf(MenuItem("Витрина компонентов", "только в debug-сборке")) else emptyList(),
        onSelect = { onChoice(MainMenuChoice.entries[it]) }
    )

    HomeScreen(
        title = "Мой дом",
        overline = "Добрый день!",
        trailing = { StarsBadge(progress.totalStars) }
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
            val summary = progress.summaryOf(room)
            val stars = "★ ${summary.stars} из ${summary.maxStars}"
            val subtitle = when {
                !progress.isUnlocked(room) -> "закрыто, сначала: ${rooms[i - 1].title}"
                progress.isDone(room) -> "пройдено · $stars"
                else -> "${progress.completedCount(room)} из ${room.requiredMissions.size} · $stars"
            }
            MenuItem(room.title, subtitle, done = progress.isDone(room))
        },
        backTitle = "В меню",
        onBack = onBack,
        onSelect = { if (progress.isUnlocked(rooms[it])) onOpen(rooms[it]) }
    )

    HomeScreen(
        title = "Комнаты",
        backProgress = control.backProgress,
        trailing = { StarsBadge(progress.totalStars) }
    ) {
        MenuList(control)
    }
}

/**
 * Цели на сегодня: 1-3 миссии из открытых комнат. Выбор цели ведёт к подходящему заданию
 * @param session - цели и план на сегодня
 * @param onOpen - перейти к заданию для цели
 * @param onOpenQuest - открыть квест в жизни (последний пункт перед «Назад»)
 */
@Composable
fun GoalsScreen(
    source: SignalSource,
    profile: SignalProfile,
    session: Session,
    onOpen: (DailyGoal) -> Unit,
    onOpenQuest: () -> Unit = {},
    onBack: () -> Unit
) {
    val goals = session.goals
    val quest = session.quest
    val questItem = quest?.let {
        MenuItem(
            title = "В жизни: ${it.title}",
            subtitle = if (session.questDone) "рассказано · +${RealQuests.REWARD} ★"
            else "сделайте по-настоящему и расскажите, как вышло · +${RealQuests.REWARD} ★",
            done = session.questDone
        )
    }
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = goals.map { goal ->
            val note = when {
                goal.done -> "выполнено · +${goal.reward} ★"
                session.isPostponed(goal) -> "отложено: рука устала"
                else -> "+${goal.reward} ★"
            }
            MenuItem(
                title = if (goal.challenge) "${goal.kind.title} · челлендж" else goal.kind.title,
                subtitle = listOfNotNull(goal.condition, goal.kind.where, note).joinToString("\n"),
                done = goal.done
            )
        } + listOfNotNull(questItem),
        backTitle = "В меню",
        onBack = onBack,
        onSelect = { index ->
            val goal = goals.getOrNull(index)
            when {
                goal == null -> if (!session.questDone) onOpenQuest()
                !goal.done && !session.isPostponed(goal) -> onOpen(goal)
            }
        }
    )

    HomeScreen(
        title = "Цели на сегодня",
        overline = "Подобрано для вас",
        subtitle = if (goals.isEmpty()) "Целей пока нет: откройте первую комнату."
        else "Выполнено ${session.goalsDone} из ${goals.size} · заданий сегодня ${session.done} из ${session.plan}",
        backProgress = control.backProgress
    ) {
        MenuList(control)
    }
}

/**
 * Дела на сегодня после сюжета: выбор дела ведёт в комнату, к его предмету
 * @param chores - задания-дела на сегодня
 * @param onOpen - перейти к делу
 */
@Composable
fun ChoresScreen(
    source: SignalSource,
    profile: SignalProfile,
    session: Session,
    chores: List<Mission>,
    onOpen: (Mission) -> Unit,
    onBack: () -> Unit
) {
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = chores.map { mission ->
            val done = mission.id in session.choresDone
            MenuItem(
                mission.title,
                "${Rooms.of(mission).title} · ${mission.wallTitle}" + if (done) " · сделано" else " · +${Chores.REWARD} ★",
                done = done
            )
        },
        backTitle = "В меню",
        onBack = onBack,
        onSelect = { index -> if (chores[index].id !in session.choresDone) onOpen(chores[index]) }
    )

    HomeScreen(
        title = "Дела на сегодня",
        overline = "Дом снова ждёт",
        subtitle = if (session.isChoresDone) "Все дела сделаны. Завтра будут новые."
        else "Сделано ${session.choresDone.size} из ${chores.size}. Это задания, которые даются хуже всего.",
        backProgress = control.backProgress
    ) {
        MenuList(control)
    }
}