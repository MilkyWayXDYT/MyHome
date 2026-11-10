package com.robotpaw.myhome.mission

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.R
import com.robotpaw.myhome.game.MissionKind
import com.robotpaw.myhome.game.PetKind
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

/**
 * Картинки предметов (res/drawable)
 * start - в начале задания, done - после выполнения (null - та же, что start)
 */
data class MissionSprite(@DrawableRes val start: Int, @DrawableRes val done: Int? = null)

object MissionSprites {
    /**
     * null - картинки пока нет, рисуется заглушка
     * @param pet - у заданий питомца картинки свои для кота, собаки и свиньи
     */
    fun of(kind: MissionKind, pet: PetKind? = null): MissionSprite? = when (kind) {
        MissionKind.DOOR -> MissionSprite(R.drawable.q_door_start, R.drawable.q_door_done)
        MissionKind.PICTURE -> MissionSprite(R.drawable.q_picture_start, R.drawable.q_picture_done)
        MissionKind.PILLOWS -> MissionSprite(R.drawable.q_pillows_start, R.drawable.q_pillows_done)
        MissionKind.VACUUM -> MissionSprite(R.drawable.q_vacuum_start, R.drawable.q_vacuum_done)
        MissionKind.FLOWER -> MissionSprite(R.drawable.q_flower_start, R.drawable.q_flower_done)
        MissionKind.TOOTHBRUSH -> MissionSprite(R.drawable.q_toothbrush_start, R.drawable.q_toothbrush_done)
        MissionKind.TOWEL -> MissionSprite(R.drawable.q_towel_start, R.drawable.q_towel_done)
        MissionKind.MIRROR -> MissionSprite(R.drawable.q_mirror_start, R.drawable.q_mirror_done)
        MissionKind.LAUNDRY -> MissionSprite(R.drawable.q_laundry_start, R.drawable.q_laundry_done)
        MissionKind.CLOTHESPINS -> MissionSprite(R.drawable.q_clothespins_start, R.drawable.q_clothespins_done)
        MissionKind.TRASH -> MissionSprite(R.drawable.q_trash_start, R.drawable.q_trash_done)
        MissionKind.TABLE -> MissionSprite(R.drawable.q_table_start, R.drawable.q_table_done)
        MissionKind.TEA -> MissionSprite(R.drawable.q_tea_start, R.drawable.q_tea_done)
        MissionKind.SALAD -> MissionSprite(R.drawable.q_salad_start, R.drawable.q_salad_done)
        MissionKind.PANCAKES -> MissionSprite(R.drawable.q_pancakes_start, R.drawable.q_pancakes_done)
        MissionKind.CUBES -> MissionSprite(R.drawable.q_cubes_start, R.drawable.q_cubes_done)
        MissionKind.PET_CHOICE -> MissionSprite(
            R.drawable.q_pet_choice_start,
            when (pet) {
                PetKind.DOG -> R.drawable.q_pet_choice_done_dog
                PetKind.PIG -> R.drawable.q_pet_choice_done_pig
                else -> R.drawable.q_pet_choice_done_cat
            }
        )
        MissionKind.PET_TOY -> when (pet) {
            PetKind.DOG -> MissionSprite(R.drawable.q_pet_toy_start_dog, R.drawable.q_pet_toy_done_dog)
            PetKind.PIG -> MissionSprite(R.drawable.q_pet_toy_start_pig, R.drawable.q_pet_toy_done_pig)
            else -> MissionSprite(R.drawable.q_pet_toy_start_cat, R.drawable.q_pet_toy_done_cat)
        }
        MissionKind.PET_BOWL -> MissionSprite(R.drawable.q_pet_bowl_start, R.drawable.q_pet_bowl_done)
        MissionKind.PET_FOOD -> MissionSprite(R.drawable.q_pet_food_start, R.drawable.q_pet_food_done)
    }
}

/**
 * Предмет задания: картинка из MissionSprites или заглушка
 * @param progress - 0 начало задания, 1 выполнено
 */
@Composable
fun MissionObject(kind: MissionKind, progress: Float, modifier: Modifier = Modifier) {
    val p = progress.coerceIn(0f, 1f)
    val sprite = MissionSprites.of(kind, LocalPet.current)
    if (sprite != null) {
        val res = if (p >= 1f) sprite.done ?: sprite.start else sprite.start
        ArtFrame(modifier) {
            Image(
                painter = painterResource(res),
                contentDescription = kind.label,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HomeColors.Card),
        contentAlignment = Alignment.Center
    ) {
        Placeholder(kind, p)
    }
}

// заглушка: название предмета, заливка снизу показывает progress
@Composable
private fun BoxScope.Placeholder(kind: MissionKind, progress: Float) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(3.dp, HomeColors.AccentSoft, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(progress)
                .background(HomeColors.AccentSoft)
        )
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Text(kind.label, fontSize = HomeType.Title, fontWeight = FontWeight.ExtraBold, color = HomeColors.Ink)
            Text("заглушка", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
        }
    }
}

@Composable
fun EmptyWall(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(HomeColors.Card),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Preview(widthDp = 390, heightDp = 900, showBackground = true)
@Composable
private fun MissionObjectsPreview() {
    Column(
        Modifier.background(HomeColors.Background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(MissionKind.PICTURE, MissionKind.VACUUM, MissionKind.FLOWER).forEach { kind ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0f, 0.5f, 1f).forEach { p ->
                    MissionObject(kind, p, Modifier.weight(1f).height(200.dp))
                }
            }
        }
    }
}