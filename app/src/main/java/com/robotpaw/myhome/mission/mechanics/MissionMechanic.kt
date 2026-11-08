package com.robotpaw.myhome.mission.mechanics

import androidx.compose.runtime.Composable
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.MissionCheckpoint
import com.robotpaw.myhome.game.MissionKind
import com.robotpaw.myhome.game.MissionLog
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource

object Mechanics {
    val implemented: Set<MissionKind> = setOf(
        MissionKind.DOOR
    )

    fun has(kind: MissionKind): Boolean = kind in implemented
}

@Composable
fun MissionMechanic(
    source: SignalSource,
    profile: SignalProfile,
    roomTitle: String,
    mission: Mission,
    checkpoint: MissionCheckpoint,
    onDone: (MissionLog) -> Unit,
    onPause: (MissionCheckpoint) -> Unit
) {
    when (mission.kind) {
        MissionKind.DOOR -> DoorMechanic(source, profile, roomTitle, mission, checkpoint, onDone, onPause)
        else -> Unit
    }
}