package com.example.alarm.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import android.content.Context

// Light Color Scheme
private val LightPrimary = Color(0xFF6366F1)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFEEEDFF)
private val LightOnPrimaryContainer = Color(0xFF1E1B4B)

private val LightSecondary = Color(0xFF7C3AED)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFF3E8FF)
private val LightOnSecondaryContainer = Color(0xFF2D1B69)

private val LightTertiary = Color(0xFF06B6D4)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFCFFAFE)
private val LightOnTertiaryContainer = Color(0xFF00474D)

private val LightError = Color(0xFFDC2626)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFFEBEE)
private val LightOnErrorContainer = Color(0xFF5F0000)

private val LightBackground = Color(0xFFFAFAFA)
private val LightOnBackground = Color(0xFF1F2937)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF1F2937)
private val LightSurfaceVariant = Color(0xFFF3F4F6)
private val LightOnSurfaceVariant = Color(0xFF6B7280)

private val LightOutline = Color(0xFFD1D5DB)
private val LightOutlineVariant = Color(0xFFE5E7EB)

// Dark Color Scheme
private val DarkPrimary = Color(0xFFC7D2FE)
private val DarkOnPrimary = Color(0xFF312E81)
private val DarkPrimaryContainer = Color(0xFF4F46E5)
private val DarkOnPrimaryContainer = Color(0xFFEEEDFF)

private val DarkSecondary = Color(0xFFE9D5FF)
private val DarkOnSecondary = Color(0xFF4C1D97)
private val DarkSecondaryContainer = Color(0xFF7C3AED)
private val DarkOnSecondaryContainer = Color(0xFFF3E8FF)

private val DarkTertiary = Color(0xFFA5F3FC)
private val DarkOnTertiary = Color(0xFF003D44)
private val DarkTertiaryContainer = Color(0xFF005A63)
private val DarkOnTertiaryContainer = Color(0xFFCFFAFE)

private val DarkError = Color(0xFFFF6B6B)
private val DarkOnError = Color(0xFF7F0000)
private val DarkErrorContainer = Color(0xFFB3261E)
private val DarkOnErrorContainer = Color(0xFFFFEBEE)

private val DarkBackground = Color(0xFF111827)
private val DarkOnBackground = Color(0xFFF3F4F6)
private val DarkSurface = Color(0xFF1F2937)
private val DarkOnSurface = Color(0xFFF3F4F6)
private val DarkSurfaceVariant = Color(0xFF374151)
private val DarkOnSurfaceVariant = Color(0xFFD1D5DB)

private val DarkOutline = Color(0xFF6B7280)
private val DarkOutlineVariant = Color(0xFF4B5563)

@Composable
fun lightColorScheme() = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
)

@Composable
fun darkColorScheme() = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
)

@Composable
fun AlarmAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    context: Context? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AlarmAppTypography,
        content = content
    )
}

object ThemeMode {
    const val LIGHT = "light"
    const val DARK = "dark"
    const val SYSTEM = "system"
}
