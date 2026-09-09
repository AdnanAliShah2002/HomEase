package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.UserEntity
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.data.model.ServiceCatalog
import com.example.ui.components.AutoLocationFetcher
import com.example.ui.components.HomeaseLogoMark
import com.example.ui.components.PrimaryCtaButton
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.DeepIndigoContainer
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SoftOrangeContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusYellowContainer
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProviderRegistrationScreen(
    phoneNumber: String,
    language: AppLanguage,
    onComplete: (UserEntity) -> Unit
) {
    // Current multi-step state: 1 to 4
    var currentStep by remember { mutableIntStateOf(1) }
    var isSubmittedConfirmation by remember { mutableStateOf(false) }

    // --- Step 1: Personal Information ---
    var fullName by remember { mutableStateOf("") }
    var profilePhotoUri by remember { mutableStateOf<String?>(null) }
    var cnicNumber by remember { mutableStateOf("") }
    var cnicFrontUri by remember { mutableStateOf<String?>(null) }
    var cnicBackUri by remember { mutableStateOf<String?>(null) }
    var dateOfBirth by remember { mutableStateOf("") }
    var homeAddress by remember { mutableStateOf("") }
    var cityArea by remember { mutableStateOf("Lahore - Gulberg") }

    // --- Step 2: Professional Information ---
    val selectedCategories = remember { mutableStateListOf<String>() }
    var experienceYears by remember { mutableStateOf("") }
    var radiusKm by remember { mutableFloatStateOf(10f) }
    var shopName by remember { mutableStateOf("") }
    var businessPhotoUri by remember { mutableStateOf<String?>(null) }
    var bio by remember { mutableStateOf("") }

    // --- Step 3: Verification (Optional at this stage) ---
    var referenceName by remember { mutableStateOf("") }
    var referencePhone by remember { mutableStateOf("") }

    // --- Step 4: Payout Preference & Consent ---
    var payoutMethod by remember { mutableStateOf("JazzCash") } // Cash, JazzCash, EasyPaisa
    var payoutAccountNumber by remember { mutableStateOf("") }
    var consentAgreed by remember { mutableStateOf(false) }

    // Photo picker launchers
    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            profilePhotoUri = uri.toString()
        }
    }

    val cnicFrontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            cnicFrontUri = uri.toString()
        }
    }

    val cnicBackPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            cnicBackUri = uri.toString()
        }
    }

    val businessPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            businessPhotoUri = uri.toString()
        }
    }

    // Validation checks for steps
    val isStep1Valid = fullName.isNotBlank() &&
            cnicNumber.isNotBlank() &&
            dateOfBirth.isNotBlank() &&
            homeAddress.isNotBlank() &&
            cityArea.isNotBlank()

    val isStep2Valid = selectedCategories.isNotEmpty() &&
            experienceYears.isNotBlank() &&
            cityArea.isNotBlank()

    val canSubmit = isStep1Valid && isStep2Valid && consentAgreed

    val constructedUser = remember(
        phoneNumber, fullName, profilePhotoUri, cityArea, homeAddress,
        selectedCategories.toList(), experienceYears, radiusKm, cnicNumber,
        cnicFrontUri, cnicBackUri, dateOfBirth, shopName, businessPhotoUri,
        bio, referenceName, referencePhone, payoutMethod, payoutAccountNumber, consentAgreed
    ) {
        UserEntity(
            phone = phoneNumber,
            role = "PROVIDER",
            name = fullName.trim(),
            profilePhotoUri = profilePhotoUri,
            cityArea = cityArea.trim(),
            homeAddress = homeAddress.trim(),
            categoriesCsv = selectedCategories.joinToString(","),
            yearsExperience = "$experienceYears years",
            serviceRadiusKm = radiusKm.toInt(),
            cnicNumber = cnicNumber.trim(),
            cnicFrontUri = cnicFrontUri,
            cnicBackUri = cnicBackUri,
            dateOfBirth = dateOfBirth.trim(),
            shopName = shopName.trim(),
            businessPhotoUri = businessPhotoUri,
            bio = bio.trim(),
            referenceName = referenceName.trim(),
            referencePhone = referencePhone.trim(),
            payoutMethod = payoutMethod,
            payoutAccountNumber = if (payoutMethod == "Cash") "" else payoutAccountNumber.trim(),
            consentAgreed = consentAgreed,
            status = "PENDING",
            isOnline = false
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        if (isSubmittedConfirmation) {
            // ==========================================
            // CONFIRMATION SCREEN (Review pending 24h)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(StatusYellowContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Pending Review",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = Strings.get("pending_thanks_title", language),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlate,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = Strings.get("pending_thanks_desc", language),
                    fontSize = 15.sp,
                    color = TextSlateMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(StatusYellowContainer)
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD97706))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Status: ${Strings.get("pending_status_badge", language)}",
                            color = Color(0xFF92400E),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Review Policy Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderStroke))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = DeepIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.URDU) "تصدیق کا عمل جاری ہے" else "Verification in Progress",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepIndigo
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (language == AppLanguage.URDU)
                                "• شناختی کارڈ اور پیشہ ورانہ اسناد کی تصدیق کی جا رہی ہے۔\n• جانچ کے دوران لائیو آرڈرز عارضی طور پر موقوف رہیں گے۔\n• منظوری کے بعد آپ کو فوراً مطلع کیا جائے گا اور آپ آرڈرز وصول کر سکیں گے۔"
                            else
                                "• Manual check of CNIC and professional credentials is under review.\n• While pending, incoming live customer requests and job pings are paused.\n• Once approved, you will be notified and can immediately start accepting nearby jobs.",
                            fontSize = 13.sp,
                            color = TextSlate,
                            lineHeight = 19.sp
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = BorderStroke
                        )

                        Text(
                            text = "${if (language == AppLanguage.URDU) "درخواست گزار:" else "Applicant:"} ${constructedUser.name}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSlate
                        )
                        Text(
                            text = "${if (language == AppLanguage.URDU) "خدمات:" else "Services:"} ${selectedCategories.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }}",
                            fontSize = 13.sp,
                            color = TextSlateMuted
                        )
                        Text(
                            text = "${if (language == AppLanguage.URDU) "دائرہ کار:" else "Coverage Area:"} ${constructedUser.cityArea} (${constructedUser.serviceRadiusKm} km)",
                            fontSize = 13.sp,
                            color = TextSlateMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                PrimaryCtaButton(
                    text = Strings.get("explore_dashboard", language),
                    onClick = {
                        onComplete(constructedUser)
                    },
                    backgroundColor = DeepIndigo,
                    isProviderStyle = true,
                    testTag = "goto_provider_dashboard_btn"
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        } else {
            // ==========================================
            // MULTI-STEP FORM
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Top App Bar & Multi-Step Indicator
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HomeaseLogoMark(size = 32.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = Strings.get("provider_reg_title", language),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepIndigo
                                    )
                                    Text(
                                        text = "Step $currentStep of 4: " + when (currentStep) {
                                            1 -> Strings.get("step_personal", language)
                                            2 -> Strings.get("step_professional", language)
                                            3 -> Strings.get("step_verification", language)
                                            else -> Strings.get("step_payout", language)
                                        },
                                        fontSize = 12.sp,
                                        color = TextSlateMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Step Number Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DeepIndigoContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$currentStep / 4",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepIndigo
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { currentStep / 4f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SoftOrange,
                            trackColor = BorderStroke,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive step tabs row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StepChip(
                                stepNumber = 1,
                                title = "Personal",
                                isCurrent = currentStep == 1,
                                isCompleted = currentStep > 1,
                                onClick = { currentStep = 1 }
                            )
                            StepChip(
                                stepNumber = 2,
                                title = "Work",
                                isCurrent = currentStep == 2,
                                isCompleted = currentStep > 2,
                                onClick = { if (isStep1Valid) currentStep = 2 }
                            )
                            StepChip(
                                stepNumber = 3,
                                title = "Verify",
                                isCurrent = currentStep == 3,
                                isCompleted = currentStep > 3,
                                onClick = { if (isStep1Valid && isStep2Valid) currentStep = 3 }
                            )
                            StepChip(
                                stepNumber = 4,
                                title = "Payout",
                                isCurrent = currentStep == 4,
                                isCompleted = currentStep > 4,
                                onClick = { if (isStep1Valid && isStep2Valid) currentStep = 4 }
                            )
                        }
                    }
                }

                // Step Content Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    when (currentStep) {
                        1 -> {
                            // -------------------------------------------------------------
                            // STEP 1: PERSONAL INFORMATION
                            // -------------------------------------------------------------
                            SectionCard(
                                title = Strings.get("step_personal", language),
                                subtitle = "Basic personal details and CNIC documentation for verification"
                            ) {
                                // Profile Photo Upload (Required)
                                Text(
                                    text = Strings.get("profile_photo", language) + " *",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSlate
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(DeepIndigoContainer)
                                            .border(2.dp, if (profilePhotoUri != null) StatusGreen else DeepIndigo, CircleShape)
                                            .clickable {
                                                profilePhotoPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (profilePhotoUri != null) {
                                            AsyncImage(
                                                model = profilePhotoUri,
                                                contentDescription = "Profile Photo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.CameraAlt,
                                                    contentDescription = "Upload Profile Photo",
                                                    tint = DeepIndigo,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                                Text("Photo", fontSize = 10.sp, color = DeepIndigo)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        OutlinedButton(
                                            onClick = {
                                                profilePhotoPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("upload_profile_photo_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (profilePhotoUri != null) Icons.Default.Check else Icons.Default.PhotoCamera,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (profilePhotoUri != null) StatusGreen else DeepIndigo
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (profilePhotoUri != null) Strings.get("change_photo", language) else Strings.get("upload_photo", language),
                                                fontSize = 13.sp,
                                                color = DeepIndigo
                                            )
                                        }
                                        Text(
                                            text = if (profilePhotoUri != null) "✓ ${Strings.get("photo_selected", language)}" else "Clear face photo required",
                                            fontSize = 11.sp,
                                            color = if (profilePhotoUri != null) StatusGreen else TextSlateMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Full Name
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text(Strings.get("full_name", language) + " *") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DeepIndigo) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("provider_fullname_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepIndigo,
                                        unfocusedBorderColor = BorderStroke
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // CNIC Number
                                OutlinedTextField(
                                    value = cnicNumber,
                                    onValueChange = { cnicNumber = it },
                                    label = { Text(Strings.get("cnic_number", language) + " *") },
                                    placeholder = { Text(Strings.get("cnic_hint", language)) },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = DeepIndigo) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("provider_cnic_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepIndigo,
                                        unfocusedBorderColor = BorderStroke
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // CNIC Photos Row (Front & Back)
                                Text(
                                    text = "CNIC Document Photos *",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSlate
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // CNIC Front
                                    DocumentUploadCard(
                                        title = Strings.get("cnic_front_photo", language),
                                        uri = cnicFrontUri,
                                        testTag = "upload_cnic_front_btn",
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            cnicFrontPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    )

                                    // CNIC Back
                                    DocumentUploadCard(
                                        title = Strings.get("cnic_back_photo", language),
                                        uri = cnicBackUri,
                                        testTag = "upload_cnic_back_btn",
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            cnicBackPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Date of Birth
                                OutlinedTextField(
                                    value = dateOfBirth,
                                    onValueChange = { dateOfBirth = it },
                                    label = { Text(Strings.get("date_of_birth", language) + " *") },
                                    placeholder = { Text(Strings.get("date_of_birth_hint", language)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("provider_dob_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepIndigo,
                                        unfocusedBorderColor = BorderStroke
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Home Address
                                OutlinedTextField(
                                    value = homeAddress,
                                    onValueChange = { homeAddress = it },
                                    label = { Text(Strings.get("home_address", language) + " *") },
                                    placeholder = { Text(Strings.get("home_address_hint", language)) },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = DeepIndigo) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("provider_address_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepIndigo,
                                        unfocusedBorderColor = BorderStroke
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // City / Area + AutoLocation
                                AutoLocationFetcher(
                                    onLocationDetected = { loc ->
                                        cityArea = loc.cityArea
                                        if (loc.fullAddress.isNotBlank()) {
                                            homeAddress = loc.fullAddress
                                        }
                                    }
                                )
                            }
                        }

                        2 -> {
                            // -------------------------------------------------------------
                            // STEP 2: PROFESSIONAL INFORMATION
                            // -------------------------------------------------------------
                            SectionCard(
                                title = Strings.get("step_professional", language),
                                subtitle = "Define your trade categories, coverage area, and business experience"
                            ) {
                                // Service Categories Multi-select Chips
                                Text(
                                    text = Strings.get("service_categories", language) + " *",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSlate
                                )
                                Text(
                                    text = Strings.get("service_categories_sub", language),
                                    fontSize = 12.sp,
                                    color = TextSlateMuted
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ServiceCatalog.categories.forEach { cat ->
                                        val isSelected = selectedCategories.contains(cat.id)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                if (isSelected) {
                                                    if (selectedCategories.size > 1) {
                                                        selectedCategories.remove(cat.id)
                                                    }
                                                } else {
                                                    selectedCategories.add(cat.id)
                                                }
                                            },
                                            label = {
                                                val catTitle = Strings.get(cat.nameKey, language)
                                                Text(
                                                    text = "${cat.emoji} $catTitle",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            leadingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = DeepIndigo,
                                                selectedLabelColor = Color.White,
                                                selectedLeadingIconColor = Color.White,
                                                containerColor = Color.White,
                                                labelColor = TextSlate
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = if (isSelected) DeepIndigo else BorderStroke
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.testTag("cat_chip_${cat.id}")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Years of Experience
                                OutlinedTextField(
                                    value = experienceYears,
                                    onValueChange = { experienceYears = it },
                                    label = { Text(Strings.get("years_experience", language) + " *") },
                                    placeholder = { Text(Strings.get("years_hint", language)) },
                                    leadingIcon = { Icon(Icons.Default.Handyman, contentDescription = null, tint = DeepIndigo) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("provider_exp_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepIndigo,
                                        unfocusedBorderColor = BorderStroke
                                    )
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Service Area & Radius Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = Strings.get("service_radius", language),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSlate
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DeepIndigoContainer)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${radiusKm.toInt()} km",
                                            color = DeepIndigo,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Slider(
                                    value = radiusKm,
                                    onValueChange = { radiusKm = it },
                                    valueRange = 2f..40f,
                                    steps = 19,
                                    colors = SliderDefaults.colors(
                                        thumbColor = DeepIndigo,
                                        activeTrackColor = DeepIndigo,
                                        inactiveTrackColor = BorderStroke
                                    ),
                                    modifier = Modifier.testTag("service_radius_slider")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Business / Shop Name (Optional)
                                OutlinedTextField(
                                    value = shopName,
                                    onValueChange = { shopName = it },
                                    label = { Text(Strings.get("business_name", language) + " (Optional)") },
                                    placeholder = { Text(Strings.get("business_name_hint", language)) },
                                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = DeepIndigo) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("provider_shop_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepIndigo,
                                        unfocusedBorderColor = BorderStroke
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Business / Shop Photo Upload (Optional)
                                DocumentUploadCard(
                                    title = Strings.get("business_photo", language),
                                    uri = businessPhotoUri,
                                    testTag = "upload_business_photo_btn",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        businessPhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Short Bio (Optional Textarea)
                                OutlinedTextField(
                                    value = bio,
                                    onValueChange = { bio = it },
                                    label = { Text(Strings.get("short_bio", language)) },
                                    placeholder = { Text(Strings.get("bio_placeholder", language)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .testTag("provider_bio_input"),
                                    maxLines = 4,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepIndigo,
                                        unfocusedBorderColor = BorderStroke
                                    )
                                )
                            }
                        }

                        3 -> {
                            // -------------------------------------------------------------
                            // STEP 3: VERIFICATION (Optional at this stage)
                            // -------------------------------------------------------------
                            SectionCard(
                                title = Strings.get("verification_section_title", language),
                                subtitle = Strings.get("verification_section_sub", language)
                            ) {
                                // Notice Banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DeepIndigoContainer)
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = DeepIndigo,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Verification reference is optional right now. You can provide a known community reference or skip to the next step. Formal trade licenses and police character certificates can be upgraded later as optional trust badges.",
                                            fontSize = 12.sp,
                                            color = DeepIndigo,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Community / Trade Reference (Optional)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSlate
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Reference Name
                                OutlinedTextField(
                                    value = referenceName,
                                    onValueChange = { referenceName = it },
                                    label = { Text(Strings.get("reference_name", language)) },
                                    placeholder = { Text(Strings.get("reference_name_hint", language)) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = DeepIndigo) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("provider_ref_name_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepIndigo,
                                        unfocusedBorderColor = BorderStroke
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Reference Phone
                                OutlinedTextField(
                                    value = referencePhone,
                                    onValueChange = { referencePhone = it },
                                    label = { Text(Strings.get("reference_phone", language)) },
                                    placeholder = { Text(Strings.get("reference_phone_hint", language)) },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = DeepIndigo) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("provider_ref_phone_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DeepIndigo,
                                        unfocusedBorderColor = BorderStroke
                                    )
                                )
                            }
                        }

                        4 -> {
                            // -------------------------------------------------------------
                            // STEP 4: PAYOUT PREFERENCE & CONSENT
                            // -------------------------------------------------------------
                            SectionCard(
                                title = Strings.get("step_payout", language),
                                subtitle = "Configure earnings payout method and accept provider terms"
                            ) {
                                Text(
                                    text = Strings.get("payout_section_title", language),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSlate
                                )
                                Text(
                                    text = Strings.get("payout_section_sub", language),
                                    fontSize = 12.sp,
                                    color = TextSlateMuted
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Payout Method Radio Group: Cash / JazzCash / EasyPaisa
                                listOf("Cash", "JazzCash", "EasyPaisa").forEach { method ->
                                    val isSelected = payoutMethod == method
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) DeepIndigoContainer else Color.White)
                                            .border(1.dp, if (isSelected) DeepIndigo else BorderStroke, RoundedCornerShape(12.dp))
                                            .clickable { payoutMethod = method }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { payoutMethod = method },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = DeepIndigo,
                                                unselectedColor = TextSlateMuted
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = when (method) {
                                                    "Cash" -> Strings.get("payout_cash", language) + " (Direct in-hand)"
                                                    "JazzCash" -> Strings.get("payout_jazzcash", language) + " Mobile Wallet"
                                                    else -> Strings.get("payout_easypaisa", language) + " Mobile Wallet"
                                                },
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) DeepIndigo else TextSlate
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Account / Mobile Wallet field (ONLY shown if JazzCash or EasyPaisa is selected)
                                AnimatedVisibility(visible = payoutMethod != "Cash") {
                                    Column {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = payoutAccountNumber,
                                            onValueChange = { payoutAccountNumber = it },
                                            label = { Text(Strings.get("payout_account_number", language)) },
                                            placeholder = { Text(Strings.get("payout_account_hint", language)) },
                                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = DeepIndigo) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("provider_payout_account_input"),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DeepIndigo,
                                                unfocusedBorderColor = BorderStroke
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Consent & Terms Checkbox
                                Text(
                                    text = Strings.get("consent_title", language),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSlate
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (consentAgreed) SoftOrangeContainer else Color.White)
                                        .border(1.dp, if (consentAgreed) SoftOrange else BorderStroke, RoundedCornerShape(12.dp))
                                        .clickable { consentAgreed = !consentAgreed }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = consentAgreed,
                                        onCheckedChange = { consentAgreed = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = SoftOrange,
                                            checkmarkColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("provider_consent_checkbox")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Strings.get("consent_checkbox", language),
                                        fontSize = 13.sp,
                                        color = TextSlate,
                                        lineHeight = 18.sp
                                    )
                                }

                                if (!isStep1Valid || !isStep2Valid) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Please ensure all required fields in Step 1 and Step 2 are completed before submitting.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Bottom Navigation Action Bar
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep -= 1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("provider_step_back_btn"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextSlate,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Strings.get("back", language),
                                    fontSize = 15.sp,
                                    color = TextSlate,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (currentStep < 4) {
                            PrimaryCtaButton(
                                text = Strings.get("continue_btn", language),
                                onClick = {
                                    if (currentStep == 1 && isStep1Valid) {
                                        currentStep = 2
                                    } else if (currentStep == 2 && isStep2Valid) {
                                        currentStep = 3
                                    } else if (currentStep == 3) {
                                        currentStep = 4
                                    }
                                },
                                enabled = when (currentStep) {
                                    1 -> isStep1Valid
                                    2 -> isStep2Valid
                                    else -> true
                                },
                                backgroundColor = DeepIndigo,
                                modifier = Modifier.weight(1f),
                                testTag = "provider_step_next_btn"
                            )
                        } else {
                            PrimaryCtaButton(
                                text = Strings.get("submit_verification", language),
                                onClick = {
                                    if (canSubmit) {
                                        isSubmittedConfirmation = true
                                    }
                                },
                                enabled = canSubmit,
                                backgroundColor = SoftOrange,
                                modifier = Modifier.weight(1f),
                                testTag = "provider_submit_registration_btn"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepChip(
    stepNumber: Int,
    title: String,
    isCurrent: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isCurrent -> DeepIndigo
        isCompleted -> StatusGreen
        else -> BorderStroke.copy(alpha = 0.5f)
    }
    val contentColor = when {
        isCurrent || isCompleted -> Color.White
        else -> TextSlateMuted
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) DeepIndigoContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Text(
                    text = "$stepNumber",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) DeepIndigo else TextSlateMuted
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderStroke))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepIndigo
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSlateMuted,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun DocumentUploadCard(
    title: String,
    uri: String?,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = if (uri != null) DeepIndigoContainer.copy(alpha = 0.3f) else BackgroundLight
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (uri != null) StatusGreen else BorderStroke
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uri != null) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, StatusGreen, RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Attached",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = DeepIndigo,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSlate,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Tap to upload",
                    fontSize = 10.sp,
                    color = TextSlateMuted
                )
            }
        }
    }
}
