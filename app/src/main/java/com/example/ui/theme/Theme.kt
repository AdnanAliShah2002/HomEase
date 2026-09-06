package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HomEaseLightColorScheme = lightColorScheme(
    primary = DeepIndigo,
    onPrimary = Color.White,
    primaryContainer = DeepIndigoContainer,
    onPrimaryContainer = DeepIndigoDark,
    
    secondary = SoftOrange,
    onSecondary = Color.White,
    secondaryContainer = SoftOrangeContainer,
    onSecondaryContainer = SoftOrangeDark,
    
    tertiary = DeepIndigoLight,
    onTertiary = Color.White,
    
    background = BackgroundLight,
    onBackground = TextSlate,
    
    surface = SurfaceLight,
    onSurface = TextSlate,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSlateMuted,
    
    outline = BorderStroke,
    outlineVariant = BorderStrokeFocused
)

private val HomEaseDarkColorScheme = darkColorScheme(
    primary = DeepIndigoLight,
    onPrimary = Color.White,
    primaryContainer = DeepIndigoDark,
    onPrimaryContainer = Color.White,
    
    secondary = SoftOrange,
    onSecondary = Color.Black,
    secondaryContainer = SoftOrangeDark,
    onSecondaryContainer = SoftOrangeLight,
    
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    
    outline = Color(0xFF475569)
)

@Composable
fun HomEaseTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HomEaseDarkColorScheme else HomEaseLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
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
