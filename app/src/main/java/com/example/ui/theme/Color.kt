package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.data.model.MobileAppTheme

// HomEase Brand Colors — Option D: "Coral Sunset" (Default Fallback)
val CoralPrimary = Color(0xFFDC5F45)
val CoralPrimaryDark = Color(0xFFB8442D)
val CoralPrimaryLight = Color(0xFFE97A63)
val CoralPrimaryContainer = Color(0xFFFDF0ED) // Warm blush / peach container

val TealAccent = Color(0xFF2A9D8F)
val TealAccentDark = Color(0xFF1F766C)
val TealAccentLight = Color(0xFF4EB8AA)
val TealAccentContainer = Color(0xFFE6F5F3) // Soft mint/teal container

val BackgroundCream = Color(0xFFFFFBF7)
val SurfaceCream = Color(0xFFFFFFFF)
val SurfaceVariantCream = Color(0xFFF7F1EA) // Warm soft cream-beige variant

val TextWarmDark = Color(0xFF292524)
val TextWarmMuted = Color(0xFF78716C) // Warm stone muted
val TextWarmSubtle = Color(0xFFA8A29E) // Warm stone subtle

val WarmBorder = Color(0xFFEDE5DB)
val WarmBorderFocused = Color(0xFFDC5F45)

// Status indicators (warm-tuned)
val StatusGreen = Color(0xFF10B981)
val StatusGreenContainer = Color(0xFFECFDF5)
val StatusYellow = Color(0xFFF59E0B)
val StatusYellowContainer = Color(0xFFFFFBEB)
val StatusRed = Color(0xFFEF4444)
val StatusRedContainer = Color(0xFFFEF2F2)

// Common modern UI accents
val EmeraldGreen = Color(0xFF059669)

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
