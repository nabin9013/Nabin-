package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PurpleThemeScheme = darkColorScheme(
    primary = NeonMagenta,
    onPrimary = CosmicObsidian,
    primaryContainer = CosmicSurfaceHigh,
    onPrimaryContainer = InkPrimary,
    secondary = LuminousCyan,
    onSecondary = CosmicObsidian,
    secondaryContainer = CosmicSurface,
    onSecondaryContainer = InkPrimary,
    tertiary = EmeraldLive,
    onTertiary = CosmicObsidian,
    background = CosmicObsidian,
    onBackground = InkPrimary,
    surface = CosmicMidnight,
    onSurface = InkPrimary,
    surfaceVariant = CosmicSurface,
    onSurfaceVariant = InkSecondary,
    outline = CosmicOutline,
    outlineVariant = CosmicSurfaceHigh,
    error = CoralAlert,
    onError = CosmicObsidian
)

private val NeonOrchidThemeScheme = darkColorScheme(
    primary = ElectricPink,
    onPrimary = CosmicObsidian,
    primaryContainer = Color(0xFF33154B),
    onPrimaryContainer = InkPrimary,
    secondary = NeonOrchid,
    onSecondary = CosmicObsidian,
    secondaryContainer = CosmicSurface,
    onSecondaryContainer = InkPrimary,
    tertiary = EmeraldLive,
    onTertiary = CosmicObsidian,
    background = Color(0xFF0C0518),
    onBackground = InkPrimary,
    surface = Color(0xFF170B2D),
    onSurface = InkPrimary,
    surfaceVariant = Color(0xFF221142),
    onSurfaceVariant = InkSecondary,
    outline = Color(0xFF4A2879),
    outlineVariant = Color(0xFF33154B),
    error = CoralAlert,
    onError = CosmicObsidian
)

private val MidnightCyanThemeScheme = darkColorScheme(
    primary = LuminousCyan,
    onPrimary = CosmicObsidian,
    primaryContainer = Color(0xFF112948),
    onPrimaryContainer = InkPrimary,
    secondary = NeonOrchid,
    onSecondary = CosmicObsidian,
    secondaryContainer = Color(0xFF0E1D38),
    onSecondaryContainer = InkPrimary,
    tertiary = EmeraldLive,
    onTertiary = CosmicObsidian,
    background = Color(0xFF050A18),
    onBackground = InkPrimary,
    surface = Color(0xFF0A142B),
    onSurface = InkPrimary,
    surfaceVariant = Color(0xFF102040),
    onSurfaceVariant = InkSecondary,
    outline = Color(0xFF234173),
    outlineVariant = Color(0xFF112948),
    error = CoralAlert,
    onError = CosmicObsidian
)

@Composable
fun MayraTheme(
    themeVariant: String = "Purple",
    content: @Composable () -> Unit
) {
    val scheme = when (themeVariant) {
        "Neon Orchid" -> NeonOrchidThemeScheme
        "Midnight Cyan" -> MidnightCyanThemeScheme
        else -> PurpleThemeScheme
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = MayraTypography,
        content = content
    )
}
