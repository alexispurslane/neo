package io.github.alexispurslane.bloc.ui.composables.misc

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

enum class TrayState {
    DEFAULT,
    LEFT_OPEN,
    RIGHT_OPEN,
}

@Composable
fun ScrollableThreeDrawerScaffold(
    left: @Composable() (() -> Unit),
    middle: @Composable() (() -> Unit),
    right: @Composable() (() -> Unit),
    traySize: Int = 64,
) {
    val configuration = LocalConfiguration.current

    var middleOffset by remember { mutableStateOf(0.dp) }
    val transition = updateTransition(targetState = middleOffset, label = "Middle Pane")
    val animatedOffset by transition.animateDp(label = "Middle Pane Offset") {
        it
    }
    var trayGestureState by remember { mutableStateOf(TrayState.DEFAULT) }
    var trayVisibilityState by remember { mutableStateOf(TrayState.DEFAULT) }

    trayVisibilityState = if (animatedOffset < 0.dp) TrayState.RIGHT_OPEN
    else if (animatedOffset > 0.dp) TrayState.LEFT_OPEN
    else if (animatedOffset == 0.dp) TrayState.DEFAULT
    else trayVisibilityState

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFF0f0f0f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, delta ->
                        change.consume()
                        val new = middleOffset + (delta.x * 0.6).roundToInt().dp
                        if ((new <= (configuration.screenWidthDp - traySize).dp && trayVisibilityState == TrayState.LEFT_OPEN) ||
                            (new >= -(configuration.screenWidthDp - traySize).dp && trayVisibilityState == TrayState.RIGHT_OPEN) ||
                            trayVisibilityState == TrayState.DEFAULT)
                            middleOffset = new
                    },
                    onDragEnd = {
                        when (trayGestureState) {
                            TrayState.DEFAULT -> {
                                if (middleOffset > traySize.dp) {
                                    trayGestureState = TrayState.LEFT_OPEN
                                    middleOffset = (configuration.screenWidthDp - traySize).dp
                                } else if (middleOffset < -traySize.dp) {
                                    trayGestureState = TrayState.RIGHT_OPEN
                                    middleOffset = -(configuration.screenWidthDp - traySize).dp
                                } else {
                                    trayGestureState = TrayState.DEFAULT
                                    middleOffset = 0.dp
                                }
                            }
                            TrayState.LEFT_OPEN -> {
                                if (middleOffset < (configuration.screenWidthDp - traySize).dp) {
                                    trayGestureState = TrayState.DEFAULT
                                    middleOffset = 0.dp
                                }
                            }
                            TrayState.RIGHT_OPEN -> {
                                if (middleOffset > -(configuration.screenWidthDp - traySize).dp) {
                                    trayGestureState = TrayState.DEFAULT
                                    middleOffset = 0.dp
                                }
                            }
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .requiredWidth(configuration.screenWidthDp.dp - traySize.dp)
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .alpha(if (trayVisibilityState == TrayState.LEFT_OPEN) 1f else 0f)
            ,
        ) {
            left()
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .requiredWidth(configuration.screenWidthDp.dp - traySize.dp)
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .alpha(if (trayVisibilityState == TrayState.RIGHT_OPEN) 1f else 0f)
        ) {
            right()
        }
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .fillMaxHeight()
                .requiredWidth(configuration.screenWidthDp.dp)
                .clickable {
                    middleOffset = 0.dp
                }
                .clip(if (trayVisibilityState != TrayState.DEFAULT) RoundedCornerShape(1) else RectangleShape)
                .background(Color.Black)
                .safeDrawingPadding()
                .shadow(1.dp)
        ) {
            middle()
        }
    }
}