package com.robotpaw.myhome.signal

/** Что меню должно сделать в ответ на команду. */
sealed interface MenuAction {
    data class Select(val index: Int) : MenuAction
    data object Back : MenuAction
}

/**
 * Навигация по меню сигналом. Получает команды от [HoldDetector] и двигает подсветку.
 *
 * Новичок — одна схема на обе мышцы, можно пользоваться любой:
 * - короткий сигнал — подсветка переходит на следующий пункт (после последнего — снова первый);
 * - удержание — выбрать подсвеченный пункт.
 * «Назад» для новичка — обычный пункт меню.
 *
 * Опытный — у мышц разные роли:
 * - короткий С — следующий пункт, короткий Р — предыдущий;
 * - удержание С — выбрать, удержание Р — назад.
 *
 * @param itemCount сколько пунктов в меню.
 */
class MenuNavigator(
    private val itemCount: Int,
    var level: UserLevel,
    initialIndex: Int = 0
) {
    init {
        require(itemCount > 0) { "В меню должен быть хотя бы один пункт" }
    }

    var selected: Int = initialIndex.coerceIn(0, itemCount - 1)
        private set

    fun onEvent(event: ControlEvent): MenuAction? = when (level) {
        UserLevel.NOVICE -> when (event) {
            is ControlEvent.Pulse -> { move(+1); null }
            is ControlEvent.Hold -> MenuAction.Select(selected)
        }
        UserLevel.EXPERIENCED -> when (event) {
            is ControlEvent.Pulse -> {
                move(if (event.channel == Channel.FLEXOR) +1 else -1)
                null
            }
            is ControlEvent.Hold ->
                if (event.channel == Channel.FLEXOR) MenuAction.Select(selected) else MenuAction.Back
        }
    }

    fun holdSelects(channel: Channel): Boolean =
        level == UserLevel.NOVICE || channel == Channel.FLEXOR

    private fun move(step: Int) {
        selected = (selected + step + itemCount) % itemCount
    }
}