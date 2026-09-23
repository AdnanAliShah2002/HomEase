package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.ui.components.HomeaseLogoMark
import com.example.ui.theme.AppleGlassSpecularBorder
import com.example.ui.theme.AppleIrisPrimary
import com.example.ui.theme.AppleLabelSecondary
import com.example.ui.theme.AppleWarmChampagne
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.LiquidAmbientCanvas
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.liquidGlassCard
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SplashScreen(
    language: AppLanguage,
    onTimeout: () -> Unit
) {
    var isStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isStarted = true
        delay(2200)
        onTimeout()
    }

    // Logo icon spring scale and fade
    val logoScale by animateFloatAsState(
        targetValue = if (isStarted) 1f else 0.45f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splash_logo_scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (isStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "splash_logo_alpha"
    )

    // Brand title slide and fade (staggered +200ms)
    val titleAlpha by animateFloatAsState(
        targetValue = if (isStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "splash_title_alpha"
    )
    val titleOffsetY by animateFloatAsState(
        targetValue = if (isStarted) 0f else 28f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splash_title_offset"
    )

    // Tagline fade and slide (staggered +400ms)
    val taglineAlpha by animateFloatAsState(
        targetValue = if (isStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "splash_tagline_alpha"
    )
    val taglineOffsetY by animateFloatAsState(
        targetValue = if (isStarted) 0f else 18f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splash_tagline_offset"
    )

    // Pulsing halo glow behind logo
    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    LiquidAmbientCanvas {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated App Icon Badge with Glowing Aura
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(logoScale)
                        .alpha(logoAlpha)
                ) {
                    // Soft Iris Ambient Halo
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(haloPulse)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AppleIrisPrimary.copy(alpha = 0.28f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Liquid Glass Emblem Tile
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(28.dp),
                                spotColor = AppleIrisPrimary.copy(alpha = 0.45f),
                                ambientColor = Color(0x18000000)
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        AppleIrisPrimary,
                                        Color(0xFF3730A3)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.40f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        HomeaseLogoMark(
                            size = 84.dp,
                            primaryColor = Color.White,
                            accentColor = AppleWarmChampagne,
                            doorColor = AppleIrisPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Animated "HomEase" Typography
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .alpha(titleAlpha)
                        .offset { IntOffset(x = 0, y = titleOffsetY.roundToInt()) }
                ) {
                    Text(
                        text = "Hom",
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                        color = AppleIrisPrimary,
                        letterSpacing = (-0.8).sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "Ease",
                        fontWeight = FontWeight.Black,
                        fontSize = 42.sp,
                        color = AppleWarmChampagne,
                        letterSpacing = (-0.8).sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Animated Tagline in a Frosted Glass Pill
                Box(
                    modifier = Modifier
                        .alpha(taglineAlpha)
                        .offset { IntOffset(x = 0, y = taglineOffsetY.roundToInt()) }
                        .liquidGlassCard(
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = Color.White.copy(alpha = 0.70f),
                            borderBrush = AppleGlassSpecularBorder
                        )
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = Strings.get("tagline", language),
                        color = AppleLabelSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    )
                }
            }

            // Subtle Apple-style bottom status indicator
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
                    .alpha(taglineAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .scale(haloPulse)
                        .clip(CircleShape)
                        .background(AppleIrisPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.URDU) "پاکستان کا قابلِ اعتماد پلیٹ فارم" else "Verified Home Services in Pakistan",
                    fontSize = 11.5.sp,
                    color = AppleLabelSecondary.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}
