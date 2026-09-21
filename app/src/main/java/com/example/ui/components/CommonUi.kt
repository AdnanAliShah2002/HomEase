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
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.ui.theme.SurfaceVariantLight
import com.example.ui.theme.TextSlateMuted
import com.example.ui.theme.StatusYellow
import com.example.ui.theme.StatusYellowContainer
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun PrimaryCtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    backgroundColor: Color = SoftOrange,
    contentColor: Color = Color.White,
    isProviderStyle: Boolean = false,
    testTag: String = "primary_cta_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "apple_button_press_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(if (isProviderStyle) 54.dp else 50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed) 1.dp else 3.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = Color(0x1A000000),
                ambientColor = Color(0x0F000000)
            )
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFFE5E5EA),
            disabledContentColor = Color(0xFF8E8E93)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = if (isProviderStyle) 16.sp else 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            )
        }
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
    onLogout: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null
) {
    Surface(
        color = Color.White.copy(alpha = 0.92f),
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 0.5.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x0F000000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HomeaseHeaderLogo(markSize = 32.dp)

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Apple-style Language Toggle Pill
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFFF2F2F7))
                        .border(0.5.dp, Color(0x1A000000), RoundedCornerShape(17.dp))
                        .clickable { onToggleLanguage() }
                        .padding(horizontal = 12.dp)
                        .testTag("language_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.ENGLISH) "EN" else "اردو",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Apple-style Profile Avatar
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEBF5FF))
                        .border(0.5.dp, Color(0x1A007AFF), CircleShape)
                        .clickable(enabled = onOpenProfile != null) { onOpenProfile?.invoke() }
                        .testTag("top_bar_profile_icon"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userAvatar ?: "👤",
                        fontSize = 16.sp
                    )
                }

                if (showRoleSwitcher) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Apple-style Role Switcher Pill
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(
                                if (currentRole == UserRole.PROVIDER) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
                            )
                            .border(
                                width = 0.5.dp,
                                color = Color(0x1A000000),
                                shape = RoundedCornerShape(17.dp)
                            )
                            .clickable { onToggleRole() }
                            .padding(horizontal = 10.dp)
                            .testTag("role_switcher"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (currentRole == UserRole.PROVIDER) Icons.Default.Handyman else Icons.Default.Home,
                                contentDescription = if (currentRole == UserRole.PROVIDER) "Switch to Customer" else "Switch to Provider",
                                tint = if (currentRole == UserRole.PROVIDER) Color.White else Color(0xFF1C1C1E),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentRole == UserRole.PROVIDER) "Pro" else "User",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentRole == UserRole.PROVIDER) Color.White else Color(0xFF1C1C1E)
                            )
                        }
                    }
                }

                onLogout?.let { logoutAction ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEA))
                            .border(0.5.dp, Color(0x20FF3B30), CircleShape)
                            .clickable { logoutAction() }
                            .testTag("logout_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(16.dp)
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

@Composable
fun AppleCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    cornerRadius: androidx.compose.ui.unit.Dp = 18.dp,
    borderWidth: androidx.compose.ui.unit.Dp = 0.5.dp,
    borderColor: Color = Color(0x0F000000),
    elevation: androidx.compose.ui.unit.Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "apple_card_scale"
    )

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        shadowElevation = elevation,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onClick() }
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun AppleSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93),
            letterSpacing = 0.6.sp
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF007AFF),
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
fun AppleSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE5E5EA))
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .then(
                            if (isSelected) {
                                Modifier.shadow(1.dp, RoundedCornerShape(8.dp), spotColor = Color(0x15000000))
                            } else Modifier
                        )
                        .clickable { onSelectIndex(index) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF8E8E93)
                    )
                }
            }
        }
    }
}

@Composable
fun AppleLiveActivityCard(
    title: String,
    subtitle: String,
    statusText: String,
    leadingIcon: ImageVector = Icons.Default.Handyman,
    trailingText: String? = null,
    isPulseActive: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x14000000)),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Frosted Squircle Icon Container
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEBF5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPulseActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34C759))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93)
                    )
                }
            }

            if (trailingText != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2F2F7))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = trailingText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                }
            }
        }
    }
}

