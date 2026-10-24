package com.tickertape.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF0B0F14)
val Panel = Color(0xFF151B24)
val PanelRaised = Color(0xFF1C2430)
val Line = Color(0xFF2C394A)
val Teal = Color(0xFF2EE6A6)
val Gold = Color(0xFFE8C547)
val Mist = Color(0xFF93A4B8)
val Paper = Color(0xFFE7EEF6)
val Up = Color(0xFF3DDC97)
val Down = Color(0xFFFF6B81)

private val scheme = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF04281A),
    secondary = Gold,
    onSecondary = Color(0xFF2A2204),
    background = Ink,
    surface = Panel,
    surfaceContainer = PanelRaised,
    surfaceContainerHigh = PanelRaised,
    onBackground = Paper,
    onSurface = Paper,
    onSurfaceVariant = Mist,
    outline = Line,
    error = Down,
    onError = Color(0xFF2A060E),
)

@Composable
fun TickertapeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

fun tone(value: Double?): Color = when {
    value == null -> Mist
    value > 0 -> Up
    value < 0 -> Down
    else -> Paper
}
