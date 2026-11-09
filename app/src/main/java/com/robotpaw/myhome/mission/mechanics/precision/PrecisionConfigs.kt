package com.robotpaw.myhome.mission.mechanics.precision

import com.robotpaw.myhome.mission.mechanics.alternation.AlternationConfig
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationStep
import com.robotpaw.myhome.signal.Channel

/**
 * Задания на точность: те же шаги, что в чередовании, но на ключевых шагах нужна сила в «зелёной зоне»
 * Сила выше зоны - ошибка (пролил, нажал слишком резко), ниже зоны - прогресс не идёт
 * Нижняя граница зон не ниже 30%: ниже сигнал обычно не считается активным
 */
object PrecisionConfigs {

    // Зубы (лёгкое): паста, щётка, чистка. Ошибки - слишком сильный нажим («В яблочко»)
    val TOOTHBRUSH = AlternationConfig(
        steps = listOf(
            AlternationStep(Channel.FLEXOR, 1200L, "Сожмите кисть: выдавите пасту", zone = 30..80),
            AlternationStep(Channel.EXTENSOR, 1200L, "Разожмите кисть: поднесите щётку", zone = 30..70),
            AlternationStep(Channel.FLEXOR, 3000L, "Сожмите кисть: чистите зубы", zone = 30..60)
        ),
        attemptLimitMs = 40_000L
    )

    // Цветочек (среднее): наклонить лейку так, чтобы вода шла тонкой струйкой («Ни капли»)
    val FLOWER = AlternationConfig(
        steps = listOf(
            AlternationStep(Channel.FLEXOR, 1000L, "Сожмите кисть: возьмите лейку"),
            AlternationStep(Channel.EXTENSOR, 3000L, "Разожмите мягко: наклоните лейку", zone = 30..55),
            AlternationStep(Channel.FLEXOR, 1000L, "Сожмите кисть: поставьте лейку")
        ),
        attemptLimitMs = 40_000L
    )

    // Полотенце (среднее): отпустить точно на перекладине («В яблочко»)
    val TOWEL = AlternationConfig(
        steps = listOf(
            AlternationStep(Channel.FLEXOR, 1000L, "Сожмите кисть: возьмите полотенце"),
            AlternationStep(Channel.EXTENSOR, 1500L, "Разожмите кисть: поднимите к перекладине", zone = 30..60),
            AlternationStep(Channel.FLEXOR, 1000L, "Сожмите кисть: перекиньте полотенце"),
            AlternationStep(Channel.EXTENSOR, 1500L, "Разожмите мягко: отпустите точно", zone = 30..50)
        ),
        attemptLimitMs = 40_000L
    )

    // Прищепки (сложное, доп.): три прищепки, нужна аккуратная сила («Нежное касание»)
    val CLOTHESPINS = AlternationConfig(
        steps = (1..3).flatMap { n ->
            listOf(
                AlternationStep(Channel.FLEXOR, 1000L, "Сожмите осторожно: возьмите прищепку $n из 3", zone = 30..55),
                AlternationStep(Channel.EXTENSOR, 1000L, "Разожмите осторожно: прикрепите прищепку $n", zone = 30..50)
            )
        },
        attemptLimitMs = 60_000L
    )

    // Чай (сложное): кружка, налить тонкой струйкой, пакетик, поставить («Ни капли»)
    val TEA = AlternationConfig(
        steps = listOf(
            AlternationStep(Channel.FLEXOR, 1000L, "Сожмите кисть: возьмите кружку", zone = 30..70),
            AlternationStep(Channel.EXTENSOR, 1000L, "Разожмите кисть: подставьте под чайник", zone = 30..60),
            AlternationStep(Channel.FLEXOR, 3000L, "Сожмите мягко: наливайте тонкой струйкой", zone = 30..50),
            AlternationStep(Channel.EXTENSOR, 1000L, "Разожмите кисть: опустите пакетик", zone = 30..60),
            AlternationStep(Channel.FLEXOR, 1000L, "Сожмите кисть: поставьте кружку", zone = 30..70)
        ),
        attemptLimitMs = 60_000L
    )

    val ALL = listOf(TOOTHBRUSH, FLOWER, TOWEL, CLOTHESPINS, TEA)
}
