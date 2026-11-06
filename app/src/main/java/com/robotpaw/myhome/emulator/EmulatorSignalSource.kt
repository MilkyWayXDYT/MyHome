package com.robotpaw.myhome.emulator

import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.NoiseGenerator
import com.robotpaw.myhome.signal.NoiseSettings
import com.robotpaw.myhome.signal.SignalData
import com.robotpaw.myhome.signal.SignalSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * входной сигнал с эмулятора
 */
data class RawInput(val flexor: Int = 0, val extensor: Int = 0)


/**
 * Настройка эмулятора
 *
 * @param crosstalkPercent - встречный сигнал
 * @param fatigueAuto - усталось нарастает сама, пока канал держат и спадает в покое
 * @param fatigueLevel - уровень усталости (0..100)
 */
data class EmulatorSettings(
    val noise: NoiseSettings = NoiseSettings(),
    val crosstalkPercent: Int = 0,
    val fatigueEnabled: Boolean = false,
    val fatigueAuto: Boolean = false,
    val fatigueLevel: Int = 0
)

/** Готовые наборы помех в различных случаях */
enum class EmulatorPreset(val title: String, val settings: EmulatorSettings){
    CLEAN("Чистый", EmulatorSettings()),
    EXPERIENCED(
        "Опытный",
        EmulatorSettings(
            noise = NoiseSettings(true, background = true, intensity = 20),
            crosstalkPercent = 5
        )
    ),
    NOVICE(
        "Новичок",
        EmulatorSettings(
            noise = NoiseSettings(true, true, background = true, intensity = 40),
            crosstalkPercent = 35,
            fatigueEnabled = true,
            fatigueAuto = true
        )
    ),
    LOOSE_SOCKET(
        "Болтается гильза",
        EmulatorSettings(
            noise = NoiseSettings(true, dropout = true, spikes = true, intensity = 60)
        )
    ),
    CHAFING(
        "Натирает",
        EmulatorSettings(
            noise = NoiseSettings(true, background = true, oscillation = true, intensity = 70)
        )
    )
}

class EmulatorSignalSource (
    private val scope: CoroutineScope,
    private val tickMs: Long = 20L,
    private val noiseGenerator: NoiseGenerator = NoiseGenerator(),
    private val clock: () -> Long = {System.nanoTime() / 1_000_000}
) : SignalSource {
    private val _raw = MutableStateFlow(RawInput())
    val raw: StateFlow<RawInput> = _raw.asStateFlow()

    private val _settings = MutableStateFlow(EmulatorSettings())
    val settings: StateFlow<EmulatorSettings> = _settings.asStateFlow()

    private val _frames = MutableStateFlow(SignalData(clock(), 0, 0))
    override val frames: StateFlow<SignalData> = _frames.asStateFlow()

    private var job: Job? = null
    private var autoFatigue = 0f

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive){
                _frames.value = produce(clock())
                delay(tickMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun setRaw(flexor: Int, extensor: Int) {
        _raw.value = RawInput(flexor.coerceIn(0, 100), extensor.coerceIn(0, 100))
    }

    fun setChannel(channel: Channel, value: Int){
        val v = value.coerceIn(0, 100)
        _raw.update {
            when (channel) {
                Channel.FLEXOR -> it.copy(flexor = v)
                Channel.EXTENSOR -> it.copy(extensor = v)
            }
        }
    }

    fun updateSettings(transform: (EmulatorSettings) -> EmulatorSettings) {
        _settings.update(transform)
    }

    fun applyPreset(preset: EmulatorPreset){
        autoFatigue = 0f
        _settings.value = preset.settings
    }
    internal fun produce(now: Long): SignalData{
        val input = _raw.value
        val s = _settings.value

        val c = s.crosstalkPercent.coerceIn(0, 100) / 100f
        var f = input.flexor + input.extensor * c
        var e = input.extensor + input.flexor * c

        if(s.fatigueEnabled){
            val level = if (s.fatigueAuto) updateAutoFatigue(input)
                        else s.fatigueLevel / 100f
            val gain = 1f - 0.5f * level
            val tremor = (sin(now / 1000.0 * 2.0 * PI * 9.0) * 8.0 * level).toFloat()

            if (f > 0f) f = f * gain + tremor
            if (e > 0f) e = e * gain + tremor
        }

        val (noisyF, noisyE) = noiseGenerator.process(
            flexor = f.roundToInt().coerceIn(0,100),
            extensor = e.roundToInt().coerceIn(0,100),
            settings = s.noise,
            elapsedMillis = now
        )

        return SignalData(timestampMs = now, flexor = noisyF, extensor = noisyE)

    }

    private fun updateAutoFatigue(input: RawInput): Float {
        val working = input.flexor > 30 || input.extensor > 30
        val perTick = tickMs / 1000f
        autoFatigue = if(working) autoFatigue + 0.05f * perTick
                      else autoFatigue - 0.10f * perTick
        autoFatigue = autoFatigue.coerceIn(0f, 1f)
        val shown = (autoFatigue * 100).roundToInt()

        if (shown != _settings.value.fatigueLevel) {
            _settings.update { it.copy(fatigueLevel = shown)}
        }
        return autoFatigue
    }
}