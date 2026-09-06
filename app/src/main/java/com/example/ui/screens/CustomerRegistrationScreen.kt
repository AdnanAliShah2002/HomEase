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
    var fullName by remember { mutableStateOf("Adnan Shah") }
    var selectedAvatarIndex by remember { mutableStateOf(0) }
    var cityArea by remember { mutableStateOf("Lahore - Gulberg") }
    var cityExpanded by remember { mutableStateOf(false) }
    var homeAddress by remember { mutableStateOf("House 42-B, Main Boulevard, Gulberg III") }
    var notifPref by remember { mutableStateOf("WHATSAPP") } // WHATSAPP, SMS, BOTH

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
                placeholder = { Text("e.g. Adnan Shah", color = TextSlateMuted) },
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

            // City / Area Dropdown
            Text(
                text = Strings.get("city_area", language) + " *",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSlate,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            ExposedDropdownMenuBox(
                expanded = cityExpanded,
                onExpandedChange = { cityExpanded = !cityExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = cityArea,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextSlate, fontSize = 15.sp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("city_area_dropdown"),
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
                ExposedDropdownMenu(
                    expanded = cityExpanded,
                    onDismissRequest = { cityExpanded = false }
                ) {
                    cityOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                cityArea = option
                                cityExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Home Address (Optional)
            Text(
                text = Strings.get("home_address", language) + " (${Strings.get("optional", language)})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSlate,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Automatic Location Detection
            AutoLocationFetcher(
                autoFetch = true,
                language = language,
                onLocationDetected = { loc ->
                    homeAddress = loc.fullAddress
                    val matchedOption = cityOptions.firstOrNull { option ->
                        option.contains(loc.cityName, ignoreCase = true) ||
                        loc.cityArea.contains(option.substringBefore(" -"), ignoreCase = true)
                    }
                    if (matchedOption != null) {
                        cityArea = matchedOption
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = homeAddress,
                onValueChange = { homeAddress = it },
                placeholder = { Text(Strings.get("home_address_hint", language), color = TextSlateMuted) },
                singleLine = false,
                maxLines = 2,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextSlate, fontSize = 15.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_address_input"),
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

            // Notification Preference
            Text(
                text = Strings.get("notif_preference", language),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSlate,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, BorderStroke, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                listOf(
                    "WHATSAPP" to "${Strings.get("notif_whatsapp", language)} (${Strings.get("recommended", language)})",
                    "SMS" to Strings.get("notif_sms", language),
                    "BOTH" to Strings.get("notif_both", language)
                ).forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { notifPref = key }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = notifPref == key,
                            onClick = { notifPref = key },
                            colors = RadioButtonDefaults.colors(selectedColor = DeepIndigo)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            color = TextSlate,
                            fontWeight = if (notifPref == key) FontWeight.Bold else FontWeight.Normal
                        )
                    }
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
                        name = fullName.ifBlank { "Valued Customer" },
                        profilePhotoUri = avatarList.getOrNull(selectedAvatarIndex),
                        cityArea = cityArea,
                        homeAddress = homeAddress,
                        notifPref = notifPref,
                        status = "ACTIVE"
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
