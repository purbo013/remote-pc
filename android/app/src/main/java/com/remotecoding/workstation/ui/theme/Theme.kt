package com.remotecoding.workstation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val scheme = darkColorScheme(
    primary = Color(0xFF4C8DFF),
    onPrimary = Color.White,
    background = Color(0xFF0E1116),
    surface = Color(0xFF161B22),
    onBackground = Color(0xFFE8EDF2),
    onSurface = Color(0xFFE8EDF2),
    secondary = Color(0xFF3DD68C),
    error = Color(0xFFFF6B6B),
)

@Composable
fun RemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
