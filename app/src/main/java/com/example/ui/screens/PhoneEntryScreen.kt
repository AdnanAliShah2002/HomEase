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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.AppleGlassSpecularBorder
import com.example.ui.theme.AppleIrisContainer
import com.example.ui.theme.AppleIrisPrimary
import com.example.ui.theme.AppleLabelPrimary
import com.example.ui.theme.AppleLabelSecondary
import com.example.ui.theme.AppleWarmChampagne
import com.example.ui.theme.LiquidAmbientCanvas
import com.example.ui.theme.liquidGlassCard
import com.example.ui.theme.liquidGlassPill

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
    var phoneNumber by remember { mutableStateOf("") }

    LiquidAmbientCanvas {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Apple-style Frosted Back Button & Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .liquidGlassPill(shape = CircleShape)
                            .clickable { onBack() }
                            .testTag("phone_entry_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppleLabelPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (isSignIn) Strings.get("sign_in_title", language) else Strings.get("create_account", language),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelPrimary,
                        letterSpacing = (-0.3).sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Floating Apple Liquid Glass Form Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassCard(shape = RoundedCornerShape(26.dp), elevation = 4.dp)
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HomeaseLogoMark(size = 56.dp)

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = Strings.get("phone_entry_title", language),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleLabelPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Strings.get("phone_entry_sub", language),
                            fontSize = 13.5.sp,
                            color = AppleLabelSecondary
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Phone Input Row with Frosted Country Code
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Country Code Glass Pill
                            Box(
                                modifier = Modifier
                                    .height(56.dp)
                                    .liquidGlassCard(
                                        shape = RoundedCornerShape(14.dp),
                                        backgroundColor = AppleIrisContainer.copy(alpha = 0.85f),
                                        elevation = 1.dp
                                    )
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🇵🇰 +92",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleIrisPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Phone Textfield with Liquid Frosted Styling
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }
                                    val withoutPrefix = when {
                                        digits.startsWith("920") -> digits.removePrefix("920")
                                        digits.startsWith("92") -> digits.removePrefix("92")
                                        digits.startsWith("0") -> digits.removePrefix("0")
                                        else -> digits
                                    }
                                    if (withoutPrefix.length <= 10) {
                                        phoneNumber = withoutPrefix
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = Strings.get("phone_placeholder", language),
                                        color = AppleLabelSecondary.copy(alpha = 0.65f),
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
                                                tint = AppleLabelSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = AppleLabelPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("phone_number_input"),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = AppleLabelPrimary,
                                    unfocusedTextColor = AppleLabelPrimary,
                                    cursorColor = AppleIrisPrimary,
                                    focusedBorderColor = AppleIrisPrimary,
                                    unfocusedBorderColor = Color(0x18000000),
                                    focusedContainerColor = Color.White.copy(alpha = 0.85f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.65f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // WhatsApp info banner - Frosted Liquid Glass Styling
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassCard(
                                    shape = RoundedCornerShape(14.dp),
                                    backgroundColor = Color.White.copy(alpha = 0.65f),
                                    elevation = 0.dp
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AppleIrisPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = Strings.get("whatsapp_notice", language),
                                    color = AppleLabelPrimary,
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
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
                    backgroundColor = if (role == UserRole.PROVIDER) AppleIrisPrimary else AppleWarmChampagne,
                    isProviderStyle = role == UserRole.PROVIDER,
                    testTag = "send_code_button"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
