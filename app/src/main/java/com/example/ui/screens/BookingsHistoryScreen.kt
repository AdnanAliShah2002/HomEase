package com.example.ui.screens

import java.text.NumberFormat
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ServiceRequestEntity
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.ui.components.JobDetailDialog
import com.example.ui.theme.BackgroundLight
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Customer Bookings & Job History View with Active / Past tabs
 */
@Composable
fun CustomerBookingsView(
    allRequests: List<ServiceRequestEntity>,
    language: AppLanguage,
    onRateJob: (ServiceRequestEntity) -> Unit,
    onViewLiveJob: (Long) -> Unit,
    onStartNewBooking: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedJobForDetails by remember { mutableStateOf<ServiceRequestEntity?>(null) }

    if (selectedJobForDetails != null) {
        JobDetailDialog(
            job = selectedJobForDetails!!,
            isProviderPerspective = false,
            language = language,
            onDismiss = { selectedJobForDetails = null }
        )
    }

    val activeRequests = allRequests.filter { it.status != "COMPLETED" && it.status != "CANCELLED" }
    val pastRequests = allRequests.filter { it.status == "COMPLETED" }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        // Tab Row: Active / Past
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = DeepIndigo,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = DeepIndigo,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "${Strings.get("tab_active", language)} (${activeRequests.size})",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.testTag("customer_tab_active")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "${Strings.get("tab_past", language)} (${pastRequests.size})",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.testTag("customer_tab_past")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            // ACTIVE TAB
            if (activeRequests.isEmpty()) {
                EmptyBookingsState(
                    title = Strings.get("no_active_bookings", language),
                    description = Strings.get("no_active_bookings_sub", language),
                    buttonText = Strings.get("request_service", language),
                    onAction = onStartNewBooking
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeRequests, key = { it.id }) { req ->
                        CustomerActiveJobCard(
                            job = req,
                            language = language,
                            onRate = { onRateJob(req) },
                            onViewDetails = { onViewLiveJob(req.id) }
                        )
                    }
                }
            }
        } else {
            // PAST TAB
            if (pastRequests.isEmpty()) {
                EmptyBookingsState(
                    title = if (language == AppLanguage.URDU) "کوئی سابقہ بکنگ نہیں ہے" else "No completed jobs yet",
                    description = Strings.get("no_past_bookings_sub", language),
                    buttonText = Strings.get("request_service", language),
                    onAction = onStartNewBooking
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pastRequests, key = { it.id }) { req ->
                        CustomerPastJobCard(
                            job = req,
                            language = language,
                            onClick = { selectedJobForDetails = req }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Provider Bookings & Job History View with Active / Past tabs
 */
@Composable
fun ProviderBookingsView(
    allJobs: List<ServiceRequestEntity>,
    language: AppLanguage,
    onOpenJobDetails: (ServiceRequestEntity) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedJobForDetails by remember { mutableStateOf<ServiceRequestEntity?>(null) }

    if (selectedJobForDetails != null) {
        JobDetailDialog(
            job = selectedJobForDetails!!,
            isProviderPerspective = true,
            language = language,
            onDismiss = { selectedJobForDetails = null }
        )
    }

    val activeJobs = allJobs.filter { it.status in listOf("ACCEPTED", "IN_PROGRESS", "AWAITING_CUSTOMER_CONFIRMATION") }
    val pastJobs = allJobs.filter { it.status == "COMPLETED" }
    val totalEarnings = pastJobs.sumOf { if (it.agreedPriceRs > 0) it.agreedPriceRs else it.budgetRs }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundLight)) {
        // Tab Row: Active / Past
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = DeepIndigo,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = DeepIndigo,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "${Strings.get("tab_active", language)} (${activeJobs.size})",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.testTag("provider_tab_active")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "${Strings.get("tab_completed", language)} (${pastJobs.size})",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.testTag("provider_tab_past")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            // ACTIVE TAB FOR PROVIDER
            if (activeJobs.isEmpty()) {
                EmptyBookingsState(
                    title = Strings.get("no_active_jobs_provider", language),
                    description = Strings.get("no_active_jobs_provider_sub", language),
                    buttonText = null,
                    onAction = {}
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeJobs, key = { it.id }) { job ->
                        ProviderActiveJobItemCard(
                            job = job,
                            language = language,
                            onClick = { selectedJobForDetails = job }
                        )
                    }
                }
            }
        } else {
            // PAST TAB FOR PROVIDER
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // "X jobs completed" counter badge + total earnings motivator
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("provider_completed_counter_badge"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderStroke)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(StatusGreenContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${pastJobs.size} ${Strings.get("jobs_completed_badge", language)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        color = TextSlate
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (language == AppLanguage.URDU) "کل کمائی" else "Total Earned",
                                    fontSize = 12.sp,
                                    color = TextSlateMuted
                                )
                                Text(
                                    text = "Rs $totalEarnings",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = StatusGreen
                                )
                            }
                        }
                    }
                }

                if (pastJobs.isEmpty()) {
                    item {
                        EmptyBookingsState(
                            title = if (language == AppLanguage.URDU) "ابھی تک کوئی کام مکمل نہیں ہوا" else "No completed jobs yet",
                            description = Strings.get("no_completed_jobs_provider_sub", language),
                            buttonText = null,
                            onAction = {}
                        )
                    }
                } else {
                    items(pastJobs, key = { it.id }) { job ->
                        ProviderPastJobCard(
                            job = job,
                            language = language,
                            onClick = { selectedJobForDetails = job }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Customer Active Job Card
 */
@Composable
fun CustomerActiveJobCard(
    job: ServiceRequestEntity,
    language: AppLanguage = AppLanguage.ENGLISH,
    onRate: () -> Unit,
    onViewDetails: () -> Unit
) {
    val isAwaitingRating = job.status == "AWAITING_CUSTOMER_CONFIRMATION"

    Card(
        modifier = Modifier.fillMaxWidth().testTag("customer_active_job_${job.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAwaitingRating) Color(0xFFEFF6FF) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isAwaitingRating) 2.dp else 1.dp,
            color = if (isAwaitingRating) DeepIndigo else BorderStroke
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status Tag & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (job.status) {
                                "AWAITING_CUSTOMER_CONFIRMATION" -> StatusYellowContainer
                                "ACCEPTED" -> StatusGreenContainer
                                else -> DeepIndigoContainer
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (job.status) {
                            "AWAITING_CUSTOMER_CONFIRMATION" -> Strings.get("needs_confirmation", language)
                            "ACCEPTED" -> if (language == AppLanguage.URDU) "کاریگر نے قبول کر لیا" else "Accepted by Provider"
                            "IN_PROGRESS" -> if (language == AppLanguage.URDU) "کام جاری ہے" else "In Progress"
                            else -> if (language == AppLanguage.URDU) "کاریگر تلاش کیے جا رہے ہیں" else "Searching Providers"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (job.status) {
                            "AWAITING_CUSTOMER_CONFIRMATION" -> Color(0xFFB45309)
                            "ACCEPTED" -> Color(0xFF065F46)
                            else -> DeepIndigo
                        }
                    )
                }

                val price = if (job.agreedPriceRs > 0) job.agreedPriceRs else job.budgetRs
                val formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(price)
                Text(
                    text = "Rs $formattedPrice",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = StatusGreen,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = job.serviceTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextSlate
            )

            if (!job.selectedProviderName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${if (language == AppLanguage.URDU) "کاریگر:" else "Provider:"} ${job.selectedProviderName}",
                    fontSize = 13.sp,
                    color = DeepIndigo,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isAwaitingRating) {
                // Prominent CTA to Confirm & Rate
                Button(
                    onClick = onRate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("rate_and_confirm_btn_${job.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftOrange)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.get("review_and_complete", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(Strings.get("view_progress", language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Customer Past Job Card
 */
@Composable
fun CustomerPastJobCard(
    job: ServiceRequestEntity,
    language: AppLanguage = AppLanguage.ENGLISH,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val dateString = dateFormat.format(Date(job.completedAt ?: job.createdAt))
    val price = if (job.agreedPriceRs > 0) job.agreedPriceRs else job.budgetRs

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("customer_past_job_${job.id}"),
        shape = RoundedCornerShape(16.dp),
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
                    text = job.serviceTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextSlate
                )
                Text(
                    text = "Rs $price",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = StatusGreen
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${if (language == AppLanguage.URDU) "کاریگر:" else "Provider:"} ${job.selectedProviderName ?: (if (language == AppLanguage.URDU) "تصدیق شدہ کاریگر" else "Verified Pro")}",
                fontSize = 13.sp,
                color = TextSlateMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = TextSlateMuted
                )

                // Rating Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (job.ratingGiven != null && job.ratingGiven > 0) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${job.ratingGiven}.0",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlate
                        )
                    } else {
                        Text(
                            text = if (language == AppLanguage.URDU) "کوئی ریٹنگ نہیں" else "No rating",
                            fontSize = 11.sp,
                            color = TextSlateMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Provider Active Job Item Card
 */
@Composable
fun ProviderActiveJobItemCard(
    job: ServiceRequestEntity,
    language: AppLanguage = AppLanguage.ENGLISH,
    onClick: () -> Unit
) {
    val isAwaitingRating = job.status == "AWAITING_CUSTOMER_CONFIRMATION"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("provider_active_job_item_${job.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAwaitingRating) Color(0xFFFFFBEB) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = if (isAwaitingRating) Color(0xFFF59E0B) else BorderStroke
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAwaitingRating) StatusYellowContainer else StatusGreenContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isAwaitingRating) {
                            if (language == AppLanguage.URDU) "مکمل — ریٹنگ کا انتظار" else "Completed — awaiting rating"
                        } else {
                            if (language == AppLanguage.URDU) "کام جاری ہے" else "Active In Progress"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAwaitingRating) Color(0xFF92400E) else Color(0xFF065F46)
                    )
                }

                val price = if (job.agreedPriceRs > 0) job.agreedPriceRs else job.budgetRs
                val formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(price)
                Text(
                    text = "Rs $formattedPrice",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = StatusGreen,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = job.serviceTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextSlate
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${if (language == AppLanguage.URDU) "کسٹمر:" else "Customer:"} ${job.customerName}",
                fontSize = 13.sp,
                color = DeepIndigo
            )
            Text(
                text = "📍 ${job.fullAddress.ifBlank { job.cityArea }}",
                fontSize = 12.sp,
                color = TextSlateMuted
            )
        }
    }
}

/**
 * Provider Past Job Card
 */
@Composable
fun ProviderPastJobCard(
    job: ServiceRequestEntity,
    language: AppLanguage = AppLanguage.ENGLISH,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val dateString = dateFormat.format(Date(job.completedAt ?: job.createdAt))
    val price = if (job.agreedPriceRs > 0) job.agreedPriceRs else job.budgetRs

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("provider_past_job_${job.id}"),
        shape = RoundedCornerShape(16.dp),
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
                    text = job.serviceTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextSlate
                )
                Text(
                    text = "Rs $price",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = StatusGreen
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${if (language == AppLanguage.URDU) "کسٹمر:" else "Customer:"} ${job.customerName}",
                fontSize = 13.sp,
                color = TextSlateMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = TextSlateMuted
                )

                // Rating received
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (job.ratingGiven != null && job.ratingGiven > 0) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${job.ratingGiven}.0",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlate
                        )
                    } else {
                        Text(
                            text = if (language == AppLanguage.URDU) "خودکار مکمل" else "Auto-completed",
                            fontSize = 11.sp,
                            color = TextSlateMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyBookingsState(
    title: String,
    description: String,
    buttonText: String? = null,
    onAction: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
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
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = DeepIndigo,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextSlate,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = TextSlateMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (buttonText != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
