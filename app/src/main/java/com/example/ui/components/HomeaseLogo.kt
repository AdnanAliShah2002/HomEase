package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted

@Composable
fun HomeaseLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    primaryColor: Color = DeepIndigo,
    accentColor: Color = SoftOrange,
    doorColor: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // House roofline + location pin peak
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.18f)
            lineTo(w * 0.88f, h * 0.44f)
            lineTo(w * 0.80f, h * 0.50f)
            lineTo(w * 0.76f, h * 0.48f)
            lineTo(w * 0.76f, h * 0.88f)
            lineTo(w * 0.24f, h * 0.88f)
            lineTo(w * 0.24f, h * 0.48f)
            lineTo(w * 0.20f, h * 0.50f)
            lineTo(w * 0.12f, h * 0.44f)
            close()
        }
        drawPath(path = path, color = primaryColor, style = Fill)

        // Door cutout inside house
        val doorWidth = w * 0.20f
        val doorHeight = h * 0.28f
        val doorLeft = (w - doorWidth) / 2f
        val doorTop = h * 0.88f - doorHeight
        drawRoundRect(
            color = doorColor,
            topLeft = Offset(doorLeft, doorTop),
            size = Size(doorWidth, doorHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f)
        )

        // Location pin orange dot accent at the roof peak tip
        val pinDotRadius = w * 0.09f
        val pinDotCenter = Offset(w * 0.5f, h * 0.16f)
        drawCircle(
            color = accentColor,
            radius = pinDotRadius,
            center = pinDotCenter
        )
        // Inner highlight dot
        drawCircle(
            color = Color.White,
            radius = pinDotRadius * 0.42f,
            center = pinDotCenter
        )
    }
}

@Composable
fun HomeaseHeaderLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = 36.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Apple-style squircle logo badge with specular border
        Box(
            modifier = Modifier
                .size(markSize)
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(10.dp), spotColor = Color(0x1A007AFF))
                .clip(RoundedCornerShape(10.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color(0xFF007AFF), Color(0xFF0056B3))
                    )
                )
                .border(
                    0.5.dp,
                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            HomeaseLogoMark(
                size = markSize * 0.72f,
                primaryColor = Color.White,
                accentColor = Color(0xFFFF9500),
                doorColor = Color(0xFF0056B3)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Hom",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1C1C1E),
                    letterSpacing = (-0.5).sp,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Ease",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF007AFF),
                    letterSpacing = (-0.5).sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
            Text(
                text = "HOME SERVICES",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E8E93),
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
fun HomeaseSplashBranding(
    modifier: Modifier = Modifier,
    tagline: String = "Home help, on demand."
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(26.dp), spotColor = DeepIndigo)
                .clip(RoundedCornerShape(26.dp))
                .background(DeepIndigo),
            contentAlignment = Alignment.Center
        ) {
            HomeaseLogoMark(
                size = 78.dp,
                primaryColor = Color.White,
                accentColor = SoftOrange,
                doorColor = DeepIndigo
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Hom",
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                color = DeepIndigo,
                letterSpacing = (-0.5).sp,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "Ease",
                fontWeight = FontWeight.Black,
                fontSize = 38.sp,
                color = SoftOrange,
                letterSpacing = (-0.5).sp,
                fontFamily = FontFamily.SansSerif
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = tagline,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSlateMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
