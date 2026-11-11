package com.robotpaw.myhome.mission

import com.robotpaw.myhome.sound.Sounds
import com.robotpaw.myhome.sound.Sfx
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.game.Stars
import com.robotpaw.myhome.game.feedback.SurveyQuestion
import com.robotpaw.myhome.game.formatTime
import com.robotpaw.myhome.game.session.Fatigue
import com.robotpaw.myhome.game.session.Session
import com.robotpaw.myhome.menu.MenuItem
import com.robotpaw.myhome.menu.MenuList
import com.robotpaw.myhome.menu.rememberMenuControl
import com.robotpaw.myhome.mission.mechanics.core.rememberMechanicInput
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.components.ProgressStrip
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

// код «обе мышцы в покое»
private const val RELAXED = "00"

/**
 * Передышка между заданиями. Длина зависит от усталости, таймер идёт только пока рука расслаблена
 * Пропустить можно всегда: лимит мягкий
 * @param session - по ней видно, почему отдых и сколько отдыхать
 * @param onDone - отдых закончен или пропущен
 */
@Composable
fun BreakScreen(
    source: SignalSource,
    profile: SignalProfile,
    session: Session,
    onDone: () -> Unit
) {
    val totalMs = session.fatigue.restSeconds * 1000L
    LaunchedEffect(Unit) { Sounds.play(Sfx.STOP) }
    var restedMs by remember { mutableLongStateOf(0L) }
    var relaxed by remember { mutableStateOf(false) }

    rememberMechanicInput(source, profile) { state, dt ->
        relaxed = state.code == RELAXED
        if (relaxed && restedMs < totalMs) restedMs = (restedMs + dt).coerceAtMost(totalMs)
    }
    val finished = restedMs >= totalMs

    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(if (finished) MenuItem("Продолжить") else MenuItem("Пропустить отдых", "можно, но руке лучше отдохнуть")),
        onSelect = { onDone() }
    )

    val signs = session.signs
    HomeScreen(
        title = "Передышка",
        overline = "Рука отдыхает",
        subtitle = if (signs.isEmpty()) "Короткий отдых между заданиями." else "Похоже, рука устаёт: ${signs.joinToString(", ") { it.text }}."
    ) {
        HomeCard {
            Text(
                clock(((totalMs - restedMs) + 999) / 1000),
                fontSize = HomeType.Display, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink
            )
            ProgressStrip(restedMs.toFloat() / totalMs, HomeColors.Accent, height = 16.dp)
            Text(
                when {
                    finished -> "Отдохнули, можно продолжать"
                    relaxed -> "Рука расслаблена, таймер идёт"
                    else -> "Таймер идёт, пока рука расслаблена"
                },
                fontSize = HomeType.Label, fontWeight = FontWeight.Bold,
                color = if (relaxed || finished) HomeColors.Accent else HomeColors.InkSecondary
            )
        }
        HomeCard {
            Text("Опустите руку и расслабьте плечо", fontSize = HomeType.Body, color = HomeColors.Ink)
            Text("Сделайте пару медленных вдохов", fontSize = HomeType.Body, color = HomeColors.Ink)
        }
        Spacer(Modifier.weight(1f))
        MenuList(control)
    }
}

/**
 * План на день выполнен или рука устала: предлагаем закончить, но можно взять ещё задание
 * Раз в день перед итогами - мини-опрос из трёх вопросов (можно пропустить)
 * @param surveyNeeded - сегодня опрос ещё не проходили
 * @param onSurvey - ответы опроса (номера ответов по порядку вопросов)
 * @param onFinish - закончить на сегодня
 * @param onMore - ещё одно задание
 * @param onShare - поделиться отчётом о тренировках
 */
@Composable
fun SessionEndScreen(
    source: SignalSource,
    profile: SignalProfile,
    session: Session,
    onFinish: () -> Unit,
    onMore: () -> Unit,
    surveyNeeded: Boolean = false,
    onSurvey: (List<Int>) -> Unit = {},
    onShare: () -> Unit = {}
) {
    // номер вопроса опроса, null - опрос закончен или не нужен
    var question by remember { mutableStateOf(if (surveyNeeded) 0 else null) }
    var answers by remember { mutableStateOf(emptyList<Int>()) }

    val current = question?.let { SurveyQuestion.entries.getOrNull(it) }
    if (current != null) {
        SurveyStep(source, profile, current, number = question!! + 1) { answer ->
            if (answer == null) {
                question = null   // пропустить опрос
            } else {
                answers = answers + answer
                val next = question!! + 1
                if (next >= SurveyQuestion.entries.size) {
                    onSurvey(answers)
                    question = null
                } else {
                    question = next
                }
            }
        }
        return
    }

    val tired = session.fatigue == Fatigue.TIRED
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = listOf(
            MenuItem("Закончить на сегодня", "в главное меню"),
            MenuItem("Ещё одно задание", if (session.fatigue != Fatigue.NONE) "сначала передышка" else null),
            MenuItem("Поделиться отчётом", "отправить команде, как прошли тренировки")
        ),
        onSelect = {
            when (it) {
                0 -> onFinish()
                1 -> onMore()
                else -> onShare()
            }
        }
    )

    HomeScreen(
        title = if (tired) "Рука устала" else "План выполнен",
        overline = "Тренировка на сегодня",
        subtitle = if (tired) "Лучше продолжить завтра: отдых тоже часть тренировки."
        else "Отличная работа! Можно закончить или сделать ещё одно задание."
    ) {
        HomeCard {
            SessionRow("Заданий сегодня", "${session.done} из ${session.plan}")
            SessionRow("Звёзд за день", "+${session.starsGained} ${Stars.word(session.starsGained)}")
            if (session.activeMs > 0) SessionRow("Время в заданиях", formatTime(session.activeMs))
            val signs = session.signs
            if (signs.isNotEmpty()) {
                Text("Заметили: ${signs.joinToString(", ") { it.text }}", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
            }
        }
        Spacer(Modifier.weight(1f))
        MenuList(control)
    }
}

/**
 * Вопрос опроса: ответы - пункты меню, последний пункт пропускает опрос
 * @param onAnswer - номер ответа, null - пропустить опрос
 */
@Composable
private fun SurveyStep(
    source: SignalSource,
    profile: SignalProfile,
    question: SurveyQuestion,
    number: Int,
    onAnswer: (Int?) -> Unit
) {
    // новый вопрос - новое меню, выбор начинается с первого ответа
    key(question) {
        val control = rememberMenuControl(
            source = source,
            profile = profile,
            items = question.options.map { MenuItem(it) } + MenuItem("Пропустить опрос"),
            onSelect = { onAnswer(if (it < question.options.size) it else null) }
        )
        HomeScreen(
            title = question.text,
            overline = "Короткий опрос · $number из ${SurveyQuestion.entries.size}",
            subtitle = "Ответы помогут сделать тренировки лучше."
        ) {
            MenuList(control)
        }
    }
}

// «1:30», «0:45»
private fun clock(seconds: Long) = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

@Composable
private fun SessionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = HomeType.Label, color = HomeColors.InkSecondary)
        Text(value, fontSize = HomeType.Body, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
    }
}