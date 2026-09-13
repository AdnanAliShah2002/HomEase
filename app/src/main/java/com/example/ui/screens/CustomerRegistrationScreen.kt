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
import androidx.compose.material3.Text
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRegistrationScreen(
    phoneNumber: String,
    language: AppLanguage,
    onComplete: (UserEntity) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var selectedAvatarIndex by remember { mutableStateOf(0) }
    var cityArea by remember { mutableStateOf("Islamabad - Blue Area") }
    var cityExpanded by remember { mutableStateOf(false) }
    var homeAddress by remember { mutableStateOf("") }
    var customerLat by remember { mutableStateOf<Double?>(null) }
    var customerLng by remember { mutableStateOf<Double?>(null) }

    val cityOptions = listOf(
        "Lahore - Gulberg",
        "Lahore - DHA Phase 5",
        "Lahore - Bahria Town",
        "Lahore - Model Town",
        "Islamabad - F-7 / F-8",
        "Islamabad - Blue Area",
        "Rawalpindi - Saddar",
        "Karachi - Clifton / DHA",
        "Karachi - Gulshan-e-Iqbal"
    )

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
            Spacer(modifier = Modifier.height(28.dp))

            // Finish Setup CTA
            PrimaryCtaButton(
                text = Strings.get("finish_setup", language),
                onClick = {
                    val user = UserEntity(
                        phone = phoneNumber,
                        role = "CUSTOMER",
                        name = fullName.ifBlank { "Valued Customer" },
                        profilePhotoUri = avatarList.getOrNull(selectedAvatarIndex),
                        cityArea = cityArea,
                        homeAddress = homeAddress,
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
