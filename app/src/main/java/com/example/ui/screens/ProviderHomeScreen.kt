package com.example.ui.screens

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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@Composable
fun ProviderHomeScreen(
    provider: UserEntity,
    incomingJobs: List<ServiceRequestEntity>,
    activeJob: ServiceRequestEntity?,
    pastJobs: List<ServiceRequestEntity> = emptyList(),
    completedJobs: List<ServiceRequestEntity>,
    language: AppLanguage,
    onToggleRole: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleOnline: (Boolean) -> Unit,
    onToggleVerification: () -> Unit = {},
    onAcceptJob: (ServiceRequestEntity) -> Unit,
    onRejectJob: (ServiceRequestEntity) -> Unit,
    onCounterJob: (ServiceRequestEntity, Int, String?) -> Unit,
    onCompleteActiveJob: (Long) -> Unit,
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
    var isOnline by remember { mutableStateOf(provider.isOnline) }
    var counterTargetJob by remember { mutableStateOf<ServiceRequestEntity?>(null) }
    var counterPriceInput by remember { mutableStateOf("") }
    var counterNoteInput by remember { mutableStateOf("") }
    var showCompleteConfirmation by remember { mutableStateOf(false) }
    var currentNavTab by remember { mutableStateOf("jobs") }
    val isApproved = provider.status == "APPROVED"

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
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfessionalBottomNavItem(
                        icon = Icons.Default.Work,
                        label = "Jobs",
                        isActive = currentNavTab == "jobs",
                        onClick = { currentNavTab = "jobs" }
                    )
                    ProfessionalBottomNavItem(
                        icon = Icons.Default.Assignment,
                        label = "Bookings",
                        isActive = currentNavTab == "bookings",
                        onClick = { currentNavTab = "bookings" }
                    )
                    ProfessionalBottomNavItem(
                        icon = Icons.Default.Person,
                        label = "Profile",
                        isActive = currentNavTab == "profile",
                        onClick = { currentNavTab = "profile" }
                    )
                }
            }
        },
        containerColor = BackgroundLight
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
                        onToggleVerification = onToggleVerification,
                        onSaveProfile = onUpdateProfile,
                        onLogout = onLogout
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
            // High-contrast Availability Header (Simplified for Lower Tech-Fluency)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isOnline) StatusGreenContainer else Color(0xFFF1F5F9))
                            .border(
                                width = 2.dp,
                                color = if (isOnline) StatusGreen else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) StatusGreen else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (isOnline) Strings.get("online", language) else Strings.get("offline", language),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isOnline) Color(0xFF065F46) else TextSlate
                                )
                                Text(
                                    text = if (isOnline) Strings.get("ready_for_jobs", language) else Strings.get("switch_on_to_get_jobs", language),
                                    fontSize = 13.sp,
                                    color = TextSlateMuted
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
                                checkedTrackColor = StatusGreen
                            ),
                            modifier = Modifier.testTag("provider_online_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Verification Status Bar + Sandbox Simulator Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isApproved) StatusGreenContainer else StatusYellowContainer)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isApproved) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = if (isApproved) StatusGreen else Color(0xFFB45309),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isApproved) "Status: Approved & Verified" else "Status: Pending Review (No Live Jobs)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isApproved) Color(0xFF065F46) else Color(0xFF92400E)
                            )
                        }

                        // Simulation toggle to allow testing both Pending and Approved states
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isApproved) Color.White else DeepIndigo)
                                .clickable { onToggleVerification() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isApproved) "Simulate Pending" else "Simulate Approved",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isApproved) TextSlate else Color.White
                            )
                        }
                    }
                }
            }

            // Case A: PROVIDER IS UNAPPROVED / PENDING
            if (!isApproved) {
                item {
                    PendingProviderVerificationCard(
                        provider = provider,
                        language = language,
                        onSimulateApproval = onToggleVerification
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
                                onComplete = { showCompleteConfirmation = true }
                            )
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
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
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

/**
 * Clean, respectful Pending Verification screen for unapproved providers.
 * Strictly enforces that pending providers do NOT receive live job pings.
 */
@Composable
fun PendingProviderVerificationCard(
    provider: UserEntity,
    language: AppLanguage,
    onSimulateApproval: () -> Unit
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

            Spacer(modifier = Modifier.height(24.dp))

            // Fast simulation trigger for evaluating the approved state
            PrimaryCtaButton(
                text = "⚡ Simulate Admin Approval (Sandbox Mode)",
                onClick = onSimulateApproval,
                backgroundColor = DeepIndigo,
                isProviderStyle = true,
                testTag = "simulate_approval_btn"
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_job_card_${job.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderStroke)
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
                            .background(DeepIndigoContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(job.categoryId),
                            contentDescription = null,
                            tint = DeepIndigo,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = job.serviceTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlate
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SoftOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${job.cityArea} • ~1.5 km",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSlateMuted
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
                        color = TextSlateMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Rs ${job.budgetRs}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepIndigo
                    )
                }
            }

            if (job.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariantLight)
                        .border(1.dp, BorderStroke, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "📝 \"${job.description}\"",
                        fontSize = 14.sp,
                        color = TextSlate,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Massive Primary Green Accept Button (60dp height)
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("accept_job_btn_${job.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${Strings.get("accept_job", language)} (Rs ${job.budgetRs})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Row of Counter Price and Decline Buttons (52dp height)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Counter Price Button
                Button(
                    onClick = onCounter,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .testTag("counter_job_btn_${job.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftOrange,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Strings.get("counter_offer", language),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Decline / Reject Button
                Button(
                    onClick = onReject,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("reject_job_btn_${job.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceVariantLight,
                        contentColor = TextSlateMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
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
 * Noticeably simpler Active Job Card with direct Call & Complete buttons.
 */
@Composable
fun SimplifiedActiveJobCard(
    job: ServiceRequestEntity,
    language: AppLanguage,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_active_job_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        border = androidx.compose.foundation.BorderStroke(2.dp, DeepIndigo)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = job.serviceTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${if (language == AppLanguage.URDU) "کسٹمر:" else "Customer:"} ${job.customerName}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = DeepIndigo
                    )
                }
                val agreedOrBudget = if (job.agreedPriceRs > 0) job.agreedPriceRs else job.budgetRs
                Text(
                    text = "Rs $agreedOrBudget",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = StatusGreen
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Unlocked Customer Address Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "📍 ${if (language == AppLanguage.URDU) "سروس کا پتہ:" else "Service Address:"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlateMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.fullAddress.ifBlank { job.cityArea },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Direct Call & WhatsApp Contact Buttons (54dp height)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { /* Simulated Call */ },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == AppLanguage.URDU) "کال کریں" else "CALL", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { /* Simulated WhatsApp */ },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WHATSAPP", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mark as Completed Button (60dp height)
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("provider_complete_job_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Strings.get("job_completed", language),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
