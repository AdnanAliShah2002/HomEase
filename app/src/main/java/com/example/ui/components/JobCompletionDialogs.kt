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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.ServiceRequestEntity
import com.example.data.localization.AppLanguage
import com.example.data.localization.Strings
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.DeepIndigoContainer
import com.example.ui.theme.SoftOrange
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenContainer
import com.example.ui.theme.StatusYellowContainer
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextSlateMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog shown to provider when tapping "Mark Job as Completed"
 */
@Composable
fun ProviderCompleteConfirmationDialog(
    jobTitle: String,
    language: AppLanguage = AppLanguage.ENGLISH,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusGreen,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = Strings.get("mark_complete_title", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = TextSlate
                )
            }
        },
        text = {
            Column {
                Text(
                    text = jobTitle,
                    fontWeight = FontWeight.Bold,
                    color = DeepIndigo,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = Strings.get("mark_complete_sub", language),
                    fontSize = 14.sp,
                    color = TextSlate,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                modifier = Modifier.testTag("confirm_complete_btn")
            ) {
                Text(Strings.get("confirm_completion_btn", language), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.get("cancel_btn", language), color = TextSlateMuted)
            }
        }
    )
}

/**
 * Customer "How did it go?" rating dialog
 */
@Composable
fun HowDidItGoDialog(
    job: ServiceRequestEntity,
    language: AppLanguage,
    onSubmitRating: (rating: Int, comment: String) -> Unit,
    onReportIssue: (category: String, description: String) -> Unit,
    onAutoCompleteWithoutRating: () -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var showIssueDialog by remember { mutableStateOf(false) }

    if (showIssueDialog) {
        ReportIssueDialog(
            job = job,
            language = language,
            onSubmit = { category, description ->
                onReportIssue(category, description)
                showIssueDialog = false
                onDismiss()
            },
            onDismiss = { showIssueDialog = false }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("how_did_it_go_dialog"),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.get("how_did_it_go_title", language),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextSlate
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSlateMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Job Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = job.serviceTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextSlate
                            )
                            val price = if (job.agreedPriceRs > 0) job.agreedPriceRs else job.budgetRs
                            Text(
                                text = "Rs $price",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = StatusGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "کاریگر: ${job.selectedProviderName ?: "تصدیق شدہ کاریگر"}" else "Provider: ${job.selectedProviderName ?: "Verified Pro"}",
                            fontSize = 13.sp,
                            color = TextSlateMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Interactive 1-5 Star Rating
                Text(
                    text = Strings.get("overall_rating", language).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlateMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { rating = i },
                            modifier = Modifier.size(46.dp).testTag("star_rating_$i")
                        ) {
                            Icon(
                                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$i Stars",
                                tint = if (i <= rating) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                val ratingLabel = if (language == AppLanguage.URDU) {
                    when (rating) {
                        5 -> "5.0 - بہترین کام"
                        4 -> "4.0 - بہت اچھا"
                        3 -> "3.0 - تسلی بخش"
                        2 -> "2.0 - مناسب"
                        else -> "1.0 - ناپسندیدہ"
                    }
                } else {
                    when (rating) {
                        5 -> "5.0 - Excellent"
                        4 -> "4.0 - Very Good"
                        3 -> "3.0 - Good / Satisfied"
                        2 -> "2.0 - Fair"
                        else -> "1.0 - Poor"
                    }
                }

                Text(
                    text = ratingLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DeepIndigo
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Optional comment box
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(Strings.get("review_optional", language)) },
                    placeholder = { Text(if (language == AppLanguage.URDU) "اپنی رائے لکھیں..." else "Tell others about your experience...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("rating_comment_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepIndigo,
                        unfocusedBorderColor = BorderStroke,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Rating Button
                Button(
                    onClick = {
                        onSubmitRating(rating, comment.trim())
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_rating_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                ) {
                    Text(
                        text = Strings.get("submit_rating", language),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary "Something wrong? Report an issue" link
                Row(
                    modifier = Modifier
                        .clickable { showIssueDialog = true }
                        .padding(vertical = 6.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = SoftOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Strings.get("report_an_issue", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftOrange
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 48-hour Auto-Complete note & simulation trigger
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = TextSlateMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Auto-completes after 48 hours if unconfirmed",
                                fontSize = 11.sp,
                                color = TextSlateMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Simulate 48h Auto-Complete",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepIndigo,
                            modifier = Modifier
                                .clickable {
                                    onAutoCompleteWithoutRating()
                                    onDismiss()
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stub form for reporting issues on a job
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueDialog(
    job: ServiceRequestEntity,
    language: AppLanguage = AppLanguage.ENGLISH,
    onSubmit: (category: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = if (language == AppLanguage.URDU) {
        listOf(
            "ادھورا کام",
            "زیادہ قیمت / بل پر تنازع",
            "تاخیر یا غیر پیشہ ورانہ رویہ",
            "نقصان پہنچایا",
            "کوئی اور مسئلہ"
        )
    } else {
        listOf(
            "Incomplete Work",
            "Overcharged / Price Dispute",
            "Late or Unprofessional",
            "Damage Caused",
            "Other Issue"
        )
    }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var issueDescription by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReportProblem,
                    contentDescription = null,
                    tint = SoftOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Strings.get("report_issue_title", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextSlate
                )
            }
        },
        text = {
            if (isSubmitted) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (language == AppLanguage.URDU) "مسئلہ کامیابی سے جمع ہو گیا" else "Issue Submitted Successfully",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextSlate
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (language == AppLanguage.URDU) "ہماری ہوم ایز سیفٹی ٹیم اس رپورٹ کا جائزہ لے کر 24 گھنٹوں کے اندر رابطہ کرے گی۔" else "Our HomEase Trust & Safety team will review this report and contact you within 24 hours.",
                        fontSize = 13.sp,
                        color = TextSlateMuted,
                        lineHeight = 18.sp
                    )
                }
            } else {
                Column {
                    Text(
                        text = "${if (language == AppLanguage.URDU) "کام:" else "Job:"} ${job.serviceTitle}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = DeepIndigo
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = Strings.get("issue_category", language),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlateMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = Strings.get("details_label", language),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlateMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = issueDescription,
                        onValueChange = { issueDescription = it },
                        placeholder = { Text(if (language == AppLanguage.URDU) "براہ کرم مسئلے کی تفصیل بتائیں..." else "Please describe what went wrong...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                ) {
                    Text(Strings.get("close_btn", language))
                }
            } else {
                Button(
                    onClick = {
                        onSubmit(selectedCategory, issueDescription)
                        isSubmitted = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftOrange)
                ) {
                    Text(Strings.get("submit_report", language), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) {
                TextButton(onClick = onDismiss) {
                    Text(Strings.get("cancel", language), color = TextSlateMuted)
                }
            }
        }
    )
}

/**
 * Read-only Job Detail Dialog shown when tapping a past booking
 */
@Composable
fun JobDetailDialog(
    job: ServiceRequestEntity,
    isProviderPerspective: Boolean = false,
    language: AppLanguage = AppLanguage.ENGLISH,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault())
    val dateString = dateFormat.format(Date(job.completedAt ?: job.createdAt))
    val price = if (job.agreedPriceRs > 0) job.agreedPriceRs else job.budgetRs

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top row with status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (job.status == "COMPLETED") StatusGreenContainer else StatusYellowContainer
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (job.status == "COMPLETED") Strings.get("status_completed", language) else Strings.get("status_awaiting_rating", language),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (job.status == "COMPLETED") Color(0xFF065F46) else Color(0xFF92400E)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSlateMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Service Title & Price
                Text(
                    text = job.serviceTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextSlate
                )
                Text(
                    text = job.categoryTitle,
                    fontSize = 13.sp,
                    color = DeepIndigo,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Price & Date Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, BorderStroke, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Strings.get("amount_paid", language),
                            fontSize = 12.sp,
                            color = TextSlateMuted
                        )
                        Text(
                            text = "Rs $price",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = StatusGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (language == AppLanguage.URDU) "تاریخ" else "Date",
                            fontSize = 12.sp,
                            color = TextSlateMuted
                        )
                        Text(
                            text = dateString,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Participant Info
                if (isProviderPerspective) {
                    Text(
                        text = if (language == AppLanguage.URDU) "کسٹمر کی معلومات" else "Customer Information",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlateMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.customerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                    Text(
                        text = "📍 ${job.fullAddress.ifBlank { job.cityArea }}",
                        fontSize = 13.sp,
                        color = TextSlateMuted
                    )
                } else {
                    Text(
                        text = if (language == AppLanguage.URDU) "سروس کاریگر" else "Service Provider",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlateMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = job.selectedProviderName ?: (if (language == AppLanguage.URDU) "تصدیق شدہ کاریگر" else "Verified Provider"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate
                    )
                    Text(
                        text = "📍 ${if (language == AppLanguage.URDU) "سروس کا مقام:" else "Service Location:"} ${job.fullAddress.ifBlank { job.cityArea }}",
                        fontSize = 13.sp,
                        color = TextSlateMuted
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rating & Review Section
                Text(
                    text = if (language == AppLanguage.URDU) "ریٹنگ اور رائے" else "Rating & Feedback",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlateMuted
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (job.ratingGiven != null && job.ratingGiven > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= job.ratingGiven) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (i <= job.ratingGiven) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${job.ratingGiven}.0 / 5.0",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextSlate
                        )
                    }
                    if (!job.ratingComment.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${job.ratingComment}\"",
                            fontSize = 13.sp,
                            color = TextSlate,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                } else {
                    Text(
                        text = "No rating submitted (Auto-completed)",
                        fontSize = 13.sp,
                        color = TextSlateMuted
                    )
                }

                // Issue status if reported
                if (!job.issueCategory.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "⚠️ Issue Reported: ${job.issueCategory}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFB91C1C)
                            )
                            if (!job.issueDescription.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = job.issueDescription,
                                    fontSize = 11.sp,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                ) {
                    Text(Strings.get("close_btn", language), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Help & Support Dialog for both Customer and Provider
 */
@Composable
fun HelpSupportDialog(
    roleLabel: String = "Customer",
    language: AppLanguage = AppLanguage.ENGLISH,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.get("help_and_support", language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextSlate
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSlateMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Direct Contact buttons
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Strings.get("live_helpline", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF166534)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "لاہور سپورٹ سنٹر پر 24 گھنٹے مفت رابطہ کریں۔" else "Call or message our Lahore support centre 24/7 for prompt resolution.",
                            fontSize = 12.sp,
                            color = TextSlateMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { /* Call intent stub */ },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == AppLanguage.URDU) "کال 042-111" else "Call 042-111", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { /* WhatsApp intent stub */ },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (language == AppLanguage.URDU) "اکثر پوچھے جانے والے سوالات" else "Frequently Asked Questions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlate
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (language == AppLanguage.URDU) {
                    FaqItem(
                        question = "نقد رقم کی ادائیگی کیسے کام کرتی ہے؟",
                        answer = "کام مکمل ہونے کے بعد کسٹمر براہ راست نقد رقم یا جاز کیش / ایزی پیسہ کے ذریعے ادائیگی کرتے ہیں۔"
                    )
                    FaqItem(
                        question = "اگر کام تسلی بخش نہ ہو تو کیا کریں؟",
                        answer = "کام مکمل ہونے کے بعد 'مسئلہ بتائیں' پر کلک کریں۔ ہماری ٹیم مفت دوبارہ کام یا معاوضے کی ضمانت دیتی ہے۔"
                    )
                    FaqItem(
                        question = "سروس فراہم کرنے والوں کی تصدیق کیسے ہوتی ہے؟",
                        answer = "ہر کاریگر نادرا شناختی کارڈ، سابقہ تجربے اور ضمانتی تصدیق کے بعد رجسٹر ہوتا ہے۔"
                    )
                } else {
                    FaqItem(
                        question = "How does cash payout work?",
                        answer = "Customers pay directly in cash after service completion, or via JazzCash/EasyPaisa wallet as arranged with the provider."
                    )
                    FaqItem(
                        question = "What if a service is unsatisfactorily done?",
                        answer = "Tap 'Report an issue' during the completion flow. Our team guarantees a free rework or mediation."
                    )
                    FaqItem(
                        question = "How are service providers verified?",
                        answer = "Every provider submits NADRA CNIC details, background references, and trade experience verification before receiving jobs."
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo)
                ) {
                    Text(Strings.get("got_it", language), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Text(
            text = question,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = TextSlate
        )
        if (expanded) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = answer,
                fontSize = 12.sp,
                color = TextSlateMuted,
                lineHeight = 17.sp
            )
        }
    }
}
