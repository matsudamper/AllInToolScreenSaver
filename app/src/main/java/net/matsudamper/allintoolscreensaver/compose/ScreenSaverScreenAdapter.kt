package net.matsudamper.allintoolscreensaver.compose

import android.graphics.Bitmap
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import net.matsudamper.allintoolscreensaver.ImageManager
import net.matsudamper.allintoolscreensaver.compose.calendar.CalendarDisplayScreen
import net.matsudamper.allintoolscreensaver.compose.eventalert.EventAlertViewModel
import net.matsudamper.allintoolscreensaver.ui.compose.ClockUiState
import net.matsudamper.allintoolscreensaver.ui.compose.EventAlertUiState
import net.matsudamper.allintoolscreensaver.ui.compose.ScreenSaverScreen
import net.matsudamper.allintoolscreensaver.ui.compose.ScreenSaverScreenUiState
import net.matsudamper.allintoolscreensaver.ui.compose.SlideShowImageItem
import net.matsudamper.allintoolscreensaver.ui.compose.SlideShowUiState
import net.matsudamper.allintoolscreensaver.ui.compose.component.SuspendLifecycleStartEffect
import net.matsudamper.allintoolscreensaver.viewmodel.DigitalClockScreenViewModel
import org.koin.core.context.GlobalContext

@Composable
fun ScreenSaverScreenAdapter() {
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

    SuspendLifecycleStartEffect(digitalClockUiState.value.listener) {
        digitalClockUiState.value.listener.onStart()
    }

    SuspendLifecycleStartEffect(eventAlertUiState.value.listener) {
        eventAlertUiState.value.listener.onStart()
    }

    val graphicsLayer = rememberGraphicsLayer()
    var clockRect by remember { mutableStateOf<Rect?>(null) }
    val pageChanged = remember { Channel<Unit>(Channel.CONFLATED) }
    var isWhite by remember { mutableStateOf(false) }

    LaunchedEffect(clockRect) {
        snapshotFlow { clockRect }
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

    val uiState = ScreenSaverScreenUiState(
        clockUiState = ClockUiState(
            dateText = digitalClockUiState.value.currentDate,
            timeText = digitalClockUiState.value.currentTime,
            shouldShowDarkBackground = isWhite,
        ),
        slideShowUiState = SlideShowUiState(
            imageItems = digitalClockUiState.value.pagerItems.map { pagerItem ->
                SlideShowImageItem(
                    id = pagerItem.id,
                    imageUri = pagerItem.imageUri?.toString(),
                )
            },
            currentPageIndex = 1,
            intervalSeconds = digitalClockUiState.value.imageSwitchIntervalSeconds ?: 30,
        ),
        eventAlertUiState = eventAlertUiState.value.currentAlert?.let { alert ->
            EventAlertUiState(
                title = alert.event.title,
                alertTypeDisplayText = alert.alertType.displayText,
                eventStartTimeText = alert.eventStartTimeText,
                description = alert.event.description.orEmpty(),
                isRepeatingAlert = alert.isRepeatingAlert,
            )
        },
    )

    ScreenSaverScreen(
        uiState = uiState,
        onPageChange = { newPage ->
            digitalClockUiState.value.listener.onPageChanged(newPage)
        },
        onPageChanged = {
            pageChanged.trySend(Unit)
        },
        onAlertDismiss = {
            eventAlertUiState.value.listener.onAlertDismiss()
        },
        calendarContent = {
            CalendarDisplayScreen(
                contentWindowInsets = WindowInsets(0),
            )
        },
    )
}

private fun isWhite(
    rect: Rect,
    imageBitmap: ImageBitmap,
): Boolean {
    val bitmap = run {
        val bitmap = imageBitmap.asAndroidBitmap()
        if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
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
