package com.robotpaw.myhome.mission.mechanics.pet

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.MissionLog
import com.robotpaw.myhome.game.PetKind
import com.robotpaw.myhome.menu.MenuItem
import com.robotpaw.myhome.menu.MenuList
import com.robotpaw.myhome.menu.rememberMenuControl
import com.robotpaw.myhome.mission.MissionObject
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource
import com.robotpaw.myhome.ui.components.HomeCard
import com.robotpaw.myhome.ui.components.HomeScreen
import com.robotpaw.myhome.ui.theme.HomeColors
import com.robotpaw.myhome.ui.theme.HomeType

/**
 * Выбор питомца: обычное меню из двух пунктов, управляется так же, как все меню игры
 * (короткий сигнал - к следующему пункту, удержание - выбрать)
 * @param onPetChosen - выбор сохраняется в прогрессе до вызова onDone
 */
@Composable
fun PetChoiceMechanic(
    source: SignalSource,
    profile: SignalProfile,
    roomTitle: String,
    mission: Mission,
    onPetChosen: (PetKind) -> Unit,
    onDone: (MissionLog) -> Unit
) {
    val control = rememberMenuControl(
        source = source,
        profile = profile,
        items = PetKind.entries.map { MenuItem(it.title) },
        onSelect = { index ->
            onPetChosen(PetKind.entries[index])
            onDone(MissionLog())
        }
    )

    HomeScreen(
        title = mission.title,
        overline = "$roomTitle · задание"
    ) {
        MissionObject(mission.kind, progress = 0f, modifier = Modifier.weight(1f))
        HomeCard {
            Text("Выберите, кто будет жить в доме", fontSize = HomeType.Body, fontWeight = FontWeight.Bold, color = HomeColors.Ink)
            Text("Короткий сигнал - следующий, удержание - выбрать.", fontSize = HomeType.Label, color = HomeColors.InkSecondary)
        }
        MenuList(control)
    }
}
