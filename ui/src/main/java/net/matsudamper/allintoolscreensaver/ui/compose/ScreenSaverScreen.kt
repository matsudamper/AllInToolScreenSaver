package net.matsudamper.allintoolscreensaver.ui.compose

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState
import net.matsudamper.allintoolscreensaver.ui.compose.component.DreamAlertDialog

@Composable
fun ScreenSaverScreen(
    uiState: ScreenSaverScreenUiState,
    onPageChange: (Int) -> Unit,
    onPageChanged: () -> Unit,
    onAlertDismiss: () -> Unit,
    calendarContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hazeState = rememberHazeState()

    Box(
        modifier = modifier
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
                SlideShowContent(
                    uiState = uiState.slideShowUiState,
                    onPageChange = onPageChange,
                    onPageChanged = onPageChanged,
                    hazeState = hazeState,
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
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
                        targetValue = if (uiState.clockUiState.shouldShowDarkBackground) {
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
                            uiState = uiState.clockUiState,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.1f),
            ) {
                calendarContent()
            }
        }

        val currentAlert = uiState.eventAlertUiState
        if (currentAlert != null) {
            DreamAlertDialog(
                title = {
                    Column {
                        Text(
                            text = currentAlert.title,
                        )
                        Text(
                            text = "${currentAlert.alertTypeDisplayText}のアラート",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                dismissRequest = onAlertDismiss,
                negativeButton = {
                    Text(text = "CLOSE")
                },
                onClickNegative = onAlertDismiss,
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

                    Text(text = currentAlert.description)
                }
            }
        }
    }
}

@Composable
private fun Clock(
    uiState: ClockUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = uiState.timeText,
            color = Color.White,
            fontSize = 48.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = uiState.dateText,
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
    }
}
