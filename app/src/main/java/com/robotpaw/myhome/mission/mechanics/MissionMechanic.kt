package com.robotpaw.myhome.mission.mechanics

import androidx.compose.runtime.Composable
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationConfigs
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationMechanic
import com.robotpaw.myhome.game.adapt.Adaptation
import com.robotpaw.myhome.mission.mechanics.door.DOOR_ATTEMPT_LIMIT_MS
import com.robotpaw.myhome.mission.mechanics.door.DOOR_HOLD_MS
import com.robotpaw.myhome.mission.mechanics.door.DoorMechanic
import com.robotpaw.myhome.mission.mechanics.mixed.MixedConfigs
import com.robotpaw.myhome.mission.mechanics.pet.PetChoiceMechanic
import com.robotpaw.myhome.mission.mechanics.pet.PetConfigs
import com.robotpaw.myhome.mission.mechanics.precision.PrecisionConfigs
import com.robotpaw.myhome.mission.mechanics.rhythm.RhythmConfigs
import com.robotpaw.myhome.mission.mechanics.rhythm.RhythmMechanic
import com.robotpaw.myhome.mission.mechanics.strength.StrengthHoldConfigs
import com.robotpaw.myhome.mission.mechanics.strength.StrengthHoldMechanic
import com.robotpaw.myhome.game.Mission
import com.robotpaw.myhome.game.MissionCheckpoint
import com.robotpaw.myhome.game.MissionKind
import com.robotpaw.myhome.game.MissionLog
import com.robotpaw.myhome.game.PetKind
import com.robotpaw.myhome.signal.SignalProfile
import com.robotpaw.myhome.signal.SignalSource

/**
 * Какие задания уже имеют настоящую механику (остальные идут через заглушку в MissionScreen)
 * Сделали механику - добавьте вид сюда и ветку в MissionMechanic
 */
object Mechanics {
    val implemented: Set<MissionKind> = setOf(
        MissionKind.DOOR,
        MissionKind.CUBES,
        MissionKind.PICTURE,
        MissionKind.PILLOWS,
        MissionKind.TRASH,
        MissionKind.VACUUM,
        MissionKind.MIRROR,
        MissionKind.TOOTHBRUSH,
        MissionKind.FLOWER,
        MissionKind.TOWEL,
        MissionKind.CLOTHESPINS,
        MissionKind.TEA,
        MissionKind.TABLE,
        MissionKind.SALAD,
        MissionKind.LAUNDRY,
        MissionKind.PANCAKES,
        MissionKind.PET_CHOICE,
        MissionKind.PET_TOY,
        MissionKind.PET_BOWL,
        MissionKind.PET_FOOD
    )

    fun has(kind: MissionKind): Boolean = kind in implemented
}

/**
 * Выбирает механику по виду задания. Контракт у всех механик одинаковый:
 * слушает сигнал, по завершении вызывает onDone с логом, когда игроку нужна пауза - onPause с чекпоинтом
 * pet - выбранный питомец (тексты финала), onPetChosen - выбор питомца в задании «Питомец»
 * level - адаптивная сложность (Adaptation): настройки задания подстраиваются под игрока, 0 - как есть
 */
@Composable
fun MissionMechanic(
    source: SignalSource,
    profile: SignalProfile,
    roomTitle: String,
    mission: Mission,
    checkpoint: MissionCheckpoint,
    onDone: (MissionLog) -> Unit,
    onPause: (MissionCheckpoint) -> Unit,
    pet: PetKind = PetKind.CAT,
    onPetChosen: (PetKind) -> Unit = {},
    level: Int = 0
) {
    when (mission.kind) {
        MissionKind.DOOR -> DoorMechanic(
            source, profile, roomTitle, mission, checkpoint, onDone, onPause,
            holdMs = Adaptation.holdMs(DOOR_HOLD_MS, level),
            attemptLimitMs = Adaptation.limitMs(DOOR_ATTEMPT_LIMIT_MS, level)
        )
        MissionKind.PICTURE -> AlternationMechanic(
            source, profile, roomTitle, mission, AlternationConfigs.PICTURE.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.PILLOWS -> AlternationMechanic(
            source, profile, roomTitle, mission, AlternationConfigs.PILLOWS.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.TRASH -> StrengthHoldMechanic(
            source, profile, roomTitle, mission, StrengthHoldConfigs.TRASH.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.VACUUM -> StrengthHoldMechanic(
            source, profile, roomTitle, mission, StrengthHoldConfigs.VACUUM.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.MIRROR -> StrengthHoldMechanic(
            source, profile, roomTitle, mission, StrengthHoldConfigs.MIRROR.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.TOOTHBRUSH -> AlternationMechanic(
            source, profile, roomTitle, mission, PrecisionConfigs.TOOTHBRUSH.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.FLOWER -> AlternationMechanic(
            source, profile, roomTitle, mission, PrecisionConfigs.FLOWER.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.TOWEL -> AlternationMechanic(
            source, profile, roomTitle, mission, PrecisionConfigs.TOWEL.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.CLOTHESPINS -> AlternationMechanic(
            source, profile, roomTitle, mission, PrecisionConfigs.CLOTHESPINS.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.TEA -> AlternationMechanic(
            source, profile, roomTitle, mission, PrecisionConfigs.TEA.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.CUBES -> AlternationMechanic(
            source, profile, roomTitle, mission, AlternationConfigs.CUBES.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.TABLE -> AlternationMechanic(
            source, profile, roomTitle, mission, AlternationConfigs.TABLE.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.SALAD -> RhythmMechanic(
            source, profile, roomTitle, mission, RhythmConfigs.SALAD.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.LAUNDRY -> AlternationMechanic(
            source, profile, roomTitle, mission, MixedConfigs.LAUNDRY.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.PANCAKES -> AlternationMechanic(
            source, profile, roomTitle, mission, MixedConfigs.PANCAKES.adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.PET_CHOICE -> PetChoiceMechanic(source, profile, roomTitle, mission, onPetChosen, onDone)
        MissionKind.PET_TOY -> AlternationMechanic(
            source, profile, roomTitle, mission, PetConfigs.toy(pet).adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.PET_BOWL -> AlternationMechanic(
            source, profile, roomTitle, mission, PetConfigs.bowl(pet).adapted(level), checkpoint, onDone, onPause
        )
        MissionKind.PET_FOOD -> AlternationMechanic(
            source, profile, roomTitle, mission, PetConfigs.food(pet).adapted(level), checkpoint, onDone, onPause
        )
        else -> Unit
    }
}