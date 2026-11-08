package com.robotpaw.myhome.game

// предмет задания, картинка для него - в MissionSprites
enum class MissionKind(val label: String) {
    // обучение
    DOOR("Дверь"),
    CUBES("Кубики"),

    // гостиная
    PICTURE("Картина"),
    PILLOWS("Подушки"),
    VACUUM("Пылесос"),
    FLOWER("Цветочек")
}

enum class Difficulty(val label: String) {
    NONE(""),
    EASY("лёгкое"),
    MEDIUM("среднее"),
    HARD("сложное")
}

/**
 * Задание в комнате (пока только данные для интерфейса, параметры механики добавятся сюда)
 * @param wallTitle - место в комнате, где стоит предмет
 * @param optional - дополнительная сложность, для прохождения комнаты не обязательно
 * @param instruction - шаги для экрана инструкции
 * @param doneTitle - заголовок экрана результата
 */
data class Mission(
    val id: String,
    val kind: MissionKind,
    val title: String,
    val wallTitle: String,
    val doneTitle: String,
    val difficulty: Difficulty = Difficulty.NONE,
    val optional: Boolean = false,
    val instruction: List<String> = listOf("Здесь будет инструкция к заданию")
)

/**
 * Итог задания для экрана результата (статистика добавится вместе с механикой)
 */
data class MissionResult(val mission: Mission)

object Missions {

    val TUTORIAL = listOf(
        Mission("tutorial_door", MissionKind.DOOR, "Откройте дверь", "Входная дверь", "Дверь открыта!"),
        Mission("tutorial_cubes", MissionKind.CUBES, "Переложите кубики", "Коробка с кубиками", "Кубики на месте!")
    )

    val LIVING_ROOM = listOf(
        Mission("living_picture", MissionKind.PICTURE, "Повесьте картину", "Стена с картиной", "Картина на месте!", Difficulty.EASY),
        Mission("living_pillows", MissionKind.PILLOWS, "Разложите подушки", "Диван", "Подушки разложены!", Difficulty.EASY),
        Mission("living_vacuum", MissionKind.VACUUM, "Пропылесосьте", "Ковёр", "Чисто!", Difficulty.MEDIUM),
        Mission("living_flower", MissionKind.FLOWER, "Полейте цветочек", "Полка с цветочком", "Цветочек полит!", Difficulty.MEDIUM)
    )
}