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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.data.model.UserRole
import com.example.ui.components.HomeaseLogoMark
import com.example.ui.components.PrimaryCtaButton
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.DeepIndigoContainer
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted

@Composable
fun PhoneEntryScreen(
    role: UserRole,
    isSignIn: Boolean,
    language: AppLanguage,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBack: () -> Unit,
    onSendCode: (String) -> Unit
) {
    var phoneNumber by remember {
        mutableStateOf("")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header with back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("phone_entry_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSlate
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSignIn) Strings.get("sign_in_title", language) else Strings.get("create_account", language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                HomeaseLogoMark(size = 56.dp)

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = Strings.get("phone_entry_title", language),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlate
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = Strings.get("phone_entry_sub", language),
                    fontSize = 14.sp,
                    color = TextSlateMuted
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Phone Input with +92 Country Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Country code box
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepIndigoContainer)
                            .border(1.dp, BorderStroke, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🇵🇰 +92",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepIndigo
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Phone textfield
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input ->
                            if (input.length <= 10 && input.all { it.isDigit() }) {
                                phoneNumber = input
                            }
                        },
                        placeholder = {
                            Text(
                                text = Strings.get("phone_placeholder", language),
                                color = TextSlateMuted.copy(alpha = 0.6f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                        },
                        trailingIcon = {
                            if (phoneNumber.isNotEmpty()) {
                                IconButton(onClick = { phoneNumber = "" }) {
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
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("phone_number_input"),
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

                Spacer(modifier = Modifier.height(14.dp))

                // WhatsApp info banner - soft neutral styling
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = DeepIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Strings.get("whatsapp_notice", language),
                            color = TextSlate,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⚠️ $errorMessage",
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom CTA
            Column(modifier = Modifier.fillMaxWidth()) {
                PrimaryCtaButton(
                    text = if (isLoading) Strings.get("sending_code", language) else Strings.get("send_code", language),
                    onClick = {
                        val fullPhone = "+92$phoneNumber"
                        onSendCode(fullPhone)
                    },
                    enabled = phoneNumber.length >= 9 && !isLoading,
                    backgroundColor = if (role == UserRole.PROVIDER) DeepIndigo else SoftOrange,
                    isProviderStyle = role == UserRole.PROVIDER,
                    testTag = "send_code_button"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
