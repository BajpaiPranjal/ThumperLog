package com.thumperlog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgColor = Color(0xFF14161A)
val PanelColor = Color(0xFF1C1F24)
val PanelColor2 = Color(0xFF22262C)
val LineColor = Color(0xFF2A2E34)
val TextColor = Color(0xFFEAE7E0)
val MutedColor = Color(0xFF8B9099)
val AmberColor = Color(0xFFF0A83C)
val RedColor = Color(0xFFE2483D)

private val scheme = darkColorScheme(
    background = BgColor,
    surface = PanelColor,
    primary = AmberColor,
    onPrimary = BgColor,
    onBackground = TextColor,
    onSurface = TextColor,
    error = RedColor
)

@Composable
fun ThumperLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
