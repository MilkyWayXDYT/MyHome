package com.robotpaw.myhome.mission.mechanics.pet

import com.robotpaw.myhome.game.PetKind
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationConfig
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationStep
import com.robotpaw.myhome.signal.Channel

/**
 * Задания финала: тексты зависят от выбранного питомца, играются механикой чередования
 */
object PetConfigs {

    // Игрушка: два броска «взять - бросить»
    fun toy(pet: PetKind) = AlternationConfig(
        steps = (1..2).flatMap { n ->
            listOf(
                AlternationStep(Channel.FLEXOR, 800L, "Сожмите кисть: возьмите ${pet.toy}"),
                AlternationStep(Channel.EXTENSOR, 800L, "Разожмите кисть: бросьте, бросок $n из 2")
            )
        },
        attemptLimitMs = 40_000L
    )

    // Миска: взять, донести, аккуратно поставить (мягкое разжатие)
    fun bowl(pet: PetKind) = AlternationConfig(
        steps = listOf(
            AlternationStep(Channel.FLEXOR, 1000L, "Сожмите кисть: возьмите миску для ${pet.genitive}"),
            AlternationStep(Channel.FLEXOR, 2000L, "Продолжайте держать: несите миску"),
            AlternationStep(Channel.EXTENSOR, 1000L, "Разожмите мягко: поставьте миску", zone = 30..60)
        ),
        attemptLimitMs = 40_000L
    )

    // Корм: взять пакет, насыпать ровной струйкой, закрыть
    fun food(pet: PetKind) = AlternationConfig(
        steps = listOf(
            AlternationStep(Channel.FLEXOR, 1000L, "Сожмите кисть: возьмите корм для ${pet.genitive}"),
            AlternationStep(Channel.EXTENSOR, 3000L, "Разожмите мягко: насыпайте корм", zone = 30..55),
            AlternationStep(Channel.FLEXOR, 1000L, "Сожмите кисть: закройте пакет")
        ),
        attemptLimitMs = 40_000L
    )

    fun all(pet: PetKind) = listOf(toy(pet), bowl(pet), food(pet))
}
