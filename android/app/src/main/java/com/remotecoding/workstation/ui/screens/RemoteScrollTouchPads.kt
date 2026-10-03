package com.remotecoding.workstation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotecoding.workstation.data.RemoteSession
import kotlin.math.abs

private const val SCROLL_SENSITIVITY = 28f

@Composable
fun VerticalScrollTouchPad(
    controller: RemoteSession,
    modifier: Modifier = Modifier,
) {
    ScrollTouchPad(
        modifier = modifier,
        hint = "↕",
        onDrag = { _, dy ->
            if (abs(dy) > 0.5f) controller.scroll(-dy / SCROLL_SENSITIVITY)
        },
    )
}

@Composable
fun HorizontalScrollTouchPad(
    controller: RemoteSession,
    modifier: Modifier = Modifier,
) {
    ScrollTouchPad(
        modifier = modifier,
        hint = "↔",
        onDrag = { dx, _ ->
            if (abs(dx) > 0.5f) controller.scroll(0f, dx / SCROLL_SENSITIVITY)
        },
    )
}

@Composable
private fun ScrollTouchPad(
    modifier: Modifier,
    hint: String,
    onDrag: (dx: Float, dy: Float) -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF2D333B), RemotePanelStyle.scrollPadBg),
    )
    Surface(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.pressed } ?: continue
                    val dx = change.position.x - change.previousPosition.x
                    val dy = change.position.y - change.previousPosition.y
                    if (abs(dx) > 0.5f || abs(dy) > 0.5f) {
                        onDrag(dx, dy)
                        change.consume()
                    }
                } while (event.changes.any { it.pressed })
            }
        },
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = hint,
                color = RemotePanelStyle.scrollPadAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(6.dp),
            )
        }
    }
}
