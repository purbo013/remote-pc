package com.remotecoding.workstation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remotecoding.workstation.data.RemoteSession
import kotlinx.coroutines.delay

private data class PanelKey(
    val label: String,
    val keys: List<String>,
    val latch: Boolean = false,
)

private val latchIds = setOf("CTRL", "SHIFT", "ALT", "WIN")

private val panelMainKeys = listOf(
    PanelKey("Ctrl", listOf("CTRL"), latch = true),
    PanelKey("Shift", listOf("SHIFT"), latch = true),
    PanelKey("Alt", listOf("ALT"), latch = true),
    PanelKey("Win", listOf("WIN"), latch = true),
    PanelKey("Esc", listOf("ESC")),
    PanelKey("Tab", listOf("TAB")),
    PanelKey("Space", listOf("SPACE")),
    PanelKey("Enter", listOf("ENTER")),
    PanelKey("Bksp", listOf("BACKSPACE")),
    PanelKey("Del", listOf("DELETE")),
    PanelKey("PgUp", listOf("PAGEUP")),
    PanelKey("PgDn", listOf("PAGEDOWN")),
)

private val panelShortcutKeys = listOf(
    PanelKey("Ctrl+S", listOf("CTRL", "S")),
    PanelKey("Ctrl+C", listOf("CTRL", "C")),
    PanelKey("Ctrl+V", listOf("CTRL", "V")),
    PanelKey("Ctrl+X", listOf("CTRL", "X")),
    PanelKey("Ctrl+Z", listOf("CTRL", "Z")),
    PanelKey("Ctrl+Y", listOf("CTRL", "Y")),
    PanelKey("Ctrl+A", listOf("CTRL", "A")),
    PanelKey("Ctrl+F", listOf("CTRL", "F")),
    PanelKey("Ctrl+H", listOf("CTRL", "H")),
    PanelKey("Ctrl+P", listOf("CTRL", "P")),
    PanelKey("Ctrl+B", listOf("CTRL", "B")),
    PanelKey("Ctrl+`", listOf("CTRL", "`")),
    PanelKey("Ctrl+⇧+P", listOf("CTRL", "SHIFT", "P")),
    PanelKey("Alt+Tab", listOf("ALT", "TAB")),
)

/** Tinggi seragam: kolom ketik, touch ↔, dan setengah tinggi touch ↕ (dua baris). */
private val touchRowHeight = 40.dp
private val touchGap = 4.dp
/** Tombol navigasi: kotak seragam (sama tinggi dengan baris ketik). */
private val navKeySize = touchRowHeight
/** Tinggi tombol baris utama & Ctrl (harus sama). */
private val rowKeyHeight = 32.dp
private val verticalPadWidth = 48.dp
private const val TYPE_FIELD_IDLE_CLEAR_MS = 5_000L

@Composable
fun RemoteKeyboardPanel(
    controller: RemoteSession,
    locked: Set<String>,
    onLockedChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var typed by remember { mutableStateOf("") }
    var typeFieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(typed, typeFieldFocused) {
        if (!typeFieldFocused || typed.isEmpty()) return@LaunchedEffect
        delay(TYPE_FIELD_IDLE_CLEAR_MS)
        typed = ""
    }
    val mainKeysScroll = rememberScrollState()
    val shortcutKeysScroll = rememberScrollState()
    val blockHeight = touchRowHeight + touchGap + touchRowHeight

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = RemotePanelStyle.panelBg,
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(touchRowHeight),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            NavKey("Hm", listOf("HOME"), controller, locked)
                            NavKey("↑", listOf("UP"), controller, locked)
                            NavKey("En", listOf("END"), controller, locked)
                            BasicTextField(
                                value = typed,
                                onValueChange = { value ->
                                    if (value.length > typed.length) {
                                        val added = value.substring(typed.length)
                                        if (locked.isNotEmpty() && added.length == 1) {
                                            val combo = orderModifiers(locked.toList() + added.uppercase())
                                            if (combo.size == 1) controller.key(combo.first()) else controller.keyCombo(combo)
                                        } else {
                                            controller.textInput(added)
                                        }
                                    } else if (value.length < typed.length) {
                                        controller.key("BACKSPACE")
                                    }
                                    typed = value
                                },
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(touchRowHeight)
                                    .onFocusChanged { state ->
                                        typeFieldFocused = state.isFocused
                                        if (!state.isFocused) typed = ""
                                    },
                                decorationBox = { inner ->
                                    Surface(
                                        color = RemotePanelStyle.inputBg,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(touchRowHeight)
                                            .border(1.dp, RemotePanelStyle.inputBorder, RoundedCornerShape(8.dp)),
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            if (typed.isEmpty()) {
                                                Text("Ketik → PC", color = RemotePanelStyle.scrollHint, fontSize = 12.sp)
                                            }
                                            inner()
                                        }
                                    }
                                },
                            )
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = touchGap)
                                .height(touchRowHeight),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            NavKey("←", listOf("LEFT"), controller, locked)
                            NavKey("↓", listOf("DOWN"), controller, locked)
                            NavKey("→", listOf("RIGHT"), controller, locked)
                            HorizontalScrollTouchPad(
                                controller = controller,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(touchRowHeight),
                            )
                        }
                    }
                    VerticalScrollTouchPad(
                        controller = controller,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .width(verticalPadWidth)
                            .height(blockHeight),
                    )
                }
            }

            KeyRow(
                keys = panelMainKeys,
                scrollState = mainKeysScroll,
                locked = locked,
                onLockedChange = onLockedChange,
                controller = controller,
                shortcutRow = false,
            )
            KeyRow(
                keys = panelShortcutKeys,
                scrollState = shortcutKeysScroll,
                locked = locked,
                onLockedChange = onLockedChange,
                controller = controller,
                shortcutRow = true,
            )
        }
    }
}

@Composable
private fun NavKey(
    label: String,
    keys: List<String>,
    controller: RemoteSession,
    locked: Set<String>,
) {
    KeyButton(
        label = label,
        selected = false,
        shortcutRow = false,
        compact = false,
        navKey = true,
        onClick = { sendKeyAction(controller, locked, PanelKey(label, keys)) },
        modifier = Modifier.size(navKeySize),
    )
}

@Composable
private fun KeyRow(
    keys: List<PanelKey>,
    scrollState: androidx.compose.foundation.ScrollState,
    locked: Set<String>,
    onLockedChange: (Set<String>) -> Unit,
    controller: RemoteSession,
    shortcutRow: Boolean,
) {
    val trackShape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        shape = trackShape,
        color = RemotePanelStyle.rowTrack,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        keys.forEach { item ->
            val id = item.keys.singleOrNull()
            val latched = item.latch && id != null && id in locked
            KeyButton(
                label = item.label,
                selected = latched,
                shortcutRow = shortcutRow,
                compact = false,
                rowKey = true,
                onClick = {
                    if (item.latch && id != null && id in latchIds) {
                        onLockedChange(if (latched) locked - id else locked + id)
                    } else {
                        sendKeyAction(controller, locked, item)
                    }
                },
                modifier = Modifier
                    .height(rowKeyHeight)
                    .defaultMinSize(minWidth = 44.dp),
            )
        }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    selected: Boolean,
    shortcutRow: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    navKey: Boolean = false,
    rowKey: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)
    val textColor = when {
        selected -> RemotePanelStyle.keyActiveText
        shortcutRow -> RemotePanelStyle.shortcutText
        else -> RemotePanelStyle.keyText
    }
    val capBrush = when {
        selected -> Brush.verticalGradient(
            listOf(Color(0xFF388BFD), RemotePanelStyle.keyActiveFill),
        )
        shortcutRow -> Brush.verticalGradient(
            listOf(Color(0xFF323B47), Color(0xFF252C36)),
        )
        else -> Brush.verticalGradient(
            listOf(RemotePanelStyle.keyCapTop, RemotePanelStyle.keyCapBottom),
        )
    }
    val fontSize = when {
        navKey && label.length <= 2 && label.any { it in "↑↓←→" } -> 17.sp
        navKey -> 11.sp
        rowKey && shortcutRow -> 11.sp
        compact -> 11.sp
        else -> 12.sp
    }
    val textPadding = when {
        navKey -> Modifier.padding(2.dp)
        rowKey -> Modifier.padding(horizontal = 8.dp)
        else -> Modifier.padding(
            horizontal = if (compact) 7.dp else 11.dp,
            vertical = if (compact) 4.dp else 7.dp,
        )
    }
    if (rowKey) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(capBrush, shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = fontSize,
                lineHeight = fontSize,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = textPadding,
            )
        }
        return
    }
    Surface(
        onClick = onClick,
        shape = shape,
        color = Color.Transparent,
        shadowElevation = when {
            selected -> 3.dp
            else -> 2.dp
        },
        modifier = modifier,
    ) {
        val boxModifier = when {
            navKey -> Modifier.fillMaxSize()
            else -> Modifier.defaultMinSize(
                minWidth = if (compact) 34.dp else 44.dp,
                minHeight = if (compact) 30.dp else 34.dp,
            )
        }
        Box(
            modifier = boxModifier.background(capBrush, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = fontSize,
                fontWeight = when {
                    selected || navKey -> FontWeight.SemiBold
                    else -> FontWeight.Normal
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = textPadding,
            )
        }
    }
}

private fun sendKeyAction(
    controller: RemoteSession,
    locked: Set<String>,
    item: PanelKey,
) {
    val combo = when {
        item.keys.size > 1 -> item.keys
        item.keys.size == 1 && item.keys[0] in latchIds -> item.keys
        item.keys.size == 1 -> {
            val key = item.keys[0]
            if (locked.isEmpty()) listOf(key) else locked.toList() + key
        }
        else -> item.keys
    }
    val ordered = orderModifiers(combo)
    if (ordered.size == 1) controller.key(ordered.first()) else controller.keyCombo(ordered)
}

private fun orderModifiers(keys: List<String>): List<String> {
    val order = listOf("CTRL", "SHIFT", "ALT", "WIN")
    val mods = keys.filter { it in order }.sortedBy { order.indexOf(it) }
    val rest = keys.filter { it !in order }
    return mods + rest
}
