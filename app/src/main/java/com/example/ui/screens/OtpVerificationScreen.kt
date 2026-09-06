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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.ui.components.HomeaseLogoMark
import com.example.ui.components.PrimaryCtaButton
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    language: AppLanguage,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBack: () -> Unit,
    onResend: () -> Unit = {},
    onVerified: (String) -> Unit
) {
    var otpValue by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(30) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 30s countdown timer for resend
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    // Auto submit on 4 digits
    LaunchedEffect(otpValue) {
        if (otpValue.length == 4 && !isLoading) {
            delay(200)
            onVerified(otpValue)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("otp_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextSlate
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    HomeaseLogoMark(size = 56.dp)

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = Strings.get("verify_number_title", language),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${Strings.get("verify_number_sub", language)} $phoneNumber",
                        fontSize = 14.sp,
                        color = TextSlateMuted
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 4-digit OTP Display Boxes with Hidden BasicTextField
                    BasicTextField(
                        value = otpValue,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                otpValue = input
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_input_field"),
                        decorationBox = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until 4) {
                                    val digit = if (i < otpValue.length) otpValue[i].toString() else ""
                                    val isFocused = i == otpValue.length
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White)
                                            .border(
                                                width = if (isFocused) 2.dp else 1.5.dp,
                                                color = if (isFocused) DeepIndigo else BorderStroke,
                                                shape = RoundedCornerShape(14.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = digit,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepIndigo,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    if (i < 3) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }
                                }
                            }
                        }
                    )

                    // Error Message Banner if validation failed
                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
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

                    Spacer(modifier = Modifier.height(28.dp))

                    // Resend Code link with 30s countdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (countdown > 0) {
                            Text(
                                text = "${Strings.get("resend_code_cooldown", language)} ${countdown}${Strings.get("seconds", language)}",
                                color = TextSlateMuted,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = Strings.get("resend_code", language),
                                color = DeepIndigo,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        countdown = 30
                                        otpValue = ""
                                        onResend()
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(if (language == AppLanguage.URDU) "تصدیقی کوڈ واٹس ایپ پر دوبارہ بھیج دیا گیا۔" else "Verification code resent via WhatsApp.")
                                        }
                                    }
                                    .testTag("resend_code_button")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fallback link: "Didn't get it? Send via SMS instead"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = Strings.get("sms_fallback", language),
                            color = SoftOrange,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(Strings.get("sms_sent_notice", language))
                                    }
                                }
                                .testTag("sms_fallback_link")
                        )
                    }
                }

                // Verify Button
                Column(modifier = Modifier.fillMaxWidth()) {
                    PrimaryCtaButton(
                        text = if (isLoading) Strings.get("verifying_code", language) else Strings.get("confirm", language),
                        onClick = { onVerified(otpValue) },
                        enabled = otpValue.length == 4 && !isLoading,
                        testTag = "verify_otp_button"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
