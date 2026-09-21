package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ServiceRequestEntity
import com.example.data.db.UserEntity
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.data.model.ServiceCatalog
import com.example.data.model.ServiceCategoryItem
import com.example.data.model.UserRole
import com.example.ui.components.AutoLocationFetcher
import com.example.ui.components.LocationPickerInput
import com.example.ui.components.HomEaseTopBar
import com.example.ui.components.HomeaseLogoMark
import com.example.ui.components.HowDidItGoDialog
import com.example.ui.components.PaymentStubDialog
import com.example.ui.components.RatingStubDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.DeepIndigoContainer
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SoftOrangeContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.SurfaceVariantLight
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted
import com.example.util.LocationHelper

@Composable
fun CustomerHomeScreen(
    user: UserEntity,
    activeRequests: List<ServiceRequestEntity>,
    awaitingRatingJob: ServiceRequestEntity? = null,
    language: AppLanguage,
    onToggleRole: () -> Unit,
    onToggleLanguage: () -> Unit,
    onStartNewRequest: (String?) -> Unit,
    onOpenRequestDetails: (Long) -> Unit,
    onOpenLiveTracking: (ServiceRequestEntity) -> Unit = {},
    onSubmitRating: (jobId: Long, rating: Int, comment: String) -> Unit = { _, _, _ -> },
    onReportIssue: (jobId: Long, category: String, description: String) -> Unit = { _, _, _ -> },
    onAutoCompleteJob: (jobId: Long) -> Unit = {},
    onUpdateProfile: (name: String, cityArea: String, savedAddresses: String) -> Unit = { _, _, _ -> },
    onLogout: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAllCategories by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf(user.cityArea.ifBlank { "" }) }
    var isGpsDetected by remember { mutableStateOf(false) }
    var activeJobForRatingDialog by remember { mutableStateOf<ServiceRequestEntity?>(null) }
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            // Permission granted, trigger location fetch
            coroutineScope.launch {
                LocationHelper.getCurrentLocation(context)?.let { loc ->
                    selectedLocation = loc.cityArea
                    isGpsDetected = true
                    onUpdateProfile(user.name, loc.cityArea, user.savedAddressesCsv)
                }
            }
        }
    }

    // Auto trigger rating dialog if there is a job awaiting confirmation
    LaunchedEffect(awaitingRatingJob?.id) {
        if (awaitingRatingJob != null) {
            activeJobForRatingDialog = awaitingRatingJob
        }
    }

    // Auto-fetch location if GPS is enabled on device, or request permission
    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context)) {
            if (LocationHelper.isLocationEnabled(context)) {
                val loc = LocationHelper.getCurrentLocation(context)
                if (loc != null) {
                    selectedLocation = loc.cityArea
                    isGpsDetected = true
                } else if (selectedLocation.isBlank()) {
                    selectedLocation = "Detecting location..."
                }
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var currentNavTab by remember { mutableStateOf("home") }

    val pendingRequests = activeRequests.filter { it.status in listOf("SEARCHING", "ACCEPTED", "IN_PROGRESS") }
    val pastBookings = activeRequests.filter { it.status == "COMPLETED" }

    val categoriesToDisplay = remember(searchQuery, showAllCategories, language) {
        if (searchQuery.isNotBlank()) {
            ServiceCatalog.categories.filter {
                Strings.get(it.nameKey, language).contains(searchQuery, ignoreCase = true) ||
                        it.popularServices.any { s -> s.contains(searchQuery, ignoreCase = true) }
            }
        } else if (showAllCategories) {
            ServiceCatalog.categories
        } else {
            ServiceCatalog.categories.take(6)
        }
    }

    // Pulsing animation for the active request dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = {
                Text(
                    text = Strings.get("select_area", language),
                    fontWeight = FontWeight.Bold,
                    color = TextSlate
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LocationPickerInput(
                        label = Strings.get("city_area", language),
                        addressValue = selectedLocation,
                        cityAreaValue = selectedLocation,
                        latitude = user.lat,
                        longitude = user.lng,
                        language = language,
                        testTagPrefix = "home_location",
                        isRequired = false,
                        showGpsShortcut = true,
                        onLocationSelected = { address, city, lat, lng ->
                            selectedLocation = if (city.isNotBlank()) city else address
                            isGpsDetected = true
                            showLocationDialog = false
                            onUpdateProfile(user.name, selectedLocation, user.savedAddressesCsv)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text(Strings.get("cancel", language), color = TextSlateMuted)
                }
            }
        )
    }

    if (showPaymentDialog) {
        PaymentStubDialog(language = language, onDismiss = { showPaymentDialog = false })
    }

    if (showRatingDialog) {
        RatingStubDialog(language = language, onDismiss = { showRatingDialog = false })
    }

    Scaffold(
        topBar = {
            HomEaseTopBar(
                currentRole = UserRole.CUSTOMER,
                language = language,
                onToggleRole = onToggleRole,
                onToggleLanguage = onToggleLanguage,
                userAvatar = user.profilePhotoUri ?: "👤",
                onLogout = onLogout,
                onOpenProfile = { currentNavTab = "profile" }
            )
        },
        bottomBar = {
            // Apple-style Frosted Glass Bottom Navigation Bar
            Surface(
                color = Color.White.copy(alpha = 0.94f),
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x14000000)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfessionalBottomNavItem(
                        icon = Icons.Default.Home,
                        label = if (language == AppLanguage.URDU) "ہوم" else "Home",
                        isActive = currentNavTab == "home",
                        onClick = { currentNavTab = "home" }
                    )
                    ProfessionalBottomNavItem(
                        icon = Icons.Default.Assignment,
                        label = if (language == AppLanguage.URDU) "بکنگز" else "Bookings",
                        isActive = currentNavTab == "bookings",
                        onClick = { currentNavTab = "bookings" }
                    )
                    ProfessionalBottomNavItem(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        label = if (language == AppLanguage.URDU) "مدد" else "Help",
                        isActive = currentNavTab == "help",
                        onClick = {
                            currentNavTab = "help"
                        }
                    )
                    ProfessionalBottomNavItem(
                        icon = Icons.Default.Person,
                        label = if (language == AppLanguage.URDU) "پروفائل" else "Profile",
                        isActive = currentNavTab == "profile",
                        onClick = { currentNavTab = "profile" }
                    )
                }
            }
        },
        containerColor = Color(0xFFF2F2F7)
    ) { innerPadding ->
        if (activeJobForRatingDialog != null) {
            HowDidItGoDialog(
                job = activeJobForRatingDialog!!,
                language = language,
                onSubmitRating = { rating, comment ->
                    onSubmitRating(activeJobForRatingDialog!!.id, rating, comment)
                    activeJobForRatingDialog = null
                },
                onReportIssue = { category, description ->
                    onReportIssue(activeJobForRatingDialog!!.id, category, description)
                    activeJobForRatingDialog = null
                },
                onAutoCompleteWithoutRating = {
                    onAutoCompleteJob(activeJobForRatingDialog!!.id)
                    activeJobForRatingDialog = null
                },
                onDismiss = { activeJobForRatingDialog = null }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentNavTab) {
                "bookings" -> {
                    CustomerBookingsView(
                        allRequests = activeRequests,
                        language = language,
                        onRateJob = { job -> activeJobForRatingDialog = job },
                        onViewLiveJob = onOpenRequestDetails,
                        onStartNewBooking = { onStartNewRequest(null) }
                    )
                }
                "help" -> {
                    CustomerHelpSupportView(
                        language = language,
                        onReportIssue = { category, description ->
                            onReportIssue(0L, category, description)
                        }
                    )
                }
                "profile" -> {
                    CustomerProfileView(
                        user = user,
                        language = language,
                        onToggleRole = onToggleRole,
                        onToggleLanguage = onToggleLanguage,
                        onSaveProfile = onUpdateProfile,
                        onLogout = onLogout
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Section 0: Urgent Awaiting Confirmation Banner if provider marked job complete
                        item {
                            val unconfirmedJob = awaitingRatingJob ?: activeRequests.firstOrNull { it.status == "AWAITING_CUSTOMER_CONFIRMATION" }
                            if (unconfirmedJob != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("customer_unconfirmed_job_banner"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = Strings.get("how_did_it_go_title", language),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF92400E)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (language == AppLanguage.URDU) "${unconfirmedJob.selectedProviderName ?: "کاریگر"} نے '${unconfirmedJob.serviceTitle}' کو مکمل نشان زد کر دیا ہے۔" else "${unconfirmedJob.selectedProviderName ?: "Provider"} marked '${unconfirmedJob.serviceTitle}' as complete.",
                                            fontSize = 13.sp,
                                            color = TextSlate
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = { activeJobForRatingDialog = unconfirmedJob },
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                                        ) {
                                            Text(Strings.get("review_and_complete", language), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Section 1: Location header (Location • مقام)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLocationDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Strings.get("city_area", language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isGpsDetected) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "GPS Active",
                                    tint = StatusGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = selectedLocation.ifBlank { if (language == AppLanguage.URDU) "مقام منتخب کریں" else "Select location" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSlate
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Location",
                                tint = DeepIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Search Pill button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceVariantLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextSlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Apple-style Search Bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(0.5.dp, Color(0x14000000), RoundedCornerShape(14.dp))
                        .shadow(1.dp, RoundedCornerShape(14.dp), spotColor = Color(0x0A000000))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 15.sp,
                                color = Color(0xFF1C1C1E),
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("customer_search_bar"),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isBlank()) {
                                    Text(
                                        text = Strings.get("search_hint", language),
                                        color = Color(0xFF8E8E93),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }
            }

            // Section 2: Apple HIG Hero Card ("Need help today?")
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 3.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = Color(0x20007AFF),
                            ambientColor = Color(0x10007AFF)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF007AFF), Color(0xFF5856D6))
                            )
                        )
                        .clickable { onStartNewRequest(null) }
                        .padding(22.dp)
                        .testTag("request_service_hero_card")
                ) {
                    // Subtle background watermark house icon
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(100.dp)
                            .alpha(0.14f)
                    ) {
                        HomeaseLogoMark(
                            size = 100.dp,
                            primaryColor = Color.White,
                            accentColor = Color.White,
                            doorColor = Color(0xFF007AFF)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.URDU) "کیا آج گھریلو مدد درکار ہے؟" else "Need help today?",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = if (language == AppLanguage.URDU) "250 سے زائد تصدیق شدہ ماہر کاریگر" else "Connecting you to 250+ vetted pros",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.88f),
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Apple-style translucent pill button
                        Surface(
                            onClick = { onStartNewRequest(null) },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier.testTag("hero_request_service_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Strings.get("request_service_cta", language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF007AFF),
                                    letterSpacing = (-0.1).sp
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Categories (3-column grid)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Strings.get("categories_header", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlate,
                            letterSpacing = (-0.2).sp
                        )

                        Text(
                            text = if (showAllCategories) Strings.get("show_less", language) else Strings.get("view_all", language),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepIndigo,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier
                                .clickable { showAllCategories = !showAllCategories }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3-Column Grid matching Design HTML exactly
                    val chunked = categoriesToDisplay.chunked(3)
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                ProfessionalCategoryCard(
                                    item = item,
                                    language = language,
                                    onClick = { onStartNewRequest(item.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Filler if row has fewer than 3 items
                            for (i in 0 until (3 - rowItems.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Section 4: Active Request Banner (border-l-4 border-[#FB923C] shadow-md)
            if (pendingRequests.isNotEmpty()) {
                val req = pendingRequests.first()
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRequestDetails(req.id) }
                            .testTag("active_request_banner"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = BorderStroke,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Orange left vertical accent bar
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(SoftOrange)
                                 )
                                Spacer(modifier = Modifier.width(12.dp))

                                // Pulsing dot
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SoftOrange.copy(alpha = pulseAlpha))
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = Strings.get("active_requests_header", language),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSlate
                                    )
                                    Text(
                                        text = when (req.status) {
                                            "SEARCHING" -> if (language == AppLanguage.URDU) "قریبی ماہرین تلاش کیے جا رہے ہیں..." else "Finding providers near you..."
                                            "ACCEPTED" -> if (language == AppLanguage.URDU) "${req.selectedProviderName ?: "کاریگر"} نے قبول کر لیا" else "Accepted by ${req.selectedProviderName ?: "Pro"}"
                                            else -> req.serviceTitle
                                        },
                                        fontSize = 11.sp,
                                        color = TextSlateMuted,
                                        maxLines = 1
                                    )
                                }
                            }

                            val isTrackable = req.status in listOf("ACCEPTED", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS")
                            Button(
                                onClick = {
                                    if (isTrackable) {
                                        onOpenLiveTracking(req)
                                    } else {
                                        onOpenRequestDetails(req.id)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTrackable) Color(0xFFDC5F45) else SurfaceVariantLight,
                                    contentColor = if (isTrackable) Color.White else TextSlate
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("track_active_request_btn")
                            ) {
                                if (isTrackable) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == AppLanguage.URDU) "لائیو ٹریکنگ" else "Track Live",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = Strings.get("view_progress", language),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Past Bookings Section
            if (pastBookings.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = Strings.get("past_bookings_header", language),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlate
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        pastBookings.forEach { booking ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = booking.serviceTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextSlate
                                        )
                                        Text(
                                            text = "Provider: ${booking.selectedProviderName ?: "Verified Pro"}",
                                            fontSize = 12.sp,
                                            color = TextSlateMuted
                                        )
                                    }
                                    Text(
                                        text = "Rs ${booking.agreedPriceRs}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = StatusGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
}
}

@Composable
fun ProfessionalCategoryCard(
    item: ServiceCategoryItem,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "category_card_press"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .testTag("cat_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x0F000000)),
        shadowElevation = if (isPressed) 0.5.dp else 1.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Apple-style Pastel rounded squircle badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(item.pastelBgColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.emoji,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Apple SF Pro bold label
            Text(
                text = Strings.get(item.nameKey, language),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E),
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun ProfessionalBottomNavItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "tab_bounce"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color(0xFF007AFF) else Color(0xFF8E8E93),
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Color(0xFF007AFF) else Color(0xFF8E8E93),
            letterSpacing = (-0.1).sp
        )
    }
}

@Composable
fun CustomerHelpSupportView(
    language: AppLanguage,
    onReportIssue: (category: String, description: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedFaqIndex by remember { mutableStateOf<Int?>(null) }
    var selectedCategory by remember { mutableStateOf("Quality of Work") }
    var issueDescription by remember { mutableStateOf("") }
    var showSuccessBanner by remember { mutableStateOf(false) }

    val faqItems = remember(language) {
        listOf(
            Pair(
                if (language == AppLanguage.URDU) "سروس کی درخواست کیسے کی جاتی ہے؟" else "How do I request a service?",
                if (language == AppLanguage.URDU)
                    "کیٹیگری منتخب کریں یا اپنا مسئلہ بیان کریں۔ اپنا بجٹ اور مقام درج کریں، اور قریبی تصدیق شدہ کاریگر آپ کی درخواست پر بولیاں لگائیں گے۔"
                else
                    "Select a category or use AI problem description. Enter your location and price offer, and nearby verified professionals will bid on your request."
            ),
            Pair(
                if (language == AppLanguage.URDU) "ادائیگی کیسے کی جاتی ہے؟" else "How does payment work?",
                if (language == AppLanguage.URDU)
                    "قیمت کا تعین کام سے پہلے باہمی رضامندی سے ہوتا ہے۔ کام تسلی بخش مکمل ہونے کے بعد آپ براہ راست کیش یا آن لائن ادائیگی کر سکتے ہیں۔"
                else
                    "Prices are agreed upon before work starts. You pay the provider directly via Cash or digital transfer only after the job is completed to your satisfaction."
            ),
            Pair(
                if (language == AppLanguage.URDU) "کیا کام کی وارنٹی یا ضمانت ہے؟" else "What is HomEase's Quality Guarantee?",
                if (language == AppLanguage.URDU)
                    "ہمارے تمام ماہرین شناختی کارڈ سے تصدیق شدہ ہیں۔ اگر کام کے دوران کوئی خرابی پیش آئے تو ہماری سپورٹ ٹیم فوری مدد فراہم کرتی ہے۔"
                else
                    "All providers are CNIC-verified and vetted. In the rare event of service dissatisfaction or issues, our support team mediates and provides coverage."
            ),
            Pair(
                if (language == AppLanguage.URDU) "کیا بکنگ منسوخ کی جا سکتی ہے؟" else "Can I cancel or reschedule a booking?",
                if (language == AppLanguage.URDU)
                    "جی ہاں، کاریگر کی آمد سے قبل آپ کسی بھی وقت بغیر کسی فیس کے بکنگ منسوخ یا وقت تبدیل کر سکتے ہیں۔"
                else
                    "Yes, you can cancel or reschedule any booking free of charge before the provider arrives at your address."
            ),
            Pair(
                if (language == AppLanguage.URDU) "کسی مسئلے یا شکایت کی اطلاع کیسے دیں؟" else "How do I report an issue or dispute?",
                if (language == AppLanguage.URDU)
                    "نیچے دیے گئے فارم سے شکایت درج کریں یا ہماری 24/7 ہیلپ لائن 042-111-HOME پر رابطہ کریں۔ ہم 15 منٹ میں مدد فراہم کرتے ہیں۔"
                else
                    "Use the issue reporting form below or contact our 24/7 hotline at 042-111-HOME (4663). Our team investigates and resolves disputes quickly."
            )
        )
    }

    val issueCategories = remember(language) {
        listOf(
            "Quality of Work" to if (language == AppLanguage.URDU) "کام کا معیار" else "Quality of Work",
            "Payment/Charges" to if (language == AppLanguage.URDU) "ادائیگی/بل" else "Payment/Billing",
            "Provider Conduct" to if (language == AppLanguage.URDU) "کاریگر کا رویہ" else "Provider Conduct",
            "Booking Delay" to if (language == AppLanguage.URDU) "تاخیر" else "Delay",
            "Other" to if (language == AppLanguage.URDU) "دیگر" else "Other"
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
            .testTag("customer_help_screen")
            .testTag("customer_help_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(DeepIndigoContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = DeepIndigo,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (language == AppLanguage.URDU) "مدد اور رہنمائی" else "Help & Support",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = TextSlate
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (language == AppLanguage.URDU) "24/7 کسٹمر اسسٹنس • اکثر پوچھے جانے والے سوالات" else "24/7 Customer Assistance • FAQs & Problem Resolution",
                        fontSize = 12.sp,
                        color = TextSlateMuted
                    )
                }
            }
        }

        // Quick Contact Channels
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (language == AppLanguage.URDU) "فوری رابطہ" else "Quick Contact",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlate
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 24/7 Helpline
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("help_call_hotline_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StatusGreenContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Helpline",
                                    tint = StatusGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "042-111-HOME",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextSlate
                            )
                            Text(
                                text = "24/7 Helpline",
                                fontSize = 10.sp,
                                color = TextSlateMuted
                            )
                        }
                    }

                    // WhatsApp Support
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("help_whatsapp_support_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DeepIndigoContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "WhatsApp",
                                    tint = DeepIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "WhatsApp Support",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextSlate
                            )
                            Text(
                                text = "Live Chat",
                                fontSize = 10.sp,
                                color = TextSlateMuted
                            )
                        }
                    }
                }
            }
        }

        // FAQs Section
        item {
            Text(
                text = if (language == AppLanguage.URDU) "عام سوالات (FAQ)" else "Frequently Asked Questions",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextSlate
            )
        }

        items(faqItems.size) { index ->
            val (question, answer) = faqItems[index]
            val isExpanded = expandedFaqIndex == index

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedFaqIndex = if (isExpanded) null else index }
                    .testTag("faq_item_$index"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = question,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextSlate,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = DeepIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = answer,
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Report an Issue / Contact Us Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = DeepIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "مسئلہ یا شکایت درج کریں" else "Report an Issue or Contact Us",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlate
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (language == AppLanguage.URDU) "مسئلے کی قسم:" else "Select Category:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSlateMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        issueCategories.take(3).forEach { (catKey, label) ->
                            FilterChip(
                                selected = selectedCategory == catKey,
                                onClick = { selectedCategory = catKey },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DeepIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        issueCategories.drop(3).forEach { (catKey, label) ->
                            FilterChip(
                                selected = selectedCategory == catKey,
                                onClick = { selectedCategory = catKey },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DeepIndigo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = issueDescription,
                        onValueChange = { issueDescription = it },
                        label = { Text(if (language == AppLanguage.URDU) "مسئلے کی تفصیل بتائیں" else "Describe your issue") },
                        placeholder = { Text(if (language == AppLanguage.URDU) "کیا مسئلہ درپیش ہے؟" else "e.g., Provider arrived late, pricing dispute...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("help_issue_description_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (showSuccessBanner) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(StatusGreenContainer)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.URDU)
                                    "آپ کی شکایت موصول ہو گئی۔ ہماری سپورٹ ٹیم جلد رابطہ کرے گی۔"
                                else
                                    "Your report has been submitted! Our support team will contact you within 15 minutes.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            if (issueDescription.isNotBlank()) {
                                onReportIssue(selectedCategory, issueDescription)
                                showSuccessBanner = true
                                issueDescription = ""
                            }
                        },
                        enabled = issueDescription.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("help_submit_issue_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "شکایت جمع کروائیں" else "Submit Report",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
