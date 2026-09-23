package com.example.ui.theme

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.intellij.lang.annotations.Language

/**
 * Apple Liquid & Glassmorphism Design System for HomEase.
 *
 * Implements the optical glass and signed distance field (SDF) shaders inspired by
 * Kyant0/AndroidLiquidGlass:
 * - AGSL Signed Distance Field (SDF) rounded rect curvature
 * - Spherical lens refraction mapping (circleMap) & spectral chromatic dispersion
 * - Directional 45-degree specular rim highlight with power falloff
 * - Multi-stop specular gradients and diffuse ambient shadows
 * - Tactile physical spring-damped interaction physics
 */

// AGSL Shaders (Android 13+ / API 33+)
@Language("AGSL")
private const val RoundedRectSDF = """
float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}
"""

@Language("AGSL")
private const val SpecularHighlightShaderString = """
uniform float2 size;
uniform float4 cornerRadii;
layout(color) uniform half4 color;
uniform float angle;
uniform float falloff;

$RoundedRectSDF

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float radius = radiusAt(coord, cornerRadii);
    
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
    float2 normal = float2(cos(angle), sin(angle));
    float d = dot(grad, normal);
    float intensity = pow(abs(d), falloff);
    float t = step(0.0, d);
    return half4(color.rgb * t, color.a * intensity);
}
"""

@Language("AGSL")
private const val LensRefractionShaderString = """
uniform shader content;
uniform float2 size;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float chromaticAberration;

$RoundedRectSDF

float circleMap(float x) {
    return 1.0 - sqrt(max(0.0, 1.0 - x * x));
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float radius = radiusAt(coord, cornerRadii);
    
    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);
    
    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius));
    
    float2 refractedCoord = coord + d * grad;
    if (chromaticAberration <= 0.0) {
        return content.eval(refractedCoord);
    }
    
    float dispersion = chromaticAberration * 3.5;
    half4 color = half4(0.0);
    color.r = content.eval(refractedCoord + grad * dispersion).r;
    color.g = content.eval(refractedCoord).g;
    color.b = content.eval(refractedCoord - grad * dispersion).b;
    color.a = content.eval(refractedCoord).a;
    return color;
}
"""

/**
 * Runtime AGSL Shader provider with safe version gating and fallback.
 */
object LiquidGlassShaderEngine {
    val isSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createLensRefractionEffect(
        width: Float,
        height: Float,
        radius: Float,
        refractionHeight: Float = 16f,
        refractionAmount: Float = -12f,
        chromaticAberration: Float = 0.65f
    ): RenderEffect? {
        return try {
            val shader = android.graphics.RuntimeShader(LensRefractionShaderString)
            shader.setFloatUniform("size", width, height)
            shader.setFloatUniform("cornerRadii", radius, radius, radius, radius)
            shader.setFloatUniform("refractionHeight", refractionHeight)
            shader.setFloatUniform("refractionAmount", refractionAmount)
            shader.setFloatUniform("chromaticAberration", chromaticAberration)
            android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        } catch (_: Exception) {
            null
        }
    }
}

// Directional Specular Borders (Simulates 45° overhead studio lighting)
val AppleGlassSpecularBorder = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.95f),   // Bright specular glint top-left
        Color.White.copy(alpha = 0.45f),   // Soft rim highlight
        Color.White.copy(alpha = 0.12f),   // Subtle edge
        Color(0x08000000)                  // Ambient contact shadow bottom-right
    ),
    start = androidx.compose.ui.geometry.Offset(0f, 0f),
    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

val AppleGlassLightBorder = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.90f),
        Color.White.copy(alpha = 0.35f),
        Color(0x0A000000)
    )
)

val AppleGlassDarkBorder = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.28f),
        Color.White.copy(alpha = 0.08f),
        Color(0x00000000)
    )
)

val AppleGlassIrisBorder = Brush.linearGradient(
    colors = listOf(
        Color(0x4D4F46E5),  // Iris rim glint
        Color(0x1A4F46E5),
        Color.Transparent
    )
)

val AppleGlassChampagneBorder = Brush.linearGradient(
    colors = listOf(
        Color(0x59D97706),  // Champagne rim glint
        Color(0x1FD97706),
        Color.Transparent
    )
)

// Legacy alias for compatibility
val AppleGlassAccentBorder = AppleGlassIrisBorder

/**
 * Applies a true liquid frosted glass surface with specular highlight,
 * subtle inner bevel, and diffuse ambient depth.
 */
fun Modifier.liquidGlassCard(
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.84f),
    elevation: Dp = 4.dp,
    borderBrush: Brush = AppleGlassSpecularBorder,
    borderWidth: Dp = 0.75.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        spotColor = Color(0x141E293B),
        ambientColor = Color(0x0A1E293B)
    )
    .clip(shape)
    .background(backgroundColor)
    .border(borderWidth, borderBrush, shape)

/**
 * Deep, sleek Apple space-glass card for hero sections and focus widgets.
 */
fun Modifier.liquidGlassDarkCard(
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 6.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        spotColor = Color(0x330F172A),
        ambientColor = Color(0x1A0F172A)
    )
    .clip(shape)
    .background(
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E2638),
                Color(0xFF131822)
            ),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    )
    .border(0.75.dp, AppleGlassDarkBorder, shape)

/**
 * Interactive pill container with glassmorphic specular finish.
 */
fun Modifier.liquidGlassPill(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.88f),
    borderBrush: Brush = AppleGlassSpecularBorder
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .border(0.5.dp, borderBrush, shape)

/**
 * Interactive liquid glass button with tactile touch feedback and specular edge.
 */
fun Modifier.liquidGlassButton(
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(18.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.90f),
    borderBrush: Brush = AppleGlassSpecularBorder
): Modifier = this
    .liquidGlassCard(shape = shape, backgroundColor = backgroundColor, borderBrush = borderBrush)
    .appleSpringPress(onClick = onClick)

/**
 * Tactile physical spring interaction directly aligned with Apple HIG:
 * Responds immediately on pointer-down (scale to 0.965f)
 * and springs back smoothly on release without bounce overshoot.
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
 * Pearlescent ambient background canvas that paints soft multi-spectral glow orbs
 * (Soft Iris, Warm Linen, and Emerald Mist) behind content.
 * Delivers deep, luxurious optical depth under frosted glass components.
 */
@Composable
fun LiquidAmbientCanvas(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7FB))
    ) {
        // Soft Iris/Sapphire ambient orb in top-right
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x0F4F46E5), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(1000f, 150f),
                        radius = 950f
                    )
                )
        )
        // Soft Champagne/Warm Linen ambient glow in mid-left
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x0AD97706), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(100f, 900f),
                        radius = 850f
                    )
                )
        )
        // Soft Emerald/Mint ambient breath in bottom-right
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x0810B981), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(900f, 1800f),
                        radius = 800f
                    )
                )
        )
        content()
    }
}
