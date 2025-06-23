package net.matsudamper.allintoolscreensaver.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.matsudamper.allintoolscreensaver.AlertManager
import net.matsudamper.allintoolscreensaver.CalendarEvent
import net.matsudamper.allintoolscreensaver.ImageManager
import net.matsudamper.allintoolscreensaver.SettingsManager


@Composable
fun ScreenSaverScreen() {
    val context = LocalContext.current
    var showAlertDialog by remember { mutableStateOf(false) }
    val currentAlertState = remember { mutableStateOf<CalendarEvent?>(null) }
    val viewModel = viewModel(
        initializer = {
            DigitalClockScreenViewModel(
                settingsManager = SettingsManager(context),
                imageManager = ImageManager(context),
            )
        },
    )
    val uiStateState = viewModel.uiState.collectAsState()
    val alertManager = remember { AlertManager(context) }
    LifecycleStartEffect(uiStateState.value.listener) {
        val scope = CoroutineScope(Job())
        scope.launch {
            uiStateState.value.listener.onStart()
        }
        onStopOrDispose { scope.cancel() }
    }

    LaunchedEffect(Unit) {
        alertManager.onAlertTriggered = { calendarEvent ->
            currentAlertState.value = calendarEvent
            showAlertDialog = true
        }
        alertManager.startAlertMonitoring()
    }

    DisposableEffect(Unit) {
        onDispose {
            alertManager.cleanup()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.1f),
            ) {
                SlideShowScreen(
                    uri = uiStateState.value.imageUri,
                )
//                modifier = Modifier
//                    .drawWithContent {
//                        graphicsLayer.record {
//                            this@drawWithContent.drawContent()
//                        }
//                        drawLayer(graphicsLayer)
//                    }
//
//                var clockRectState = remember { mutableStateOf<Rect?>(null) }
//                val graphicsLayer = rememberGraphicsLayer()
//                val coroutineScope = rememberCoroutineScope()
//                LaunchedEffect(graphicsLayer) {
//                    graphicsLayer.
//                }

                Clock(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
//                        .onGloballyPositioned {
//                            clockRectState.value = it.boundsInParent()
//                        }
                        .padding(
                            end = 24.dp,
                            bottom = 24.dp,
                        ),
                    date = uiStateState.value.currentDate,
                    time = uiStateState.value.currentTime,
                )
            }

            CalendarDisplayScreen(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.1f),
            )
        }

        // アラートダイアログ
        val currentAlert = currentAlertState.value
        if (showAlertDialog && currentAlert != null) {
            ClockAlertDialog(
                alertTime = currentAlert,
                onDismiss = {
                    showAlertDialog = false
                    currentAlertState.value = null
                },
                alertManager = alertManager,
            )
        }
    }
}

@Composable
private fun Clock(
    date: String,
    time: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = time,
            color = Color.White,
            fontSize = 48.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = date,
            color = Color.Gray,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
        )
    }
}
