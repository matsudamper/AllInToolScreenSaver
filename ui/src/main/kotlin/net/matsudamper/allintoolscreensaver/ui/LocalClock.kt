package net.matsudamper.allintoolscreensaver.ui

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Clock

val LocalClock = staticCompositionLocalOf<Clock> { Clock.systemDefaultZone() }
