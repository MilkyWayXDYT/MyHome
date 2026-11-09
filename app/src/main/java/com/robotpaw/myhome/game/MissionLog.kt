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
    val bestStreak: Int = 0
) {
    // средняя сила нужного канала, пока он был активен (0..100)
    val averageStrength: Int
        get() = if (strengthFrames == 0) 0 else (strengthSum / strengthFrames).toInt()

    // доля времени без встречного сигнала (0..100)
    val cleanPercent: Int
        get() = if (durationMs <= 0L) 100 else (100 - coContractionMs * 100 / durationMs).toInt().coerceIn(0, 100)

    // задание выполнено без единой ошибки и встречного сигнала
    val isFlawless: Boolean get() = errors == 0 && coContractionEvents == 0
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
