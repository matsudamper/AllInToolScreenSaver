package net.matsudamper.allintoolscreensaver.ui.screensaver

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun ScreenSaverScreen(
    slideshowContent: @Composable () -> Unit,
    eventAlertContent: @Composable () -> Unit,
    calendarContent: @Composable () -> Unit,
    clockContent: @Composable () -> Unit,
    notificationOverlayContent: @Composable () -> Unit,
    updateIsDarkClockBackground: (Boolean) -> Unit,
    slideShowPagerState: PagerState?,
    modifier: Modifier = Modifier,
) {
    val graphicsLayer = rememberGraphicsLayer()
    var clockRect by remember { mutableStateOf<Rect?>(null) }
    val currentUpdateIsDarkClockBackground by rememberUpdatedState(updateIsDarkClockBackground)

    LaunchedEffect(clockRect, slideShowPagerState) {
        snapshotFlow { clockRect }
            .filterNotNull()
            .combine(snapshotFlow { slideShowPagerState?.currentPage }.filterNotNull()) { rect, _ -> rect }
            .collectLatest { rect ->
                val imageBitmap = graphicsLayer.toImageBitmap()
                currentUpdateIsDarkClockBackground(
                    isWhite(
                        rect = rect,
                        imageBitmap = imageBitmap,
                    ),
                )
            }
    }

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
                    .weight(1f),
            ) {
                Box(
                    modifier = Modifier.drawWithCache {
                        onDrawWithContent {
                            graphicsLayer.record {
                                this@onDrawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                    },
                ) {
                    slideshowContent()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 12.dp,
                            bottom = 12.dp,
                        )
                        .onGloballyPositioned {
                            clockRect = it.boundsInRoot()
                        },
                ) {
                    clockContent()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                ) {
                    notificationOverlayContent()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
            ) {
                calendarContent()
            }

            eventAlertContent()
        }
    }
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
