package com.robotpaw.myhome.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

/**
 * Размеры текста «Мой дом».
 */
object HomeType {
    val Display = 38.sp        // главный заголовок результата: «Готово!»
    val DisplayLine = 44.sp

    val Title = 32.sp          // заголовок экрана
    val TitleLine = 38.sp

    val Heading = 24.sp        // пункт меню, заголовок окна, крупные числа
    val HeadingLine = 30.sp

    val Body = 20.sp           // основной текст, пояснения
    val BodyLine = 27.sp

    val Label = 18.sp          // подписи под пунктами, шапка экрана, подсказки
    val LabelLine = 24.sp

    val Caption = 16.sp        // самый мелкий текст: подписи к столбикам, служебное
}