package com.robotpaw.myhome.mission

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.robotpaw.myhome.R
import com.robotpaw.myhome.game.MissionKind
import kotlin.math.PI
import kotlin.math.sin

/**
 * Слой задания на сцене комнаты (все картинки одного размера, ложатся точно друг на друга)
 * @param start - предмет до выполнения
 * @param done - предмет после выполнения
 * @param highlight - тёплая обводка предмета «до», когда задание выбрано
 * @param highlightDone - тёплая обводка предмета «после»
 */
data class SceneLayer(
    @DrawableRes val start: Int,
    @DrawableRes val done: Int,
    @DrawableRes val highlight: Int,
    @DrawableRes val highlightDone: Int
)

/**
 * Сцена комнаты: фон и задания, предметы которых на ней стоят
 */
data class SceneArt(@DrawableRes val background: Int, val kinds: List<MissionKind>)

object RoomScenes {
    private val ALL = listOf(
        SceneArt(R.drawable.scene_bg_door, listOf(MissionKind.DOOR)),
        SceneArt(R.drawable.scene_bg_window, listOf(MissionKind.FLOWER)),
        SceneArt(R.drawable.scene_bg_picture, listOf(MissionKind.PICTURE)),
        SceneArt(R.drawable.scene_bg_sofa, listOf(MissionKind.PILLOWS, MissionKind.VACUUM)),
        SceneArt(R.drawable.scene_bg_washer, listOf(MissionKind.LAUNDRY)),
        SceneArt(R.drawable.scene_bg_sink, listOf(MissionKind.TOOTHBRUSH, MissionKind.TOWEL, MissionKind.MIRROR)),
        SceneArt(R.drawable.scene_bg_tub, listOf(MissionKind.CLOTHESPINS)),
        SceneArt(R.drawable.scene_bg_counter, listOf(MissionKind.TEA, MissionKind.PANCAKES)),
        SceneArt(R.drawable.scene_bg_board, listOf(MissionKind.SALAD, MissionKind.TRASH)),
        SceneArt(R.drawable.scene_bg_table, listOf(MissionKind.TABLE))
    )

    // null - сцены пока нет, рисуется MissionObject
    fun of(kind: MissionKind): SceneArt? = ALL.firstOrNull { kind in it.kinds }

    fun layerOf(kind: MissionKind): SceneLayer? = when (kind) {
        MissionKind.DOOR -> SceneLayer(R.drawable.scene_door_start, R.drawable.scene_door_done, R.drawable.scene_door_hl, R.drawable.scene_door_hl_done)
        MissionKind.FLOWER -> SceneLayer(R.drawable.scene_flower_start, R.drawable.scene_flower_done, R.drawable.scene_flower_hl, R.drawable.scene_flower_hl_done)
        MissionKind.PICTURE -> SceneLayer(R.drawable.scene_picture_start, R.drawable.scene_picture_done, R.drawable.scene_picture_hl, R.drawable.scene_picture_hl_done)
        MissionKind.PILLOWS -> SceneLayer(R.drawable.scene_pillows_start, R.drawable.scene_pillows_done, R.drawable.scene_pillows_hl, R.drawable.scene_pillows_hl_done)
        MissionKind.VACUUM -> SceneLayer(R.drawable.scene_vacuum_start, R.drawable.scene_vacuum_done, R.drawable.scene_vacuum_hl, R.drawable.scene_vacuum_hl_done)
        MissionKind.LAUNDRY -> SceneLayer(R.drawable.scene_laundry_start, R.drawable.scene_laundry_done, R.drawable.scene_laundry_hl, R.drawable.scene_laundry_hl_done)
        MissionKind.TOOTHBRUSH -> SceneLayer(R.drawable.scene_toothbrush_start, R.drawable.scene_toothbrush_done, R.drawable.scene_toothbrush_hl, R.drawable.scene_toothbrush_hl_done)
        MissionKind.TOWEL -> SceneLayer(R.drawable.scene_towel_start, R.drawable.scene_towel_done, R.drawable.scene_towel_hl, R.drawable.scene_towel_hl_done)
        MissionKind.MIRROR -> SceneLayer(R.drawable.scene_mirror_start, R.drawable.scene_mirror_done, R.drawable.scene_mirror_hl, R.drawable.scene_mirror_hl_done)
        MissionKind.CLOTHESPINS -> SceneLayer(R.drawable.scene_clothespins_start, R.drawable.scene_clothespins_done, R.drawable.scene_clothespins_hl, R.drawable.scene_clothespins_hl_done)
        MissionKind.TEA -> SceneLayer(R.drawable.scene_tea_start, R.drawable.scene_tea_done, R.drawable.scene_tea_hl, R.drawable.scene_tea_hl_done)
        MissionKind.PANCAKES -> SceneLayer(R.drawable.scene_pancakes_start, R.drawable.scene_pancakes_done, R.drawable.scene_pancakes_hl, R.drawable.scene_pancakes_hl_done)
        MissionKind.SALAD -> SceneLayer(R.drawable.scene_salad_start, R.drawable.scene_salad_done, R.drawable.scene_salad_hl, R.drawable.scene_salad_hl_done)
        MissionKind.TRASH -> SceneLayer(R.drawable.scene_trash_start, R.drawable.scene_trash_done, R.drawable.scene_trash_hl, R.drawable.scene_trash_hl_done)
        MissionKind.TABLE -> SceneLayer(R.drawable.scene_table_start, R.drawable.scene_table_done, R.drawable.scene_table_hl, R.drawable.scene_table_hl_done)
        else -> null
    }
}

/**
 * Сцена комнаты из слоёв: фон, предметы всех заданий сцены, обводка выбранного предмета, свет сверху
 * @param art - сцена из RoomScenes.of
 * @param selected - выбранное задание, его предмет обводится
 * @param isDone - выполнено ли задание: от этого зависит, какой слой предмета показать
 * @param change - null обычная сцена; 0..1 - предмет выбранного задания меняется с «до» на «после»
 */
@Composable
fun RoomSceneView(
    art: SceneArt,
    selected: MissionKind,
    isDone: (MissionKind) -> Boolean,
    modifier: Modifier = Modifier,
    change: Float? = null
) {
    ArtFrame(modifier) {
        SceneImage(art.background)
        art.kinds.forEach { kind ->
            val layer = RoomScenes.layerOf(kind) ?: return@forEach
            if (change != null && kind == selected) {
                SceneImage(layer.start, alpha = 1f - change)
                SceneImage(layer.done, alpha = change)
            } else {
                SceneImage(if (isDone(kind)) layer.done else layer.start)
            }
        }
        RoomScenes.layerOf(selected)?.let { layer ->
            when {
                // во время смены обводка вспыхивает и гаснет
                change != null -> SceneImage(layer.highlightDone, alpha = sin(PI * change).toFloat())
                isDone(selected) -> SceneImage(layer.highlightDone)
                else -> SceneImage(layer.highlight)
            }
        }
        SceneImage(R.drawable.scene_light)
    }
}

// все слои одного размера и растягиваются на одну и ту же рамку, поэтому совпадают
@Composable
private fun SceneImage(@DrawableRes res: Int, alpha: Float = 1f) {
    if (alpha <= 0f) return
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        alpha = alpha.coerceAtMost(1f),
        modifier = Modifier.fillMaxSize()
    )
}
