package com.robotpaw.myhome.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuNavigatorTest {

    @Test
    fun novice_anyPulseMovesForward_andWraps() {
        val menu = MenuNavigator(itemCount = 3, level = UserLevel.NOVICE)
        assertNull(menu.onEvent(ControlEvent.Pulse(Channel.FLEXOR)))
        assertEquals(1, menu.selected)
        menu.onEvent(ControlEvent.Pulse(Channel.EXTENSOR))   // Р у новичка тоже «дальше»
        assertEquals(2, menu.selected)
        menu.onEvent(ControlEvent.Pulse(Channel.FLEXOR))
        assertEquals(0, menu.selected)                       // после последнего — снова первый
    }

    @Test
    fun novice_anyHoldSelects() {
        val menu = MenuNavigator(itemCount = 3, level = UserLevel.NOVICE, initialIndex = 2)
        assertEquals(MenuAction.Select(2), menu.onEvent(ControlEvent.Hold(Channel.FLEXOR)))
        assertEquals(MenuAction.Select(2), menu.onEvent(ControlEvent.Hold(Channel.EXTENSOR)))
        assertTrue(menu.holdSelects(Channel.EXTENSOR))
    }

    @Test
    fun experienced_channelsHaveRoles() {
        val menu = MenuNavigator(itemCount = 3, level = UserLevel.EXPERIENCED)
        menu.onEvent(ControlEvent.Pulse(Channel.EXTENSOR))   // вверх с первого — на последний
        assertEquals(2, menu.selected)
        menu.onEvent(ControlEvent.Pulse(Channel.FLEXOR))
        assertEquals(0, menu.selected)
        assertEquals(MenuAction.Select(0), menu.onEvent(ControlEvent.Hold(Channel.FLEXOR)))
        assertEquals(MenuAction.Back, menu.onEvent(ControlEvent.Hold(Channel.EXTENSOR)))
        assertFalse(menu.holdSelects(Channel.EXTENSOR))
    }
}