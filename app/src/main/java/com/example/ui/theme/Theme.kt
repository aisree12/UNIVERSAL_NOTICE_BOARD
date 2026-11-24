package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
    }
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NoticeBoardTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentPreset: AccentPreset = AccentPreset.INDIGO,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = accentPreset.primary.copy(alpha = 0.9f),
            onPrimary = Color.White,
            primaryContainer = accentPreset.primary.copy(alpha = 0.25f),
            onPrimaryContainer = Color(0xFFE0E7FF),
            secondary = accentPreset.secondary,
            onSecondary = Color.White,
            secondaryContainer = accentPreset.secondary.copy(alpha = 0.22f),
            onSecondaryContainer = Color(0xFFCCFBF1),
            tertiary = accentPreset.tertiary,
            onTertiary = Color.Black,
            tertiaryContainer = accentPreset.tertiary.copy(alpha = 0.2f),
            onTertiaryContainer = Color(0xFFFEF3C7),
            background = DarkBackground,
            onBackground = DarkOnSurface,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            outline = DarkOutline,
            error = Color(0xFFF87171),
            errorContainer = EmergencyContainerDark,
            onErrorContainer = Color(0xFFFECACA)
        )
    } else {
        lightColorScheme(
            primary = accentPreset.primary,
            onPrimary = Color.White,
            primaryContainer = accentPreset.primary.copy(alpha = 0.12f),
            onPrimaryContainer = accentPreset.primary,
            secondary = accentPreset.secondary,
            onSecondary = Color.White,
            secondaryContainer = accentPreset.secondary.copy(alpha = 0.12f),
            onSecondaryContainer = accentPreset.secondary,
            tertiary = accentPreset.tertiary,
            onTertiary = Color.White,
            tertiaryContainer = accentPreset.tertiary.copy(alpha = 0.14f),
            onTertiaryContainer = AmberDeep,
            background = LightBackground,
            onBackground = LightOnSurface,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            outline = LightOutline,
            error = EmergencyRed,
            errorContainer = EmergencyContainerLight,
            onErrorContainer = EmergencyRed
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
