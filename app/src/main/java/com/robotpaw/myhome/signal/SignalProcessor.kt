
package com.robotpaw.myhome.signal

class SignalProcessor(
    private val noiseGenerator: NoiseGenerator = NoiseGenerator()
) {

    fun process(
        rawFlexor: Int,
        rawExtensor: Int,
        noiseSettings: NoiseSettings,
        elapsedMillis: Long
    ): SignalData {

        val processed = noiseGenerator.process(
            flexor = rawFlexor,
            extensor = rawExtensor,
            settings = noiseSettings,
            elapsedMillis = elapsedMillis
        )

        val state = calculateSignalState(
            flexor = processed.first,
            extensor = processed.second
        )

        return SignalData(
            rawFlexor = rawFlexor.coerceIn(0, 100),
            rawExtensor = rawExtensor.coerceIn(0, 100),
            flexor = state.flexor,
            extensor = state.extensor,
            stateCode = state.code,
            noiseEnabled = noiseSettings.enabled
        )
    }
}
