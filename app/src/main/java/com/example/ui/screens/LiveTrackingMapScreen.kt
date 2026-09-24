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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.ServiceRequestEntity
import com.example.data.localization.AppLanguage
import com.example.data.model.MobileAppTheme
import com.example.data.model.ProviderLocation
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted
import com.example.ui.viewmodel.HomeaseViewModel
import com.example.util.GeoapifyRoute
import com.example.util.GeoapifyService
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * InDrive-style real-time live location tracking screen powered by Geoapify Maps & Routing API.
 * - Displays Geoapify OpenStreetMap Bright street map tiles framed around both provider & customer.
 * - Animates the provider vehicle marker smoothly with continuous coordinate glide and heading angle rotation.
 * - Real-time Driving Route, Distance, and ETA calculations powered directly by Geoapify Routing API.
 * - Displays next turn-by-turn instruction and trip status (On the way -> Arrived -> In Progress).
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

    val canonicalJobId = job.remoteId ?: job.id.toString()

    // Collect latest live location from Supabase Realtime channel or local simulation
    val latestLocation by viewModel.getLiveTrackingLocationFlow(canonicalJobId)
        .collectAsState(
            initial = ProviderLocation(
                jobId = canonicalJobId,
                providerId = job.selectedProviderPhone ?: "",
                lat = 33.6912,
                lng = 73.0315,
                heading = 45.0,
                updatedAt = ""
            )
        )

    // Customer Destination coordinates
    val customerLat = job.lat ?: 33.6844
    val customerLng = job.lng ?: 73.0479

    // Smooth continuous interpolation of provider coordinates
    val animatedLat = remember { Animatable(33.6912f) }
    val animatedLng = remember { Animatable(73.0315f) }
    val animatedHeading = remember { Animatable(45f) }

    LaunchedEffect(latestLocation) {
        val targetLat = latestLocation.lat.toFloat()
        val targetLng = latestLocation.lng.toFloat()
        val targetHeading = (latestLocation.heading ?: 45.0).toFloat()

        // Interpolate smoothly over 1800ms
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

    // Straight-line fallback distance
    val haversineDistanceKm = remember(animatedLat.value, animatedLng.value) {
        calculateHaversineDistanceKm(
            lat1 = animatedLat.value.toDouble(),
            lon1 = animatedLng.value.toDouble(),
            lat2 = customerLat,
            lon2 = customerLng
        )
    }

    // Real Driving Route & ETA fetched from Geoapify Routing API
    var geoapifyRoute by remember { mutableStateOf<GeoapifyRoute?>(null) }

    LaunchedEffect(latestLocation.lat, latestLocation.lng) {
        val route = GeoapifyService.getDrivingRoute(
            startLat = latestLocation.lat,
            startLng = latestLocation.lng,
            destLat = customerLat,
            destLng = customerLng
        )
        if (route != null) {
            geoapifyRoute = route
        }
    }

    val displayDistanceKm = geoapifyRoute?.distanceKm ?: haversineDistanceKm
    val displayEtaMins = geoapifyRoute?.etaMinutes ?: (haversineDistanceKm * 3.5).toInt().coerceAtLeast(1)
    val nextInstruction = geoapifyRoute?.instructions?.firstOrNull()

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

    // Build Geoapify Live Tracking Map URL framing both points
    val geoapifyMapUrl = remember(
        latestLocation.lat,
        latestLocation.lng,
        customerLat,
        customerLng
    ) {
        GeoapifyService.buildLiveTrackingMapUrl(
            providerLat = latestLocation.lat,
            providerLng = latestLocation.lng,
            customerLat = customerLat,
            customerLng = customerLng,
            width = 800,
            height = 1000
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .testTag("live_tracking_screen")
    ) {
        // ====================================================================
        // 1. Geoapify Real Map Layer (Crisp OpenStreetMap Bright Rendering)
        // ====================================================================
        AsyncImage(
            model = geoapifyMapUrl,
            contentDescription = "Geoapify Live Tracking Map",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .testTag("live_tracking_geoapify_map")
        )

        // ====================================================================
        // 2. Animated Overlay: Moving Provider Vehicle, Radar Rings & Route
        // ====================================================================
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("live_tracking_map_canvas")
        ) {
            val width = size.width
            val height = size.height

            // Coordinate mapping relative to bounding box
            val pLat = animatedLat.value.toDouble()
            val pLng = animatedLng.value.toDouble()

            val minLat = Math.min(pLat, customerLat) - 0.008
            val maxLat = Math.max(pLat, customerLat) + 0.008
            val minLng = Math.min(pLng, customerLng) - 0.008
            val maxLng = Math.max(pLng, customerLng) + 0.008

            fun projectToScreen(lat: Double, lng: Double): Offset {
                val spanLng = Math.max(0.002, maxLng - minLng)
                val spanLat = Math.max(0.002, maxLat - minLat)
                val nx = ((lng - minLng) / spanLng).coerceIn(0.12, 0.88)
                val ny = (1.0 - ((lat - minLat) / spanLat)).coerceIn(0.22, 0.76)
                return Offset((nx * width).toFloat(), (ny * height).toFloat())
            }

            val customerPos = projectToScreen(customerLat, customerLng)
            val providerPos = projectToScreen(pLat, pLng)

            // Dotted route line from provider to customer
            val routePath = Path().apply {
                moveTo(providerPos.x, providerPos.y)
                val midX = (providerPos.x + customerPos.x) / 2f + 25f
                val midY = (providerPos.y + customerPos.y) / 2f - 20f
                quadraticTo(midX, midY, customerPos.x, customerPos.y)
            }

            // Route glow
            drawPath(
                path = routePath,
                color = DeepIndigo.copy(alpha = 0.25f),
                style = Stroke(width = 12f)
            )

            // Route line with dash effect
            drawPath(
                path = routePath,
                color = DeepIndigo.copy(alpha = 0.90f),
                style = Stroke(
                    width = 6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                )
            )

            // Customer Destination Pin (Glowing radar pulse + House pin)
            drawCircle(
                color = DeepIndigo.copy(alpha = pulseAlpha),
                radius = pulseRadius * 2.2f,
                center = customerPos
            )
            drawCircle(
                color = DeepIndigo.copy(alpha = 0.25f),
                radius = 28f,
                center = customerPos
            )
            drawCircle(
                color = DeepIndigo,
                radius = 16f,
                center = customerPos
            )
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = customerPos
            )

            // Moving Provider Vehicle Marker (Rotates smoothly to heading)
            // Drop shadow
            drawCircle(
                color = Color(0x44000000),
                radius = 28f,
                center = Offset(providerPos.x, providerPos.y + 4f)
            )
            // Outer white ring
            drawCircle(
                color = Color.White,
                radius = 24f,
                center = providerPos
            )
            // Vibrant Emerald green body
            drawCircle(
                color = EmeraldGreen,
                radius = 19f,
                center = providerPos
            )

            // Directional pointer arrowhead rotated by animated heading
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
        // 3. Top Navigation & Status Bar Overlay
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
                        .shadow(6.dp, CircleShape)
                        .testTag("tracking_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextSlate
                    )
                }

                // Live Tracking Status Pill with Geoapify indication
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
                            text = if (language == AppLanguage.URDU) "لائیو ٹریکنگ • جیو ایپفائی" else "LIVE TRACKING • GEOAPIFY",
                            fontSize = 11.5.sp,
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
                        .shadow(6.dp, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Support",
                        tint = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // InDrive-style Distance & ETA Floating Badge (Geoapify Routing API)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("eta_status_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val distanceText = if (displayDistanceKm < 0.25) {
                                    if (language == AppLanguage.URDU) "پہنچ گیا ہے" else "Arriving now"
                                } else {
                                    String.format(Locale.US, "~%.1f km %s", displayDistanceKm, if (language == AppLanguage.URDU) "دور" else "away")
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

                        // Estimated Minutes Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DeepIndigo.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = if (displayDistanceKm < 0.25) "NOW" else "~$displayEtaMins MIN",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = DeepIndigo,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Turn-by-turn driving step instruction from Geoapify if available
                    if (!nextInstruction.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = nextInstruction,
                                fontSize = 11.5.sp,
                                color = TextSlate,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // ====================================================================
        // 4. Bottom Sheet Card (Provider Profile, Address & Direct Contact)
        // ====================================================================
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = Color(0x1A000000),
                    ambientColor = Color(0x0F000000)
                )
                .testTag("provider_details_bottom_card"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x14000000))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Apple Sheet Capsule Drag Handle
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1D1D6))
                )

                Spacer(modifier = Modifier.height(14.dp))

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
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEBF5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (job.selectedProviderName?.firstOrNull() ?: 'U').toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007AFF)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = job.selectedProviderName ?: (if (language == AppLanguage.URDU) "کاریگر" else "Service Provider"),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C1E),
                                letterSpacing = (-0.2).sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFCC00),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "4.9 • ${job.serviceTitle}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93),
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
                            color = Color(0xFF8E8E93),
                            letterSpacing = 0.5.sp
                        )
                        val price = if (job.agreedPriceRs > 0) job.agreedPriceRs else job.budgetRs
                        Text(
                            text = "Rs $price",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF),
                            letterSpacing = (-0.3).sp
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
                        tint = DeepIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = job.fullAddress,
                        fontSize = 13.sp,
                        color = TextSlate,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val unreadCount by viewModel.getUnreadCountFlow(canonicalJobId).collectAsState(initial = 0)

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
