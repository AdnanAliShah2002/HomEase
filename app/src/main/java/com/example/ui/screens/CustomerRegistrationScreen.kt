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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.db.UserEntity
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.ui.components.AutoLocationFetcher
import com.example.ui.components.LocationPickerInput
import com.example.ui.components.HomeaseLogoMark
import com.example.ui.components.PrimaryCtaButton
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.DeepIndigoContainer
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted
import com.example.util.LocationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRegistrationScreen(
    phoneNumber: String,
    language: AppLanguage,
    onComplete: (UserEntity) -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var selectedAvatarIndex by remember { mutableStateOf(0) }
    var cityArea by remember { mutableStateOf("") }
    var homeAddress by remember { mutableStateOf("") }
    var customerLat by remember { mutableStateOf<Double?>(null) }
    var customerLng by remember { mutableStateOf<Double?>(null) }
    var notificationPreferenceEnabled by remember { mutableStateOf(true) }

    // Auto-detect device GPS location dynamically on screen launch
    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context) && LocationHelper.isLocationEnabled(context)) {
            val loc = LocationHelper.getCurrentLocation(context)
            if (loc != null) {
                homeAddress = loc.fullAddress
                cityArea = loc.cityArea
                customerLat = loc.latitude
                customerLng = loc.longitude
            }
        }
    }

    val avatarList = listOf("👨‍💼", "👩‍💼", "🧔", "🧕", "🧑‍🦱")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            HomeaseLogoMark(size = 54.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = Strings.get("customer_reg_title", language),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextSlate
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${if (language == AppLanguage.URDU) "فون:" else "Phone:"} $phoneNumber",
                fontSize = 13.sp,
                color = DeepIndigo,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Photo (Optional avatar selector)
            Text(
                text = Strings.get("profile_photo", language) + " (${Strings.get("optional", language)})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSlate,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                avatarList.forEachIndexed { index, emoji ->
                    val isSelected = selectedAvatarIndex == index
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) DeepIndigoContainer else Color.White)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) DeepIndigo else BorderStroke,
                                shape = CircleShape
                            )
                            .clickable { selectedAvatarIndex = index }
                            .testTag("avatar_option_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Full Name Input
            Text(
                text = Strings.get("full_name", language) + " *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSlate,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = { Text(if (language == AppLanguage.URDU) "مثلاً محمد علی" else "e.g. Muhammad Ali", color = TextSlateMuted) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextSlate, fontSize = 15.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_name_input"),
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

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Location Input (InDrive / Uber style search autocomplete & center-pin map picker)
            LocationPickerInput(
                label = Strings.get("home_address", language),
                hint = Strings.get("home_address_hint", language),
                addressValue = homeAddress,
                cityAreaValue = cityArea,
                latitude = customerLat,
                longitude = customerLng,
                language = language,
                testTagPrefix = "customer_address",
                isRequired = false,
                showGpsShortcut = true,
                onLocationSelected = { address, city, lat, lng ->
                    homeAddress = address
                    cityArea = city
                    customerLat = lat
                    customerLng = lng
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Notification Preference
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DeepIndigoContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = DeepIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.URDU) "آرڈر کی اطلاعات موصول کریں" else "Order & Status Notifications",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSlate
                            )
                            Text(
                                text = if (language == AppLanguage.URDU) "بکنگ کی تصدیق اور کاریگر کی آمد پر الرٹس" else "Receive instant alerts for bookings & arrival",
                                fontSize = 12.sp,
                                color = TextSlateMuted
                            )
                        }
                    }
                    Switch(
                        checked = notificationPreferenceEnabled,
                        onCheckedChange = { notificationPreferenceEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DeepIndigo
                        ),
                        modifier = Modifier.testTag("notification_pref_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Finish Setup CTA
            PrimaryCtaButton(
                text = Strings.get("finish_setup", language),
                onClick = {
                    val user = UserEntity(
                        phone = phoneNumber,
                        role = "CUSTOMER",
                        name = fullName.trim(),
                        profilePhotoUri = avatarList.getOrNull(selectedAvatarIndex),
                        cityArea = cityArea.trim(),
                        homeAddress = homeAddress.trim(),
                        status = "ACTIVE",
                        lat = customerLat,
                        lng = customerLng
                    )
                    onComplete(user)
                },
                enabled = fullName.isNotBlank(),
                testTag = "finish_customer_setup_button"
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
