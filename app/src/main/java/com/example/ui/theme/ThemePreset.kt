package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AppThemePreset(val displayName: String) {
    SYSTEM("System Default"),
    GEOMETRIC_BALANCE("Geometric Balance 📐"),
    LAVENDER_NIGHT("Lavender Night 🌌"),
    OCEAN_BLUE("Ocean Blue 🌊"),
    MOONLIGHT_PURPLE("Moonlight Purple 🌙"),
    MINT_CALM("Calm Mint 🍃"),
    SAKURA_DREAM("Sakura Dream 🌸"),
    ARCTIC_WHITE("Arctic White ❄️"),
    SUNSET_PEACH("Sunset Peach 🍑"),
    AMOLED("Pure AMOLED 🪵")
}

// Pastel Palette Definitions
object ThemePalettes {

    // 0. Geometric Balance (Rich Indigo/Violet-Dark Background, Pastel Lavender accents, and beautiful curves)
    val GeometricBalanceDark = darkColorScheme(
        primary = Color(0xFFD0BCFF),
        secondary = Color(0xFF4A4458),
        tertiary = Color(0xFF334D43),
        background = Color(0xFF0F0D13),
        surface = Color(0xFF1D1B22),
        onPrimary = Color(0xFF381E72),
        onSecondary = Color(0xFFE6E1E5),
        onBackground = Color(0xFFE6E1E5),
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = Color(0xFF2D235C),
        onSurfaceVariant = Color(0xFF938F99)
    )
    val GeometricBalanceLight = lightColorScheme(
        primary = Color(0xFF4F378B),
        secondary = Color(0xFF334D43),
        tertiary = Color(0xFF2D235C),
        background = Color(0xFFFAF8FF),
        surface = Color(0xFFE6E1E5),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF1D1B22),
        onSurface = Color(0xFF1D1B22),
        surfaceVariant = Color(0xFFE5DEFF),
        onSurfaceVariant = Color(0xFF381E72)
    )

    // 1. Lavender Night (Pastel Purple & Soft Mystical Indigo background)
    val LavenderNightDark = darkColorScheme(
        primary = Color(0xFFD3C2FF),
        secondary = Color(0xFFAFA0FF),
        tertiary = Color(0xFFF1C7FF),
        background = Color(0xFF14111F),
        surface = Color(0xFF1E1A33),
        onPrimary = Color(0xFF28135C),
        onSecondary = Color(0xFF201054),
        onBackground = Color(0xFFE5DEFF),
        onSurface = Color(0xFFE5DEFF),
        surfaceVariant = Color(0xFF2D274A),
        onSurfaceVariant = Color(0xFFD6C8FF)
    )
    val LavenderNightLight = lightColorScheme(
        primary = Color(0xFF6B4BBF),
        secondary = Color(0xFF563B9B),
        tertiary = Color(0xFF8B47AC),
        background = Color(0xFFFAF8FF),
        surface = Color(0xFFFFFBFE),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF1B1A22),
        onSurface = Color(0xFF1B1A22),
        surfaceVariant = Color(0xFFF0ECF8),
        onSurfaceVariant = Color(0xFF4A4458)
    )

    // 2. Ocean Blue (Calm Marine Blues & Soft Teal)
    val OceanBlueDark = darkColorScheme(
        primary = Color(0xFF90E0EF),
        secondary = Color(0xFF00B4D8),
        tertiary = Color(0xFFCAF0F8),
        background = Color(0xFF0D1B2A),
        surface = Color(0xFF1B263B),
        onPrimary = Color(0xFF03045E),
        onSecondary = Color(0xFF004E64),
        onBackground = Color(0xFFE0FAFF),
        onSurface = Color(0xFFE0FAFF),
        surfaceVariant = Color(0xFF22354F),
        onSurfaceVariant = Color(0xFFBBE5FA)
    )
    val OceanBlueLight = lightColorScheme(
        primary = Color(0xFF0077B6),
        secondary = Color(0xFF0096C7),
        tertiary = Color(0xFF03045E),
        background = Color(0xFFF0F9FF),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF011627),
        onSurface = Color(0xFF011627)
    )

    // 3. Moonlight Purple (Midnight Dark Indigo & Vibrant Glowing Cyan details)
    val MoonlightPurpleDark = darkColorScheme(
        primary = Color(0xFFB79CED),
        secondary = Color(0xFF80ED99),
        tertiary = Color(0xFF57CC99),
        background = Color(0xFF0B0914),
        surface = Color(0xFF16122C),
        onPrimary = Color(0xFF22005C),
        onSecondary = Color(0xFF003810),
        onBackground = Color(0xFFECE6FF),
        onSurface = Color(0xFFECE6FF)
    )
    val MoonlightPurpleLight = lightColorScheme(
        primary = Color(0xFF7551D3),
        secondary = Color(0xFF1B905F),
        tertiary = Color(0xFF13704C),
        background = Color(0xFFF9F7FF),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF0E082B),
        onSurface = Color(0xFF0E082B)
    )

    // 4. Mint Calm (Sage Green & Muted Earth forest tones)
    val MintCalmDark = darkColorScheme(
        primary = Color(0xFFA3E4D7),
        secondary = Color(0xFF76D7C4),
        tertiary = Color(0xFFE8F8F5),
        background = Color(0xFF11221D),
        surface = Color(0xFF1D352F),
        onPrimary = Color(0xFF083E32),
        onSecondary = Color(0xFF023225),
        onBackground = Color(0xFFE0F4F0),
        onSurface = Color(0xFFE0F4F0)
    )
    val MintCalmLight = lightColorScheme(
        primary = Color(0xFF117A65),
        secondary = Color(0xFF16A085),
        tertiary = Color(0xFF0E6251),
        background = Color(0xFFF2FBF9),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF092922),
        onSurface = Color(0xFF092922)
    )

    // 5. Sakura Dream (Sweet Rose Cherry Blossom pink & Rich Cocoa/Sandalwood highlights)
    val SakuraDreamDark = darkColorScheme(
        primary = Color(0xFFFFB5C2),
        secondary = Color(0xFFFF8B94),
        tertiary = Color(0xFFFFE3E5),
        background = Color(0xFF2A1B1F),
        surface = Color(0xFF3D252B),
        onPrimary = Color(0xFF5A001C),
        onSecondary = Color(0xFF4C0012),
        onBackground = Color(0xFFFFEAED),
        onSurface = Color(0xFFFFEAED)
    )
    val SakuraDreamLight = lightColorScheme(
        primary = Color(0xFFB82C53),
        secondary = Color(0xFF9E1F42),
        tertiary = Color(0xFF5A001C),
        background = Color(0xFFFFF7F8),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF2E0C15),
        onSurface = Color(0xFF2E0C15)
    )

    // 6. Arctic White (Pristine Icy Blue accents & Minimal frosty slate backgrounds)
    val ArcticWhiteDark = darkColorScheme(
        primary = Color(0xFFCCFBFF),
        secondary = Color(0xFF99F6FF),
        tertiary = Color(0xFFE6FDFF),
        background = Color(0xFF121B22),
        surface = Color(0xFF1F2C37),
        onPrimary = Color(0xFF00363D),
        onSecondary = Color(0xFF003038),
        onBackground = Color(0xFFEDFCFF),
        onSurface = Color(0xFFEDFCFF)
    )
    val ArcticWhiteLight = lightColorScheme(
        primary = Color(0xFF006877),
        secondary = Color(0xFF005A68),
        tertiary = Color(0xFF00363D),
        background = Color(0xFFF3FBFC),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF0B1B20),
        onSurface = Color(0xFF0B1B20)
    )

    // 7. Sunset Peach (Warm cozy bonfire evening colors)
    val SunsetPeachDark = darkColorScheme(
        primary = Color(0xFFFFC09F),
        secondary = Color(0xFFFF9F1C),
        tertiary = Color(0xFFFFEE93),
        background = Color(0xFF261005),
        surface = Color(0xFF3A1C0E),
        onPrimary = Color(0xFF501C00),
        onSecondary = Color(0xFF4C0000),
        onBackground = Color(0xFFFFEDE6),
        onSurface = Color(0xFFFFEDE6)
    )
    val SunsetPeachLight = lightColorScheme(
        primary = Color(0xFF9E4B00),
        secondary = Color(0xFFB15F00),
        tertiary = Color(0xFF5A1C00),
        background = Color(0xFFFFFBF9),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF330900),
        onSurface = Color(0xFF330900)
    )

    // 8. Pure AMOLED (Pure #000000 jet black background)
    val AmoledDark = darkColorScheme(
        primary = Color(0xFFBB86FC),
        secondary = Color(0xFF03DAC6),
        tertiary = Color(0xFFCF6679),
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1C1C1D),
        onSurfaceVariant = Color(0xFFD6C8FF)
    )
}
