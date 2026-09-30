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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.localization.AppLanguage
import com.example.data.model.UserRole
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted

private data class CancellationReasonItem(
    val id: String,
    val titleEn: String,
    val titleUr: String
)

private val customerReasons = listOf(
    CancellationReasonItem("too_long", "Provider is taking too long / not moving", "کاریگر بہت دیر لگا رہا ہے"),
    CancellationReasonItem("changed_mind", "Changed my mind / No longer needed", "اب سروس کی ضرورت نہیں ہے"),
    CancellationReasonItem("booked_mistake", "Booked by mistake", "غلطی سے بک ہو گیا"),
    CancellationReasonItem("price_dispute", "Price disagreement", "قیمت پر اتفاق نہیں ہوا"),
    CancellationReasonItem("found_other", "Found another service provider", "کوئی دوسرا انتظام ہو گیا"),
    CancellationReasonItem("emergency", "Personal emergency", "ہنگامی صورتحال"),
    CancellationReasonItem("other", "Other reason", "دیگر وجہ")
)

private val providerReasons = listOf(
    CancellationReasonItem("unreachable", "Customer unreachable / phone off", "کسٹمر سے رابطہ نہیں ہو رہا"),
    CancellationReasonItem("customer_requested", "Customer requested cancellation", "کسٹمر نے منسوخ کرنے کو کہا"),
    CancellationReasonItem("too_far", "Location too far or unreachable", "پتہ بہت دور یا غیر واضح ہے"),
    CancellationReasonItem("scope_mismatch", "Job scope changed / not as described", "کام کی نوعیت مختلف ہے"),
    CancellationReasonItem("vehicle_issue", "Vehicle or equipment problem", "گاڑی یا سامان میں خرابی"),
    CancellationReasonItem("emergency", "Personal emergency", "ذاتی مجبوری"),
    CancellationReasonItem("other", "Other reason", "دیگر وجہ")
)

/**
 * InDrive-style cancellation bottom sheet / modal dialog for Customer and Provider.
 * Allows choosing a standard cancellation reason or entering a custom reason.
 */
@Composable
fun JobCancellationDialog(
    role: UserRole,
    language: AppLanguage = AppLanguage.ENGLISH,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    val reasons = if (role == UserRole.CUSTOMER) customerReasons else providerReasons
    var selectedReasonId by remember { mutableStateOf<String?>(null) }
    var customReasonText by remember { mutableStateOf("") }

    val isUrdu = language == AppLanguage.URDU

    val title = if (role == UserRole.CUSTOMER) {
        if (isUrdu) "بکنگ منسوخ کریں" else "Cancel Booking"
    } else {
        if (isUrdu) "کام منسوخ کریں" else "Cancel Job"
    }

    val subtitle = if (isUrdu) {
        "براہ کرم منسوخی کی بنیادی وجہ منتخب کریں"
    } else {
        "Please select a reason for cancelling this booking"
    }

    val finalReason = remember(selectedReasonId, customReasonText) {
        val selectedItem = reasons.find { it.id == selectedReasonId }
        val baseTitle = if (isUrdu) selectedItem?.titleUr else selectedItem?.titleEn
        if (selectedReasonId == "other" || customReasonText.isNotBlank()) {
            if (baseTitle != null && selectedReasonId != "other") {
                "$baseTitle - $customReasonText"
            } else {
                customReasonText.ifBlank { baseTitle ?: "Cancelled" }
            }
        } else {
            baseTitle.orEmpty()
        }
    }

    val canConfirm = finalReason.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("job_cancellation_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header with Warning Icon & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSlate
                            )
                            Text(
                                text = subtitle,
                                fontSize = 11.5.sp,
                                color = TextSlateMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSlateMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Reason Options
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reasons.forEach { item ->
                        val isSelected = selectedReasonId == item.id
                        val itemText = if (isUrdu) item.titleUr else item.titleEn

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) Color(0xFFFF3B30) else Color(0x1F000000),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedReasonId = item.id },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFFF3B30).copy(alpha = 0.05f) else Color(0xFFF9FAFB)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = itemText,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFFFF3B30) else TextSlate,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Radio check indicator
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFFF3B30) else Color(0xFFCBD5E1),
                                            shape = CircleShape
                                        )
                                        .background(if (isSelected) Color(0xFFFF3B30) else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Optional Custom Reason Text Field (shown if "other" is selected or as an additional detail)
                if (selectedReasonId == "other" || selectedReasonId != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customReasonText,
                        onValueChange = { customReasonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_cancellation_reason_input"),
                        label = {
                            Text(
                                text = if (selectedReasonId == "other") {
                                    if (isUrdu) "تفصیل لکھیں (ضروری)" else "Specify reason (Required)"
                                } else {
                                    if (isUrdu) "اضافی تفصیلات (اختیاری)" else "Additional details (Optional)"
                                },
                                fontSize = 12.sp
                            )
                        },
                        placeholder = {
                            Text(
                                text = if (isUrdu) "یہاں لکھیں..." else "Type here...",
                                fontSize = 12.sp,
                                color = TextSlateMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF3B30),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = false,
                        maxLines = 3
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Dismiss & Confirm Cancellation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSlate)
                    ) {
                        Text(
                            text = if (isUrdu) "واپس جائیں" else "Don't Cancel",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            if (canConfirm) {
                                onConfirm(finalReason)
                            }
                        },
                        enabled = canConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_cancel_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF3B30),
                            disabledContainerColor = Color(0xFFFF3B30).copy(alpha = 0.35f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(
                            text = if (isUrdu) "منسوخ کریں" else "Confirm Cancel",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
