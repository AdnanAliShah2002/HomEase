package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.JobOfferEntity
import com.example.data.db.ServiceRequestEntity
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.data.model.ServiceCatalog
import com.example.data.remote.CategoryDetectionRemoteService
import com.example.data.remote.CategoryDetectionResult
import com.example.ui.components.AutoLocationFetcher
import com.example.ui.components.PrimaryCtaButton
import com.example.ui.components.getCategoryIcon
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerRequestFlowScreen(
    initialCategoryId: String?,
    customerPhone: String,
    customerName: String,
    savedAddress: String,
    cityArea: String,
    language: AppLanguage,
    activeLiveRequest: ServiceRequestEntity?,
    incomingOffers: List<JobOfferEntity>,
    onBack: () -> Unit,
    onSubmitRequest: (ServiceRequestEntity) -> Unit,
    onSelectOffer: (JobOfferEntity) -> Unit,
    onDoneViewingConfirmed: () -> Unit,
    onDetectCategory: (suspend (String) -> CategoryDetectionResult)? = null
) {
    // If there is already an active live request being viewed (either searching or accepted)
    if (activeLiveRequest != null) {
        if (activeLiveRequest.status == "ACCEPTED") {
            // Screen 10 Step 7: Job Confirmed
            JobConfirmedView(
                request = activeLiveRequest,
                language = language,
                onDone = onDoneViewingConfirmed
            )
        } else {
            // Screen 10 Step 6: Finding providers + InDrive responses list
            FindingProvidersView(
                request = activeLiveRequest,
                offers = incomingOffers,
                language = language,
                onBack = onBack,
                onSelectOffer = onSelectOffer
            )
        }
        return
    }

    // Step-by-step Request Creation
    val defaultDetector = remember { CategoryDetectionRemoteService() }
    val detectCategoryAction: suspend (String) -> CategoryDetectionResult = onDetectCategory ?: { defaultDetector.detectCategory(it) }
    val coroutineScope = rememberCoroutineScope()

    var aiProblemInput by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }
    var aiDetectionResult by remember { mutableStateOf<CategoryDetectionResult?>(null) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }

    var selectedCatId by remember {
        mutableStateOf(initialCategoryId ?: ServiceCatalog.categories.first().id)
    }
    val currentCat = ServiceCatalog.categories.firstOrNull { it.id == selectedCatId }
        ?: ServiceCatalog.categories.first()

    // Laundry specific state
    var laundryServiceType by remember { mutableStateOf("wash_and_iron") } // "iron_only", "wash_only", "wash_and_iron"
    var laundrySuitCount by remember { mutableIntStateOf(5) }
    var laundryItemsNote by remember { mutableStateOf("") }

    var selectedServiceDetail by remember {
        mutableStateOf(currentCat.popularServices.firstOrNull() ?: "General Service")
    }
    var problemDescription by remember {
        mutableStateOf("Water is dripping from under the sink connection. Needs quick check & fix.")
    }
    var activeCityArea by remember { mutableStateOf(cityArea) }
    var addressInput by remember {
        mutableStateOf(savedAddress.ifBlank { "House 42-B, Main Boulevard, Gulberg III, Lahore" })
    }

    // Dynamic reference pricing calculation
    val (refMin, refMax, refBasis) = if (selectedCatId == "dry_cleaning") {
        val (rMin, rMax) = when (laundryServiceType) {
            "iron_only" -> 40 to 60
            "wash_only" -> 60 to 90
            else -> 90 to 150
        }
        Triple(rMin * laundrySuitCount, rMax * laundrySuitCount, "$laundrySuitCount ${if (language == AppLanguage.URDU) "سوٹ" else "suits"}")
    } else {
        Triple(currentCat.priceMin, currentCat.priceMax, currentCat.priceBasis)
    }
    val suggestedMidpoint = (refMin + refMax) / 2

    var budgetInput by remember { mutableStateOf(suggestedMidpoint.toString()) }

    // Pre-fill suggested asking price when category or laundry selections change
    LaunchedEffect(selectedCatId, laundryServiceType, laundrySuitCount) {
        val newMidpoint = if (selectedCatId == "dry_cleaning") {
            val (rMin, rMax) = when (laundryServiceType) {
                "iron_only" -> 40 to 60
                "wash_only" -> 60 to 90
                else -> 90 to 150
            }
            ((rMin + rMax) / 2) * laundrySuitCount
        } else {
            (currentCat.priceMin + currentCat.priceMax) / 2
        }
        budgetInput = newMidpoint.toString()
    }

    Scaffold(
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("request_flow_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSlate
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings.get("request_service_cta", language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                }
            }
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // AI Smart Assistant: Describe Your Problem Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_assistant_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(DeepIndigoContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = DeepIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = Strings.get("ai_describe_title", language),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSlate
                            )
                            Text(
                                text = Strings.get("ai_describe_subtitle", language),
                                fontSize = 12.sp,
                                color = TextSlateMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = aiProblemInput,
                        onValueChange = {
                            aiProblemInput = it
                            aiErrorMessage = null
                        },
                        placeholder = {
                            Text(
                                text = Strings.get("ai_describe_placeholder", language),
                                fontSize = 13.sp,
                                color = TextSlateMuted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_problem_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepIndigo,
                            unfocusedBorderColor = BorderStroke,
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight
                        ),
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (aiProblemInput.isNotBlank() && !isAiLoading) {
                                    coroutineScope.launch {
                                        isAiLoading = true
                                        aiErrorMessage = null
                                        aiDetectionResult = null
                                        val res = detectCategoryAction(aiProblemInput)
                                        isAiLoading = false
                                        if (res.success && res.categoryId != null) {
                                            aiDetectionResult = res
                                        } else {
                                            aiErrorMessage = res.error
                                                ?: Strings.get("ai_unclassified_notice", language)
                                        }
                                    }
                                }
                            },
                            enabled = aiProblemInput.isNotBlank() && !isAiLoading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepIndigo,
                                disabledContainerColor = DeepIndigo.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("ai_suggest_service_btn")
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Strings.get("ai_analyzing", language),
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Strings.get("ai_suggest_service_button", language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Show AI Suggestion if available
                    if (aiDetectionResult != null) {
                        val result = aiDetectionResult!!
                        val isLowConfidence = result.confidence?.lowercase() == "low"
                        val framingPrefix = if (isLowConfidence) {
                            Strings.get("ai_might_be_prefix", language)
                        } else {
                            Strings.get("ai_looks_like_prefix", language)
                        }
                        val continuePrompt = Strings.get("ai_continue_prompt", language)

                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_suggestion_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLowConfidence) SoftOrangeContainer else DeepIndigoContainer.copy(alpha = 0.45f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isLowConfidence) SoftOrange.copy(alpha = 0.5f) else DeepIndigo.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = framingPrefix,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isLowConfidence) SoftOrange else DeepIndigo
                                    )
                                    val badgeKey = when (result.confidence?.lowercase()) {
                                        "high" -> "ai_confidence_high"
                                        "medium" -> "ai_confidence_medium"
                                        else -> "ai_confidence_low"
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isLowConfidence) SoftOrange.copy(alpha = 0.2f) else StatusGreenContainer
                                    ) {
                                        Text(
                                            text = Strings.get(badgeKey, language),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLowConfidence) SoftOrange else StatusGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "${result.category} → ${result.serviceNote}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSlate
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = continuePrompt,
                                    fontSize = 13.sp,
                                    color = TextSlateMuted
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Confirm Button (Pre-fills existing form)
                                    Button(
                                        onClick = {
                                            result.categoryId?.let { catId ->
                                                selectedCatId = catId
                                                selectedServiceDetail = result.serviceNote ?: ""
                                                problemDescription = aiProblemInput.trim()
                                                if (catId == "dry_cleaning") {
                                                    laundryItemsNote = aiProblemInput.trim()
                                                }
                                            }
                                            aiDetectionResult = null
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ai_confirm_suggestion_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = Strings.get("ai_confirm_button", language),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Fallback Button: "Not quite, let me choose myself"
                                    OutlinedButton(
                                        onClick = {
                                            aiDetectionResult = null
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSlate),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ai_dismiss_suggestion_btn")
                                    ) {
                                        Text(
                                            text = Strings.get("ai_choose_myself", language),
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Show Unclassified Notice if any
                    if (aiErrorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SoftOrangeContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = SoftOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = aiErrorMessage!!,
                                    fontSize = 12.sp,
                                    color = TextSlate,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { aiErrorMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("✕", fontSize = 12.sp, color = TextSlateMuted)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Select Category (Always available manual grid)
            Text(
                text = Strings.get("ai_or_manual_header", language),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DeepIndigo
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ServiceCatalog.categories.forEach { cat ->
                    val isSelected = cat.id == selectedCatId
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) DeepIndigo else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) DeepIndigo else BorderStroke,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedCatId = cat.id
                                selectedServiceDetail = cat.popularServices.firstOrNull() ?: ""
                                if (cat.id == "dry_cleaning") {
                                    problemDescription = ""
                                } else if (problemDescription.isBlank()) {
                                    problemDescription = "Need experienced technician to inspect and resolve issue."
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(cat.iconType),
                            contentDescription = null,
                            tint = if (isSelected) Color.White else DeepIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Strings.get(cat.nameKey, language),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSlate
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Specific Requirements: Laundry selector OR General Category Form
            Text(
                text = Strings.get("step_details", language),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DeepIndigo
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (selectedCatId == "dry_cleaning") {
                // Dedicated Laundry & Ironing Selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Strings.get("laundry_service_type", language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlate
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Service Types: Wash & Iron, Ironing Only, Wash Only
                        val serviceTypes = listOf(
                            Triple("wash_and_iron", Strings.get("laundry_wash_and_iron", language), "Rs 90–150/suit"),
                            Triple("iron_only", Strings.get("laundry_iron_only", language), "Rs 40–60/suit"),
                            Triple("wash_only", Strings.get("laundry_wash_only", language), "Rs 60–90/suit")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            serviceTypes.forEach { (typeId, label, rateText) ->
                                val isChosen = laundryServiceType == typeId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isChosen) DeepIndigoContainer else Color.White)
                                        .border(
                                            width = 1.dp,
                                            color = if (isChosen) DeepIndigo else BorderStroke,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { laundryServiceType = typeId }
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isChosen) DeepIndigo else TextSlate,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = rateText,
                                            fontSize = 10.sp,
                                            color = if (isChosen) DeepIndigo else TextSlateMuted,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Suit Count Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = Strings.get("laundry_suit_count", language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSlate
                                )
                                Text(
                                    text = if (language == AppLanguage.URDU) "شلوار قمیض / پینٹ شرٹ" else "Number of Shalwar Kameez / Pants & Shirts",
                                    fontSize = 11.sp,
                                    color = TextSlateMuted
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BackgroundLight)
                                    .border(1.dp, BorderStroke, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { if (laundrySuitCount > 1) laundrySuitCount -= 1 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepIndigo)
                                }
                                Text(
                                    text = "$laundrySuitCount",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = DeepIndigo,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                IconButton(
                                    onClick = { if (laundrySuitCount < 50) laundrySuitCount += 1 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepIndigo)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Optional Details
                        OutlinedTextField(
                            value = laundryItemsNote,
                            onValueChange = { laundryItemsNote = it },
                            placeholder = { Text(Strings.get("laundry_items_hint", language), color = TextSlateMuted, fontSize = 12.sp) },
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
                }
            } else {
                // Free-text and service chip selector for other categories
                Text(
                    text = Strings.get("service_detail_label", language),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSlateMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currentCat.popularServices.forEach { sName ->
                        val isChosen = sName == selectedServiceDetail
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isChosen) DeepIndigoContainer else Color.White)
                                .border(
                                    width = 1.dp,
                                    color = if (isChosen) DeepIndigo else BorderStroke,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedServiceDetail = sName }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = sName,
                                fontSize = 13.sp,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                color = if (isChosen) DeepIndigo else TextSlate
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = Strings.get("problem_desc_label", language),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSlateMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = problemDescription,
                    onValueChange = { problemDescription = it },
                    placeholder = { Text(Strings.get("problem_desc_hint", language), color = TextSlateMuted) },
                    maxLines = 3,
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextSlate, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth().testTag("problem_desc_input"),
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
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.get("step_location", language),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepIndigo
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Automatic Location Detection Banner
            AutoLocationFetcher(
                autoFetch = true,
                language = language,
                onLocationDetected = { loc ->
                    addressInput = loc.fullAddress
                    activeCityArea = loc.cityArea
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = addressInput,
                onValueChange = { addressInput = it },
                label = { Text(if (language == AppLanguage.URDU) "سروس کا مکمل پتہ" else "Delivery / Service Address") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = DeepIndigo)
                },
                textStyle = androidx.compose.ui.text.TextStyle(color = TextSlate, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth().testTag("request_address_input"),
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

            Spacer(modifier = Modifier.height(22.dp))

            // 4. Reference Price Range & Customer Asking Price (Unified Pricing Model)
            Text(
                text = Strings.get("customer_asking_price_label", language),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DeepIndigo
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Strings.get("customer_asking_price_desc", language),
                fontSize = 12.sp,
                color = TextSlateMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Reference Price Range Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Strings.get("reference_price_title", language),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSlateMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Rs $refMin – Rs $refMax",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = DeepIndigo
                            )
                            Text(
                                text = refBasis,
                                fontSize = 11.sp,
                                color = TextSlateMuted
                            )
                        }

                        // Reset to suggested button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DeepIndigoContainer,
                            modifier = Modifier.clickable { budgetInput = suggestedMidpoint.toString() }
                        ) {
                            Text(
                                text = "${Strings.get("suggested_price_chip", language)}: Rs $suggestedMidpoint",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepIndigo,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Estimate Only Warning (Plumbing, Electrical, Appliance Repair, Carpentry, Painting)
                    if (currentCat.isEstimateOnly) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoftOrangeContainer)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = SoftOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = Strings.get("estimate_only_badge", language),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftOrange
                                )
                                Text(
                                    text = Strings.get("estimate_only_explanation", language),
                                    fontSize = 11.sp,
                                    color = TextSlate
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Asking Price Input
            OutlinedTextField(
                value = budgetInput,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    if (digits.length <= 6) {
                        budgetInput = digits
                    }
                },
                prefix = { Text(if (language == AppLanguage.URDU) "روپے " else "Rs ", fontWeight = FontWeight.Bold, color = DeepIndigo, fontSize = 18.sp) },
                placeholder = { Text(if (language == AppLanguage.URDU) "اپنی پیشکش درج کریں" else "Enter asking price", color = TextSlateMuted) },
                trailingIcon = {
                    if (budgetInput.isNotEmpty()) {
                        IconButton(onClick = { budgetInput = "" }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Clear",
                                tint = TextSlateMuted,
                                modifier = Modifier.size(18.dp)
                            )
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
                modifier = Modifier.fillMaxWidth().testTag("request_budget_input"),
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

            Spacer(modifier = Modifier.height(8.dp))

            // Preset asking price shortcuts
            val presets = listOf(refMin, suggestedMidpoint, refMax, (refMax * 1.25).toInt()).distinct()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = budgetInput == preset.toString()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DeepIndigoContainer else Color.White)
                            .border(1.dp, if (isSelected) DeepIndigo else BorderStroke, RoundedCornerShape(8.dp))
                            .clickable { budgetInput = preset.toString() }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Rs $preset",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) DeepIndigo else TextSlate
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Mathematical increment chips: +200, +500, +1000
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(200, 500, 1000).forEach { inc ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftOrangeContainer)
                            .clickable {
                                val currentVal = budgetInput.toIntOrNull() ?: suggestedMidpoint
                                budgetInput = (currentVal + inc).toString()
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Rs $inc",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Submit Button
            PrimaryCtaButton(
                text = Strings.get("submit_request", language),
                onClick = {
                    val parsed = budgetInput.toIntOrNull() ?: suggestedMidpoint
                    val budget = parsed.coerceIn(100, 100000)

                    val finalDescription = if (selectedCatId == "dry_cleaning") {
                        val typeLabel = when (laundryServiceType) {
                            "iron_only" -> Strings.get("laundry_iron_only", language)
                            "wash_only" -> Strings.get("laundry_wash_only", language)
                            else -> Strings.get("laundry_wash_and_iron", language)
                        }
                        "$laundrySuitCount suits ($typeLabel)${if (laundryItemsNote.isNotBlank()) " - Details: $laundryItemsNote" else ""}"
                    } else {
                        problemDescription
                    }

                    val finalServiceTitle = if (selectedCatId == "dry_cleaning") {
                        val typeLabel = when (laundryServiceType) {
                            "iron_only" -> Strings.get("laundry_iron_only", language)
                            "wash_only" -> Strings.get("laundry_wash_only", language)
                            else -> Strings.get("laundry_wash_and_iron", language)
                        }
                        "Laundry ($laundrySuitCount suits $typeLabel)"
                    } else {
                        selectedServiceDetail.ifBlank { Strings.get(currentCat.nameKey, language) }
                    }

                    val req = ServiceRequestEntity(
                        customerPhone = customerPhone,
                        customerName = customerName,
                        categoryId = selectedCatId,
                        categoryTitle = Strings.get(currentCat.nameKey, language),
                        serviceTitle = finalServiceTitle,
                        description = finalDescription,
                        cityArea = activeCityArea,
                        fullAddress = addressInput,
                        budgetRs = budget,
                        customerAskingPrice = budget,
                        status = "SEARCHING"
                    )
                    onSubmitRequest(req)
                },
                enabled = addressInput.isNotBlank() && budgetInput.isNotBlank(),
                testTag = "submit_service_request_btn"
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun FindingProvidersView(
    request: ServiceRequestEntity,
    offers: List<JobOfferEntity>,
    language: AppLanguage,
    onBack: () -> Unit,
    onSelectOffer: (JobOfferEntity) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar"
    )

    Scaffold(
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextSlate)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = request.serviceTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                }
            }
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Radar Animation & Search Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(DeepIndigoContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = "Searching",
                                tint = DeepIndigo,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = Strings.get("finding_providers_title", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSlate
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Strings.get("finding_providers_desc", language),
                            fontSize = 13.sp,
                            color = TextSlateMuted,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoftOrangeContainer)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${if (language == AppLanguage.URDU) "آپ کی پیشکش: روپے" else "Your Offer: Rs"} ${request.budgetRs} • 📍 ${request.cityArea}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftOrange
                            )
                        }
                    }
                }
            }

            // InDrive-style Responses Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.get("incoming_offers", language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                    val responseCountText = if (offers.size == 1) "1 response" else "${offers.size} responses"
                    Text(
                        text = responseCountText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepIndigo
                    )
                }
            }

            // List of Provider Responses
            if (offers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Text(
                            text = "Notifying nearby plumbers, electricians & technicians... Responses appear in seconds!",
                            color = TextSlateMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(offers) { offer ->
                    ProviderOfferCard(
                        offer = offer,
                        customerBudget = request.budgetRs,
                        language = language,
                        onSelect = { onSelectOffer(offer) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProviderOfferCard(
    offer: JobOfferEntity,
    customerBudget: Int,
    language: AppLanguage,
    onSelect: () -> Unit
) {
    val isCounter = offer.counterPriceRs != customerBudget
    val displayPrice = if (offer.offerPriceRs > 0) offer.offerPriceRs else offer.counterPriceRs

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_offer_card_${offer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isCounter) SoftOrangeContainer else DeepIndigoContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = offer.providerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = SoftOrange, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${offer.providerRating}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSlate)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "• ${offer.distanceKm} ${Strings.get("km_away", language)}", fontSize = 13.sp, color = TextSlateMuted)
                    }
                }

                // Price Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isCounter) (if (language == AppLanguage.URDU) "کاریگر کی پیشکش" else "Counter Price") else (if (language == AppLanguage.URDU) "منظور شدہ بجٹ" else "Accepted At Asking Price"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCounter) SoftOrange else StatusGreen
                    )
                    Text(
                        text = "${if (language == AppLanguage.URDU) "روپے" else "Rs"} $displayPrice",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isCounter) SoftOrange else DeepIndigo
                    )
                }
            }

            // Note (e.g., "Rs 300 to inspect and quote" or "Accepted at your asking price")
            if (!offer.offerNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariantLight)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = DeepIndigo,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = offer.offerNote,
                        fontSize = 12.sp,
                        color = TextSlate,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            PrimaryCtaButton(
                text = "${Strings.get("select_provider", language)} (Rs $displayPrice)",
                onClick = onSelect,
                backgroundColor = if (isCounter) SoftOrange else DeepIndigo,
                testTag = "accept_offer_btn_${offer.id}"
            )
        }
    }
}

@Composable
fun JobConfirmedView(
    request: ServiceRequestEntity,
    language: AppLanguage,
    onDone: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(StatusGreenContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = StatusGreen,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = Strings.get("job_confirmed_title", language),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextSlate
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = Strings.get("job_confirmed_desc", language),
                fontSize = 14.sp,
                color = TextSlateMuted,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Provider Contact Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (language == AppLanguage.URDU) "منتخب شدہ سروس کاریگر" else "Assigned Service Provider",
                        fontSize = 12.sp,
                        color = TextSlateMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = request.selectedProviderName ?: (if (language == AppLanguage.URDU) "محمد راشد (تصدیق شدہ کاریگر)" else "Muhammad Rashid (Verified Pro)"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (language == AppLanguage.URDU) "طے شدہ قیمت: روپے" else "Agreed Price: Rs"} ${if (request.agreedPriceRs > 0) request.agreedPriceRs else request.budgetRs}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contact action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { /* Simulated Call */ },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Strings.get("call_provider", language), fontSize = 13.sp)
                        }

                        Button(
                            onClick = { /* Simulated WhatsApp */ },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Strings.get("whatsapp_provider", language), fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            PrimaryCtaButton(
                text = if (language == AppLanguage.URDU) "ہوم اسکرین پر واپس جائیں" else "Back to Home",
                onClick = onDone,
                backgroundColor = DeepIndigo,
                testTag = "job_confirmed_done_btn"
            )
        }
    }
}
