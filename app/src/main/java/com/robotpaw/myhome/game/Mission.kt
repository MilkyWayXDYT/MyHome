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
    FLOWER("Цветочек"),

    // ванная
    TOOTHBRUSH("Зубная щётка"),
    TOWEL("Полотенце"),
    MIRROR("Зеркало"),
    LAUNDRY("Стирка"),
    CLOTHESPINS("Прищепки"),

    // кухня
    TRASH("Мусор"),
    TABLE("Стол"),
    TEA("Чай"),
    SALAD("Салат"),
    PANCAKES("Блинчики"),

    // финал
    PET_CHOICE("Питомец"),
    PET_TOY("Игрушка"),
    PET_BOWL("Миска"),
    PET_FOOD("Корм")
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
 * Итог задания для экрана результата
 * @param log - что происходило во время задания (по умолчанию пустой, для заглушек)
 */
data class MissionResult(val mission: Mission, val log: MissionLog = MissionLog())

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

    val BATHROOM = listOf(
        Mission("bath_teeth", MissionKind.TOOTHBRUSH, "Почистите зубы", "Раковина", "Зубы чистые!", Difficulty.EASY),
        Mission("bath_towel", MissionKind.TOWEL, "Повесьте полотенце", "Крючок", "Полотенце на месте!", Difficulty.MEDIUM),
        Mission("bath_mirror", MissionKind.MIRROR, "Протрите зеркало", "Зеркало", "Зеркало блестит!", Difficulty.MEDIUM),
        Mission("bath_laundry", MissionKind.LAUNDRY, "Постирайте вещи", "Стиральная машина", "Вещи постираны!", Difficulty.HARD),
        Mission(
            "bath_clothespins", MissionKind.CLOTHESPINS, "Развесьте бельё на прищепки", "Сушилка",
            "Бельё сушится!", Difficulty.HARD, optional = true
        )
    )

    val KITCHEN = listOf(
        Mission("kitchen_trash", MissionKind.TRASH, "Вынесите мусор", "Мусорное ведро", "Мусор вынесен!", Difficulty.EASY),
        Mission("kitchen_table", MissionKind.TABLE, "Накройте на стол", "Обеденный стол", "Стол накрыт!", Difficulty.MEDIUM),
        Mission("kitchen_tea", MissionKind.TEA, "Заварите чай", "Чайник и кружка", "Чай готов!", Difficulty.HARD),
        Mission("kitchen_salad", MissionKind.SALAD, "Нарежьте салат", "Разделочная доска", "Салат готов!", Difficulty.HARD),
        Mission("kitchen_pancakes", MissionKind.PANCAKES, "Испеките блинчики", "Плита", "Блинчики готовы!", Difficulty.HARD)
    )

    val PET = listOf(
        Mission("pet_choice", MissionKind.PET_CHOICE, "Выберите питомца", "Коврик у двери", "Знакомьтесь!"),
        Mission("pet_toy", MissionKind.PET_TOY, "Подарите игрушку", "Корзинка с игрушками", "Питомец играет!"),
        Mission("pet_bowl", MissionKind.PET_BOWL, "Поставьте миску", "Уголок питомца", "Миска на месте!"),
        Mission("pet_food", MissionKind.PET_FOOD, "Насыпьте корм", "Миска", "Питомец сыт!")
    )
}
