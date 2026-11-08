package com.robotpaw.myhome.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * Цвета «Мой дом»: тёплая кремово-коричневая гамма.
 * Мышцы везде одинаковые и не меняются: С — синий, Р — оранжевый.
 * Синий не используется для выделения пунктов — для этого Accent, чтобы не путать с мышцей.
 */
object HomeColors {
    // Фон и поверхности
    val Background = Color(0xFFF2E3C6)   // светлее Golden Batter (#E8D1A7) — основной фон
    val Card = Color(0xFFFFFAF1)         // светлый крем — карточки и пункты меню
    val Track = Color(0xFFF3E8D3)        // пустая дорожка столбиков и полос
    val Divider = Color(0xFFD3B988)      // разделители, пустое кольцо, неактивные точки

    // Текст
    val Ink = Color(0xFF442D1C)          // Cowhide Cocoa — основной текст
    val InkSecondary = Color(0xFF6E5640) // подписи и подсказки

    // Акцент: выделение пункта, прогресс шагов, отметки «выполнено»
    val Accent = Color(0xFF84592B)       // Toasted Caramel
    val AccentSoft = Color(0xFFD9B98A)   // мягкий тёплый тон для фонов и заливок

    // Мышцы
    val Flexor = Color(0xFF2F6FDB)
    val Extensor = Color(0xFFE8873E)

    // Встречный сигнал (С+Р) и предупреждения — красный, как был: его нужно замечать сразу
    val Warning = Color(0xFFB3261E)
    val WarningSoft = Color(0xFFFBE4E2)
}