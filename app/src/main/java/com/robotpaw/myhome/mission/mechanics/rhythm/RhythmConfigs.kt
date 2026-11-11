package com.robotpaw.myhome.mission.mechanics.rhythm

import com.robotpaw.myhome.signal.Channel

object RhythmConfigs {

    // Салат (сложное): восемь движений ножа в такт, «вниз - вверх» (миссия «В ритм»)
    val SALAD = RhythmConfig(
        beats = List(8) { if (it % 2 == 0) Channel.FLEXOR else Channel.EXTENSOR },
        noviceIntervalMs = 1800L,
        experiencedIntervalMs = 1200L,
        experiencedMaxMisses = 2
    )
}
