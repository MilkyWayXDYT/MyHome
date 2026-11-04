package com.robotpaw.myhome.developer

import android.os.SystemClock
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
import kotlinx.coroutines.delay
import com.robotpaw.myhome.signal.NoiseSettings
import com.robotpaw.myhome.signal.SignalProcessor

@Composable
fun SignalOverlay(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    flexor: Int,
    extensor: Int,
    onFlexorChange: (Int) -> Unit,
    onExtensorChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var noiseSettings by remember {
        mutableStateOf(NoiseSettings())
    }

    var elapsedMillis by remember {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }

    // Усталость — отдельный параметр эмулятора.
    var fatigueEnabled by remember {
        mutableStateOf(false)
    }

    var fatigueLevel by remember {
        mutableIntStateOf(0)
    }

    val processor = remember {
        SignalProcessor()
    }

    // Обновляем время, чтобы шумы менялись непрерывно.
    LaunchedEffect(noiseSettings.enabled) {
        if (noiseSettings.enabled) {
            while (true) {
                elapsedMillis = SystemClock.elapsedRealtime()
                delay(50)
            }
        }
    }

    val signal = processor.process(
        rawFlexor = flexor,
        rawExtensor = extensor,
        noiseSettings = noiseSettings,
        elapsedMillis = elapsedMillis
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

                Text(
                    "Исходные сигналы",
                    fontWeight = FontWeight.Bold
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
                    enabled = fatigueEnabled
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
                                "Распознанное состояние",
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
