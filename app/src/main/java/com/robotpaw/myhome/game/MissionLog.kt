package com.robotpaw.myhome.game

/**
 * Лог выполнения задания: по нему считаются статистика на экране результата и миссии.
 * Заполняется через MissionRecorder, сами механики числа не считают.
 * @param durationMs - сколько времени игрок провёл в задании (без пауз)
 * @param activeMs - сколько времени был активен нужный заданию канал
 * @param coContractionMs - сколько времени были напряжены оба канала (код 11)
 * @param coContractionEvents - сколько раз начался встречный сигнал
 * @param errors - ошибки самой механики (пролил, промахнулся, отпустил раньше времени)
 * @param pauses - сколько раз открывалось окно поддержки
 * @param peakStrength - максимальная сила нужного канала (0..100)
 * @param strengthSum - сумма силы по кадрам, пока канал активен
 * @param strengthFrames - число кадров, пока канал активен
 * @param bestStreak - лучшая серия попаданий подряд (ритм-задания, миссия «В ритм»)
 * @param interruptions - срывы сигнала: нужный канал был включён и пропал больше чем на 0,2 с
 * @param wrongChannelEvents - сколько раз игрок напрягал не ту мышцу вместо нужной
 * @param offTargetMs - сколько времени сигнал был «мимо»: после срыва или не тот канал
 * @param jitterSum - сумма скачков силы между соседними кадрами, пока нужный канал активен
 * @param jitterFrames - сколько таких скачков измерено
 * @param longestRestMs - самая долгая пауза в покое (обе мышцы расслаблены) посреди задания, для миссии «Замри»
 */
data class MissionLog(
    val durationMs: Long = 0L,
    val activeMs: Long = 0L,
    val coContractionMs: Long = 0L,
    val coContractionEvents: Int = 0,
    val errors: Int = 0,
    val pauses: Int = 0,
    val peakStrength: Int = 0,
    val strengthSum: Long = 0L,
    val strengthFrames: Int = 0,
    val bestStreak: Int = 0,
    val interruptions: Int = 0,
    val wrongChannelEvents: Int = 0,
    val offTargetMs: Long = 0L,
    val jitterSum: Long = 0L,
    val jitterFrames: Int = 0,
    val longestRestMs: Long = 0L
) {
    // средняя сила нужного канала, пока он был активен (0..100)
    val averageStrength: Int
        get() = if (strengthFrames == 0) 0 else (strengthSum / strengthFrames).toInt()

    // доля времени с чистым сигналом: без встречного сигнала, срывов и не того канала (0..100)
    val cleanPercent: Int
        get() = if (durationMs <= 0L) 100
        else (100 - (coContractionMs + offTargetMs) * 100 / durationMs).toInt().coerceIn(0, 100)

    // ровность нажатия (0..100): 100 - сила не скачет, чем больше скачков между кадрами (шум, дрожание), тем ниже
    // 12 - коэффициент: средний скачок в 8 процентов за кадр даёт 0
    val steadinessPercent: Int
        get() = if (jitterFrames == 0) 100
        else (100.0 - jitterSum * 12.0 / jitterFrames).toInt().coerceIn(0, 100)

    // все «ошибки сигнала» вместе: встречные сигналы, срывы, не тот канал, ошибки самой механики
    val mistakes: Int get() = errors + coContractionEvents + interruptions + wrongChannelEvents

    // задание выполнено без единой ошибки
    val isFlawless: Boolean get() = mistakes == 0
}

/**
 * Состояние задания на момент паузы, чтобы после окна поддержки продолжить с того же места
 * @param progress - прогресс механики (0..1)
 * @param log - лог, накопленный до паузы
 */
data class MissionCheckpoint(
    val progress: Float = 0f,
    val log: MissionLog = MissionLog()
)