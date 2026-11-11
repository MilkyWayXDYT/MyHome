package com.robotpaw.myhome.mission.mechanics

import com.robotpaw.myhome.game.adapt.Adaptation
import com.robotpaw.myhome.mission.mechanics.alternation.AlternationConfig
import com.robotpaw.myhome.mission.mechanics.rhythm.RhythmConfig
import com.robotpaw.myhome.mission.mechanics.strength.StrengthHoldConfig

// Подстройка настроек механик под игрока. Сами механики и их правила не меняются:
// они получают те же настройки, только с другими числами. level 0 - настройки как есть

/**
 * Чередование, точность, смешанные: время шагов, зона силы, время на попытку
 */
fun AlternationConfig.adapted(level: Int): AlternationConfig {
    if (level == 0) return this
    return copy(
        steps = steps.map { step ->
            step.copy(
                holdMs = Adaptation.holdMs(step.holdMs, level),
                zone = step.zone?.let { Adaptation.zone(it, level) }
            )
        },
        attemptLimitMs = Adaptation.limitMs(attemptLimitMs, level)
    )
}

/**
 * Сила и удержание: сколько держать, какая нужна сила, время на попытку
 */
fun StrengthHoldConfig.adapted(level: Int): StrengthHoldConfig {
    if (level == 0) return this
    return copy(
        holdMs = Adaptation.holdMs(holdMs, level),
        minStrength = Adaptation.minStrength(minStrength, level),
        attemptLimitMs = Adaptation.limitMs(attemptLimitMs, level)
    )
}

/**
 * Ритм: темп и сколько промахов можно
 */
fun RhythmConfig.adapted(level: Int): RhythmConfig {
    if (level == 0) return this
    return copy(
        noviceIntervalMs = Adaptation.intervalMs(noviceIntervalMs, level),
        experiencedIntervalMs = Adaptation.intervalMs(experiencedIntervalMs, level),
        experiencedMaxMisses = Adaptation.maxMisses(experiencedMaxMisses, level)
    )
}