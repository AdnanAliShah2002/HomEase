package com.example.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple Liquid & Glassmorphism Design System for HomEase.
 *
 * Implements translucent materials, specular highlight borders,
 * soft ambient shadows, and physical spring-based interactions
 * directly aligned with Apple's Human Interface Guidelines.
 */

// Specular Highlight Gradient - simulates light catching the beveled glass edge
val AppleGlassLightBorder = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.90f),
        Color.White.copy(alpha = 0.30f),
        Color(0x0A000000)
    )
)

val AppleGlassDarkBorder = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.22f),
        Color.White.copy(alpha = 0.06f),
        Color(0x00000000)
    )
)

val AppleGlassAccentBorder = Brush.verticalGradient(
    colors = listOf(
        AppleSystemBlue.copy(alpha = 0.35f),
        AppleSystemBlue.copy(alpha = 0.10f),
        Color.Transparent
    )
)

/**
 * Applies a frosted glass surface with specular highlight and ambient depth.
 */
fun Modifier.liquidGlassCard(
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.88f),
    elevation: Dp = 4.dp,
    borderBrush: Brush = AppleGlassLightBorder,
    borderWidth: Dp = 0.75.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        spotColor = Color(0x12000000),
        ambientColor = Color(0x08000000)
    )
    .clip(shape)
    .background(backgroundColor)
    .border(borderWidth, borderBrush, shape)

/**
 * Deep, sleek Apple space-glass card for hero cards and focus elements.
 */
fun Modifier.liquidGlassDarkCard(
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 6.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        spotColor = Color(0x28000000),
        ambientColor = Color(0x14000000)
    )
    .clip(shape)
    .background(
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E2638),
                Color(0xFF151922)
            )
        )
    )
    .border(0.75.dp, AppleGlassDarkBorder, shape)

/**
 * Interactive pill container with glassmorphic finish.
 */
fun Modifier.liquidGlassPill(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.90f),
    borderBrush: Brush = AppleGlassLightBorder
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(0.5.dp, borderBrush, shape)

/**
 * Tactile physical spring interaction on tap:
 * Responds immediately on pointer-down (scale to 0.965f)
 * and springs back smoothly on release.
 */
fun Modifier.appleSpringPress(
    pressedScale: Float = 0.965f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "apple_spring_scale"
    )

    val modifier = this.scale(scale)
    if (onClick != null) {
        modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        modifier
    }
}

/**
 * Ambient background container that paints soft, subtle liquid glow orbs
 * behind content to give depth to frosted glass elements.
 */
@Composable
fun LiquidAmbientCanvas(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F5F9))
    ) {
        // Soft ice-blue ambient blur in top-right
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x0C007AFF), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(1000f, 150f),
                        radius = 900f
                    )
                )
        )
        // Soft warm amber/peach ambient glow in mid-left
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x08FF9500), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(100f, 900f),
                        radius = 800f
                    )
                )
        )
        content()
    }
}
