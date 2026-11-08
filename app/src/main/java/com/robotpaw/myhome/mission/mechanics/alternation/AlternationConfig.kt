package com.robotpaw.myhome.mission.mechanics.alternation

import com.robotpaw.myhome.signal.Channel

/**
 * Настройки заданий на чередование
 * @param steps - шаги по порядку
 * @param attemptLimitMs - если за это время задание не выполнено, показываем окно поддержки
 */
data class AlternationConfig(
    val steps: List<AlternationStep>,
    val attemptLimitMs: Long
)

object AlternationConfigs {

    // Картина: взять, поднять, прижать, отпустить в нужное место (последний шаг - только в зоне силы, мягко)
    val PICTURE = AlternationConfig(
        steps = listOf(
            AlternationStep(Channel.FLEXOR, 1200L, "Сожмите кисть: возьмите картину"),
            AlternationStep(Channel.EXTENSOR, 1200L, "Разожмите кисть: поднимите к крючку"),
            AlternationStep(Channel.FLEXOR, 1200L, "Сожмите кисть: поймайте крючок"),
            AlternationStep(
                Channel.EXTENSOR,
                1500L,
                "Разожмите мягко: отпустите картину",
                zone = 20..70
            )
        ),
        attemptLimitMs = 30_000L
    )

    // Подушки: четыре раза подряд «взять - положить»
    val PILLOWS = AlternationConfig(
        steps = (1..4).flatMap { n ->
            listOf(
                AlternationStep(Channel.FLEXOR, 800L, "Сожмите кисть: возьмите подушку $n из 4"),
                AlternationStep(Channel.EXTENSOR, 800L, "Разожмите кисть: положите подушку $n")
            )
        },
        attemptLimitMs = 30_000L
    )
}
