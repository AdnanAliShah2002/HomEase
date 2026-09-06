package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.data.model.UserRole
import com.example.ui.components.HomeaseSplashBranding
import com.example.ui.components.PrimaryCtaButton
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.DeepIndigoContainer
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SoftOrangeContainer
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted

@Composable
fun AuthChoiceScreen(
    role: UserRole,
    language: AppLanguage,
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit
) {
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
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                HomeaseSplashBranding(
                    tagline = Strings.get("tagline", language)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Role indicator badge
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (role == UserRole.PROVIDER) SoftOrangeContainer else DeepIndigoContainer)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (role == UserRole.PROVIDER) Icons.Default.Handyman else Icons.Default.Home,
                        contentDescription = null,
                        tint = if (role == UserRole.PROVIDER) SoftOrange else DeepIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = if (role == UserRole.PROVIDER) {
                            Strings.get("role_provider_title", language)
                        } else {
                            Strings.get("role_customer_title", language)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (role == UserRole.PROVIDER) SoftOrange else DeepIndigo
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = Strings.get("welcome_title", language),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlate
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Strings.get("welcome_subtitle", language),
                    fontSize = 14.sp,
                    color = TextSlateMuted
                )
            }

            // Bottom Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrimaryCtaButton(
                    text = Strings.get("create_account", language),
                    onClick = onCreateAccount,
                    backgroundColor = if (role == UserRole.PROVIDER) DeepIndigo else SoftOrange,
                    isProviderStyle = role == UserRole.PROVIDER,
                    testTag = "create_account_button"
                )

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = onSignIn,
                    modifier = Modifier.testTag("sign_in_link")
                ) {
                    Text(
                        text = Strings.get("already_have_account", language),
                        color = DeepIndigo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
