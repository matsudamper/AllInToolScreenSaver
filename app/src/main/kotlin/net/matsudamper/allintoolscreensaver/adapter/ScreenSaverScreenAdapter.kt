package net.matsudamper.allintoolscreensaver.adapter

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Clock
import dev.chrisbanes.haze.HazeState
import net.matsudamper.allintoolscreensaver.feature.ImageManager
import net.matsudamper.allintoolscreensaver.ui.LocalClock
import net.matsudamper.allintoolscreensaver.ui.alert.ClockContent
import net.matsudamper.allintoolscreensaver.ui.alert.EventAlertDialog
import net.matsudamper.allintoolscreensaver.ui.calendar.CalendarDisplayScreen
import net.matsudamper.allintoolscreensaver.ui.screensaver.ScreenSaverScreen
import net.matsudamper.allintoolscreensaver.ui.slideshow.SlideShowScreen
import net.matsudamper.allintoolscreensaver.viewmodel.CalendarDisplayScreenViewModel
import net.matsudamper.allintoolscreensaver.viewmodel.ClockViewModel
import net.matsudamper.allintoolscreensaver.viewmodel.EventAlertViewModel
import net.matsudamper.allintoolscreensaver.viewmodel.SlideshowScreenViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext

@Composable
fun ScreenSaverScreenAdapter() {
    val hazeState = remember { HazeState() }
    var isDarkBackground by remember { mutableStateOf(false) }
    var slideShowPagerState: PagerState? by remember { mutableStateOf(null) }
    ScreenSaverScreen(
        slideshowContent = {
            SlideShowScreenAdapter(
                hazeState = hazeState,
                updatedPagerState = { pagerState ->
                    slideShowPagerState = pagerState
                },
            )
        },
        eventAlertContent = {
            EventAlertContentAdapter()
        },
        calendarContent = {
            CalendarDisplayScreenAdapter(
                contentWindowInsets = WindowInsets(0),
            )
        },
        clockContent = {
            ClockAdapter(
                hazeState = hazeState,
                isDarkBackground = isDarkBackground,
            )
        },
        notificationOverlayContent = {
            NotificationAdapter(hazeState = hazeState)
        },
        updateIsDarkClockBackground = {
            isDarkBackground = it
        },
        slideShowPagerState = slideShowPagerState,
    )
}

@Composable
private fun EventAlertContentAdapter() {
    val eventAlertViewModel: EventAlertViewModel = viewModel(
        initializer = {
            val koin = GlobalContext.get()
            EventAlertViewModel(
                alertManager = koin.get(),
            )
        },
    )
    val eventAlertUiState by eventAlertViewModel.uiState.collectAsState()
    EventAlertDialog(
        uiState = eventAlertUiState,
    )
}

@Composable
private fun ClockAdapter(
    hazeState: HazeState,
    isDarkBackground: Boolean,
) {
    val clockViewModel = viewModel<ClockViewModel>(
        initializer = {
            val koin = GlobalContext.get()
            ClockViewModel(
                clock = koin.get(),
            )
        },
    )
    val clockUiState by clockViewModel.uiStateFlow.collectAsState()
    ClockContent(
        uiState = clockUiState,
        hazeState = hazeState,
        isDarkBackground = isDarkBackground,
    )
}

@Composable
fun SlideShowScreenAdapter(
    hazeState: HazeState = remember { HazeState() },
    updatedPagerState: (PagerState) -> Unit = {},
) {
    val context = LocalContext.current
    val slideshowScreenViewModel: SlideshowScreenViewModel = viewModel(
        initializer = {
            val koin = GlobalContext.get()
            SlideshowScreenViewModel(
                settingsRepositor = koin.get(),
                imageManager = ImageManager(context),
                inMemoryCache = koin.get(),
                clock = koin.get(),
            )
        },
    )
    val slideshowUiState by slideshowScreenViewModel.uiState.collectAsState()
    SlideShowScreen(
        uiState = slideshowUiState,
        hazeState = hazeState,
        updatedPagerState = { pagerState ->
            updatedPagerState(pagerState)
        },
    )
}

@Composable
fun CalendarDisplayScreenAdapter(
    contentWindowInsets: WindowInsets,
    modifier: Modifier = Modifier,
    viewModel: CalendarDisplayScreenViewModel = koinViewModel(),
    clock: Clock = LocalClock.current,
) {
    val uiState by viewModel.uiState.collectAsState()
    CalendarDisplayScreen(
        modifier = modifier,
        uiState = uiState,
        clock = clock,
        contentWindowInsets = contentWindowInsets,
    )
}
