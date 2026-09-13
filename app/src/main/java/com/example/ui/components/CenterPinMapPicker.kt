package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted
import com.example.data.localization.AppLanguage
import com.example.util.GeoapifyService
import com.example.util.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Full-screen InDrive/Uber style Map Pin Picker powered by Geoapify API.
 * Features a fixed pin at the center of the screen where the Geoapify map pans underneath.
 * Dynamically reverse-geocodes the location under the pin via Geoapify API and captures both
 * address string and exact lat/lng coordinates upon confirmation.
 */
@Composable
fun CenterPinMapPickerDialog(
    initialLat: Double = 33.6844, // Islamabad default
    initialLng: Double = 73.0479,
    language: AppLanguage = AppLanguage.ENGLISH,
    onDismiss: () -> Unit,
    onConfirmLocation: (fullAddress: String, cityArea: String, lat: Double, lng: Double) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var centerLat by remember { mutableDoubleStateOf(initialLat) }
    var centerLng by remember { mutableDoubleStateOf(initialLng) }
    var zoomLevel by remember { mutableIntStateOf(16) }
    var isDragging by remember { mutableStateOf(false) }

    var resolvedAddress by remember { mutableStateOf("") }
    var resolvedCityArea by remember { mutableStateOf("") }
    var isGeocoding by remember { mutableStateOf(false) }
    var geocodeJob by remember { mutableStateOf<Job?>(null) }

    fun triggerReverseGeocode(lat: Double, lng: Double) {
        centerLat = lat
        centerLng = lng
        geocodeJob?.cancel()
        geocodeJob = coroutineScope.launch {
            isGeocoding = true
            delay(350) // Debounce while user is actively dragging
            val result = LocationHelper.reverseGeocode(context, lat, lng)
            resolvedAddress = result.fullAddress
            resolvedCityArea = result.cityArea
            isGeocoding = false
        }
    }

    LaunchedEffect(Unit) {
        triggerReverseGeocode(initialLat, initialLng)
    }

    // Pin elevation bounce animation when map is actively dragging
    val pinElevation by animateDpAsState(
        targetValue = if (isDragging) (-16).dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "pinBounce"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("center_pin_map_picker_dialog"),
            color = Color(0xFFF1F5F9)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // 1. Continuous Slippy Tile Map View underneath the center pin
                InteractiveTileMap(
                    centerLat = centerLat,
                    centerLng = centerLng,
                    zoomLevel = zoomLevel,
                    isDragging = isDragging,
                    onDragStateChanged = { dragging ->
                        isDragging = dragging
                        if (!dragging) {
                            triggerReverseGeocode(centerLat, centerLng)
                        }
                    },
                    onLocationChanged = { lat, lng ->
                        centerLat = lat
                        centerLng = lng
                        if (!isDragging) {
                            triggerReverseGeocode(lat, lng)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("geoapify_map_canvas")
                )

                // 2. Fixed Center Pin (stays centered on screen while map pans underneath)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = pinElevation),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Location pin icon (InDrive Crimson / Deep Indigo styling)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(8.dp, CircleShape)
                                .background(Color(0xFFE11D48), CircleShape), // Vibrant Crimson
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Center Map Pin",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Needle tip & target dot on the ground
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(12.dp)
                                .background(Color(0xFFBE123C))
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .shadow(4.dp, CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        )
                    }
                }

                // Instruction pill above the pin
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-58).dp + pinElevation),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.82f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.URDU) "نقشہ گھسیٹ کر پن سیٹ کریں" else "Drag map to position pin",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // 3. Top Floating Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(6.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .testTag("map_picker_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSlate
                        )
                    }

                    // Geoapify Branding Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Geoapify Maps",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepIndigo
                            )
                        }
                    }

                    // Re-center on current GPS location
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                val currentLoc = LocationHelper.getCurrentLocation(context)
                                if (currentLoc != null) {
                                    triggerReverseGeocode(currentLoc.latitude, currentLoc.longitude)
                                }
                            }
                        },
                        containerColor = Color.White,
                        contentColor = DeepIndigo,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(6.dp, CircleShape)
                            .testTag("map_picker_gps_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "My GPS Location",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Zoom controls on right side
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (zoomLevel < 19) zoomLevel += 1
                        },
                        containerColor = Color.White,
                        contentColor = TextSlate,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                    }

                    FloatingActionButton(
                        onClick = {
                            if (zoomLevel > 11) zoomLevel -= 1
                        },
                        containerColor = Color.White,
                        contentColor = TextSlate,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                    }
                }

                // 4. Bottom Location Confirmation Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .testTag("map_picker_bottom_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(DeepIndigo.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = DeepIndigo,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.URDU) "منتخب کردہ مقام" else "Selected Location",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSlateMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (resolvedAddress.isNotBlank()) resolvedAddress else "${String.format(Locale.US, "%.5f", centerLat)}, ${String.format(Locale.US, "%.5f", centerLng)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSlate,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isGeocoding) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = DeepIndigo
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Coordinates
                        Text(
                            text = "📍 Lat: ${String.format(Locale.US, "%.5f", centerLat)} • Lng: ${String.format(Locale.US, "%.5f", centerLng)}",
                            fontSize = 11.sp,
                            color = TextSlateMuted,
                            modifier = Modifier.padding(start = 50.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Confirm Location button
                        Button(
                            onClick = {
                                val finalAddress = if (resolvedAddress.isNotBlank()) resolvedAddress else LocationHelper.estimatePakistanCity(centerLat, centerLng)
                                val finalCity = if (resolvedCityArea.isNotBlank()) resolvedCityArea else LocationHelper.estimatePakistanCity(centerLat, centerLng)
                                onConfirmLocation(finalAddress, finalCity, centerLat, centerLng)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("confirm_map_location_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepIndigo,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.URDU) "اس مقام کی تصدیق کریں" else "Confirm Location",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
