package com.robotpaw.myhome

import android.os.Bundle
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.nio.file.WatchEvent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isDebug = (applicationInfo.flags and
                ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyHomeApp(isDebug = isDebug)
                }
            }
        }
    }
}

@Composable
fun MyHomeApp(isDebug: Boolean) {
    var currentScreen by rememberSaveable {
        mutableStateOf("home")
    }

    var developerTapCount by rememberSaveable {
        mutableIntStateOf(0)
    }

    when (currentScreen) {
        "home" -> HomeScreen(
            onTrainingClick = {
                currentScreen = "training"
            },
            onSettingsClick = {
                currentScreen = "settings"
            },
            onVersionClick = {
                if (isDebug) {
                    developerTapCount++

                    if (developerTapCount >= 7) {
                        developerTapCount = 0
                        currentScreen = "developer"
                    }
                }
            }
        )

        "training" -> SimpleScreen(
            title = "Тренировка",
            description = "Здесь появится игровая часть приложения.",
            onBack = { currentScreen = "home" }
        )

        "settings" -> SimpleScreen(
            title = "Настройки",
            description = "Здесь будут настройки приложения и профиля.",
            onBack = { currentScreen = "home" }
        )

        "developer" -> DeveloperScreen(
            onBack = { currentScreen = "home" },
            onEmulatorClick = {
                currentScreen = "emulator"
            },
            onMonitorClick = {
                currentScreen = "monitor"
            },
            onSettingsClick = {
                currentScreen = "dev_settings"
            }
        )

        "emulator" -> SimpleScreen(
            title = "Эмулятор сигналов",
            description = "Здесь будут ползунки, управление DualShock 4 и генерация сигналов.",
            onBack = { currentScreen = "developer" }
        )

        "monitor" -> SimpleScreen(
            title = "Мониторинг",
            description = "Здесь появятся графики и журнал сигналов.",
            onBack = { currentScreen = "developer" }
        )

        "dev_settings" -> SimpleScreen(
            title = "Настройки эмулятора",
            description = "Здесь будут параметры порогов, шумов и усталости.",
            onBack = { currentScreen = "developer" }
        )
    }
}

@Composable
fun HomeScreen(
    onTrainingClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onVersionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Мой дом",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Тренировка и реабилитация",
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.secondary
    )

    Spacer(modifier = Modifier.height(48.dp))

    Button(
        onClick = onTrainingClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Начать тренировку")
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = onSettingsClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Настройка")
    }

    Spacer(modifier = Modifier.height(64.dp))

    Text(
        text = "Версия 0.1.0",
        color = MaterialTheme.colorScheme.secondary,
        fontSize = 12.sp,
        modifier = Modifier.clickable {
            onVersionClick()
        }
    )
}

@Composable
fun DeveloperScreen(
    onBack: () -> Unit,
    onEmulatorClick: () -> Unit,
    onMonitorClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ){
        Text(
            text = "Панель разработчика",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Инструменты тестирования приложения",
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        DeveloperButton(
            title = "Эмулятор сигналов",
            description = "Ползунки и управление геймпадом",
            onClick = onEmulatorClick
        )

        DeveloperButton(
            title = "Мониторинг сигналов",
            description = "Состояния каналов и журнал событий",
            onClick = onMonitorClick
        )

        DeveloperButton(
            title = "Настройки эмулятора",
            description = "Пороги, шумы и усталость",
            onClick = onSettingsClick
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Назад")
        }
    }
}

@Composable
fun DeveloperButton(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable{onClick()}
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun SimpleScreen(
    title: String,
    description: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(onClick = onBack) {
            Text("Назад")
        }
    }
}
