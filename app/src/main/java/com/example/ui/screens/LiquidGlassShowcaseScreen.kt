package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleGlassChampagneBorder
import com.example.ui.theme.AppleGlassDarkBorder
import com.example.ui.theme.AppleGlassIrisBorder
import com.example.ui.theme.AppleGlassSpecularBorder
import com.example.ui.theme.LiquidGlassShaderEngine
import com.example.ui.theme.appleSpringPress
import com.example.ui.theme.liquidGlassCard
import com.example.ui.theme.liquidGlassPill
import com.example.ui.theme.liquidGlassSurface
import kotlin.math.cos
import kotlin.math.sin

/**
 * Liquid Glass Studio & Interactive Playground.
 * Showcases the full optical power of Kyant0-inspired AGSL shaders,
 * signed distance field curvature, multi-spectral chromatic dispersion,
 * hardware blur, and Apple tactile spring physics.
 */
@Composable
fun LiquidGlassShowcaseScreen(
    onBack: () -> Unit
) {
    // 1. Dynamic Interactive Tuning Parameters
    var blurRadius by remember { mutableFloatStateOf(24f) }
    var glassAlpha by remember { mutableFloatStateOf(0.25f) }
    var selectedPreset by remember { mutableStateOf("Crystal") }
    var isChromaticEnabled by remember { mutableStateOf(true) }
    var isOrbAnimationActive by remember { mutableStateOf(true) }
    var isLightMode by remember { mutableStateOf(true) }
    var interactiveSliderValue by remember { mutableFloatStateOf(0.68f) }
    var interactiveToggleState by remember { mutableStateOf(true) }

    // Manual pan offset for glowing orb
    var manualOrbOffset by remember { mutableStateOf(Offset.Zero) }

    // Ambient floating orbital animations
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_orbs")
    val orbitPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_phase"
    )

    // Preset color palettes
    val activeBorderBrush = when (selectedPreset) {
        "Obsidian" -> AppleGlassDarkBorder
        "Iris" -> AppleGlassIrisBorder
        "Champagne" -> AppleGlassChampagneBorder
        else -> AppleGlassSpecularBorder
    }

    val activeGlassTint = when (selectedPreset) {
        "Obsidian" -> Color(0xFF0F172A).copy(alpha = glassAlpha.coerceAtLeast(0.45f))
        "Iris" -> Color(0xFF4F46E5).copy(alpha = (glassAlpha * 0.7f).coerceIn(0.12f, 0.45f))
        "Champagne" -> Color(0xFFD97706).copy(alpha = (glassAlpha * 0.7f).coerceIn(0.12f, 0.45f))
        else -> Color.White.copy(alpha = glassAlpha)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isLightMode) Color(0xFF0B0F19) else Color(0xFF020408))
    ) {
        // ====================================================================
        // LAYER 1: Dynamic Vivid Wallpaper with Glowing Light Orbs
        // (This is what shines and refracts through the liquid glass!)
        // ====================================================================
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        manualOrbOffset += dragAmount
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            val phase = if (isOrbAnimationActive) orbitPhase else 0f

            // Orb 1: Sunset Coral & Crimson Laser Orb (Top Right / Center)
            val orb1X = width * 0.75f + cos(phase) * 160f + manualOrbOffset.x
            val orb1Y = height * 0.22f + sin(phase) * 130f + manualOrbOffset.y
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF3366), Color(0xFFFF6B00), Color.Transparent),
                    center = Offset(orb1X, orb1Y),
                    radius = 380f
                ),
                radius = 380f,
                center = Offset(orb1X, orb1Y)
            )

            // Orb 2: Electric Violet / Cyber Magenta Orb (Mid Left)
            val orb2X = width * 0.25f + sin(phase * 0.8f) * 140f + manualOrbOffset.x * 0.5f
            val orb2Y = height * 0.52f + cos(phase * 0.8f) * 180f + manualOrbOffset.y * 0.5f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color.Transparent),
                    center = Offset(orb2X, orb2Y),
                    radius = 420f
                ),
                radius = 420f,
                center = Offset(orb2X, orb2Y)
            )

            // Orb 3: Cyan / Neon Emerald Wave (Bottom Right)
            val orb3X = width * 0.70f + cos(phase * 1.2f) * 130f
            val orb3Y = height * 0.82f + sin(phase * 1.2f) * 150f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF06B6D4), Color(0xFF10B981), Color.Transparent),
                    center = Offset(orb3X, orb3Y),
                    radius = 360f
                ),
                radius = 360f,
                center = Offset(orb3X, orb3Y)
            )

            // Orb 4: Warm Gold / Amber Focus Orb (Top Left)
            val orb4X = width * 0.15f + cos(phase * 0.6f) * 90f
            val orb4Y = height * 0.15f + sin(phase * 0.6f) * 90f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFBBF24), Color.Transparent),
                    center = Offset(orb4X, orb4Y),
                    radius = 280f
                ),
                radius = 280f,
                center = Offset(orb4X, orb4Y)
            )
        }

        // ====================================================================
        // LAYER 2: Hardware Frosted Blur Layer (Android 12+ API 31+)
        // Blurs the radiant colored wallpaper directly beneath foreground glass
        // ====================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0f) {
                        try {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                blurRadius,
                                blurRadius,
                                android.graphics.Shader.TileMode.CLAMP
                            ).asComposeRenderEffect()
                        } catch (_: Exception) {
                        }
                    }
                }
        )

        // ====================================================================
        // LAYER 3: Scrollable Foreground Liquid Glass UI & Controls
        // ====================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Navigation Bar (Liquid Glass Capsule)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .liquidGlassPill(
                            shape = CircleShape,
                            backgroundColor = activeGlassTint,
                            borderBrush = activeBorderBrush
                        )
                        .appleSpringPress(onClick = onBack)
                        .testTag("showcase_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Title Pill
                Box(
                    modifier = Modifier
                        .liquidGlassPill(
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = activeGlassTint,
                            borderBrush = activeBorderBrush
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "LIQUID GLASS STUDIO",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Reset Orbs Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .liquidGlassPill(
                            shape = CircleShape,
                            backgroundColor = activeGlassTint,
                            borderBrush = activeBorderBrush
                        )
                        .appleSpringPress(onClick = { manualOrbOffset = Offset.Zero }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = "Reset Orbs",
                        tint = Color.White
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Hero Translucent Glass Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            shape = RoundedCornerShape(28.dp),
                            tintColor = activeGlassTint,
                            specularSheenAlpha = 0.45f,
                            borderBrush = activeBorderBrush,
                            elevation = 12.dp
                        )
                        .padding(22.dp)
                        .testTag("hero_liquid_glass_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF))
                                )
                                Text(
                                    text = "APPLE HIG • AGSL SHADERS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    letterSpacing = 1.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .border(0.5.dp, activeBorderBrush, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "API ${Build.VERSION.SDK_INT} NATIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            text = "Dynamic Frosted Glass & Lens Refraction",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )

                        Text(
                            text = "Touch and drag anywhere on the screen to physically move the glowing light orbs behind this frosted surface.",
                            fontSize = 13.5.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 19.sp
                        )

                        // Preset Selector Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Crystal", "Obsidian", "Iris", "Champagne").forEach { preset ->
                                val isSelected = selectedPreset == preset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.35f)
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(
                                            width = if (isSelected) 1.25.dp else 0.5.dp,
                                            brush = if (isSelected) AppleGlassSpecularBorder else AppleGlassDarkBorder,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .appleSpringPress { selectedPreset = preset }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Real-World HomEase Live Service Glass Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            shape = RoundedCornerShape(26.dp),
                            tintColor = activeGlassTint,
                            specularSheenAlpha = 0.38f,
                            borderBrush = activeBorderBrush,
                            elevation = 8.dp
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB).copy(alpha = 0.30f))
                                        .border(1.dp, AppleGlassSpecularBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Emergency Electrician",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Tariq Mahmood • Verified Pro",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }

                            // Live ETA Glass Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.25f))
                                    .border(1.dp, AppleGlassSpecularBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "~3 MIN",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }

                        // Glass Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .liquidGlassPill(
                                        shape = RoundedCornerShape(14.dp),
                                        backgroundColor = Color(0xFF10B981).copy(alpha = 0.35f),
                                        borderBrush = AppleGlassSpecularBorder
                                    )
                                    .appleSpringPress {},
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text("Free HD Call", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .liquidGlassPill(
                                        shape = RoundedCornerShape(14.dp),
                                        backgroundColor = Color(0xFF3B82F6).copy(alpha = 0.35f),
                                        borderBrush = AppleGlassSpecularBorder
                                    )
                                    .appleSpringPress {},
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Chat, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text("Job Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Section 3: Live Optical Parameter Tuner (Control Center)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            shape = RoundedCornerShape(26.dp),
                            tintColor = activeGlassTint,
                            specularSheenAlpha = 0.35f,
                            borderBrush = activeBorderBrush,
                            elevation = 8.dp
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text(
                                text = "REAL-TIME OPTICAL TUNER",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // 1. Hardware Blur Radius Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Frosted Blur Radius", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                                Text("${blurRadius.toInt()} px", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                            }
                            Slider(
                                value = blurRadius,
                                onValueChange = { blurRadius = it },
                                valueRange = 0f..60f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF3B82F6),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }

                        // 2. Glass Surface Opacity Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Glass Transparency Alpha", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                                Text("${(glassAlpha * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            }
                            Slider(
                                value = glassAlpha,
                                onValueChange = { glassAlpha = it },
                                valueRange = 0.08f..0.85f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF10B981),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }

                        // 3. Toggles: Chromatic Aberration & Animated Orbs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Orb Motion Physics", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Auto-glide glowing lights", fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f))
                            }
                            Switch(
                                checked = isOrbAnimationActive,
                                onCheckedChange = { isOrbAnimationActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF3B82F6)
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Chromatic Prism Dispersion", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("Spectral 7-band RGB split", fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f))
                            }
                            Switch(
                                checked = isChromaticEnabled,
                                onCheckedChange = { isChromaticEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF8B5CF6)
                                )
                            )
                        }
                    }
                }

                // Section 4: Interactive Liquid Slider & Liquid Toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassSurface(
                            shape = RoundedCornerShape(26.dp),
                            tintColor = activeGlassTint,
                            specularSheenAlpha = 0.35f,
                            borderBrush = activeBorderBrush,
                            elevation = 8.dp
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Layers, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text(
                                text = "TACTILE LIQUID CONTROLS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Interactive Liquid Slider
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Fluid Haptic Slider", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                                Text("${(interactiveSliderValue * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                            }
                            Slider(
                                value = interactiveSliderValue,
                                onValueChange = { interactiveSliderValue = it },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFFF59E0B),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }

                        // Interactive Tactile Button with Apple Spring Physics
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .liquidGlassPill(
                                    shape = RoundedCornerShape(16.dp),
                                    backgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.40f),
                                    borderBrush = AppleGlassSpecularBorder
                                )
                                .appleSpringPress {
                                    // Bounces with Apple spring physics!
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.WaterDrop, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "Tap for Apple Spring Scale Feedback",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
