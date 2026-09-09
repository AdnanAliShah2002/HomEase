package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ServiceRequestEntity
import com.example.data.localization.AppLanguage
import com.example.data.model.MobileAppTheme
import com.example.data.model.ProviderLocation
import kotlinx.coroutines.launch
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted
import com.example.ui.viewmodel.HomeaseViewModel
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox

/**
 * InDrive-style real-time live location tracking screen for customer.
 * - Continuously listens to Supabase Realtime (or local fallback) for the active job.
 * - Animates the provider vehicle marker smoothly over 1.5 - 2.0 seconds with no snapping.
 * - Displays live distance calculation ("~X km away") and estimated arrival time.
 * - Shows clear trip progression: On the way -> Arrived -> In Progress -> Completed.
 */
@Composable
fun LiveTrackingMapScreen(
    job: ServiceRequestEntity,
    viewModel: HomeaseViewModel,
    theme: MobileAppTheme,
    language: AppLanguage,
    onBack: () -> Unit,
    onOpenChat: () -> Unit = {},
    onStartCall: () -> Unit = {}
) {
    val context = LocalContext.current
    val primaryColor = Color(theme.primaryColorInt)
    val accentColor = Color(theme.accentColorInt)

    // Collect latest live location from Supabase Realtime channel
    val latestLocation by viewModel.getLiveTrackingLocationFlow(job.id)
        .collectAsState(
            initial = ProviderLocation(
                jobId = job.id.toString(),
                providerId = job.selectedProviderPhone ?: "",
                lat = 31.5120,
                lng = 74.3450,
                heading = 45.0,
                updatedAt = ""
            )
        )

    // Customer Destination coordinates (Lahore Gulberg center)
    val customerLat = 31.5204
    val customerLng = 74.3587

    // Smooth continuous interpolation of provider coordinates
    val animatedLat = remember { Animatable(31.5120f) }
    val animatedLng = remember { Animatable(74.3450f) }
    val animatedHeading = remember { Animatable(45f) }

    LaunchedEffect(latestLocation) {
        val targetLat = latestLocation.lat.toFloat()
        val targetLng = latestLocation.lng.toFloat()
        val targetHeading = (latestLocation.heading ?: 45.0).toFloat()

        // Interpolate smoothly over 1800ms (so each 5-8s update glides continuously)
        launch {
            animatedLat.animateTo(
                targetValue = targetLat,
                animationSpec = tween(durationMillis = 1800, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            animatedLng.animateTo(
                targetValue = targetLng,
                animationSpec = tween(durationMillis = 1800, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            animatedHeading.animateTo(
                targetValue = targetHeading,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
            )
        }
    }

    // Live distance calculation in km
    val currentDistanceKm = remember(animatedLat.value, animatedLng.value) {
        calculateHaversineDistanceKm(
            lat1 = animatedLat.value.toDouble(),
            lon1 = animatedLng.value.toDouble(),
            lat2 = customerLat,
            lon2 = customerLng
        )
    }

    // Pulsing animation for active live indicator and customer pin
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_radar")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 38f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .testTag("live_tracking_screen")
    ) {
        // ====================================================================
        // 1. Live Animated Map Surface (Vector Roads, Destination Pin, Moving Vehicle)
        // ====================================================================
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("live_tracking_map_canvas")
        ) {
            val width = size.width
            val height = size.height

            // Clean modern map background
            drawRect(color = Color(0xFFF8FAFC))

            // Road grid network
            val roadColor = Color(0xFFE2E8F0)
            val majorRoadColor = Color(0xFFCBD5E1)

            // Horizontal roads
            val hLines = 8
            for (i in 1..hLines) {
                val y = height * (i.toFloat() / (hLines + 1))
                val isMajor = i % 3 == 0
                drawLine(
                    color = if (isMajor) majorRoadColor else roadColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = if (isMajor) 18f else 10f
                )
            }

            // Vertical roads
            val vLines = 6
            for (i in 1..vLines) {
                val x = width * (i.toFloat() / (vLines + 1))
                val isMajor = i % 2 == 0
                drawLine(
                    color = if (isMajor) majorRoadColor else roadColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = if (isMajor) 18f else 10f
                )
            }

            // Diagonal boulevard (Main Boulevard Gulberg)
            val boulevardPath = Path().apply {
                moveTo(width * 0.1f, height * 0.85f)
                cubicTo(
                    width * 0.35f, height * 0.65f,
                    width * 0.65f, height * 0.45f,
                    width * 0.9f, height * 0.2f
                )
            }
            drawPath(
                path = boulevardPath,
                color = Color(0xFFE2E8F0),
                style = Stroke(width = 24f)
            )

            // Map projection mapping:
            // Normalize relative to bounds around Lahore Gulberg
            val minLat = 31.5050
            val maxLat = 31.5280
            val minLng = 74.3350
            val maxLng = 74.3680

            fun projectToScreen(lat: Double, lng: Double): Offset {
                val nx = ((lng - minLng) / (maxLng - minLng)).coerceIn(0.12, 0.88)
                val ny = (1.0 - ((lat - minLat) / (maxLat - minLat))).coerceIn(0.22, 0.78)
                return Offset((nx * width).toFloat(), (ny * height).toFloat())
            }

            val customerPos = projectToScreen(customerLat, customerLng)
            val providerPos = projectToScreen(animatedLat.value.toDouble(), animatedLng.value.toDouble())

            // Dotted route line from provider to customer
            val routePath = Path().apply {
                moveTo(providerPos.x, providerPos.y)
                // Gentle curve simulating city turns
                val midX = (providerPos.x + customerPos.x) / 2f + 35f
                val midY = (providerPos.y + customerPos.y) / 2f - 25f
                quadraticTo(midX, midY, customerPos.x, customerPos.y)
            }

            drawPath(
                path = routePath,
                color = accentColor.copy(alpha = 0.85f),
                style = Stroke(
                    width = 7f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                )
            )

            // Customer Destination Pin (Glowing radar pulse + House pin)
            drawCircle(
                color = primaryColor.copy(alpha = pulseAlpha),
                radius = pulseRadius * 2f,
                center = customerPos
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.2f),
                radius = 28f,
                center = customerPos
            )
            drawCircle(
                color = primaryColor,
                radius = 16f,
                center = customerPos
            )
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = customerPos
            )

            // Moving Provider Vehicle Marker (Rotates smoothly to heading)
            // Outer shadow / glow
            drawCircle(
                color = Color(0x33000000),
                radius = 26f,
                center = Offset(providerPos.x, providerPos.y + 4f)
            )
            drawCircle(
                color = Color.White,
                radius = 22f,
                center = providerPos
            )
            drawCircle(
                color = accentColor,
                radius = 18f,
                center = providerPos
            )

            // Directional pointer arrowhead
            rotate(degrees = animatedHeading.value, pivot = providerPos) {
                val arrowPath = Path().apply {
                    moveTo(providerPos.x, providerPos.y - 12f)
                    lineTo(providerPos.x + 8f, providerPos.y + 8f)
                    lineTo(providerPos.x, providerPos.y + 4f)
                    lineTo(providerPos.x - 8f, providerPos.y + 8f)
                    close()
                }
                drawPath(path = arrowPath, color = Color.White)
            }
        }

        // ====================================================================
        // 2. Top Navigation & Status Bar Overlay
        // ====================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(4.dp, CircleShape)
                        .testTag("tracking_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextSlate
                    )
                }

                // Live Tracking Status Pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(StatusGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "لائیو ٹریکنگ" else "LIVE TRACKING",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlate,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Call Emergency/Support Shortcut
                IconButton(
                    onClick = {
                        val phoneIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:042111466327"))
                        context.startActivity(phoneIntent)
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(4.dp, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Support",
                        tint = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // InDrive-style Distance & ETA Floating Badge
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("eta_status_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val distanceText = if (currentDistanceKm < 0.25) {
                                if (language == AppLanguage.URDU) "پہنچ گیا ہے" else "Arriving now"
                            } else {
                                String.format(Locale.US, "~%.1f km %s", currentDistanceKm, if (language == AppLanguage.URDU) "دور" else "away")
                            }
                            Text(
                                text = distanceText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSlate
                            )
                            val statusLabel = when (job.status.uppercase()) {
                                "ON_THE_WAY" -> if (language == AppLanguage.URDU) "راستے میں ہے" else "On the way to your home"
                                "ARRIVED" -> if (language == AppLanguage.URDU) "پہنچ گیا ہے" else "Arrived outside"
                                "IN_PROGRESS" -> if (language == AppLanguage.URDU) "کام جاری ہے" else "Work in progress"
                                else -> if (language == AppLanguage.URDU) "بکنگ تصدیق شدہ" else "Booking confirmed"
                            }
                            Text(
                                text = statusLabel,
                                fontSize = 12.sp,
                                color = TextSlateMuted
                            )
                        }
                    }

                    // Estimated Minutes
                    val estMins = (currentDistanceKm * 3.5).toInt().coerceAtLeast(1)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = if (currentDistanceKm < 0.25) "NOW" else "~$estMins MIN",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // ====================================================================
        // 3. Bottom Sheet Card (Provider Profile, Address & Direct Contact)
        // ====================================================================
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .testTag("provider_details_bottom_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Provider Avatar, Name, Rating & Service Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (job.selectedProviderName?.firstOrNull() ?: 'U').toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = job.selectedProviderName ?: (if (language == AppLanguage.URDU) "کاریگر" else "Service Provider"),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSlate
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "4.9 • ${job.serviceTitle}",
                                    fontSize = 13.sp,
                                    color = TextSlateMuted,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Agreed Price
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (language == AppLanguage.URDU) "طے شدہ رقم" else "AGREED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlateMuted
                        )
                        val price = if (job.agreedPriceRs > 0) job.agreedPriceRs else job.budgetRs
                        Text(
                            text = "Rs $price",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Customer Destination Address
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = job.fullAddress,
                        fontSize = 13.sp,
                        color = TextSlate,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val unreadCount by viewModel.getUnreadCountFlow(job.id.toString()).collectAsState(initial = 0)

                // Primary Action Buttons: In-App Voice Call (Agora HD) & In-App Chat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. In-App Agora HD Voice Call Button
                    Button(
                        onClick = onStartCall,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("in_app_call_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = if (language == AppLanguage.URDU) "صوتی کال" else "VOICE CALL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Free HD",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    // 2. In-App Chat Button with Unread Badge
                    Button(
                        onClick = onOpenChat,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("in_app_chat_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFDC5F45),
                                        contentColor = Color.White
                                    ) {
                                        Text(text = "$unreadCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = if (language == AppLanguage.URDU) "چیٹ کریں" else "JOB CHAT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (unreadCount > 0) "$unreadCount unread" else "Instant",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action Buttons: WhatsApp & Phone Dialer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val rawPhone = job.selectedProviderPhone.orEmpty()
                            if (rawPhone.isNotBlank()) {
                                val phone = rawPhone.replace("+", "")
                                val url = "https://api.whatsapp.com/send?phone=$phone"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("whatsapp_provider_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF1E7E34)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF25D366)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val phone = job.selectedProviderPhone.orEmpty()
                            if (phone.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("call_provider_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSlate
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TextSlate
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "سیلولر ڈائلر" else "Cellular Dial",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calculates straight line distance in km between two lat/lng coordinates.
 */
private fun calculateHaversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Radius of earth in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
