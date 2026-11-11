package com.robotpaw.myhome.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.formatTime
import com.robotpaw.myhome.game.history.DayStats
import com.robotpaw.myhome.game.history.History
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType
import java.time.LocalDate
import kotlin.math.roundToInt

private val WEEKDAYS = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")

/**
 * Мой прогресс: как меняется качество управления по дням
 * Главный показатель - чистота сигнала и встречные сигналы (так прогресс оценивают и в «Моторике»)
 * @param history - история тренировок по дням
 * @param today - сегодняшняя дата «2026-11-11»
 */
@Composable
fun ProgressScreen(
    source: SignalSource,
    profile: SignalProfile,
    progress: GameProgress,
    history: History,
    today: String,
    onBack: () -> Unit
) {
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = emptyList(),
        backTitle = "В меню",
        onBack = onBack,
        onSelect = {}
    )
    val week = history.lastDays(today)
    val todayStats = week.last()
    val change = history.cleanChange()

    HomeScreen(
        title = "Мой прогресс",
        overline = "Качество управления",
        backProgress = control.backProgress
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (history.totalTasks == 0) {
                HomeCard {
                    Text("Пока пусто", fontSize = HomeType.Heading, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
                    Text(
                        "Пройдите первое задание - здесь появятся чистота сигнала и встречные сигналы по дням.",
                        fontSize = HomeType.Label, lineHeight = HomeType.LabelLine, color = HomeColors.InkSecondary
                    )
                }
            } else {
                HomeCard {
                    Text("Чистота сигнала", fontSize = HomeType.Body, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
                    Text(
                        listOfNotNull(
                            if (todayStats.tasks > 0) "сегодня ${todayStats.clean}%" else null,
                            change?.let { if (it >= 0) "с начала +$it%" else "с начала $it%" }
                        ).joinToString(" · ").ifEmpty { "сколько времени работала только нужная мышца" },
                        fontSize = HomeType.Label, color = HomeColors.InkSecondary
                    )
                    WeekBars(week, HomeColors.Accent, max = 100f, value = { it.clean.toFloat() }, label = { "${it.clean}" })
                }
                HomeCard {
                    Text("Встречные сигналы", fontSize = HomeType.Body, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
                    Text("в среднем на задание, чем ниже - тем лучше", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
                    val max = week.maxOf { it.coPerTask }.coerceAtLeast(1f)
                    WeekBars(week, HomeColors.Extensor, max = max, value = { it.coPerTask }, label = { "%.1f".format(it.coPerTask) })
                }
            }
            HomeCard {
                ProgressRow("Дней подряд", history.streak(today).toString())
                ProgressRow("Заданий всего", history.totalTasks.toString())
                val weekTasks = week.filter { it.steadyTasks > 0 }
                if (weekTasks.isNotEmpty()) {
                    ProgressRow("Ровность за неделю", "${weekTasks.sumOf { it.steadySum } / weekTasks.sumOf { it.steadyTasks }}%")
                }
                ProgressRow("Время за неделю", formatTime(week.sumOf { it.activeMs }))
                ProgressRow("Звёзд", progress.totalStars.toString())
            }
        }
        MenuList(control)
    }
}

/**
 * Столбики за неделю: дни без тренировок - пустые, под столбиком день недели
 * @param value - высота столбика
 * @param label - подпись над столбиком
 */
@Composable
private fun WeekBars(
    week: List<DayStats>,
    color: Color,
    max: Float,
    value: (DayStats) -> Float,
    label: (DayStats) -> String
) {
    Row(
        Modifier.fillMaxWidth().height(110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        week.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                if (day.tasks > 0) Text(label(day), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
                Box(Modifier.width(22.dp).height(70.dp), contentAlignment = Alignment.BottomCenter) {
                    val fraction = if (day.tasks > 0) (value(day) / max).coerceIn(0.04f, 1f) else 0f
                    Box(
                        Modifier
                            .width(22.dp)
                            .fillMaxHeight(fraction.takeIf { it > 0f } ?: 0.02f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (day.tasks > 0) color else HomeColors.Divider)
                    )
                }
                Text(weekday(day.day), fontSize = 11.sp, color = HomeColors.InkSecondary)
            }
        }
    }
}

private fun weekday(day: String): String = WEEKDAYS[LocalDate.parse(day).dayOfWeek.value - 1]

@Composable
private fun ProgressRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // подпись занимает оставшееся место, значение всегда в одну строку
        Text(label, fontSize = HomeType.Label, color = HomeColors.InkSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = HomeType.Body, fontWeight = FontWeight.Bold, color = HomeColors.Ink, maxLines = 1, softWrap = false)
    }
}