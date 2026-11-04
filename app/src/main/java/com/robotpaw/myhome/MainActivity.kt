
package com.robotpaw.myhome

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.robotpaw.myhome.developer.SignalOverlay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isDebug = (applicationInfo.flags and
                ApplicationInfo.FLAG_DEBUGGABLE) != 0

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

    var flexor by rememberSaveable {
        mutableIntStateOf(0)
    }

    var extensor by rememberSaveable {
        mutableIntStateOf(0)
    }

    var overlayExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Главная страница
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "МОЙ ДОМ",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Эмулятор доступен только после скрытого входа
        if (isDebug) {

            SignalOverlay(
                expanded = overlayExpanded,

                onExpandedChange = {
                    overlayExpanded = it
                },

                flexor = flexor,
                extensor = extensor,

                onFlexorChange = {
                    flexor = it
                },

                onExtensorChange = {
                    extensor = it
                },

                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
            )
        }
    }
}
