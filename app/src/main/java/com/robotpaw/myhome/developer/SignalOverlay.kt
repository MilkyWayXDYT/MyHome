package com.robotpaw.myhome.developer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotpaw.myhome.emulator.EmulatorPreset
import com.robotpaw.myhome.emulator.EmulatorSignalSource
import com.robotpaw.myhome.signal.Channel
import com.robotpaw.myhome.signal.NoiseSettings
import kotlin.reflect.KProperty

/**
 * Панель эмулятора.
 *
 * @param stateCode код состояния после обработки игрой (с порогами профиля).
 * @param latencyMs задержка от выдачи кадра до кадра отрисовки.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignalOverlay(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    emulator: EmulatorSignalSource,
    stateCode: String,
    latencyMs: Long?,
    modifier: Modifier = Modifier
) {
    val raw by emulator.raw.collectAsState()
    val settings by emulator.settings.collectAsState()
    val frame by emulator.frames.collectAsState()

    val flexor = raw.flexor
    val extensor = raw.extensor
    val onFlexorChange: (Int) -> Unit = { emulator.setChannel(Channel.FLEXOR, it) }
    val onExtensorChange: (Int) -> Unit = { emulator.setChannel(Channel.EXTENSOR, it) }

    var noiseSettings by EmulatorField({ settings.noise }) { v ->
        emulator.updateSettings { it.copy(noise = v) }
    }

    var fatigueEnabled by EmulatorField({ settings.fatigueEnabled }) { v ->
        emulator.updateSettings { it.copy(fatigueEnabled = v) }
    }

    var fatigueLevel by EmulatorField({ settings.fatigueLevel }) { v ->
        emulator.updateSettings { it.copy(fatigueLevel = v) }
    }

    val signal = OverlaySignal(
        rawFlexor = flexor,
        rawExtensor = extensor,
        flexor = frame.flexor,
        extensor = frame.extensor,
        stateCode = stateCode
    )

    val activeNoiseNames = listOfNotNull(
        "Колебания".takeIf { noiseSettings.oscillation },
        "Пропадания".takeIf { noiseSettings.dropout },
        "Резкие скачки".takeIf { noiseSettings.spikes },
        "Фоновый шум".takeIf { noiseSettings.background }
    )

    if (!expanded) {
        FilledTonalButton(
            onClick = { onExpandedChange(true) },
            modifier = modifier
        ) {
            Text("⚙ Эмулятор")
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 700.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Эмулятор сигналов",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = { onExpandedChange(false) }
                    ) {
                        Text("Свернуть")
                    }
                }

                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // БЫСТРЫЕ НАБОРЫ ПОМЕХ

                Text(
                    "Быстрые наборы",
                    fontWeight = FontWeight.Bold
                )

                // Наборов пять — переносим кнопки на следующую строку, если не влезают.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EmulatorPreset.entries.forEach { preset ->
                        OutlinedButton(onClick = { emulator.applyPreset(preset) }) {
                            Text(preset.title)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text(
                    "Исходные сигналы",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Клавиатура: F — сгибатель, J — разгибатель, с Shift — сильнее. Геймпад: L2 / R2.",
                    fontSize = 12.sp
                )

                Text(
                    "Сгибатель: $flexor%",
                    fontWeight = FontWeight.SemiBold
                )

                Slider(
                    value = flexor.toFloat(),
                    onValueChange = { onFlexorChange(it.toInt()) },
                    valueRange = 0f..100f
                )

                Text(
                    "Разгибатель: $extensor%",
                    fontWeight = FontWeight.SemiBold
                )

                Slider(
                    value = extensor.toFloat(),
                    onValueChange = { onExtensorChange(it.toInt()) },
                    valueRange = 0f..100f
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // ПЕРЕКРЁСТНАЯ НАВОДКА

                Text(
                    "Перекрёстная наводка: ${settings.crosstalkPercent}%",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Часть сигнала одного канала попадает в другой — так выглядит встречный сигнал новичка.",
                    fontSize = 12.sp
                )

                Slider(
                    value = settings.crosstalkPercent.toFloat(),
                    onValueChange = { v ->
                        emulator.updateSettings { it.copy(crosstalkPercent = v.toInt()) }
                    },
                    valueRange = 0f..60f
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // НАСТРОЙКИ ШУМОВ

                Text(
                    "Генератор шумов",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Включить шумы")

                    Switch(
                        checked = noiseSettings.enabled,
                        onCheckedChange = {
                            noiseSettings = noiseSettings.copy(
                                enabled = it
                            )
                        }
                    )
                }

                NoiseOption(
                    title = "Колебания",
                    checked = noiseSettings.oscillation,
                    onCheckedChange = {
                        noiseSettings = noiseSettings.copy(
                            oscillation = it
                        )
                    }
                )

                NoiseOption(
                    title = "Пропадания сигнала",
                    checked = noiseSettings.dropout,
                    onCheckedChange = {
                        noiseSettings = noiseSettings.copy(
                            dropout = it
                        )
                    }
                )

                NoiseOption(
                    title = "Резкие скачки",
                    checked = noiseSettings.spikes,
                    onCheckedChange = {
                        noiseSettings = noiseSettings.copy(
                            spikes = it
                        )
                    }
                )

                NoiseOption(
                    title = "Фоновый шум",
                    checked = noiseSettings.background,
                    onCheckedChange = {
                        noiseSettings = noiseSettings.copy(
                            background = it
                        )
                    }
                )

                Text(
                    "Интенсивность шумов: ${noiseSettings.intensity}%",
                    fontWeight = FontWeight.SemiBold
                )

                Slider(
                    value = noiseSettings.intensity.toFloat(),
                    onValueChange = {
                        noiseSettings = noiseSettings.copy(
                            intensity = it.toInt()
                        )
                    },
                    valueRange = 0f..100f
                )

                // ОТДЕЛЬНОЕ ОТОБРАЖЕНИЕ ПОМЕХ

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            "Монитор помех",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            when {
                                !noiseSettings.enabled ->
                                    "Помехи выключены"

                                activeNoiseNames.isEmpty() ->
                                    "Не выбран ни один тип помех"

                                else ->
                                    "Активные типы: ${activeNoiseNames.joinToString()}"
                            }
                        )

                        Text(
                            "Изменение сгибателя: ${
                                signal.flexor - signal.rawFlexor
                            }"
                        )

                        Text(
                            "Изменение разгибателя: ${
                                signal.extensor - signal.rawExtensor
                            }"
                        )
                    }
                }

                TextButton(
                    onClick = {
                        noiseSettings = NoiseSettings()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Сбросить шумы")
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // ОТОБРАЖЕНИЕ УСТАЛОСТИ

                Text(
                    "Усталость",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Включить усталость")

                    Switch(
                        checked = fatigueEnabled,
                        onCheckedChange = {
                            fatigueEnabled = it
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Нарастает сама при нагрузке")

                    Switch(
                        checked = settings.fatigueAuto,
                        enabled = fatigueEnabled,
                        onCheckedChange = { checked ->
                            emulator.updateSettings { it.copy(fatigueAuto = checked) }
                        }
                    )
                }

                Text(
                    if (fatigueEnabled) {
                        "Уровень усталости: $fatigueLevel%"
                    } else {
                        "Усталость не активна"
                    },
                    fontWeight = FontWeight.SemiBold
                )

                Slider(
                    value = fatigueLevel.toFloat(),
                    onValueChange = {
                        fatigueLevel = it.toInt()
                    },
                    valueRange = 0f..100f,
                    enabled = fatigueEnabled && !settings.fatigueAuto
                )

                LinearProgressIndicator(
                    progress = fatigueLevel / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )

                Text(
                    when {
                        !fatigueEnabled -> "Состояние: выключена"
                        fatigueLevel < 25 -> "Состояние: минимальная"
                        fatigueLevel < 50 -> "Состояние: умеренная"
                        fatigueLevel < 75 -> "Состояние: выраженная"
                        else -> "Состояние: высокая"
                    },
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // РЕЗУЛЬТАТ ОБРАБОТКИ

                Text(
                    "Результат обработки сигнала",
                    fontWeight = FontWeight.Bold
                )

                Text("Исходный сгибатель: ${signal.rawFlexor}%")
                Text("Исходный разгибатель: ${signal.rawExtensor}%")

                Spacer(Modifier.height(6.dp))

                Text(
                    "После помех — сгибатель: ${signal.flexor}%",
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    "После помех — разгибатель: ${signal.extensor}%",
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    "Задержка сигнал → кадр: ${latencyMs?.let { "$it мс" } ?: "—"} (по ТЗ — до 100 мс)",
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Распознанное состояние (с порогами профиля)",
                                fontSize = 13.sp
                            )

                            Text(
                                when (signal.stateCode) {
                                    "00" -> "Покой"
                                    "10" -> "Сгибание"
                                    "01" -> "Разгибание"
                                    "11" -> "Активны оба канала"
                                    else -> "Неизвестное состояние"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            signal.stateCode,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        onFlexorChange(0)
                        onExtensorChange(0)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Сбросить сигналы")
                }
            }
        }
    }
}

/**Блок «Результат обработки сигнала». */
private data class OverlaySignal(
    val rawFlexor: Int,
    val rawExtensor: Int,
    val flexor: Int,
    val extensor: Int,
    val stateCode: String
)

private class EmulatorField<T>(
    private val read: () -> T,
    private val write: (T) -> Unit
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = read()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = write(value)
}

@Composable
private fun NoiseOption(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title)

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}