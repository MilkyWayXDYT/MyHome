package com.robotpaw.myhome.developer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import com.robotpaw.myhome.signal.calculateSignalState

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
    val signal = calculateSignalState(
        flexor = flexor,
        extensor = extensor
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
                modifier = Modifier.padding(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Эмулятор сигналов",
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Сгибатель: $flexor%",
                    fontWeight = FontWeight.SemiBold
                )

                Slider(
                    value = flexor.toFloat(),
                    onValueChange = {
                        onFlexorChange(it.toInt())
                    },
                    valueRange = 0f..100f
                )

                Text(
                    text = "Разгибатель: $extensor%",
                    fontWeight = FontWeight.SemiBold
                )

                Slider(
                    value = extensor.toFloat(),
                    onValueChange = {
                        onExtensorChange(it.toInt())
                    },
                    valueRange = 0f..100f
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Распознанное состояние",
                                fontSize = 13.sp
                            )

                            Text(
                                text = signal.description,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = signal.code,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
