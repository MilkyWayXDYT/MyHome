package com.robotpaw.myhome.signal

sealed interface MenuAction {
    data class Select(val index: Int) : MenuAction

    data object Back : MenuAction
}

/**
 * Навигация по меню командами HoldDetector
 * Новичок (любая мышца): короткий - следующий пункт, удержание - выбрать
 * Опытный: короткий С / Р - вниз / вверх, удержание С - выбрать, удержание Р - назад
 */
class MenuNavigator(
    private val itemCount: Int,
    var level: UserLevel,
    initialIndex: Int = 0
) {
    // 0 пунктов бывает у опытного, когда на экране можно только вернуться назад
    init {
        require(itemCount >= 0)
    }

    var selected: Int = initialIndex.coerceIn(0, maxOf(itemCount - 1, 0))
        private set

    fun onEvent(event: ControlEvent): MenuAction? = when (level) {
        UserLevel.NOVICE -> when (event) {
            is ControlEvent.Pulse -> { move(+1); null }
            is ControlEvent.Hold -> if (itemCount > 0) MenuAction.Select(selected) else null
        }
        UserLevel.EXPERIENCED -> when (event) {
            is ControlEvent.Pulse -> {
                move(if (event.channel == Channel.FLEXOR) +1 else -1)
                null
            }
            is ControlEvent.Hold -> when {
                event.channel == Channel.EXTENSOR -> MenuAction.Back
                itemCount > 0 -> MenuAction.Select(selected)
                else -> null
            }
        }
    }

    // удержание канала выбирает пункт, а не «назад»
    fun holdSelects(channel: Channel): Boolean =
        level == UserLevel.NOVICE || channel == Channel.FLEXOR

    private fun move(step: Int) {
        if (itemCount == 0) return
        selected = (selected + step + itemCount) % itemCount
    }
}