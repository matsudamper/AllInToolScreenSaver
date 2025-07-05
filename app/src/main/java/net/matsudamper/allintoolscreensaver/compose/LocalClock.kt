package net.matsudamper.allintoolscreensaver.compose

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Clock

val LocalClock = staticCompositionLocalOf<Clock> { Clock.systemDefaultZone() }
