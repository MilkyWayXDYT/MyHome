package com.robotpaw.myhome.game

// предмет задания, картинка для него - в MissionSprites
enum class MissionKind(val label: String) {
    DOOR("Дверь"),
    CUBES("Кубики"),
    PICTURE("Картина"),
    PILLOWS("Подушки"),
    VACUUM("Пылесос"),
    FLOWER("Цветочек")
}

/**
 * Задание в комнате (пока только данные для интерфейса, параметры механики добавятся сюда)
 * @param wallTitle - место в комнате, где стоит предмет
 * @param instruction - шаги для экрана инструкции
 * @param doneTitle - заголовок экрана результата
 */
data class Mission(
    val id: String,
    val kind: MissionKind,
    val title: String,
    val wallTitle: String,
    val instruction: List<String>,
    val doneTitle: String
)

/**
 * Итог задания для экрана результата (статистика добавится вместе с механикой)
 */
data class MissionResult(val mission: Mission)

object Missions {

    private val STUB_INSTRUCTION = listOf("Здесь будет инструкция к заданию")

    // обучение

    val DOOR = Mission(
        id = "tutorial_door",
        kind = MissionKind.DOOR,
        title = "Откройте дверь",
        wallTitle = "Входная дверь",
        instruction = STUB_INSTRUCTION,
        doneTitle = "Дверь открыта!"
    )

    val CUBES = Mission(
        id = "tutorial_cubes",
        kind = MissionKind.CUBES,
        title = "Переложите кубики",
        wallTitle = "Коробка с кубиками",
        instruction = STUB_INSTRUCTION,
        doneTitle = "Кубики на месте!"
    )

    val TUTORIAL: List<Mission> = listOf(DOOR, CUBES)

    // гостиная

    val PICTURE = Mission(
        id = "living_picture",
        kind = MissionKind.PICTURE,
        title = "Повесьте картину",
        wallTitle = "Стена с картиной",
        instruction = STUB_INSTRUCTION,
        doneTitle = "Картина на месте!"
    )

    val PILLOWS = Mission(
        id = "living_pillows",
        kind = MissionKind.PILLOWS,
        title = "Разложите подушки",
        wallTitle = "Диван",
        instruction = STUB_INSTRUCTION,
        doneTitle = "Подушки разложены!"
    )

    val VACUUM = Mission(
        id = "living_vacuum",
        kind = MissionKind.VACUUM,
        title = "Пропылесосьте",
        wallTitle = "Ковёр",
        instruction = STUB_INSTRUCTION,
        doneTitle = "Чисто!"
    )

    val FLOWER = Mission(
        id = "living_flower",
        kind = MissionKind.FLOWER,
        title = "Полейте цветочек",
        wallTitle = "Полка с цветочком",
        instruction = STUB_INSTRUCTION,
        doneTitle = "Цветочек полит!"
    )

    val LIVING_ROOM: List<Mission> = listOf(PICTURE, PILLOWS, VACUUM, FLOWER)
}