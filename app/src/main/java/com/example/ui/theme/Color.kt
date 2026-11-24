package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand & Animated Title Spectrum
val IndigoPrimary = Color(0xFF4338CA)
val IndigoLight = Color(0xFF6366F1)
val IndigoDark = Color(0xFF312E81)

val TealAccent = Color(0xFF0D9488)
val TealBright = Color(0xFF14B8A6)
val AmberWarm = Color(0xFFF59E0B)
val AmberDeep = Color(0xFFD97706)
val RoseCrimson = Color(0xFFE11D48)
val EmeraldSuccess = Color(0xFF059669)
val SkyCyan = Color(0xFF0284C7)

// Neutral Surfaces - Light
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF2FF)
val LightOutline = Color(0xFFCBD5E1)
val LightOnSurface = Color(0xFF0F172A)
val LightOnSurfaceVariant = Color(0xFF475569)

// Neutral Surfaces - Dark
val DarkBackground = Color(0xFF090D16)
val DarkSurface = Color(0xFF111827)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkOutline = Color(0xFF334155)
val DarkOnSurface = Color(0xFFF8FAFC)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)

// Semantic Priority & Status Colors
val EmergencyRed = Color(0xFFDC2626)
val EmergencyContainerLight = Color(0xFFFEE2E2)
val EmergencyContainerDark = Color(0xFF450A0A)

val UrgentAmber = Color(0xFFD97706)
val UrgentContainerLight = Color(0xFFFEF3C7)
val UrgentContainerDark = Color(0xFF451A03)

val AnimatedGradientSpectrum = listOf(
    Color(0xFF4F46E5), // Electric Indigo
    Color(0xFF0D9488), // Vivid Teal
    Color(0xFFF59E0B), // Warm Amber
    Color(0xFFE11D48), // Rose Crimson
    Color(0xFF4F46E5)  // Loop back smoothly
)

enum class AccentPreset(
    val key: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
) {
    INDIGO("INDIGO", "Royal Indigo", Color(0xFF4338CA), Color(0xFF0D9488), Color(0xFFF59E0B)),
    TEAL("TEAL", "Ocean Teal", Color(0xFF0F766E), Color(0xFF2563EB), Color(0xFFF59E0B)),
    CRIMSON("CRIMSON", "Campus Crimson", Color(0xFFBE123C), Color(0xFF4F46E5), Color(0xFF0D9488)),
    AMBER("AMBER", "Heritage Amber", Color(0xFFB45309), Color(0xFF0F766E), Color(0xFF4338CA)),
    EMERALD("EMERALD", "Forest Emerald", Color(0xFF047857), Color(0xFF0284C7), Color(0xFFF59E0B));

    companion object {
        fun fromKey(key: String?): AccentPreset =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: INDIGO
    }
}
