package com.robotpaw.myhome.mission.mechanics.rhythm

import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.SignalState

/**
 * Настройки ритм-задания
 * @param beats - какой канал нужен на каждом ударе по порядку
 * @param noviceIntervalMs - длина одного удара для новичка (медленнее)
 * @param experiencedIntervalMs - длина одного удара для опытного
 * @param experiencedMaxMisses - сколько промахов допустимо опытному; у новичка задание проходится при любом числе промахов
 * @param hitMs - сколько нужно держать нужный канал, чтобы удар засчитался (защита от случайных дёрганий)
 * @param countdownMs - время на подготовку перед первым ударом
 */
data class RhythmConfig(
    val beats: List<Channel>,
    val noviceIntervalMs: Long,
    val experiencedIntervalMs: Long,
    val experiencedMaxMisses: Int,
    val hitMs: Long = 120L,
    val countdownMs: Long = 1500L
)

/**
 * Что произошло на кадре
 * @param hit - удар засчитан
 * @param miss - удар закончился без попадания
 */
data class RhythmEvent(val hit: Boolean = false, val miss: Boolean = false)

/**
 * Правила ритм-заданий: удары идут в постоянном темпе, на каждом нужно напрячь нужный канал
 * Удар засчитывается, когда нужный канал (и только он) держали hitMs; темп идёт дальше в любом случае
 * Прогресс только растёт: промах не откатывает его назад
 * Соседние удары должны использовать разные каналы, иначе удержание с прошлого удара засчитается само
 * Чистая логика без Compose, чтобы проверять юнит-тестами
 */
class RhythmRules(
    val beats: List<Channel>,
    val intervalMs: Long,
    private val maxMisses: Int,
    private val hitMs: Long = 120L,
    countdownMs: Long = 0L
) {
    var beatIndex: Int = 0
        private set
    var beatElapsedMs: Long = 0L
        private set
    var countdownLeftMs: Long = countdownMs
        private set
    var hits: Int = 0
        private set
    var misses: Int = 0
        private set
    var streak: Int = 0
        private set
    var bestStreak: Int = 0
        private set

    private var heldMs = 0L
    private var resolved = false

    val isFinished: Boolean get() = beatIndex >= beats.size
    val isSuccess: Boolean get() = isFinished && misses <= maxMisses
    val currentChannel: Channel? get() = if (countdownLeftMs > 0L) null else beats.getOrNull(beatIndex)

    // доля удара, которая уже прошла (0..1)
    val beatFraction: Float get() = (beatElapsedMs.toFloat() / intervalMs).coerceIn(0f, 1f)

    // общий прогресс по ударам (0..1)
    val progress: Float
        get() = if (isFinished) 1f else ((beatIndex + beatFraction) / beats.size).coerceIn(0f, 1f)

    fun update(state: SignalState, dtMs: Long): RhythmEvent {
        if (isFinished) return RhythmEvent()
        if (countdownLeftMs > 0L) {
            countdownLeftMs = (countdownLeftMs - dtMs).coerceAtLeast(0L)
            return RhythmEvent()
        }

        val required = beats[beatIndex]
        val match = when (required) {
            Channel.FLEXOR -> state.code == "10"
            Channel.EXTENSOR -> state.code == "01"
        }

        var hit = false
        if (match) {
            heldMs += dtMs
            if (!resolved && heldMs >= hitMs) {
                resolved = true
                hits++
                streak++
                if (streak > bestStreak) bestStreak = streak
                hit = true
            }
        } else {
            heldMs = 0L
        }

        beatElapsedMs += dtMs
        var miss = false
        if (beatElapsedMs >= intervalMs) {
            if (!resolved) {
                misses++
                streak = 0
                miss = true
            }
            beatIndex++
            beatElapsedMs = 0L
            heldMs = 0L
            resolved = false
        }
        return RhythmEvent(hit = hit, miss = miss)
    }
}
