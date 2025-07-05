package net.matsudamper.allintoolscreensaver.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import net.matsudamper.allintoolscreensaver.ImageManager
import net.matsudamper.allintoolscreensaver.compose.calendar.CalendarDisplayScreen
import net.matsudamper.allintoolscreensaver.compose.component.DreamAlertDialog
import net.matsudamper.allintoolscreensaver.compose.component.DreamDialogHost
import net.matsudamper.allintoolscreensaver.compose.component.SuspendLifecycleStartEffect
import net.matsudamper.allintoolscreensaver.compose.eventalert.EventAlertViewModel
import net.matsudamper.allintoolscreensaver.viewmodel.DigitalClockScreenViewModel
import org.koin.core.context.GlobalContext

@Composable
fun ScreenSaverScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val digitalClockViewModel: DigitalClockScreenViewModel = viewModel(
        initializer = {
            val koin = GlobalContext.get()
            DigitalClockScreenViewModel(
                settingsRepositor = koin.get(),
                imageManager = ImageManager(context),
                inMemoryCache = koin.get(),
                clock = koin.get(),
            )
        },
    )
    val eventAlertViewModel: EventAlertViewModel = viewModel(
        initializer = {
            val koin = GlobalContext.get()
            EventAlertViewModel(
                alertManager = koin.get(),
            )
        },
    )
    val digitalClockUiState = digitalClockViewModel.uiState.collectAsState()
    val eventAlertUiState = eventAlertViewModel.uiState.collectAsState()
    val hazeState = rememberHazeState()

    SuspendLifecycleStartEffect(digitalClockUiState.value.listener) {
        digitalClockUiState.value.listener.onStart()
    }

    SuspendLifecycleStartEffect(eventAlertUiState.value.listener) {
        eventAlertUiState.value.listener.onStart()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            val clockRectState = remember { mutableStateOf<Rect?>(null) }
            val graphicsLayer = rememberGraphicsLayer()
            val pageChanged = remember { Channel<Unit>(Channel.CONFLATED) }

            var isWhite by remember { mutableStateOf(false) }
            LaunchedEffect(clockRectState) {
                snapshotFlow { clockRectState.value }
                    .combine(pageChanged.receiveAsFlow()) { rect, _ -> rect }
                    .filterNotNull()
                    .collectLatest { rect ->
                        val imageBitmap = graphicsLayer.toImageBitmap()
                        isWhite = isWhite(
                            rect = rect,
                            imageBitmap = imageBitmap,
                        )
                    }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.1f),
            ) {
                SlideShowScreen(
                    modifier = Modifier
                        .hazeSource(hazeState)
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        },
                    pagerItems = digitalClockUiState.value.pagerItems,
                    onPageChange = { newPage ->
                        digitalClockUiState.value.listener.onPageChanged(newPage)
                    },
                    onPageChanged = {
                        pageChanged.trySend(Unit)
                    },
                    imageSwitchIntervalSeconds = digitalClockUiState.value.imageSwitchIntervalSeconds,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .onGloballyPositioned {
                            clockRectState.value = it.boundsInParent()
                        }
                        .padding(
                            end = 12.dp,
                            bottom = 12.dp,
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle.Unspecified.copy(
                                blurRadius = 8.dp,
                            ),
                        ),
                ) {
                    val animatedBackgroundColor by animateColorAsState(
                        targetValue = if (isWhite) {
                            Color.Black.copy(alpha = 0.4f)
                        } else {
                            Color.Transparent
                        },
                        label = "clock_background_animation",
                    )

                    Column(
                        modifier = Modifier
                            .background(color = animatedBackgroundColor)
                            .padding(
                                horizontal = 12.dp,
                                vertical = 12.dp,
                            ),
                    ) {
                        Clock(
                            date = digitalClockUiState.value.currentDate,
                            time = digitalClockUiState.value.currentTime,
                        )
                    }
                }
            }

            CalendarDisplayScreen(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.1f),
                contentWindowInsets = WindowInsets(),
            )
        }

        val currentAlert = eventAlertUiState.value.currentAlert
        if (currentAlert != null) {
            DreamAlertDialog(
                title = {
                    Column {
                        Text(
                            text = currentAlert.event.title,
                        )
                        Text(
                            text = "${currentAlert.alertType.displayText}のアラート",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                dismissRequest = {
                    eventAlertUiState.value.listener.onAlertDismiss()
                },
                negativeButton = {
                    Text(text = "CLOSE")
                },
                onClickNegative = {
                    eventAlertUiState.value.listener.onAlertDismiss()
                },
                positiveButton = null,
            ) {
                Column {
                    Text(
                        text = currentAlert.eventStartTimeText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentAlert.isRepeatingAlert) {
                        Text(
                            text = "※ このアラートは10秒おきに5分間繰り返されます",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(text = currentAlert.event.description.orEmpty())
                }
            }
        }

        DreamDialogHost()
    }
}

private fun isWhite(
    rect: Rect,
    imageBitmap: ImageBitmap,
): Boolean {
    val bitmap = run {
        val bitmap = imageBitmap.asAndroidBitmap()
        if (bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
            bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        } else {
            bitmap
        }
    }

    val step = 4
    val colorArray = IntArray((rect.width).toInt() * (rect.height).toInt()).also { array ->
        bitmap.getPixels(
            array,
            0,
            rect.width.toInt(),
            rect.left.toInt(),
            rect.top.toInt(),
            rect.width.toInt(),
            rect.height.toInt(),
        )
    }.toList().windowed(size = step, step = step).map { it.first() }

    val vList = run {
        val hsvArray = FloatArray(3)
        colorArray.map { color ->
            android.graphics.Color.colorToHSV(color, hsvArray)
            hsvArray[2]
        }
    }
    val isWhiteCount = vList.count { it > 0.9f }

    val whitePar = isWhiteCount / vList.size.toFloat()
    return whitePar > 0.2f
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
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = date,
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
    }
}
