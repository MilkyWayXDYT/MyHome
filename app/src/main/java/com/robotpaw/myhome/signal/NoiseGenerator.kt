
package com.robotpaw.myhome.signal

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

data class NoiseSettings(
    val enabled: Boolean = false,
    val oscillation: Boolean = false,
    val dropout: Boolean = false,
    val spikes: Boolean = false,
    val background: Boolean = false,
    val intensity: Int = 30
)

// Результат обработки одного канала
data class ChannelNoiseResult(
    val input: Int,
    val output: Int,
    val difference: Int,
    val activeEffects: List<String>
)

// Подробный результат обработки двух каналов
data class NoiseProcessResult(
    val flexor: ChannelNoiseResult,
    val extensor: ChannelNoiseResult
)

class NoiseGenerator {

    // Основная функция с подробным результатом
    fun processDetailed(
        flexor: Int,
        extensor: Int,
        settings: NoiseSettings,
        elapsedMillis: Long
    ): NoiseProcessResult {

        val safeFlexor = flexor.coerceIn(0, 100)
        val safeExtensor = extensor.coerceIn(0, 100)

        if (!settings.enabled) {
            return NoiseProcessResult(
                flexor = unchanged(safeFlexor),
                extensor = unchanged(safeExtensor)
            )
        }

        val strength = settings.intensity.coerceIn(0, 100) / 100f

        fun applyNoise(value: Int, offset: Long): ChannelNoiseResult {
            var result = value.toFloat()
            val time = elapsedMillis + offset
            val effects = mutableListOf<String>()

            if (settings.oscillation) {
                val wave = sin(time / 250.0 * 2.0 * PI).toFloat()
                val change = wave * 15f * strength
                result += change

                if (change != 0f) effects.add("Колебания")
            }

            if (settings.dropout) {
                val phase = time % 4000L

                if (phase in 1500L..1750L) {
                    result *= (1f - strength)
                    effects.add("Выпадение сигнала")
                }
            }

            if (settings.spikes) {
                val phase = time % 3000L

                if (phase in 500L..580L) {
                    result += 40f * strength
                    effects.add("Пиковая помеха")
                }
            }

            if (settings.background) {
                val bucket = time / 80L
                val random = Random(bucket + offset)
                val noise = random.nextFloat() * 2f - 1f
                val change = noise * 10f * strength

                result += change
                effects.add("Фоновый шум")
            }

            val output = result.toInt().coerceIn(0, 100)

            return ChannelNoiseResult(
                input = value,
                output = output,
                difference = output - value,
                activeEffects = effects
            )
        }

        return NoiseProcessResult(
            flexor = applyNoise(safeFlexor, 0L),
            extensor = applyNoise(safeExtensor, 1000L)
        )
    }

    // Сохраняем прежний интерфейс, чтобы не ломать SignalProcessor
    fun process(
        flexor: Int,
        extensor: Int,
        settings: NoiseSettings,
        elapsedMillis: Long
    ): Pair<Int, Int> {
        val result = processDetailed(
            flexor,
            extensor,
            settings,
            elapsedMillis
        )

        return Pair(
            result.flexor.output,
            result.extensor.output
        )
    }

    private fun unchanged(value: Int) = ChannelNoiseResult(
        input = value,
        output = value,
        difference = 0,
        activeEffects = emptyList()
    )
}
