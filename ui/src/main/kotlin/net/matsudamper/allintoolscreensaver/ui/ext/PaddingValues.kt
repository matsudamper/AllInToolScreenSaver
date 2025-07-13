package net.matsudamper.allintoolscreensaver.ui.ext

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun PaddingValues.plus(values: PaddingValues, direction: LayoutDirection = LocalLayoutDirection.current): PaddingValues {
    return PaddingValues(
        start = this.calculateStartPadding(direction) + values.calculateStartPadding(direction),
        top = this.calculateTopPadding() + values.calculateTopPadding(),
        end = this.calculateEndPadding(direction) + values.calculateEndPadding(direction),
        bottom = this.calculateBottomPadding() + values.calculateBottomPadding(),
    )
}
