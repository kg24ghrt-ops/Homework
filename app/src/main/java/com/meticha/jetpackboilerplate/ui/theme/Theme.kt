package com.meticha.jetpackboilerplate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Force the "Command Center" look
private val TacticalColorScheme = darkColorScheme(
    primary = CommandCyan,
    secondary = RadarGreen,
    tertiary = AlertOrange,
    background = CommandBlack,
    surface = PanelGrey,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = RadarGreen // Makes text on panels look like a radar screen
)

@Composable
fun CallBudyTheme(
    content: @Composable () -> Unit
) {
    // We remove the dynamicColor and lightTheme logic to keep it Tactical
    MaterialTheme(
        colorScheme = TacticalColorScheme,
        typography = Typography,
        content = content
    )
}
