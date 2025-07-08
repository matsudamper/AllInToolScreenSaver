package net.matsudamper.allintoolscreensaver.ui.compose

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Clock

val LocalClock = staticCompositionLocalOf<Clock> { Clock.systemDefaultZone() }
