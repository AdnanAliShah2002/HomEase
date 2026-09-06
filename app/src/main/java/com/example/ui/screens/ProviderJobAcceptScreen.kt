package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.db.ServiceRequestEntity
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.ui.components.PrimaryCtaButton
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.DeepIndigoContainer
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.SoftOrangeContainer
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted
import kotlinx.coroutines.delay

@Composable
fun ProviderJobAcceptScreen(
    job: ServiceRequestEntity,
    language: AppLanguage,
    onAccept: (ServiceRequestEntity) -> Unit,
    onReject: (ServiceRequestEntity) -> Unit,
    onCounter: (ServiceRequestEntity, Int, String?) -> Unit
) {
    var timerSeconds by remember { mutableIntStateOf(45) }
    var showCounterSheet by remember { mutableStateOf(false) }
    var counterPriceInput by remember { mutableStateOf((job.budgetRs + 300).toString()) }
    var counterNoteInput by remember { mutableStateOf("") }

    // 45s countdown timer for incoming job ping
    LaunchedEffect(timerSeconds) {
        if (timerSeconds > 0) {
            delay(1000)
            timerSeconds -= 1
        } else {
            // Auto reject if timer runs out
            onReject(job)
        }
    }

    val progress = timerSeconds / 45f

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Ping Bar & Close
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFEF2F2))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "New Job Request Nearby!",
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { onReject(job) },
                        modifier = Modifier.testTag("dismiss_ping_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSlateMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Countdown progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (timerSeconds < 15) Color(0xFFDC2626) else SoftOrange,
                    trackColor = Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Expires in ${timerSeconds}s",
                        fontSize = 12.sp,
                        color = TextSlateMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Main Details Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("incoming_job_fullscreen_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DeepIndigoContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    // Category & Distance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(DeepIndigoContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(job.categoryId),
                                    contentDescription = null,
                                    tint = DeepIndigo,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = job.categoryTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepIndigo
                                )
                                Text(
                                    text = job.serviceTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextSlate
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location & Distance
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = SoftOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Approx. 1.5 km away",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSlate
                            )
                            Text(
                                text = job.cityArea,
                                fontSize = 12.sp,
                                color = TextSlateMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Customer description
                    Text(
                        text = "Customer Notes:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlateMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.description,
                        fontSize = 14.sp,
                        color = TextSlate,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Customer Offered Price Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DeepIndigoContainer)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Customer's Offered Price",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepIndigo
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Rs ${job.budgetRs}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = DeepIndigo
                            )
                        }
                    }
                }
            }

            // Three Large Action Buttons (Accept, Counter Price, Reject)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Accept at Offered Price (Large 56dp CTA)
                PrimaryCtaButton(
                    text = "${Strings.get("accept_btn", language)} (Rs ${job.budgetRs})",
                    onClick = { onAccept(job) },
                    backgroundColor = DeepIndigo,
                    isProviderStyle = true,
                    testTag = "accept_job_fullscreen_btn"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Reject Button
                    Button(
                        onClick = { onReject(job) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("reject_job_fullscreen_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = TextSlateMuted
                        )
                    ) {
                        Text(
                            text = Strings.get("reject_btn", language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Counter Price Button
                    Button(
                        onClick = { showCounterSheet = true },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(54.dp)
                            .testTag("open_counter_sheet_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = Strings.get("counter_btn", language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Counter Price Dialog / Bottom Sheet
        if (showCounterSheet) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showCounterSheet = false },
                title = {
                    Text(
                        text = Strings.get("counter_dialog_title", language),
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                },
                text = {
                    Column {
                        Text(
                            text = Strings.get("counter_dialog_desc", language),
                            fontSize = 13.sp,
                            color = TextSlateMuted
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = counterPriceInput,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                if (digits.length <= 6) {
                                    counterPriceInput = digits
                                }
                            },
                            prefix = { Text("Rs ", fontWeight = FontWeight.Bold, color = DeepIndigo, fontSize = 18.sp) },
                            trailingIcon = {
                                if (counterPriceInput.isNotEmpty()) {
                                    IconButton(onClick = { counterPriceInput = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
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
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("counter_sheet_input"),
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick inspection quote preset chip (especially for plumbing/electrical/appliances)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SoftOrangeContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    counterPriceInput = "300"
                                    counterNoteInput = "Rs 300 to inspect and quote"
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💡", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Rs 300 to inspect and quote",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftOrange
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick increments: +200, +500, +1000 (mathematically add to current amount)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(200, 500, 1000).forEach { inc ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DeepIndigoContainer)
                                        .clickable {
                                            val current = counterPriceInput.toIntOrNull() ?: job.budgetRs
                                            counterPriceInput = (current + inc).coerceIn(100, 100000).toString()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+ Rs $inc",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepIndigo
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Optional Counter Note
                        OutlinedTextField(
                            value = counterNoteInput,
                            onValueChange = { counterNoteInput = it },
                            placeholder = { Text(if (language == AppLanguage.URDU) "کوئی وضاحتی نوٹ (اختیاری)" else "Optional note (e.g. available in 20 mins)", color = TextSlateMuted, fontSize = 12.sp) },
                            maxLines = 2,
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextSlate, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
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
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val price = counterPriceInput.toIntOrNull() ?: job.budgetRs
                            onCounter(job, price, counterNoteInput.ifBlank { null })
                            showCounterSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftOrange)
                    ) {
                        Text(Strings.get("send_counter_offer", language), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showCounterSheet = false }) {
                        Text(Strings.get("cancel", language), color = TextSlateMuted)
                    }
                }
            )
        }
    }
}
