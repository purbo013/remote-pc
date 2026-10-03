package com.remotecoding.workstation.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotecoding.workstation.data.RemoteSession
import com.remotecoding.workstation.data.SessionState
import kotlin.math.abs
import kotlin.math.max

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 6f

private class Viewport {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    fun zoomTo(newScale: Float, centroid: Offset, viewSize: IntSize) {
        val next = newScale.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val center = Offset(viewSize.width / 2f, viewSize.height / 2f)
        val factor = if (scale == 0f) 1f else next / scale
        offset = if (next <= MIN_ZOOM + 0.01f) {
            Offset.Zero
        } else {
            clampOffset((centroid - center) * (1f - factor) + offset * factor, next, viewSize)
        }
        scale = next
    }
}

@Composable
fun RemoteScreen(
    session: SessionState,
    controller: RemoteSession,
    onBack: () -> Unit,
) {
    var showKeys by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(setOf<String>()) }
    val viewport = remember { Viewport() }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableStateOf(false) }
    var lastTap by remember { mutableLongStateOf(0L) }

    val frameBytes = session.frameJpeg
    val bitmap = remember(frameBytes) {
        frameBytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
    val imageWidth = bitmap?.width ?: session.frameWidth
    val imageHeight = bitmap?.height ?: session.frameHeight
    val scale = viewport.scale
    val offset = viewport.offset

    fun applyZoom(newScale: Float, centroid: Offset) {
        viewport.zoomTo(newScale, centroid, viewSize)
    }

    fun resetZoom() {
        viewport.reset()
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("${(scale * 100).toInt()}%", color = Color(0xFF93A1B0), fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val center = Offset(viewSize.width / 2f, viewSize.height / 2f)
                    applyZoom(scale - 0.4f, center)
                }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out", tint = Color.White)
                }
                IconButton(onClick = {
                    val center = Offset(viewSize.width / 2f, viewSize.height / 2f)
                    applyZoom(scale + 0.4f, center)
                }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in", tint = Color.White)
                }
                OutlinedButton(onClick = { resetZoom() }, modifier = Modifier.padding(start = 4.dp)) {
                    Text("FIT", fontSize = 12.sp)
                }
                IconButton(onClick = { showKeys = !showKeys }) {
                    Icon(
                        if (showKeys) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                        contentDescription = "Toggle keyboard",
                        tint = Color.White,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { viewSize = it }
                .pointerInput(viewSize, imageWidth, imageHeight) {
                    detectTapGestures(
                        onLongPress = { pos ->
                            sendMove(controller, pos, viewSize, imageWidth, imageHeight, viewport.scale, viewport.offset)
                            controller.mouseClick("right")
                        },
                        onDoubleTap = { pos ->
                            if (viewport.scale > 1.2f) {
                                resetZoom()
                            } else {
                                applyZoom(2.5f, pos)
                            }
                        },
                        onPress = { pos ->
                            sendMove(controller, pos, viewSize, imageWidth, imageHeight, viewport.scale, viewport.offset)
                            val now = System.currentTimeMillis()
                            val released = try {
                                awaitRelease()
                                true
                            } catch (_: Exception) {
                                false
                            }
                            if (released) {
                                val held = System.currentTimeMillis() - now
                                if (held > 180) {
                                    controller.mouseDown("left")
                                    dragging = true
                                } else if (now - lastTap > 280) {
                                    controller.mouseClick("left")
                                }
                                lastTap = now
                                if (dragging) {
                                    controller.mouseUp("left")
                                    dragging = false
                                }
                            }
                        },
                    )
                }
                .pointerInput(viewSize, imageWidth, imageHeight) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var zooming = false
                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size >= 2) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid()
                                if (abs(zoom - 1f) > 0.01f) {
                                    zooming = true
                                    applyZoom(viewport.scale * zoom, centroid)
                                } else if (viewport.scale > 1.01f) {
                                    viewport.offset = clampOffset(viewport.offset + pan, viewport.scale, viewSize)
                                } else if (abs(pan.y) > 2f || abs(pan.x) > 2f) {
                                    controller.scroll(-pan.y / 40f, pan.x / 40f)
                                }
                                event.changes.filter { it.positionChanged() }.forEach { it.consume() }
                            } else if (pressed.size == 1 && !zooming) {
                                val change = pressed[0]
                                sendMove(
                                    controller,
                                    change.position,
                                    viewSize,
                                    imageWidth,
                                    imageHeight,
                                    viewport.scale,
                                    viewport.offset,
                                )
                                if (dragging) change.consume()
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "PC screen",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                )
            } else {
                Text(
                    "Waiting for screen…",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        if (showKeys) {
            RemoteKeyboardPanel(
                controller = controller,
                locked = locked,
                onLockedChange = { locked = it },
            )
        }
    }
}

private fun clampOffset(value: Offset, scale: Float, viewSize: IntSize): Offset {
    if (scale <= 1.01f || viewSize.width == 0 || viewSize.height == 0) return Offset.Zero
    val maxX = viewSize.width * (scale - 1f) / 2f
    val maxY = viewSize.height * (scale - 1f) / 2f
    return Offset(
        value.x.coerceIn(-maxX, maxX),
        value.y.coerceIn(-maxY, maxY),
    )
}

private fun sendMove(
    controller: RemoteSession,
    pos: Offset,
    size: IntSize,
    imageWidth: Int,
    imageHeight: Int,
    scale: Float,
    offset: Offset,
) {
    if (size.width == 0 || size.height == 0) return
    val mapped = viewToNormalized(pos, size, imageWidth, imageHeight, scale, offset) ?: return
    controller.mouseMove(mapped.x, mapped.y)
}

private fun viewToNormalized(
    pos: Offset,
    size: IntSize,
    imageWidth: Int,
    imageHeight: Int,
    scale: Float,
    offset: Offset,
): Offset? {
    val viewW = size.width.toFloat()
    val viewH = size.height.toFloat()
    val center = Offset(viewW / 2f, viewH / 2f)
    val local = Offset(
        (pos.x - center.x - offset.x) / max(scale, 0.01f) + center.x,
        (pos.y - center.y - offset.y) / max(scale, 0.01f) + center.y,
    )
    if (imageWidth <= 0 || imageHeight <= 0) {
        return Offset((local.x / viewW).coerceIn(0f, 1f), (local.y / viewH).coerceIn(0f, 1f))
    }
    val fit = minOf(viewW / imageWidth, viewH / imageHeight)
    val drawnW = imageWidth * fit
    val drawnH = imageHeight * fit
    val left = (viewW - drawnW) / 2f
    val top = (viewH - drawnH) / 2f
    val x = (local.x - left) / drawnW
    val y = (local.y - top) / drawnH
    if (x !in 0f..1f || y !in 0f..1f) return Offset(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
    return Offset(x, y)
}
