package com.example.ui.screens

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
import androidx.compose.runtime.setValue
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
    onSubmitRating: (jobId: Long, rating: Int, comment: String) -> Unit = { _, _, _ -> },
    onReportIssue: (jobId: Long, category: String, description: String) -> Unit = { _, _, _ -> },
    onAutoCompleteJob: (jobId: Long) -> Unit = {},
    onUpdateProfile: (name: String, cityArea: String, notifPref: String, savedAddresses: String) -> Unit = { _, _, _, _ -> },
    onLogout: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAllCategories by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf(user.cityArea.ifBlank { "Gulberg III, Lahore" }) }
    var isGpsDetected by remember { mutableStateOf(false) }
    var activeJobForRatingDialog by remember { mutableStateOf<ServiceRequestEntity?>(null) }
    val context = LocalContext.current

    // Auto trigger rating dialog if there is a job awaiting confirmation
    LaunchedEffect(awaitingRatingJob?.id) {
        if (awaitingRatingJob != null) {
            activeJobForRatingDialog = awaitingRatingJob
        }
    }

    // Auto-fetch location if GPS is enabled on device
    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context) && LocationHelper.isLocationEnabled(context)) {
            val loc = LocationHelper.getCurrentLocation(context)
            if (loc != null) {
                selectedLocation = loc.cityArea
                isGpsDetected = true
            }
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AutoLocationFetcher(
                        autoFetch = true,
                        onLocationDetected = { loc ->
                            selectedLocation = loc.cityArea
                            isGpsDetected = true
                            showLocationDialog = false
                        }
                    )

                    Text(
                        text = "Or choose predefined area:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSlateMuted
                    )

                    listOf(
                        "Gulberg III, Lahore",
                        "DHA Phase 5, Lahore",
                        "Model Town, Lahore",
                        "Johar Town, Lahore",
                        "F-7 / Blue Area, Islamabad",
                        "Clifton Block 4, Karachi"
                    ).forEach { loc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedLocation == loc) DeepIndigoContainer else Color(0xFFF8FAFC))
                                .clickable {
                                    selectedLocation = loc
                                    showLocationDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (selectedLocation == loc) DeepIndigo else TextSlateMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = loc,
                                fontSize = 14.sp,
                                fontWeight = if (selectedLocation == loc) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedLocation == loc) DeepIndigo else TextSlate
                            )
                        }
                    }
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
                onLogout = onLogout
            )
        },
        bottomBar = {
            // Professional Polish Bottom Navigation Bar
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
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
                            showPaymentDialog = true
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
        containerColor = BackgroundLight
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
                                text = selectedLocation,
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
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search input field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = Strings.get("search_hint", language),
                            color = TextSlateMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = DeepIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_search_bar"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepIndigo,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            // Section 2: Professional Polish Hero Card ("Need help today?")
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = Color(0x334338CA),
                            ambientColor = Color(0x224338CA)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(DeepIndigo, Color(0xFF3730A3))
                            )
                        )
                        .clickable { onStartNewRequest(null) }
                        .padding(24.dp)
                        .testTag("request_service_hero_card")
                ) {
                    // Subtle background watermark house icon
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(110.dp)
                            .alpha(0.12f)
                    ) {
                        HomeaseLogoMark(
                            size = 110.dp,
                            primaryColor = Color.White,
                            accentColor = Color.White,
                            doorColor = DeepIndigo
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
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = if (language == AppLanguage.URDU) "250 سے زائد تصدیق شدہ کاریگر" else "Connecting you to 250+ pros",
                            fontSize = 13.sp,
                            color = Color(0xFFE0E7FF),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Soft Orange Action Button with pure language prompt
                        Button(
                            onClick = { onStartNewRequest(null) },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SoftOrange,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            modifier = Modifier.testTag("hero_request_service_btn")
                        ) {
                            Text(
                                text = Strings.get("request_service_cta", language),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3-Column Grid matching Design HTML exactly
                    val chunked = categoriesToDisplay.chunked(3)
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        Spacer(modifier = Modifier.height(10.dp))
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
                                    color = Color(0xFFF1F5F9),
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
                                        color = Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                }
                            }

                            Button(
                                onClick = { onOpenRequestDetails(req.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF1F5F9),
                                    contentColor = TextSlate
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
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
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("cat_card_${item.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pastel rounded badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(item.pastelBgColor)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.emoji,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pure single-language Category Name
            Text(
                text = Strings.get(item.nameKey, language),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSlate,
                maxLines = 1
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) DeepIndigo else Color(0xFF94A3B8),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) DeepIndigo else Color(0xFF94A3B8)
        )
    }
}
