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

private val EcoDarkColorScheme = darkColorScheme(
    primary = EcoPrimary,
    secondary = EcoSecondary,
    tertiary = EcoAccent,
    background = Color(0xFF0F172A), // Slate 900
    surface = Color(0xFF1E293B), // Slate 800
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

private val EcoLightColorScheme = lightColorScheme(
    primary = EcoPrimary,
    secondary = EcoSecondary,
    tertiary = EcoAccent,
    background = EcoBackground,
    surface = EcoCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = EcoTextDark,
    onSurface = EcoTextDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep branded colors on for high hackathon visual fidelity!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EcoDarkColorScheme
        else -> EcoLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
