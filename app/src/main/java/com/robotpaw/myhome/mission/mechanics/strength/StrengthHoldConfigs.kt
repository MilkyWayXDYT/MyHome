package com.robotpaw.myhome.mission.mechanics.strength

object StrengthHoldConfigs {

    // Мусор (лёгкое): держать мешок, пока несёте к баку; уронить - ошибка
    val TRASH = StrengthHoldConfig(
        text = "Сожмите кисть и держите: несите мешок к баку",
        holdMs = 5000L,
        minStrength = 35,
        dropIsError = true
    )

    // Пылесос (среднее): нужна заметная сила и время; отпустить раньше - ошибка
    val VACUUM = StrengthHoldConfig(
        text = "Сожмите сильнее и держите: ведите пылесос по ковру",
        holdMs = 6000L,
        minStrength = 50,
        dropIsError = true
    )

    // Зеркало (среднее): чем сильнее жмёте, тем быстрее протираете
    val MIRROR = StrengthHoldConfig(
        text = "Жмите сильнее, чтобы протирать быстрее",
        holdMs = 5000L,
        minStrength = 20,
        proportional = true,
        attemptLimitMs = 40_000L
    )
}
