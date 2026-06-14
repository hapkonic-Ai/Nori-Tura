package com.example.nori_tura.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = NorituraColors.PrimaryBlue,
    onPrimary = NorituraColors.Surface,
    primaryContainer = NorituraColors.PrimaryBlueLight,
    onPrimaryContainer = NorituraColors.PrimaryBlue,
    secondary = NorituraColors.AccentGreen,
    onSecondary = NorituraColors.Surface,
    secondaryContainer = NorituraColors.AccentGreenLight,
    onSecondaryContainer = NorituraColors.AccentGreen,
    background = NorituraColors.Background,
    onBackground = NorituraColors.TextPrimary,
    surface = NorituraColors.Surface,
    onSurface = NorituraColors.TextPrimary,
    surfaceVariant = NorituraColors.SurfaceVariant,
    onSurfaceVariant = NorituraColors.TextSecondary,
    outline = NorituraColors.Outline,
    error = NorituraColors.Error,
    onError = NorituraColors.Surface,
    errorContainer = NorituraColors.ErrorLight,
    onErrorContainer = NorituraColors.Error
)

private val DarkColorScheme = darkColorScheme(
    primary = NorituraColors.PrimaryBlue,
    onPrimary = NorituraColors.Surface,
    primaryContainer = NorituraColors.PrimaryBlue.copy(alpha = 0.2f),
    onPrimaryContainer = NorituraColors.PrimaryBlueLight,
    secondary = NorituraColors.AccentGreen,
    onSecondary = NorituraColors.Surface,
    secondaryContainer = NorituraColors.AccentGreen.copy(alpha = 0.2f),
    onSecondaryContainer = NorituraColors.AccentGreenLight,
    background = NorituraColors.TextPrimary,
    onBackground = NorituraColors.Surface,
    surface = Color(0xFF2A2D3A),
    onSurface = NorituraColors.Surface,
    surfaceVariant = Color(0xFF3A3E4D),
    onSurfaceVariant = NorituraColors.TextTertiary,
    outline = NorituraColors.TextTertiary,
    error = NorituraColors.Error,
    onError = NorituraColors.Surface,
    errorContainer = NorituraColors.Error.copy(alpha = 0.2f),
    onErrorContainer = NorituraColors.ErrorLight
)

@Composable
fun NorituraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NorituraTypography,
        content = content
    )
}
