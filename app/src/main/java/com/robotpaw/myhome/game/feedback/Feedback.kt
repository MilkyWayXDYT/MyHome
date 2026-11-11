package com.robotpaw.myhome.game.feedback

import com.robotpaw.myhome.game.GameProgress
import com.robotpaw.myhome.game.Rooms
import com.robotpaw.myhome.game.formatTime
import com.robotpaw.myhome.game.history.History
import com.robotpaw.myhome.game.session.Session

/**
 * Вопрос мини-опроса после тренировки, ответ выбирается сигналом как пункт меню
 * @param short - коротко для отчёта
 * @param options - ответы, от лучшего к худшему
 */
enum class SurveyQuestion(val text: String, val short: String, val options: List<String>) {
    CLARITY("Понятно, что делать?", "понятно", listOf("Да, всё понятно", "Иногда путаюсь", "Сложно понять")),
    FATIGUE("Как рука после тренировки?", "рука", listOf("Не устала", "Немного устала", "Сильно устала")),
    FUN("Интересно играть?", "интересно", listOf("Да", "Нормально", "Скучно"))
}

/**
 * Ответы опроса за день
 * @param answers - номер ответа на каждый вопрос SurveyQuestion по порядку
 */
data class SurveyAnswer(val day: String, val answers: List<Int>) {
    // «понятно - да, всё понятно; рука - немного устала; интересно - да»
    val text: String
        get() = SurveyQuestion.entries.mapIndexedNotNull { i, q ->
            answers.getOrNull(i)?.let { q.options.getOrNull(it) }?.let { "${q.short} - ${it.lowercase()}" }
        }.joinToString("; ")
}

/**
 * Отчёт о тренировках для команды: игрок отправляет его через «Поделиться» после теста
 * Субъективное (опрос, квесты в жизни) и объективное (чистота сигнала, встречные сигналы, усталость) вместе
 */
object Report {
    const val SURVEY_DAYS = 7

    /**
     * @param levelText - режим управления: «новичок» или «опытный»
     */
    fun build(
        today: String,
        levelText: String,
        progress: GameProgress,
        session: Session,
        history: History,
        surveys: List<SurveyAnswer>
    ): String = buildString {
        appendLine("«Мой дом» - отчёт о тренировках")
        appendLine("Дата: $today")
        appendLine("Режим управления: $levelText")
        val roomsDone = Rooms.ALL.count { progress.isDone(it) }
        appendLine("Комнат пройдено: $roomsDone из ${Rooms.ALL.size}, звёзд: ${progress.totalStars}")
        appendLine()

        appendLine("Опрос")
        val lastSurveys = surveys.takeLast(SURVEY_DAYS)
        if (lastSurveys.isEmpty()) appendLine("ответов пока нет")
        lastSurveys.forEach { appendLine("${it.day}: ${it.text}") }
        appendLine()

        appendLine("Последние 7 дней")
        history.lastDays(today).filter { it.tasks > 0 }.ifEmpty { null }?.forEach { d ->
            appendLine(
                "${d.day}: заданий ${d.tasks}, чистота ${d.clean}%, встречных на задание ${"%.1f".format(d.coPerTask)}, " +
                        "ошибок ${d.mistakes}, ровность ${d.steady}%, время ${formatTime(d.activeMs)}"
            )
        } ?: appendLine("тренировок не было")
        history.cleanChange()?.let { appendLine("Чистота сигнала с начала: ${if (it >= 0) "+" else ""}$it%") }
        appendLine("Дней подряд: ${history.streak(today)}")
        appendLine()

        appendLine("Сегодня")
        appendLine("Заданий: ${session.done} из ${session.plan}, звёзд за день: ${session.starsGained}")
        val signs = session.signs
        appendLine("Усталость: ${session.fatigue.title}" + if (signs.isEmpty()) "" else " (${signs.joinToString(", ") { it.text }})")
        session.goals.forEach { goal ->
            val challenge = if (goal.challenge) " (челлендж)" else ""
            appendLine("Цель «${goal.kind.title}»$challenge: ${if (goal.done) "выполнена" else "не выполнена"} (${goal.condition.lowercase()})")
        }
        session.quest?.let { quest ->
            val answer = if (session.questDone) progress.reflectionOf(quest)?.feeling?.title ?: "ответ есть" else "не пройден"
            appendLine("Квест в жизни: ${quest.title} - $answer")
        }
        appendLine()

        if (progress.reflections.isNotEmpty()) {
            appendLine("Квесты в жизни (последний ответ)")
            progress.reflections.forEach { (quest, r) -> appendLine("${quest.title}: ${r.feeling.title}") }
            appendLine()
        }

        val missions = Rooms.ALL.flatMap { it.missions }
        val levels = missions.filter { progress.levelOf(it) != 0 }
        if (levels.isNotEmpty()) {
            appendLine("Адаптивная сложность заданий")
            levels.forEach { m ->
                val level = progress.levelOf(m)
                appendLine("${m.title}: ${if (level > 0) "+$level" else "$level"}")
            }
        }
    }.trimEnd()
}