package com.robotpaw.myhome.mission.mechanics.mixed

import com.robotpaw.myhome.mission.mechanics.alternation.AlternationConfig
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationStep
import com.robotpaw.myhome.signal.Channel

/**
 * Смешанные задания: быстрое чередование, зоны силы и шаги на расслабление в одной цепочке
 * Играются той же механикой чередования, что и остальные
 */
object MixedConfigs {

    // Стирка (сложное): три вещи быстрым чередованием, мягко насыпать порошок, закрыть дверцу, сильно нажать «Пуск»
    val LAUNDRY = AlternationConfig(
        steps = (1..3).flatMap { n ->
            listOf(
                AlternationStep(Channel.FLEXOR, 500L, "Сожмите кисть: возьмите вещь $n из 3"),
                AlternationStep(Channel.EXTENSOR, 500L, "Разожмите кисть: положите в барабан")
            )
        } + listOf(
            AlternationStep(Channel.FLEXOR, 2000L, "Сожмите мягко: насыпьте порошок", zone = 30..55),
            AlternationStep(Channel.EXTENSOR, 1000L, "Разожмите кисть: закройте дверцу"),
            AlternationStep(Channel.FLEXOR, 1200L, "Сожмите сильно: нажмите «Пуск»", zone = 60..100)
        ),
        attemptLimitMs = 60_000L
    )

    // Блинчики (сложное): тонкая струйка теста, жарка (покой), перевернуть и переложить аккуратно («Нежное касание»)
    val PANCAKES = AlternationConfig(
        steps = listOf(
            AlternationStep(Channel.FLEXOR, 2000L, "Сожмите мягко: налейте тесто тонкой струйкой", zone = 30..50),
            AlternationStep(Channel.EXTENSOR, 1000L, "Разожмите кисть: уберите половник"),
            AlternationStep(Channel.FLEXOR, 2500L, "Расслабьте руку: блин жарится", rest = true),
            AlternationStep(Channel.FLEXOR, 1000L, "Сожмите осторожно: поддеть лопаткой", zone = 30..60),
            AlternationStep(Channel.EXTENSOR, 1000L, "Разожмите мягко: переверните блин", zone = 30..50),
            AlternationStep(Channel.FLEXOR, 2000L, "Расслабьте руку: вторая сторона", rest = true),
            AlternationStep(Channel.FLEXOR, 1000L, "Сожмите осторожно: переложите на тарелку", zone = 30..55),
            AlternationStep(Channel.EXTENSOR, 1000L, "Разожмите мягко: отпустите блин", zone = 30..50)
        ),
        attemptLimitMs = 70_000L
    )

    val ALL = listOf(LAUNDRY, PANCAKES)
}
