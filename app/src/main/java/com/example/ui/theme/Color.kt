package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.data.model.MobileAppTheme

// Apple Human Interface Guidelines (HIG) System Palette
val AppleSystemBlue = Color(0xFF007AFF)
val AppleSystemBlueDark = Color(0xFF0056B3)
val AppleSystemBlueLight = Color(0xFF47A0FF)
val AppleSystemBlueContainer = Color(0xFFEBF5FF)

val AppleSystemOrange = Color(0xFFFF9500)
val AppleSystemOrangeDark = Color(0xFFCC7700)
val AppleSystemOrangeLight = Color(0xFFFFB340)
val AppleSystemOrangeContainer = Color(0xFFFFF4E5)

val AppleSystemGreen = Color(0xFF34C759)
val AppleSystemGreenContainer = Color(0xFFEAF9EE)
val AppleSystemYellow = Color(0xFFFFCC00)
val AppleSystemYellowContainer = Color(0xFFFFFBE5)
val AppleSystemRed = Color(0xFFFF3B30)
val AppleSystemRedContainer = Color(0xFFFFEBEA)
val AppleSystemIndigo = Color(0xFF5856D6)
val AppleSystemIndigoContainer = Color(0xFFEFEFFB)
val AppleSystemTeal = Color(0xFF30B0C7)
val AppleSystemTealContainer = Color(0xFFE6F7F9)
val AppleSystemPurple = Color(0xFFAF52DE)
val AppleSystemPink = Color(0xFFFF2D55)

// Apple Neutrals & Grouped Surfaces
val AppleGroupedBackground = Color(0xFFF2F2F7)
val AppleSecondaryGroupedBackground = Color(0xFFFFFFFF)
val AppleTertiaryGroupedBackground = Color(0xFFF9F9FB)

val AppleLabelPrimary = Color(0xFF1C1C1E)
val AppleLabelSecondary = Color(0xFF8E8E93)
val AppleLabelTertiary = Color(0xFFAEAEB2)
val AppleLabelQuaternary = Color(0xFFC7C7CC)

val AppleSeparator = Color(0x333C3C43) // 20% overlay
val AppleOpaqueSeparator = Color(0xFFC6C6C8)
val AppleHairlineBorder = Color(0x0F000000) // ~6% black hairline border

// HomEase Brand Colors — Modern Apple HIG Palette
val CoralPrimary = AppleSystemBlue
val CoralPrimaryDark = AppleSystemBlueDark
val CoralPrimaryLight = AppleSystemBlueLight
val CoralPrimaryContainer = AppleSystemBlueContainer

val TealAccent = AppleSystemOrange
val TealAccentDark = AppleSystemOrangeDark
val TealAccentLight = AppleSystemOrangeLight
val TealAccentContainer = AppleSystemOrangeContainer

val BackgroundCream = AppleGroupedBackground
val SurfaceCream = AppleSecondaryGroupedBackground
val SurfaceVariantCream = AppleTertiaryGroupedBackground

val TextWarmDark = AppleLabelPrimary
val TextWarmMuted = AppleLabelSecondary
val TextWarmSubtle = AppleLabelTertiary

val WarmBorder = AppleOpaqueSeparator
val WarmBorderFocused = AppleSystemBlue

// Status indicators (Apple-tuned)
val StatusGreen = AppleSystemGreen
val StatusGreenContainer = AppleSystemGreenContainer
val StatusYellow = AppleSystemYellow
val StatusYellowContainer = AppleSystemYellowContainer
val StatusRed = AppleSystemRed
val StatusRedContainer = AppleSystemRedContainer

// Common modern UI accents
val EmeraldGreen = AppleSystemGreen

/**
 * Dynamic runtime theme state holder.
 * Backed by Compose mutableStateOf properties, allowing any composable reading
 * semantic colors to automatically recompose when a new theme is fetched remotely.
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

// Dynamic semantic bindings mapped directly to the active runtime theme
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
