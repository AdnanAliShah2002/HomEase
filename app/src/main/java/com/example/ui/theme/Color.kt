package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.data.model.MobileAppTheme

/**
 * Apple Pro Liquid Glass Palette for HomEase.
 *
 * Eliminates the harsh electric-blue (#007AFF) and carrot-orange (#FF9500) clash.
 * Delivers an authentic luxury aesthetic with:
 * - Apple Iris / Royal Sapphire (#4F46E5) as the primary brand tone
 * - Warm Champagne / Muted Amber (#D97706) as the craftsman warm accent
 * - Apple System Emerald (#10B981) for live active states
 * - Deep Slate (#1E293B) and clean grouped surfaces (#F6F7FB)
 */

// Apple Pro Primary - Deep Iris Sapphire
val AppleIrisPrimary = Color(0xFF4F46E5)
val AppleIrisPrimaryDark = Color(0xFF3730A3)
val AppleIrisPrimaryLight = Color(0xFF6366F1)
val AppleIrisContainer = Color(0xFFEEF2FF)

// Apple Pro Accent - Warm Champagne Bronze & Amber
val AppleWarmChampagne = Color(0xFFD97706)
val AppleWarmChampagneDark = Color(0xFFB45309)
val AppleWarmChampagneLight = Color(0xFFF59E0B)
val AppleWarmChampagneContainer = Color(0xFFFEF3C7)

// Apple System Functional Colors
val AppleSystemEmerald = Color(0xFF10B981)
val AppleSystemEmeraldDark = Color(0xFF059669)
val AppleSystemEmeraldLight = Color(0xFF34D399)
val AppleSystemEmeraldContainer = Color(0xFFECFDF5)

val AppleSystemGreen = AppleSystemEmerald
val AppleSystemGreenContainer = AppleSystemEmeraldContainer

val AppleSystemYellow = Color(0xFFF59E0B)
val AppleSystemYellowContainer = Color(0xFFFEF3C7)

val AppleSystemRed = Color(0xFFEF4444)
val AppleSystemRedContainer = Color(0xFFFEE2E2)

val AppleSystemIndigo = Color(0xFF6366F1)
val AppleSystemIndigoContainer = Color(0xFFEEF2FF)

val AppleSystemTeal = Color(0xFF0EA5E9)
val AppleSystemTealContainer = Color(0xFFE0F2FE)

val AppleSystemPurple = Color(0xFF8B5CF6)
val AppleSystemPink = Color(0xFFEC4899)

// Unified aliases so all existing references cleanly inherit the refined Apple Pro palette
val AppleSystemBlue = AppleIrisPrimary
val AppleSystemBlueDark = AppleIrisPrimaryDark
val AppleSystemBlueLight = AppleIrisPrimaryLight
val AppleSystemBlueContainer = AppleIrisContainer

val AppleSystemOrange = AppleWarmChampagne
val AppleSystemOrangeDark = AppleWarmChampagneDark
val AppleSystemOrangeLight = AppleWarmChampagneLight
val AppleSystemOrangeContainer = AppleWarmChampagneContainer

// Apple Neutrals & Grouped Surfaces
val AppleGroupedBackground = Color(0xFFF6F7FB)
val AppleSecondaryGroupedBackground = Color(0xFFFFFFFF)
val AppleTertiaryGroupedBackground = Color(0xFFF9FAFC)

val AppleLabelPrimary = Color(0xFF1E293B)
val AppleLabelSecondary = Color(0xFF64748B)
val AppleLabelTertiary = Color(0xFF94A3B8)
val AppleLabelQuaternary = Color(0xFFCBD5E1)

val AppleSeparator = Color(0x1F1E293B)
val AppleOpaqueSeparator = Color(0xFFE2E8F0)
val AppleHairlineBorder = Color(0x0F000000)

// HomEase Brand Colors
val CoralPrimary = AppleIrisPrimary
val CoralPrimaryDark = AppleIrisPrimaryDark
val CoralPrimaryLight = AppleIrisPrimaryLight
val CoralPrimaryContainer = AppleIrisContainer

val TealAccent = AppleWarmChampagne
val TealAccentDark = AppleWarmChampagneDark
val TealAccentLight = AppleWarmChampagneLight
val TealAccentContainer = AppleWarmChampagneContainer

val BackgroundCream = AppleGroupedBackground
val SurfaceCream = AppleSecondaryGroupedBackground
val SurfaceVariantCream = AppleTertiaryGroupedBackground

val TextWarmDark = AppleLabelPrimary
val TextWarmMuted = AppleLabelSecondary
val TextWarmSubtle = AppleLabelTertiary

val WarmBorder = AppleOpaqueSeparator
val WarmBorderFocused = AppleIrisPrimary

// Status indicators
val StatusGreen = AppleSystemEmerald
val StatusGreenContainer = AppleSystemEmeraldContainer
val StatusYellow = AppleSystemYellow
val StatusYellowContainer = AppleSystemYellowContainer
val StatusRed = AppleSystemRed
val StatusRedContainer = AppleSystemRedContainer

val EmeraldGreen = AppleSystemEmerald

/**
 * Dynamic runtime theme state holder.
 */
object DynamicThemeHolder {
    var currentTheme by mutableStateOf(MobileAppTheme.DEFAULT_FALLBACK_THEME)
        private set

    var primary by mutableStateOf(CoralPrimary)
    var primaryDark by mutableStateOf(CoralPrimaryDark)
    var primaryLight by mutableStateOf(CoralPrimaryLight)
    var primaryContainer by mutableStateOf(CoralPrimaryContainer)

    var accent by mutableStateOf(TealAccent)
    var accentDark by mutableStateOf(TealAccentDark)
    var accentLight by mutableStateOf(TealAccentLight)
    var accentContainer by mutableStateOf(TealAccentContainer)

    var background by mutableStateOf(BackgroundCream)
    var surface by mutableStateOf(SurfaceCream)
    var surfaceVariant by mutableStateOf(SurfaceVariantCream)

    var text by mutableStateOf(TextWarmDark)
    var textMuted by mutableStateOf(TextWarmMuted)
    var textSubtle by mutableStateOf(TextWarmSubtle)

    var border by mutableStateOf(WarmBorder)
    var borderFocused by mutableStateOf(WarmBorderFocused)

    fun updateFromTheme(theme: MobileAppTheme) {
        currentTheme = theme
        val p = parseColorSafe(theme.primaryColor, CoralPrimary)
        val a = parseColorSafe(theme.accentColor, TealAccent)
        val bg = parseColorSafe(theme.backgroundColor, BackgroundCream)
        val t = parseColorSafe(theme.textColor, TextWarmDark)

        primary = p
        primaryDark = darken(p, 0.18f)
        primaryLight = lighten(p, 0.18f)
        primaryContainer = p.copy(alpha = 0.12f)

        accent = a
        accentDark = darken(a, 0.18f)
        accentLight = lighten(a, 0.18f)
        accentContainer = a.copy(alpha = 0.12f)

        background = bg
        surface = Color.White
        surfaceVariant = if (bg == Color.White) Color(0xFFF8FAFC) else bg.copy(alpha = 0.85f)

        text = t
        textMuted = t.copy(alpha = 0.65f)
        textSubtle = t.copy(alpha = 0.45f)

        border = t.copy(alpha = 0.14f)
        borderFocused = p
    }
}

fun parseColorSafe(colorString: String?, fallback: Color): Color {
    if (colorString.isNullOrBlank()) return fallback
    return try {
        val clean = colorString.trim()
        val formatted = if (clean.startsWith("#")) clean else "#$clean"
        Color(android.graphics.Color.parseColor(formatted))
    } catch (_: Exception) {
        fallback
    }
}

fun darken(color: Color, factor: Float = 0.2f): Color {
    val r = (color.red * (1f - factor)).coerceIn(0f, 1f)
    val g = (color.green * (1f - factor)).coerceIn(0f, 1f)
    val b = (color.blue * (1f - factor)).coerceIn(0f, 1f)
    return Color(r, g, b, color.alpha)
}

fun lighten(color: Color, factor: Float = 0.2f): Color {
    val r = (color.red + (1f - color.red) * factor).coerceIn(0f, 1f)
    val g = (color.green + (1f - color.green) * factor).coerceIn(0f, 1f)
    val b = (color.blue + (1f - color.blue) * factor).coerceIn(0f, 1f)
    return Color(r, g, b, color.alpha)
}

// Semantic dynamic tokens
val DeepIndigo: Color get() = DynamicThemeHolder.primary
val DeepIndigoDark: Color get() = DynamicThemeHolder.primaryDark
val DeepIndigoLight: Color get() = DynamicThemeHolder.primaryLight
val DeepIndigoContainer: Color get() = DynamicThemeHolder.primaryContainer

val SoftOrange: Color get() = DynamicThemeHolder.accent
val SoftOrangeDark: Color get() = DynamicThemeHolder.accentDark
val SoftOrangeLight: Color get() = DynamicThemeHolder.accentLight
val SoftOrangeContainer: Color get() = DynamicThemeHolder.accentContainer

val BackgroundLight: Color get() = DynamicThemeHolder.background
val SurfaceLight: Color get() = DynamicThemeHolder.surface
val SurfaceVariantLight: Color get() = DynamicThemeHolder.surfaceVariant

val TextSlate: Color get() = DynamicThemeHolder.text
val TextSlateMuted: Color get() = DynamicThemeHolder.textMuted
val TextSlateSubtle: Color get() = DynamicThemeHolder.textSubtle

val BorderStroke: Color get() = DynamicThemeHolder.border
val BorderStrokeFocused: Color get() = DynamicThemeHolder.borderFocused
