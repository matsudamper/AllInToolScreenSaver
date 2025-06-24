package net.matsudamper.allintoolscreensaver.compose

sealed interface CalendarDisplayScreenTestTag {
    object ZoomInButton : CalendarDisplayScreenTestTag
    object ZoomOutButton : CalendarDisplayScreenTestTag
    object ScrollUpButton : CalendarDisplayScreenTestTag
    object ScrollDownButton : CalendarDisplayScreenTestTag
    object CalendarLayout : CalendarDisplayScreenTestTag

    fun testTag(): String {
        return this::class.simpleName.orEmpty()
    }
} 
