package com.robotpaw.myhome.mission.mechanics.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.ui.theme.HomeColors

// цвет «зелёной зоны»; если в теме появится свой зелёный, лучше перенести его туда
private val ZoneGreen = Color(0xFF6FA86A)

/**
 * Шкала силы с «зелёной зоной»: игрок видит, где нужно держать силу
 * @param strength - текущая сила канала в процентах (0..100)
 * @param zone - нужный диапазон силы в процентах
 * @param markerColor - цвет метки текущей силы
 */
@Composable
fun ZoneGauge(
    strength: Int,
    zone: IntRange,
    markerColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.fillMaxWidth().height(36.dp)) {
        val round = CornerRadius(size.height / 2f)
        drawRoundRect(color = HomeColors.Track, size = size, cornerRadius = round)

        val zoneLeft = size.width * zone.first.coerceIn(0, 100) / 100f
        val zoneRight = size.width * zone.last.coerceIn(0, 100) / 100f
        drawRect(
            color = ZoneGreen,
            topLeft = Offset(zoneLeft, 0f),
            size = Size((zoneRight - zoneLeft).coerceAtLeast(0f), size.height)
        )

        val markerWidth = 8.dp.toPx()
        val x = (size.width * strength.coerceIn(0, 100) / 100f - markerWidth / 2f)
            .coerceIn(0f, size.width - markerWidth)
        drawRoundRect(
            color = markerColor,
            topLeft = Offset(x, 0f),
            size = Size(markerWidth, size.height),
            cornerRadius = CornerRadius(markerWidth / 2f)
        )
    }
}
