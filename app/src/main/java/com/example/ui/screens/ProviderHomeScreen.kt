package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import java.text.NumberFormat
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ServiceRequestEntity
import com.example.data.db.UserEntity
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.data.model.UserRole
import com.example.ui.components.HomEaseTopBar
import com.example.ui.components.PrimaryCtaButton
import com.example.ui.components.ProviderCompleteConfirmationDialog
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.DeepIndigoContainer
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SoftOrangeContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusYellowContainer
import com.example.ui.theme.SurfaceVariantLight
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted
import com.example.ui.theme.LiquidAmbientCanvas
import com.example.ui.theme.liquidGlassCard
import com.example.ui.theme.AppleSystemBlue
import com.example.ui.theme.AppleSystemGreen
import com.example.ui.theme.AppleSystemGreenContainer
import com.example.ui.theme.AppleSystemOrange
import com.example.ui.theme.AppleSystemOrangeDark
import com.example.ui.theme.AppleSystemOrangeContainer
import com.example.ui.theme.AppleSystemIndigo
import com.example.ui.theme.AppleSystemTeal
import com.example.ui.theme.AppleLabelPrimary
import com.example.ui.theme.AppleLabelSecondary

@Composable
fun ProviderHomeScreen(
    provider: UserEntity,
    incomingJobs: List<ServiceRequestEntity>,
    activeJob: ServiceRequestEntity?,
    pastJobs: List<ServiceRequestEntity> = emptyList(),
    completedJobs: List<ServiceRequestEntity>,
    language: AppLanguage,
    isRefreshingStatus: Boolean = false,
    statusCheckMessage: String? = null,
    providerActionError: String? = null,
    onClearProviderActionError: () -> Unit = {},
    onToggleRole: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleOnline: (Boolean) -> Unit,
    onCheckVerificationStatus: () -> Unit = {},
    onClearStatusMessage: () -> Unit = {},
    onAcceptJob: (ServiceRequestEntity) -> Unit,
    onRejectJob: (ServiceRequestEntity) -> Unit,
    onCounterJob: (ServiceRequestEntity, Int, String?) -> Unit,
    onStartTrip: (ServiceRequestEntity) -> Unit = {},
    onArrived: (ServiceRequestEntity) -> Unit = {},
    onStartWork: (ServiceRequestEntity) -> Unit = {},
    onCompleteActiveJob: (Long) -> Unit,
    onOpenChat: (ServiceRequestEntity) -> Unit = {},
    onStartCall: (ServiceRequestEntity) -> Unit = {},
    onUpdateProfile: (
        name: String,
        cityArea: String,
        categoriesCsv: String,
        yearsExperience: String,
        serviceRadiusKm: Int,
        bio: String,
        shopName: String,
        payoutMethod: String,
        payoutAccountNumber: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(provider.isOnline) }
    var counterTargetJob by remember { mutableStateOf<ServiceRequestEntity?>(null) }
    var counterPriceInput by remember { mutableStateOf("") }
    var counterNoteInput by remember { mutableStateOf("") }
    var showCompleteConfirmation by remember { mutableStateOf(false) }
    var currentNavTab by remember { mutableStateOf("jobs") }
    val isApproved = provider.status == "APPROVED"

    var showLocationPermissionExplanation by remember { mutableStateOf(false) }
    var pendingTripJob by remember { mutableStateOf<ServiceRequestEntity?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        pendingTripJob?.let { job ->
            onStartTrip(job)
            pendingTripJob = null
        }
    }

    // Counter Dialog with clear contrast and mathematical increments
    if (counterTargetJob != null) {
        val job = counterTargetJob!!
        AlertDialog(
            onDismissRequest = { counterTargetJob = null },
            title = {
                Text(
                    text = Strings.get("counter_dialog_title", language),
                    fontWeight = FontWeight.Bold,
                    color = TextSlate,
                    fontSize = 19.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "${job.serviceTitle} (${job.cityArea})",
                        fontWeight = FontWeight.Bold,
                        color = DeepIndigo,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (language == AppLanguage.URDU) "کسٹمر کی پیشکش:" else "Customer Offer:"} Rs ${job.budgetRs}",
                        color = TextSlateMuted,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = Strings.get("counter_dialog_desc", language),
                        color = TextSlate,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = counterPriceInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 6) {
                                counterPriceInput = digits
                            }
                        },
                        prefix = { Text("Rs ", fontWeight = FontWeight.Bold, color = DeepIndigo, fontSize = 18.sp) },
                        trailingIcon = {
                            if (counterPriceInput.isNotEmpty()) {
                                IconButton(onClick = { counterPriceInput = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSlateMuted)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = TextSlate,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("counter_price_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextSlate,
                            unfocusedTextColor = TextSlate,
                            cursorColor = DeepIndigo,
                            focusedBorderColor = DeepIndigo,
                            unfocusedBorderColor = BorderStroke,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // Quick inspection quote preset chip
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SoftOrangeContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                counterPriceInput = "300"
                                counterNoteInput = "Rs 300 to inspect and quote"
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💡", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Rs 300 to inspect and quote",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftOrange
                            )
                        }
                    }

                    // Quick increments: mathematical addition
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(200, 500, 1000).forEach { inc ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DeepIndigoContainer)
                                    .clickable {
                                        val currentVal = counterPriceInput.toIntOrNull() ?: job.budgetRs
                                        counterPriceInput = (currentVal + inc).coerceIn(100, 100000).toString()
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+ Rs $inc",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepIndigo
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Optional Counter Note
                    OutlinedTextField(
                        value = counterNoteInput,
                        onValueChange = { counterNoteInput = it },
                        placeholder = { Text(if (language == AppLanguage.URDU) "کوئی وضاحتی نوٹ (اختیاری)" else "Optional note (e.g. available in 20 mins)", color = TextSlateMuted, fontSize = 12.sp) },
                        maxLines = 2,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextSlate, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextSlate,
                            unfocusedTextColor = TextSlate,
                            cursorColor = DeepIndigo,
                            focusedBorderColor = DeepIndigo,
                            unfocusedBorderColor = BorderStroke,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = counterPriceInput.toIntOrNull() ?: job.budgetRs
                        onCounterJob(job, price, counterNoteInput.ifBlank { null })
                        counterTargetJob = null
                        counterNoteInput = ""
                    },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftOrange)
                ) {
                    Text(
                        text = Strings.get("send_counter_offer", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    counterTargetJob = null
                    counterNoteInput = ""
                }) {
                    Text(Strings.get("cancel", language), color = TextSlateMuted, fontSize = 14.sp)
                }
            }
        )
    }

    if (showCompleteConfirmation && activeJob != null) {
        ProviderCompleteConfirmationDialog(
            jobTitle = activeJob.serviceTitle,
            onConfirm = {
                showCompleteConfirmation = false
                onCompleteActiveJob(activeJob.id)
            },
            onDismiss = {
                showCompleteConfirmation = false
            }
        )
    }

    if (showLocationPermissionExplanation && pendingTripJob != null) {
        AlertDialog(
            onDismissRequest = {
                showLocationPermissionExplanation = false
                pendingTripJob = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = DeepIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.URDU) "لائیو لوکیشن شیئرنگ" else "Live Location Sharing",
                        fontWeight = FontWeight.Bold,
                        color = TextSlate,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Text(
                    text = if (language == AppLanguage.URDU)
                        "کسٹمر کو آپ کے سفر کی لائیو ٹریکنگ دکھانے کے لیے، ہوم ایز آپ کی لوکیشن کو پس منظر میں شیئر کرے گا۔ ٹریکنگ کے دوران ایک مستقل نوٹیفکیشن ظاہر رہے گا، اور آپ کے پہنچنے پر ٹریکنگ خود بخود بند ہو جائے گی۔"
                    else
                        "To provide the customer with InDrive-style live tracking of your arrival, HomEase will share your location while you are on the way. A persistent notification will remain visible, and location sharing stops immediately when you tap 'Arrived'.",
                    fontSize = 14.sp,
                    color = TextSlate,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationPermissionExplanation = false
                        val perms = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        locationPermissionLauncher.launch(perms.toTypedArray())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.URDU) "اجازت دیں اور شروع کریں" else "Allow & Start Trip",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLocationPermissionExplanation = false
                    pendingTripJob = null
                }) {
                    Text(text = Strings.get("cancel", language), color = TextSlateMuted)
                }
            }
        )
    }

    LiquidAmbientCanvas {
        Scaffold(
            topBar = {
                HomEaseTopBar(
                    currentRole = UserRole.PROVIDER,
                    language = language,
                    onToggleRole = onToggleRole,
                    onToggleLanguage = onToggleLanguage,
                    userAvatar = provider.profilePhotoUri ?: "👤",
                    onLogout = onLogout,
                    onOpenProfile = { currentNavTab = "profile" }
                )
            },
            bottomBar = {
                // Apple-style Frosted Glass Bottom Navigation Bar
                Surface(
                    color = Color.White.copy(alpha = 0.82f),
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
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfessionalBottomNavItem(
                        icon = Icons.Default.Work,
                        label = if (language == AppLanguage.URDU) "کام" else "Jobs",
                        isActive = currentNavTab == "jobs",
                        onClick = { currentNavTab = "jobs" }
                    )
                    ProfessionalBottomNavItem(
                        icon = Icons.Default.Assignment,
                        label = if (language == AppLanguage.URDU) "تاریخچہ" else "History",
                        isActive = currentNavTab == "bookings",
                        onClick = { currentNavTab = "bookings" }
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
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentNavTab) {
                "bookings" -> {
                    ProviderBookingsView(
                        allJobs = pastJobs.ifEmpty { completedJobs },
                        language = language
                    )
                }
                "profile" -> {
                    ProviderProfileView(
                        user = provider,
                        language = language,
                        onToggleRole = onToggleRole,
                        onToggleLanguage = onToggleLanguage,
                        onCheckVerificationStatus = onCheckVerificationStatus,
                        onSaveProfile = onUpdateProfile,
                        onLogout = onLogout
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
            // Apple Liquid Glass Availability Card & Verification Status
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlassCard(shape = RoundedCornerShape(22.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) AppleSystemGreen else AppleLabelSecondary)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isOnline) Strings.get("online", language) else Strings.get("offline", language),
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppleLabelPrimary,
                                        letterSpacing = (-0.3).sp
                                    )
                                    Text(
                                        text = if (isOnline) Strings.get("ready_for_jobs", language) else Strings.get("switch_on_to_get_jobs", language),
                                        fontSize = 13.sp,
                                        color = AppleLabelSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = isOnline,
                                onCheckedChange = {
                                    isOnline = it
                                    onToggleOnline(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AppleSystemGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFE5E5EA)
                                ),
                                modifier = Modifier.testTag("provider_online_switch")
                            )
                        }
                    }

                    // Apple Inset Verification Status Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isApproved) AppleSystemGreenContainer else AppleSystemOrangeContainer)
                            .border(0.5.dp, if (isApproved) AppleSystemGreen.copy(alpha = 0.25f) else AppleSystemOrange.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isApproved) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = if (isApproved) AppleSystemGreen else AppleSystemOrangeDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isApproved) {
                                if (language == AppLanguage.URDU) "حیثیت: تصدیق شدہ کاریگر" else "Status: Approved & Verified"
                            } else {
                                if (language == AppLanguage.URDU) "حیثیت: جائزہ جاری ہے" else "Status: Under Review"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isApproved) Color(0xFF065F46) else Color(0xFF92400E)
                        )
                    }
                }
            }

            // Case A: PROVIDER IS UNAPPROVED / PENDING
            if (!isApproved) {
                item {
                    PendingProviderVerificationCard(
                        provider = provider,
                        language = language,
                        isRefreshingStatus = isRefreshingStatus,
                        statusCheckMessage = statusCheckMessage,
                        onCheckVerificationStatus = onCheckVerificationStatus,
                        onClearStatusMessage = onClearStatusMessage
                    )
                }
            } else {
                // Case B: PROVIDER IS APPROVED -> Show Active Job & Job Requests

                // 1. Current Active Job (if any)
                if (activeJob != null) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = Strings.get("active_job", language).uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = DeepIndigo,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            SimplifiedActiveJobCard(
                                job = activeJob,
                                language = language,
                                onStartTrip = {
                                    val hasFine = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasFine) {
                                        onStartTrip(activeJob)
                                    } else {
                                        pendingTripJob = activeJob
                                        showLocationPermissionExplanation = true
                                    }
                                },
                                onArrived = { onArrived(activeJob) },
                                onStartWork = { onStartWork(activeJob) },
                                onComplete = { showCompleteConfirmation = true },
                                onOpenChat = { onOpenChat(activeJob) },
                                onStartCall = { onStartCall(activeJob) }
                            )
                        }
                    }
                }

                // Error banner for action failures (e.g. job already claimed by another provider)
                if (!providerActionError.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Error",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = providerActionError,
                                    color = Color(0xFF991B1B),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onClearProviderActionError,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color(0xFF991B1B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Incoming Job Requests Feed (Simplified, Large Buttons, Icon-First)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.URDU) "نئی درخواستیں" else "NEW REQUESTS",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = TextSlate,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DeepIndigoContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${incomingJobs.size} ${if (language == AppLanguage.URDU) "دستیاب" else "Available"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepIndigo
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (incomingJobs.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No job requests in your radius right now.",
                                        color = TextSlate,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Keep your status ONLINE to receive incoming pings automatically.",
                                        color = TextSlateMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                incomingJobs.forEach { job ->
                                    SimplifiedJobPingCard(
                                        job = job,
                                        language = language,
                                        onAccept = { onAcceptJob(job) },
                                        onReject = { onRejectJob(job) },
                                        onCounter = {
                                            counterTargetJob = job
                                            counterPriceInput = (job.budgetRs + 200).toString()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Simple Earnings Summary
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val totalEarned = completedJobs.sumOf { it.agreedPriceRs }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassCard(shape = RoundedCornerShape(16.dp))
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = Strings.get("completed_jobs", language),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSlate
                                )
                                Text(
                                    text = "${completedJobs.size} ${Strings.get("jobs_completed_badge", language)}",
                                    fontSize = 13.sp,
                                    color = TextSlateMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (language == AppLanguage.URDU) "کل کمائی" else "Total Earned",
                                    fontSize = 12.sp,
                                    color = TextSlateMuted
                                )
                                Text(
                                    text = "Rs $totalEarned",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
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

/**
 * Clean, respectful Pending Verification screen for unapproved providers.
 * Strictly enforces that pending providers do NOT receive live job pings.
 */
@Composable
fun PendingProviderVerificationCard(
    provider: UserEntity,
    language: AppLanguage,
    isRefreshingStatus: Boolean = false,
    statusCheckMessage: String? = null,
    onCheckVerificationStatus: () -> Unit = {},
    onClearStatusMessage: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderStroke)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(StatusYellowContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Account Under Review",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextSlate
            )
            Text(
                text = "اکاؤنٹ کی تصدیق جاری ہے",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepIndigo
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your profile, trade credentials, and CNIC are being verified by HomEase Admin. Verification normally completes in 24–48 hours.",
                fontSize = 14.sp,
                color = TextSlate,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Policy reassurance pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceVariantLight)
                    .padding(12.dp)
            ) {
                Text(
                    text = "🔒 Policy Guard: You will not receive live customer job requests until admin approval.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSlateMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Registered details summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BackgroundLight)
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.URDU) "رجسٹرڈ معلومات:" else "Registered Information:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepIndigo
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "• ${if (language == AppLanguage.URDU) "نام:" else "Name:"} ${provider.name}", fontSize = 13.sp, color = TextSlate)
                Text(text = "• ${if (language == AppLanguage.URDU) "فون:" else "Phone:"} ${provider.phone}", fontSize = 13.sp, color = TextSlate)
                Text(text = "• ${if (language == AppLanguage.URDU) "شعبہ:" else "Trade:"} ${provider.categoriesCsv.ifBlank { if (language == AppLanguage.URDU) "گھریلو خدمات" else "Household Services" }}", fontSize = 13.sp, color = TextSlate)
                Text(text = "• ${if (language == AppLanguage.URDU) "علاقہ:" else "Area:"} ${provider.cityArea} (${provider.serviceRadiusKm} km)", fontSize = 13.sp, color = TextSlate)
                if (provider.cnicNumber.isNotBlank()) {
                    Text(text = "• CNIC: ${provider.cnicNumber}", fontSize = 13.sp, color = TextSlate)
                }
            }

            if (isRefreshingStatus) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = DeepIndigo
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (language == AppLanguage.URDU) "سرور سے تصدیق کی جا رہی ہے..." else "Checking verification status with server...",
                        fontSize = 13.sp,
                        color = TextSlateMuted
                    )
                }
            }

            if (statusCheckMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                val isApprovedMessage = statusCheckMessage.contains("approved", ignoreCase = true)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isApprovedMessage) Color(0xFFF0FDF4) else Color(0xFFFEF3C7)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isApprovedMessage) Color(0xFF86EFAC) else Color(0xFFFDE68A)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isApprovedMessage) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isApprovedMessage) Color(0xFF16A34A) else Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusCheckMessage,
                            fontSize = 12.sp,
                            color = if (isApprovedMessage) Color(0xFF15803D) else Color(0xFF92400E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryCtaButton(
                text = if (isRefreshingStatus) {
                    if (language == AppLanguage.URDU) "چیک کیا جا رہا ہے..." else "Checking Server Status..."
                } else {
                    if (language == AppLanguage.URDU) "حیثیت دوبارہ چیک کریں" else "Check Verification Status"
                },
                onClick = {
                    if (!isRefreshingStatus) {
                        onClearStatusMessage()
                        onCheckVerificationStatus()
                    }
                },
                backgroundColor = DeepIndigo,
                isProviderStyle = true,
                testTag = "check_status_btn"
            )
        }
    }
}

/**
 * Noticeably simpler Job Ping Card designed for lower tech-fluency providers.
 * - Icon-first visual hierarchy
 * - Minimal text
 * - Large, prominent price offer
 * - Giant touch targets (54dp - 60dp height)
 */
@Composable
fun SimplifiedJobPingCard(
    job: ServiceRequestEntity,
    language: AppLanguage,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCounter: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_job_card_${job.id}")
            .liquidGlassCard(shape = RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Service Icon, Title, and Offer Price
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
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppleSystemBlue.copy(alpha = 0.12f))
                            .border(0.5.dp, AppleSystemBlue.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(job.categoryId),
                            contentDescription = null,
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = job.serviceTitle,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleLabelPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AppleSystemOrange,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${job.cityArea} • ~1.5 km",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppleLabelSecondary
                            )
                        }
                    }
                }

                // Big Customer Price Offer
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "OFFER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Rs ${job.budgetRs}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = AppleLabelPrimary
                    )
                }
            }

            if (job.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2F2F7).copy(alpha = 0.8f))
                        .border(0.5.dp, Color(0x10000000), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "📝 \"${job.description}\"",
                        fontSize = 13.sp,
                        color = TextSlate,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Primary Apple Green Accept Button
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("accept_job_btn_${job.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppleSystemGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${Strings.get("accept_job", language)} (Rs ${job.budgetRs})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Row of Counter Price and Decline Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Counter Price Button
                Button(
                    onClick = onCounter,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                        .testTag("counter_job_btn_${job.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleSystemOrangeContainer,
                        contentColor = AppleSystemOrangeDark
                    ),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, AppleSystemOrange.copy(alpha = 0.35f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Strings.get("counter_offer", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Decline / Reject Button
                Button(
                    onClick = onReject,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("reject_job_btn_${job.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2F2F7),
                        contentColor = AppleLabelSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x14000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Strings.get("decline_job", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Noticeably simpler Active Job Card with direct Call, Trip Tracking, & Complete buttons.
 */
@Composable
fun SimplifiedActiveJobCard(
    job: ServiceRequestEntity,
    language: AppLanguage,
    onStartTrip: () -> Unit = {},
    onArrived: () -> Unit = {},
    onStartWork: () -> Unit = {},
    onComplete: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    onStartCall: () -> Unit = {}
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_active_job_card")
            .liquidGlassCard(shape = RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Top Status & Agreed Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Trip Status Chip
                val (statusText, statusColor) = when (job.status.uppercase()) {
                    "ON_THE_WAY" -> Pair(
                        if (language == AppLanguage.URDU) "راستے میں ہے • لائیو ٹریکنگ" else "ON THE WAY • LIVE SHARING",
                        AppleSystemBlue
                    )
                    "ARRIVED" -> Pair(
                        if (language == AppLanguage.URDU) "پتہ پر پہنچ چکے ہیں" else "ARRIVED AT LOCATION",
                        AppleSystemIndigo
                    )
                    "IN_PROGRESS" -> Pair(
                        if (language == AppLanguage.URDU) "کام جاری ہے" else "WORK IN PROGRESS",
                        AppleSystemOrange
                    )
                    "AWAITING_CUSTOMER_CONFIRMATION" -> Pair(
                        if (language == AppLanguage.URDU) "کسٹمر کی تصدیق کا انتظار" else "AWAITING CONFIRMATION",
                        AppleLabelSecondary
                    )
                    else -> Pair(
                        if (language == AppLanguage.URDU) "روانگی کے لیے تیار" else "READY FOR DEPARTURE",
                        AppleSystemBlue
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(0.5.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                val agreedOrBudget = if (job.agreedPriceRs > 0) job.agreedPriceRs else job.budgetRs
                val formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(agreedOrBudget)
                Text(
                    text = "Rs $formattedPrice",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = AppleLabelPrimary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Text(
                    text = job.serviceTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleLabelPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${if (language == AppLanguage.URDU) "کسٹمر:" else "Customer:"} ${job.customerName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppleSystemBlue
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Unlocked Customer Address Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF2F2F7).copy(alpha = 0.8f))
                    .border(0.5.dp, Color(0x10000000), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AppleSystemBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "سروس کا پتہ:" else "SERVICE ADDRESS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleLabelSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = job.fullAddress.ifBlank { job.cityArea },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleLabelPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Direct Communication Buttons (Voice Call & Chat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartCall,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("provider_call_customer_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleSystemGreen)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == AppLanguage.URDU) "صوتی کال" else "Voice Call", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = onOpenChat,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("provider_chat_customer_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleSystemBlue)
                ) {
                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == AppLanguage.URDU) "چیٹ کریں" else "Job Chat", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary WhatsApp & Phone Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val phone = job.customerPhone.replace("+", "")
                        val url = "https://api.whatsapp.com/send?phone=$phone"
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366).copy(alpha = 0.10f),
                        contentColor = Color(0xFF1E7E34)
                    ),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF25D366).copy(alpha = 0.35f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val phone = job.customerPhone
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2F2F7),
                        contentColor = TextSlate
                    ),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x14000000)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(if (language == AppLanguage.URDU) "سیلولر ڈائلر" else "Cellular Dial", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================================================================
            // Contextual Trip Progression Action Button
            // ================================================================
            when (job.status.uppercase()) {
                "ACCEPTED" -> {
                    Button(
                        onClick = onStartTrip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("provider_on_the_way_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppleSystemBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "راستے میں ہیں (شروع کریں)" else "On the way",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                "ON_THE_WAY" -> {
                    Button(
                        onClick = onArrived,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("provider_arrived_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppleSystemIndigo,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "پتہ پر پہنچ گئے ہیں" else "Arrived",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                "ARRIVED" -> {
                    Button(
                        onClick = onStartWork,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("provider_start_work_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppleSystemTeal,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "کام شروع کریں" else "Start Work",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                "IN_PROGRESS" -> {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("provider_complete_job_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppleSystemGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.get("job_completed", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF2F2F7))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == AppLanguage.URDU) "کسٹمر کی تصدیق کا انتظار ہے..." else "Awaiting customer confirmation...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppleLabelSecondary
                        )
                    }
                }
            }
        }
    }
}
