package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Iron
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.data.model.UserRole
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.DeepIndigoContainer
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SoftOrangeContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.StatusYellowContainer
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted

@Composable
fun PrimaryCtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = SoftOrange,
    contentColor: Color = Color.White,
    isProviderStyle: Boolean = false,
    testTag: String = "primary_cta_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(if (isProviderStyle) 58.dp else 52.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), spotColor = backgroundColor)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFFCBD5E1),
            disabledContentColor = Color.White
        )
    ) {
        Text(
            text = text,
            fontSize = if (isProviderStyle) 17.sp else 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HomEaseTopBar(
    currentRole: UserRole,
    language: AppLanguage,
    onToggleRole: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier,
    showRoleSwitcher: Boolean = true,
    userAvatar: String? = null,
    onLogout: (() -> Unit)? = null
) {
    Surface(
        color = Color.White,
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HomeaseHeaderLogo(markSize = 34.dp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Professional Polish Language Toggle Button: round 38dp
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                        .clickable { onToggleLanguage() }
                        .testTag("language_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.ENGLISH) "EN" else "اردو",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF475569)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Professional Polish Avatar container: 38dp indigo-50 with soft border
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DeepIndigoContainer)
                        .border(1.5.dp, Color(0xFFE0E7FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userAvatar ?: "👤",
                        fontSize = 17.sp
                    )
                }

                if (showRoleSwitcher) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Role switch pill (Customer <-> Provider)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (currentRole == UserRole.PROVIDER) DeepIndigo else Color(0xFFF1F5F9)
                            )
                            .border(
                                width = 1.dp,
                                color = if (currentRole == UserRole.PROVIDER) DeepIndigo else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onToggleRole() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("role_switcher"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentRole == UserRole.PROVIDER) Icons.Default.Handyman else Icons.Default.Home,
                            contentDescription = "Role",
                            tint = if (currentRole == UserRole.PROVIDER) Color.White else DeepIndigo,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentRole == UserRole.PROVIDER) "Pro" else "User",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentRole == UserRole.PROVIDER) Color.White else DeepIndigo
                        )
                    }
                }

                onLogout?.let { logoutAction ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFECACA), CircleShape)
                            .clickable { logoutAction() }
                            .testTag("logout_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    statusText: String,
    isSuccess: Boolean = false,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSuccess -> StatusGreenContainer
        isWarning -> StatusYellowContainer
        else -> DeepIndigoContainer
    }
    val textColor = when {
        isSuccess -> StatusGreen
        isWarning -> StatusYellow
        else -> DeepIndigo
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = statusText,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun getCategoryIcon(iconType: String): ImageVector {
    return when (iconType) {
        "plumbing" -> Icons.Default.Plumbing
        "electrical" -> Icons.Default.ElectricBolt
        "carpentry" -> Icons.Default.Build
        "dry_cleaning" -> Icons.Default.Iron
        "car_care" -> Icons.Default.DirectionsCar
        "ac_repair" -> Icons.Default.Thermostat
        "painting" -> Icons.Default.FormatPaint
        "cleaning" -> Icons.Default.CleaningServices
        else -> Icons.Default.Handyman
    }
}

@Composable
fun PaymentStubDialog(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Payment Method",
                fontWeight = FontWeight.Bold,
                color = TextSlate
            )
        },
        text = {
            Column {
                Text(
                    text = Strings.get("payment_placeholder", language),
                    color = TextSlateMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💵 Cash on completion\n📱 JazzCash / EasyPaisa QR to provider",
                    color = TextSlate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
            ) {
                Text(Strings.get("done", language))
            }
        }
    )
}

@Composable
fun RatingStubDialog(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Strings.get("ratings_stub_title", language),
                fontWeight = FontWeight.Bold,
                color = TextSlate
            )
        },
        text = {
            Text(
                text = Strings.get("ratings_stub_desc", language),
                color = TextSlateMuted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
            ) {
                Text(Strings.get("done", language))
            }
        }
    )
}
