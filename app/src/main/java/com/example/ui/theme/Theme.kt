package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.data.model.MobileAppTheme

/**
 * Dynamic Compose theme wrapper that applies colors from the active MobileAppTheme.
 * Supports runtime updates triggered remotely via the admin dashboard without a new app build.
 */
@Composable
fun HomEaseDynamicTheme(
    activeTheme: MobileAppTheme,
    content: @Composable () -> Unit
) {
    // Keep dynamic semantic holder in sync with active theme state
    DynamicThemeHolder.updateFromTheme(activeTheme)

    val primary = remember(activeTheme.primaryColor) {
        parseColorSafe(activeTheme.primaryColor, CoralPrimary)
    }
    val accent = remember(activeTheme.accentColor) {
        parseColorSafe(activeTheme.accentColor, TealAccent)
    }
    val bg = remember(activeTheme.backgroundColor) {
        parseColorSafe(activeTheme.backgroundColor, BackgroundCream)
    }
    val text = remember(activeTheme.textColor) {
        parseColorSafe(activeTheme.textColor, TextWarmDark)
    }

    val colorScheme = lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.12f),
        onPrimaryContainer = primary,

        secondary = accent,
        onSecondary = Color.White,
        secondaryContainer = accent.copy(alpha = 0.12f),
        onSecondaryContainer = accent,

        background = bg,
        onBackground = text,

        surface = Color.White,
        onSurface = text,
        surfaceVariant = if (bg == Color.White) Color(0xFFF8FAFC) else bg.copy(alpha = 0.85f),
        onSurfaceVariant = text.copy(alpha = 0.65f),

        outline = text.copy(alpha = 0.14f),
        outlineVariant = text.copy(alpha = 0.08f)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun HomEaseTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    HomEaseDynamicTheme(
        activeTheme = DynamicThemeHolder.currentTheme,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    HomEaseTheme(darkTheme = darkTheme, content = content)
}

