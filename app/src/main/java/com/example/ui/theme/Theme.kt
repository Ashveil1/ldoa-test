package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UbuntuYaruColorScheme = darkColorScheme(
    primary = UbuntuOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6B2200),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = UbuntuAubergine,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C1046),
    onSecondaryContainer = Color(0xFFFFD7F5),
    tertiary = GoogleGreen,
    onTertiary = Color.White,
    background = YaruWindowBody,
    onBackground = YaruTextWhite,
    surface = YaruWindowHeader,
    onSurface = YaruTextWhite,
    surfaceVariant = UbuntuCoolGrey,
    onSurfaceVariant = YaruTextMuted,
    outline = YaruBorder,
    outlineVariant = Color(0xFF454545)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = UbuntuYaruColorScheme,
        typography = Typography,
        content = content
    )
}
