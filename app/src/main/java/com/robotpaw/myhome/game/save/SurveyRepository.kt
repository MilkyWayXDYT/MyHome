package com.robotpaw.myhome.game.save

import com.robotpaw.myhome.game.feedback.SurveyAnswer

/**
 * Сохраняет ответы мини-опроса (по одному на день, последние KEEP_DAYS)
 * Формат: v1|день;ответ;ответ;ответ|...
 */
class SurveyRepository(private val storage: KeyValueStorage) {

    fun load(): List<SurveyAnswer> {
        val parts = storage.getString(KEY_SURVEY)?.split("|") ?: return emptyList()
        if (parts.firstOrNull() != VERSION) return emptyList()
        return parts.drop(1).mapNotNull { item ->
            val p = item.split(";")
            if (p.size < 2) return@mapNotNull null
            val answers = p.drop(1).map { it.toIntOrNull() ?: return@mapNotNull null }
            SurveyAnswer(p[0], answers)
        }
    }

    fun save(answers: List<SurveyAnswer>) {
        storage.putString(
            KEY_SURVEY,
            if (answers.isEmpty()) null
            else (listOf(VERSION) + answers.map { (listOf(it.day) + it.answers).joinToString(";") }).joinToString("|")
        )
    }

    companion object {
        const val KEY_SURVEY = "survey"
        const val KEEP_DAYS = 30
        private const val VERSION = "v1"

        /**
         * Добавить ответы дня: повторный опрос в тот же день заменяет прежний
         */
        fun withAnswer(list: List<SurveyAnswer>, answer: SurveyAnswer): List<SurveyAnswer> =
            (list.filter { it.day != answer.day } + answer).sortedBy { it.day }.takeLast(KEEP_DAYS)
    }
}