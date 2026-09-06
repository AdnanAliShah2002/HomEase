package com.example.ui.components

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.localization.AppLanguage
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.DeepIndigoContainer
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SoftOrangeContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusYellowContainer
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted
import com.example.util.LocationFetchState
import com.example.util.LocationHelper
import com.example.util.UserLocationResult
import kotlinx.coroutines.launch

@Composable
fun AutoLocationFetcher(
    modifier: Modifier = Modifier,
    autoFetch: Boolean = true,
    language: AppLanguage = AppLanguage.ENGLISH,
    onLocationDetected: (UserLocationResult) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<LocationFetchState>(LocationFetchState.Idle) }
    var hasRequestedPermission by remember { mutableStateOf(false) }

    fun doFetch() {
        if (!LocationHelper.hasLocationPermission(context)) {
            state = LocationFetchState.PermissionRequired
            return
        }
        if (!LocationHelper.isLocationEnabled(context)) {
            state = LocationFetchState.LocationDisabled
            return
        }

        state = LocationFetchState.Fetching
        scope.launch {
            try {
                val loc = LocationHelper.getCurrentLocation(context)
                if (loc != null) {
                    state = LocationFetchState.Success(loc)
                    onLocationDetected(loc)
                } else {
                    state = LocationFetchState.Error(if (language == AppLanguage.URDU) "جی پی ایس کوآرڈینیٹس حاصل نہیں ہو سکے۔" else "Could not retrieve GPS coordinates.")
                }
            } catch (e: Exception) {
                state = LocationFetchState.Error(e.message ?: (if (language == AppLanguage.URDU) "مقام معلوم کرنے میں ناکامی" else "Failed to get location"))
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            doFetch()
        } else {
            state = LocationFetchState.PermissionRequired
        }
    }

    // Auto-fetch when location is ON and permission is granted, or auto-request permission if autoFetch is true
    LaunchedEffect(Unit) {
        if (autoFetch) {
            if (LocationHelper.hasLocationPermission(context)) {
                if (LocationHelper.isLocationEnabled(context)) {
                    doFetch()
                } else {
                    state = LocationFetchState.LocationDisabled
                }
            } else if (!hasRequestedPermission) {
                hasRequestedPermission = true
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        when (val s = state) {
            LocationFetchState.Checking,
            is LocationFetchState.Fetching -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DeepIndigoContainer)
                        .border(1.dp, DeepIndigo.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = DeepIndigo
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (language == AppLanguage.URDU) "جی پی ایس سے مقام حاصل کیا جا رہا ہے..." else "Auto-fetching your current location via GPS...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DeepIndigo
                    )
                }
            }

            is LocationFetchState.Success -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StatusGreenContainer)
                        .border(1.dp, StatusGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${if (language == AppLanguage.URDU) "جی پی ایس مقام:" else "Auto-detected GPS Location:"} ${s.result.cityArea}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = s.result.fullAddress,
                                fontSize = 11.sp,
                                color = Color(0xFF047857),
                                maxLines = 1
                            )
                        }
                    }
                    IconButton(
                        onClick = { doFetch() },
                        modifier = Modifier.size(24.dp).testTag("refresh_location_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Location",
                            tint = Color(0xFF065F46),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            is LocationFetchState.LocationDisabled -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StatusYellowContainer)
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "جی پی ایس بند ہے۔ مقام کے لیے آن کریں۔" else "GPS is turned OFF. Turn on location to auto-detect.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF92400E)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFB45309))
                            .clickable { LocationHelper.openLocationSettings(context) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("enable_gps_settings_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == AppLanguage.URDU) "جی پی ایس آن کریں" else "Turn On GPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            is LocationFetchState.PermissionRequired -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DeepIndigoContainer)
                        .border(1.dp, BorderStroke, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = DeepIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "مقام معلوم کرنے کی اجازت دیں۔" else "Allow location to auto-fetch your address.",
                            fontSize = 12.sp,
                            color = DeepIndigo,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DeepIndigo)
                            .clickable {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("grant_location_permission_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == AppLanguage.URDU) "اجازت دیں" else "Allow GPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            is LocationFetchState.Error -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SoftOrangeContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GPS: ${s.message}",
                        fontSize = 12.sp,
                        color = SoftOrange,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (language == AppLanguage.URDU) "دوبارہ کوشش" else "Retry",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftOrange,
                        modifier = Modifier.clickable { doFetch() }.padding(4.dp)
                    )
                }
            }

            LocationFetchState.Idle -> {
                // Quick trigger pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeepIndigoContainer)
                        .clickable { doFetch() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = DeepIndigo,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (language == AppLanguage.URDU) "موجودہ مقام حاصل کریں" else "Auto-Detect Current Location",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepIndigo
                    )
                }
            }
        }
    }
}
