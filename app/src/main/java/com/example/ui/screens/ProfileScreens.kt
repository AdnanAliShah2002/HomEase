package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.db.UserEntity
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.data.model.ServiceCatalog
import com.example.ui.components.AutoLocationFetcher
import com.example.ui.components.LocationPickerInput
import com.example.ui.components.HelpSupportDialog
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

/**
 * Customer Profile Screen
 */
@Composable
fun CustomerProfileView(
    user: UserEntity,
    language: AppLanguage,
    onToggleRole: () -> Unit,
    onToggleLanguage: () -> Unit,
    onSaveProfile: (name: String, cityArea: String, savedAddresses: String) -> Unit,
    onLogout: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(user.name) }
    var cityArea by remember { mutableStateOf(user.cityArea) }
    var addressesList by remember {
        mutableStateOf<MutableList<String>>(
            if (user.savedAddressesCsv.isNotBlank()) {
                user.savedAddressesCsv.split("|").filter { it.isNotBlank() }.toMutableList()
            } else {
                mutableListOf<String>()
            }
        )
    }

    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSaveToast by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        HelpSupportDialog(
            roleLabel = "Customer",
            onDismiss = { showHelpDialog = false }
        )
    }

    if (showAddAddressDialog) {
        AddAddressDialog(
            onAdd = { label, addr ->
                val newEntry = "$label: $addr"
                addressesList.add(newEntry)
                val combined = addressesList.joinToString("|")
                onSaveProfile(name, cityArea, combined)
                showAddAddressDialog = false
            },
            onDismiss = { showAddAddressDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("customer_profile_view")
    ) {
        // Top Header Card with Avatar & Name - centered, polished layout
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered Avatar with Camera Badge
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .padding(bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(DeepIndigoContainer)
                            .border(2.5.dp, Color(0xFFFBDAD3), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user.profilePhotoUri.isNullOrBlank() && user.profilePhotoUri.length <= 4) {
                            Text(
                                text = user.profilePhotoUri,
                                fontSize = 40.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = DeepIndigo,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(DeepIndigo)
                            .border(2.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isEditingName) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Your Name") },
                            modifier = Modifier.weight(1f).testTag("customer_name_edit_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                isEditingName = false
                                onSaveProfile(name, cityArea, addressesList.joinToString("|"))
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = StatusGreen)
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextSlate
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { isEditingName = true },
                            modifier = Modifier.size(26.dp).testTag("edit_customer_name_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit name", tint = DeepIndigo, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Phone with verified badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = user.phone,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSlateMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusGreenContainer)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Verified",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // City & Area Section with Auto-Detection and Manual Override
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                        text = Strings.get("city_and_area", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlateMuted
                    )
                    Text(
                        text = if (language == AppLanguage.URDU) "جی پی ایس خودکار" else "GPS Auto-detect",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = DeepIndigo
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LocationPickerInput(
                    label = Strings.get("city_area", language),
                    addressValue = cityArea,
                    cityAreaValue = cityArea,
                    latitude = user.lat,
                    longitude = user.lng,
                    language = language,
                    testTagPrefix = "customer_profile_city",
                    isRequired = false,
                    showGpsShortcut = true,
                    onLocationSelected = { address, city, _, _ ->
                        cityArea = if (city.isNotBlank()) city else address
                        onSaveProfile(name, cityArea, addressesList.joinToString("|"))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Saved Addresses Section
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                        text = Strings.get("saved_addresses", language),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                    IconButton(
                        onClick = { showAddAddressDialog = true },
                        modifier = Modifier.size(28.dp).testTag("add_address_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add address", tint = DeepIndigo)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (addressesList.isEmpty()) {
                    Text("No saved addresses yet.", fontSize = 13.sp, color = TextSlateMuted)
                } else {
                    addressesList.forEachIndexed { index, addr ->
                        val parts = addr.split(":", limit = 2)
                        val tag = if (parts.size > 1) parts[0].trim() else "Address"
                        val fullAddr = if (parts.size > 1) parts[1].trim() else addr

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceVariantLight)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tag,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = DeepIndigo
                                )
                                Text(
                                    text = fullAddr,
                                    fontSize = 13.sp,
                                    color = TextSlate
                                )
                            }
                            IconButton(
                                onClick = {
                                    addressesList.removeAt(index)
                                    onSaveProfile(name, cityArea, addressesList.joinToString("|"))
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete address",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings & Actions Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Switch Language
                ProfileMenuRow(
                    icon = Icons.Default.Language,
                    title = Strings.get("app_language", language),
                    subtitle = if (language == AppLanguage.ENGLISH) "English" else "اردو",
                    onClick = onToggleLanguage
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Switch Role
                ProfileMenuRow(
                    icon = Icons.Default.SwapHoriz,
                    title = Strings.get("role_provider_title", language),
                    subtitle = Strings.get("switch_to_provider_sub", language),
                    onClick = onToggleRole
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Help & Support
                ProfileMenuRow(
                    icon = Icons.Default.HelpOutline,
                    title = Strings.get("help_and_support", language),
                    subtitle = if (language == AppLanguage.URDU) "ہیلپ لائن، عمومی سوالات اور واٹس ایپ" else "Helpline, FAQs & WhatsApp assistance",
                    onClick = { showHelpDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout Button
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("customer_logout_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(Strings.get("logout_btn", language), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Provider Profile Screen
 */
@Composable
fun ProviderProfileView(
    user: UserEntity,
    language: AppLanguage,
    onToggleRole: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleVerification: () -> Unit,
    onSaveProfile: (
        name: String,
        cityArea: String,
        categoriesCsv: String,
        yearsExperience: String,
        serviceRadiusKm: Int,
        bio: String,
        shopName: String,
        payoutMethod: String,
        payoutAccountNumber: String
    ) -> Unit,
    onLogout: () -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var cityArea by remember { mutableStateOf(user.cityArea) }
    var yearsExperience by remember { mutableStateOf(user.yearsExperience) }
    var serviceRadiusKm by remember { mutableFloatStateOf(user.serviceRadiusKm.toFloat().coerceIn(2f, 30f)) }
    var bio by remember { mutableStateOf(user.bio) }
    var shopName by remember { mutableStateOf(user.shopName) }
    var payoutMethod by remember { mutableStateOf(user.payoutMethod) }
    var payoutAccountNumber by remember { mutableStateOf(user.payoutAccountNumber) }

    val allCategoryOptions = ServiceCatalog.categories.map {
        it.id to Strings.get(it.nameKey, language)
    }

    var selectedCategories by remember {
        mutableStateOf(
            user.categoriesCsv.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        )
    }

    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        HelpSupportDialog(
            roleLabel = "Service Provider",
            onDismiss = { showHelpDialog = false }
        )
    }

    val isApproved = user.status == "APPROVED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("provider_profile_view")
    ) {
        // Provider Top Header with Avatar, Name, and Prominent Rating / Completed Jobs
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered Avatar with Camera Badge
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .padding(bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(DeepIndigoContainer)
                            .border(2.5.dp, Color(0xFFFBDAD3), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user.profilePhotoUri.isNullOrBlank() && user.profilePhotoUri.length <= 4) {
                            Text(
                                text = user.profilePhotoUri,
                                fontSize = 40.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = DeepIndigo,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(DeepIndigo)
                            .border(2.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextSlate
                )

                if (shopName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = shopName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DeepIndigo
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Phone & Verified badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = user.phone,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSlateMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusGreenContainer)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verified", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PROMINENT RATING & COMPLETED JOBS BANNER
                // "Provider profiles should display a running average rating and total completed job count, recalculated automatically whenever a new rating comes in."
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFFBEB))
                        .border(1.5.dp, Color(0xFFFDE68A), RoundedCornerShape(16.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                        .testTag("provider_rating_stats_card"),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rating column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", user.avgRating),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = TextSlate
                            )
                        }
                        Text(
                            text = Strings.get("average_rating", language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(Color(0xFFFCD34D))
                    )

                    // Total Jobs column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${user.totalJobs}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = StatusGreen
                        )
                        Text(
                            text = Strings.get("jobs_completed", language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verification Status Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isApproved) StatusGreenContainer else StatusYellowContainer)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isApproved) Icons.Default.Verified else Icons.Default.Work,
                            contentDescription = null,
                            tint = if (isApproved) StatusGreen else Color(0xFFB45309),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isApproved) Strings.get("status_verified_approved", language) else Strings.get("status_pending_review", language),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isApproved) Color(0xFF065F46) else Color(0xFF92400E)
                        )
                    }
                    Text(
                        text = "Toggle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepIndigo,
                        modifier = Modifier
                            .clickable { onToggleVerification() }
                            .padding(4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Professional Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Strings.get("professional_details", language),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlate
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Service Categories Chips
                Text(
                    text = Strings.get("service_categories", language),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlateMuted
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allCategoryOptions.chunked(2).forEach { rowPair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPair.forEach { (catId, catLabel) ->
                                val isSelected = selectedCategories.contains(catId)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategories = if (isSelected) {
                                            if (selectedCategories.size > 1) selectedCategories - catId else selectedCategories
                                        } else {
                                            selectedCategories + catId
                                        }
                                        onSaveProfile(
                                            name,
                                            cityArea,
                                            selectedCategories.joinToString(","),
                                            yearsExperience,
                                            serviceRadiusKm.toInt(),
                                            bio,
                                            shopName,
                                            payoutMethod,
                                            payoutAccountNumber
                                        )
                                    },
                                    label = { Text(catLabel, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DeepIndigo,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            if (rowPair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Service Base Location Input
                LocationPickerInput(
                    label = Strings.get("service_area", language),
                    addressValue = user.homeAddress,
                    cityAreaValue = cityArea,
                    latitude = user.lat,
                    longitude = user.lng,
                    language = language,
                    testTagPrefix = "provider_profile_service_area",
                    isRequired = true,
                    showGpsShortcut = true,
                    onLocationSelected = { address, city, _, _ ->
                        cityArea = if (city.isNotBlank()) city else address
                        onSaveProfile(
                            name,
                            cityArea,
                            selectedCategories.joinToString(","),
                            yearsExperience,
                            serviceRadiusKm.toInt(),
                            bio,
                            shopName,
                            payoutMethod,
                            payoutAccountNumber
                        )
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Service Radius Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Strings.get("service_radius", language),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlateMuted
                    )
                    Text(
                        text = "${serviceRadiusKm.toInt()} km",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepIndigo
                    )
                }
                Slider(
                    value = serviceRadiusKm,
                    onValueChange = { serviceRadiusKm = it },
                    onValueChangeFinished = {
                        onSaveProfile(
                            name,
                            cityArea,
                            selectedCategories.joinToString(","),
                            yearsExperience,
                            serviceRadiusKm.toInt(),
                            bio,
                            shopName,
                            payoutMethod,
                            payoutAccountNumber
                        )
                    },
                    valueRange = 2f..30f,
                    colors = SliderDefaults.colors(
                        thumbColor = DeepIndigo,
                        activeTrackColor = DeepIndigo
                    ),
                    modifier = Modifier.testTag("provider_radius_slider")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Experience & Shop
                OutlinedTextField(
                    value = yearsExperience,
                    onValueChange = { yearsExperience = it },
                    label = { Text("Years of Experience") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Business / Shop Name (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio (Tell customers about your work)") },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payout Method Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Strings.get("payout_method", language),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlate
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val payoutOptions = listOf(
                        Triple("Cash", "💵", Color(0xFF10B981)),
                        Triple("JazzCash", "JC", Color(0xFFD32F2F)),
                        Triple("EasyPaisa", "EP", Color(0xFF00A950))
                    )
                    payoutOptions.forEach { (method, badge, badgeBg) ->
                        val isSelected = payoutMethod.equals(method, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) DeepIndigo.copy(alpha = 0.08f) else Color(0xFFF8FAFC))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) DeepIndigo else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    payoutMethod = method
                                    onSaveProfile(
                                        name,
                                        cityArea,
                                        selectedCategories.joinToString(","),
                                        yearsExperience,
                                        serviceRadiusKm.toInt(),
                                        bio,
                                        shopName,
                                        payoutMethod,
                                        payoutAccountNumber
                                    )
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(badgeBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = badge,
                                        fontSize = if (badge.length > 1) 9.sp else 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = method,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) DeepIndigo else TextSlate,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (payoutMethod != "Cash") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = payoutAccountNumber,
                        onValueChange = { payoutAccountNumber = it },
                        label = { Text("$payoutMethod Account / Mobile Number") },
                        placeholder = { Text("03XXXXXXXXX") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Changes Button
        Button(
            onClick = {
                onSaveProfile(
                    name,
                    cityArea,
                    selectedCategories.joinToString(","),
                    yearsExperience,
                    serviceRadiusKm.toInt(),
                    bio,
                    shopName,
                    payoutMethod,
                    payoutAccountNumber
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_provider_profile_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
        ) {
            Text(Strings.get("save_profile_changes", language).uppercase(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings & Navigation Rows
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                ProfileMenuRow(
                    icon = Icons.Default.Language,
                    title = Strings.get("app_language", language),
                    subtitle = if (language == AppLanguage.ENGLISH) "English" else "اردو",
                    onClick = onToggleLanguage
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                ProfileMenuRow(
                    icon = Icons.Default.SwapHoriz,
                    title = Strings.get("role_customer_title", language),
                    subtitle = Strings.get("switch_to_customer_sub", language),
                    onClick = onToggleRole
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                ProfileMenuRow(
                    icon = Icons.Default.HelpOutline,
                    title = Strings.get("help_and_support", language),
                    subtitle = if (language == AppLanguage.URDU) "ہیلپ لائن، تنازعات اور واٹس ایپ" else "Helpline, Dispute Support & WhatsApp",
                    onClick = { showHelpDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout Button
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("provider_logout_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(Strings.get("logout_btn", language), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ProfileMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = DeepIndigo, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextSlate
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSlateMuted
            )
        }
    }
}

@Composable
fun AddAddressDialog(
    onAdd: (label: String, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("Home") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Saved Address", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column {
                Text("Label (e.g., Home, Office, Mother's House)", fontSize = 12.sp, color = TextSlateMuted)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LocationPickerInput(
                    label = "Full Street Address & Area",
                    addressValue = address,
                    testTagPrefix = "add_saved_address",
                    isRequired = true,
                    showGpsShortcut = true,
                    onLocationSelected = { selectedAddr, _, _, _ ->
                        address = selectedAddr
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (address.isNotBlank()) {
                        onAdd(label.ifBlank { "Address" }, address.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
            ) {
                Text("Save Address")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSlateMuted)
            }
        }
    )
}
