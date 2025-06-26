package net.matsudamper.allintoolscreensaver

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

object NavKeys {
    @Serializable
    data object Main : NavKey

    @Serializable
    data object CalendarSelection : NavKey

    @Serializable
    data object CalendarDisplay : NavKey
}
