package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricGold,
    onPrimary = Color(0xFF1E1200),
    primaryContainer = Color(0xFF3D2A00),
    onPrimaryContainer = ElectricGoldLight,
    secondary = ElectricCyan,
    onSecondary = Color(0xFF001F29),
    secondaryContainer = Color(0xFF003648),
    onSecondaryContainer = Color(0xFF70F3FF),
    tertiary = StatusNormalGreen,
    onTertiary = Color(0xFF002014),
    background = GridDarkNavy,
    onBackground = TextPrimary,
    surface = GridSurfaceNavy,
    onSurface = TextPrimary,
    surfaceVariant = GridCardNavy,
    onSurfaceVariant = TextSecondary,
    outline = GridBorderNavy,
    error = StatusFaultRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB57400),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDFA0),
    onPrimaryContainer = Color(0xFF281800),
    secondary = Color(0xFF006780),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8EAFF),
    onSecondaryContainer = Color(0xFF001F29),
    tertiary = Color(0xFF006C4C),
    background = GridLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = GridLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = GridLightCard,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = StatusFaultRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Substation UI is preferred in high-visibility dark mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
