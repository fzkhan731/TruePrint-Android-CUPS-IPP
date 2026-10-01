package com.trueprint.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TruePrintBlueDark,
    secondary = TruePrintTealDark,
    background = TruePrintDarkBackground,
    surface = TruePrintDarkSurface,
    surfaceVariant = TruePrintDarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = TruePrintBlue,
    secondary = TruePrintTeal,
    background = TruePrintBackground,
    surface = TruePrintSurface,
    surfaceVariant = TruePrintSurface
)

@Composable
fun TruePrintTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
