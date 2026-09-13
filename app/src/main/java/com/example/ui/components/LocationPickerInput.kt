package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted
import com.example.data.localization.AppLanguage
import com.example.util.GeoapifyPlace
import com.example.util.GeoapifyService
import com.example.util.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Production-ready InDrive / Uber style Location Input Component powered by Geoapify API.
 *
 * Provides:
 * 1. Autocomplete Search Dialog powered directly by Geoapify API (Key: 6aeb38ece1144d409b4e3a84261139a8).
 * 2. Dedicated Map Pin button opening the full-screen Center-Pin Map Picker.
 * 3. One-tap "Use Current Location" GPS shortcut.
 * 4. Captures BOTH human-readable formatted address string AND precise lat/lng coordinates.
 */
@Composable
fun LocationPickerInput(
    modifier: Modifier = Modifier,
    label: String = "Location",
    hint: String? = null,
    addressValue: String,
    cityAreaValue: String = "",
    latitude: Double? = null,
    longitude: Double? = null,
    language: AppLanguage = AppLanguage.ENGLISH,
    testTagPrefix: String = "location_picker",
    isRequired: Boolean = true,
    showGpsShortcut: Boolean = true,
    onLocationSelected: (address: String, cityArea: String, lat: Double, lng: Double) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showSearchDialog by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var isFetchingGps by remember { mutableStateOf(false) }

    // Coordinates fallback (Islamabad center)
    val currentLat = latitude ?: 33.6844
    val currentLng = longitude ?: 73.0479

    // Permission launcher for GPS shortcut
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            isFetchingGps = true
            coroutineScope.launch {
                val loc = LocationHelper.getCurrentLocation(context)
                isFetchingGps = false
                if (loc != null) {
                    onLocationSelected(loc.fullAddress, loc.cityArea, loc.latitude, loc.longitude)
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Label Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRequired) "$label *" else label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSlate
            )

            if (latitude != null && longitude != null) {
                Text(
                    text = "📍 ${String.format(Locale.US, "%.4f, %.4f", latitude, longitude)}",
                    fontSize = 11.sp,
                    color = EmeraldGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Input Box (InDrive / Uber search field styling)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderStroke, RoundedCornerShape(12.dp))
                .clickable { showSearchDialog = true }
                .testTag("${testTagPrefix}_input_box"),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Location Pin
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(DeepIndigo.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location Pin",
                        tint = DeepIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Address & City Area Display / Hint
                Column(modifier = Modifier.weight(1f)) {
                    if (addressValue.isNotBlank()) {
                        Text(
                            text = addressValue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (cityAreaValue.isNotBlank() && cityAreaValue != addressValue) {
                            Text(
                                text = cityAreaValue,
                                fontSize = 12.sp,
                                color = TextSlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = hint ?: (if (language == AppLanguage.URDU) "پتہ تلاش کریں یا نقشہ کھولیں..." else "Search address or tap map to locate..."),
                            fontSize = 13.5.sp,
                            color = TextSlateMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Clear button (if text present)
                if (addressValue.isNotBlank()) {
                    IconButton(
                        onClick = { onLocationSelected("", "", currentLat, currentLng) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextSlateMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Dedicated Map Pin Picker Icon Button (opens full-screen map with fixed pin)
                Surface(
                    onClick = { showMapPicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.testTag("${testTagPrefix}_map_pin_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Open Map Pin Picker",
                            tint = DeepIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "نقشہ" else "Map",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepIndigo
                        )
                    }
                }
            }
        }

        // Quick "Use Current Location" (GPS) chip
        if (showGpsShortcut) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (LocationHelper.hasLocationPermission(context)) {
                            isFetchingGps = true
                            coroutineScope.launch {
                                val loc = LocationHelper.getCurrentLocation(context)
                                isFetchingGps = false
                                if (loc != null) {
                                    onLocationSelected(loc.fullAddress, loc.cityArea, loc.latitude, loc.longitude)
                                }
                            }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .testTag("${testTagPrefix}_use_current_location_btn"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFetchingGps) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = DeepIndigo
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = DeepIndigo,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (language == AppLanguage.URDU) "موجودہ مقام استعمال کریں (GPS)" else "Use current location (GPS)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DeepIndigo
                )
            }
        }
    }

    // Geoapify Autocomplete Search Dialog
    if (showSearchDialog) {
        GeoapifySearchDialog(
            initialQuery = addressValue,
            proximityLat = currentLat,
            proximityLng = currentLng,
            language = language,
            onDismiss = { showSearchDialog = false },
            onOpenMapPicker = {
                showSearchDialog = false
                showMapPicker = true
            },
            onPlaceSelected = { place ->
                showSearchDialog = false
                val cityArea = if (place.suburb.isNotBlank() && place.city.isNotBlank()) {
                    "${place.suburb}, ${place.city}"
                } else if (place.city.isNotBlank()) {
                    place.city
                } else {
                    place.displayTitle
                }
                onLocationSelected(place.formatted, cityArea, place.lat, place.lng)
            }
        )
    }

    // Full-screen Center-Pin Map Picker Dialog
    if (showMapPicker) {
        CenterPinMapPickerDialog(
            initialLat = currentLat,
            initialLng = currentLng,
            language = language,
            onDismiss = { showMapPicker = false },
            onConfirmLocation = { address, cityArea, lat, lng ->
                showMapPicker = false
                onLocationSelected(address, cityArea, lat, lng)
            }
        )
    }
}

/**
 * InDrive / Uber style Place Autocomplete Search Dialog powered by Geoapify API.
 */
@Composable
fun GeoapifySearchDialog(
    initialQuery: String = "",
    proximityLat: Double = 33.6844,
    proximityLng: Double = 73.0479,
    language: AppLanguage = AppLanguage.ENGLISH,
    onDismiss: () -> Unit,
    onOpenMapPicker: () -> Unit,
    onPlaceSelected: (GeoapifyPlace) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<GeoapifyPlace>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun executeSearch(q: String) {
        searchJob?.cancel()
        if (q.trim().length < 2) {
            results = emptyList()
            isSearching = false
            return
        }

        searchJob = coroutineScope.launch {
            isSearching = true
            delay(300) // Debounce typing
            results = GeoapifyService.searchAutocomplete(
                query = q,
                proximityLat = proximityLat,
                proximityLng = proximityLng,
                limit = 8
            )
            isSearching = false
        }
    }

    LaunchedEffect(Unit) {
        if (initialQuery.isNotBlank()) {
            executeSearch(initialQuery)
        }
    }

    val quickSectors = listOf(
        "F-7 Markaz, Islamabad",
        "Blue Area, Islamabad",
        "Sector G-9, Islamabad",
        "Saddar, Rawalpindi",
        "Commercial Market, Rawalpindi",
        "Bahria Town Phase 4",
        "DHA Phase 2, Islamabad",
        "Gulberg III, Lahore"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("geoapify_search_dialog"),
            color = Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header & Search Input Box
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextSlate
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    executeSearch(it)
                                },
                                placeholder = {
                                    Text(
                                        text = if (language == AppLanguage.URDU) "پتہ، سیکٹر، یا علاقہ تلاش کریں..." else "Search street, sector, or landmark...",
                                        fontSize = 14.sp,
                                        color = TextSlateMuted
                                    )
                                },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = DeepIndigo)
                                },
                                trailingIcon = {
                                    if (isSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = DeepIndigo
                                        )
                                    } else if (searchQuery.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                searchQuery = ""
                                                results = emptyList()
                                            }
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSlateMuted)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("geoapify_search_query_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DeepIndigo,
                                    unfocusedBorderColor = BorderStroke,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Select on Map Action Pill
                        Surface(
                            onClick = onOpenMapPicker,
                            shape = RoundedCornerShape(10.dp),
                            color = DeepIndigo.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth().testTag("search_open_map_pin_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = DeepIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (language == AppLanguage.URDU) "نقشے پر پن سے مقام منتخب کریں" else "Set location on map with pin",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepIndigo
                                )
                            }
                        }
                    }
                }

                // Quick Sector Chips
                if (searchQuery.isBlank() && results.isEmpty()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (language == AppLanguage.URDU) "مشہور مقامات" else "Popular Areas in Islamabad / Rawalpindi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlateMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(quickSectors) { sector ->
                                Surface(
                                    onClick = {
                                        searchQuery = sector
                                        executeSearch(sector)
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NearMe,
                                            contentDescription = null,
                                            tint = DeepIndigo,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sector,
                                            fontSize = 13.sp,
                                            color = TextSlate,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Results list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (results.isNotEmpty()) {
                        item {
                            Text(
                                text = if (language == AppLanguage.URDU) "نتائج (Geoapify API)" else "Search Results (Geoapify)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSlateMuted,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(results) { place ->
                            Card(
                                onClick = { onPlaceSelected(place) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("search_result_item_${place.displayTitle.take(10)}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEEF2FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = DeepIndigo,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = place.displayTitle,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextSlate,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = place.displaySubtitle,
                                            fontSize = 12.5.sp,
                                            color = TextSlateMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    } else if (searchQuery.isNotBlank() && !isSearching) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (language == AppLanguage.URDU) "کوئی نتیجہ نہیں ملا۔ براہ کرم نقشہ استعمال کریں۔" else "No matching addresses found. Tap 'Set location on map'.",
                                    color = TextSlateMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Attribution Footer
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Powered by Geoapify Places & OpenStreetMap",
                                fontSize = 11.sp,
                                color = TextSlateMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
