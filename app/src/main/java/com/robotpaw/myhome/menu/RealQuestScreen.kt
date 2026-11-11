package com.robotpaw.myhome.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.Stars
import com.robotpaw.myhome.game.quests.QuestFeeling
import com.robotpaw.myhome.game.quests.RealQuest
import com.robotpaw.myhome.game.quests.RealQuests
import com.robotpaw.myhome.game.quests.Reflection
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

private enum class QuestPhase { INTRO, FEELING, THANKS }

/**
 * Квест в жизни и рефлексия после него: что сделать, «Как получилось?», совет
 * Ответ выбирается тем же сигналом, что и меню
 * @param practice - похожее задание игры, где потренироваться (null - нет открытого)
 * @param onAnswered - игрок ответил, звезда за ответ
 * @param onPractice - пойти тренироваться в задание
 * @param onBack - вернуться к целям
 */
@Composable
fun RealQuestScreen(
    source: SignalSource,
    profile: SignalProfile,
    quest: RealQuest,
    practice: Mission?,
    onAnswered: (Reflection) -> Unit,
    onPractice: (Mission) -> Unit,
    onBack: () -> Unit
) {
    var phase by remember { mutableStateOf(QuestPhase.INTRO) }
    var reflection by remember { mutableStateOf(Reflection(QuestFeeling.EASY)) }

    when (phase) {
        QuestPhase.INTRO -> {
            val control = rememberMenuControl(
                source = source,
                profile = profile,
                items = listOf(MenuItem("Сделал(а), рассказать", "+${RealQuests.REWARD} ★ за ответ")),
                backTitle = "Сделаю позже",
                onBack = onBack,
                onSelect = { phase = QuestPhase.FEELING }
            )
            HomeScreen(
                title = quest.title,
                overline = "Квест в жизни",
                subtitle = "Навык: ${quest.skill.lowercase()} · как в задании «${quest.like}»",
                backProgress = control.backProgress
            ) {
                HomeCard {
                    quest.steps.forEachIndexed { i, line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${i + 1}", fontSize = HomeType.Body, fontWeight = FontWeight.ExtraBold, color = HomeColors.Accent)
                            Text(line, fontSize = HomeType.Body, color = HomeColors.Ink)
                        }
                    }
                }
                Text(RealQuest.SAFETY, fontSize = HomeType.Label, lineHeight = HomeType.LabelLine, color = HomeColors.InkSecondary)
                MenuList(control)
            }
        }

        QuestPhase.FEELING -> {
            val control = rememberMenuControl(
                source = source,
                profile = profile,
                items = QuestFeeling.entries.map { MenuItem(it.title) },
                onSelect = {
                    reflection = Reflection(QuestFeeling.entries[it])
                    onAnswered(reflection)
                    phase = QuestPhase.THANKS
                }
            )
            HomeScreen(title = "Как получилось?", overline = quest.title, subtitle = "Любой ответ честный и полезный.") {
                MenuList(control)
            }
        }

        QuestPhase.THANKS -> {
            val suggest = if (reflection.needsPractice) practice else null
            val control = rememberMenuControl(
                source = source,
                profile = profile,
                items = listOfNotNull(
                    suggest?.let { MenuItem("Потренироваться", it.title) },
                    MenuItem("К целям")
                ),
                onSelect = { if (suggest != null && it == 0) onPractice(suggest) else onBack() }
            )
            HomeScreen(
                title = "Спасибо!",
                overline = quest.title,
                subtitle = when (reflection.feeling) {
                    QuestFeeling.EASY -> "Отлично: навык из игры работает и дома."
                    QuestFeeling.HARD -> "Получилось - это главное. С каждым разом будет проще."
                    QuestFeeling.NOT_YET -> "Это нормально. Завтра в целях будет этот навык, а квест можно повторить в другой день."
                }
            ) {
                HomeCard {
                    Text(
                        "+${RealQuests.REWARD} ${Stars.word(RealQuests.REWARD)} за ответ",
                        fontSize = HomeType.Body, fontWeight = FontWeight.ExtraBold, color = HomeColors.Accent
                    )
                    if (suggest != null) {
                        Text(
                            "${quest.skill} тренируется в задании «${suggest.title}».",
                            fontSize = HomeType.Label, lineHeight = HomeType.LabelLine, color = HomeColors.InkSecondary
                        )
                    }
                }
                MenuList(control)
            }
        }
    }
}