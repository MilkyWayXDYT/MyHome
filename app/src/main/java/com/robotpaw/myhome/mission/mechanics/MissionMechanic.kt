package com.robotpaw.myhome.mission.mechanics

import androidx.compose.runtime.Composable
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.MissionCheckpoint
import com.robotpaw.myhome.game.MissionKind
import com.robotpaw.myhome.game.MissionLog
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationConfigs
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationMechanic
import com.robotpaw.myhome.mission.mechanics.door.DoorMechanic
import com.robotpaw.myhome.mission.mechanics.strength.StrengthHoldConfigs
import com.robotpaw.myhome.mission.mechanics.strength.StrengthHoldMechanic
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource

/**
 * Какие задания уже имеют настоящую механику (остальные идут через заглушку в MissionScreen)
 * Сделали механику - добавьте вид сюда и ветку в MissionMechanic
 */
object Mechanics {
    val implemented: Set<MissionKind> = setOf(
        MissionKind.DOOR,
        MissionKind.PICTURE,
        MissionKind.PILLOWS,
        MissionKind.TRASH,
        MissionKind.VACUUM,
        MissionKind.MIRROR
    )

    fun has(kind: MissionKind): Boolean = kind in implemented
}

/**
 * Выбирает механику по виду задания. Контракт у всех механик одинаковый:
 * слушает сигнал, по завершении вызывает onDone с логом, когда игроку нужна пауза - onPause с чекпоинтом
 */
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
        MissionKind.DOOR -> DoorMechanic(
            source,
            profile,
            roomTitle,
            mission,
            checkpoint,
            onDone,
            onPause
        )
        MissionKind.PICTURE -> AlternationMechanic(
            source,
            profile,
            roomTitle,
            mission,
            AlternationConfigs.PICTURE,
            checkpoint,
            onDone,
            onPause
        )
        MissionKind.PILLOWS -> AlternationMechanic(
            source,
            profile,
            roomTitle,
            mission,
            AlternationConfigs.PILLOWS,
            checkpoint,
            onDone,
            onPause
        )
        MissionKind.TRASH -> StrengthHoldMechanic(
            source,
            profile,
            roomTitle,
            mission,
            StrengthHoldConfigs.TRASH,
            checkpoint,
            onDone,
            onPause
        )
        MissionKind.VACUUM -> StrengthHoldMechanic(
            source,
            profile,
            roomTitle,
            mission,
            StrengthHoldConfigs.VACUUM,
            checkpoint,
            onDone,
            onPause
        )
        MissionKind.MIRROR -> StrengthHoldMechanic(
            source,
            profile,
            roomTitle,
            mission,
            StrengthHoldConfigs.MIRROR,
            checkpoint,
            onDone,
            onPause
        )
        else -> Unit
    }
}
