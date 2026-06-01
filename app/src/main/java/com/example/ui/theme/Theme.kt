package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun MyApplicationTheme(
    themePreset: AppThemePreset = AppThemePreset.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themePreset) {
        AppThemePreset.SYSTEM -> {
            if (darkTheme) ThemePalettes.GeometricBalanceDark else ThemePalettes.GeometricBalanceLight
        }
        AppThemePreset.GEOMETRIC_BALANCE -> {
            if (darkTheme) ThemePalettes.GeometricBalanceDark else ThemePalettes.GeometricBalanceLight
        }
        AppThemePreset.LAVENDER_NIGHT -> {
            if (darkTheme) ThemePalettes.LavenderNightDark else ThemePalettes.LavenderNightLight
        }
        AppThemePreset.OCEAN_BLUE -> {
            if (darkTheme) ThemePalettes.OceanBlueDark else ThemePalettes.OceanBlueLight
        }
        AppThemePreset.MOONLIGHT_PURPLE -> {
            if (darkTheme) ThemePalettes.MoonlightPurpleDark else ThemePalettes.MoonlightPurpleLight
        }
        AppThemePreset.MINT_CALM -> {
            if (darkTheme) ThemePalettes.MintCalmDark else ThemePalettes.MintCalmLight
        }
        AppThemePreset.SAKURA_DREAM -> {
            if (darkTheme) ThemePalettes.SakuraDreamDark else ThemePalettes.SakuraDreamLight
        }
        AppThemePreset.ARCTIC_WHITE -> {
            if (darkTheme) ThemePalettes.ArcticWhiteDark else ThemePalettes.ArcticWhiteLight
        }
        AppThemePreset.SUNSET_PEACH -> {
            if (darkTheme) ThemePalettes.SunsetPeachDark else ThemePalettes.SunsetPeachLight
        }
        AppThemePreset.AMOLED -> {
            ThemePalettes.AmoledDark
        }
        AppThemePreset.COSMIC_FOREST -> {
            if (darkTheme) ThemePalettes.CosmicForestDark else ThemePalettes.CosmicForestLight
        }
        AppThemePreset.STARRY_NEBULA -> {
            if (darkTheme) ThemePalettes.StarryNebulaDark else ThemePalettes.StarryNebulaLight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
