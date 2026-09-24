package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.JobMessageEntity
import com.example.data.db.ServiceRequestEntity
import com.example.data.localization.AppLanguage
import com.example.data.model.UserRole
import com.example.ui.viewmodel.HomeaseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobChatScreen(
    job: ServiceRequestEntity,
    viewModel: HomeaseViewModel,
    onBack: () -> Unit,
    onStartCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canonicalJobId = job.remoteId ?: job.id.toString()
    val messages by viewModel.getJobMessagesFlow(canonicalJobId).collectAsStateWithLifecycle(initialValue = emptyList())
    val language by viewModel.language.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val currentPhone = currentUser?.phone ?: viewModel.currentPhoneNumber.collectAsStateWithLifecycle().value
    val isProvider = activeRole == UserRole.PROVIDER

    val targetName = if (isProvider) {
        job.customerName.ifBlank { "Customer" }
    } else {
        job.selectedProviderName ?: "Service Provider"
    }

    val targetRole = if (isProvider) {
        if (language == AppLanguage.URDU) "صارف (کسٹمر)" else "Customer"
    } else {
        if (language == AppLanguage.URDU) "ماہر کاریگر" else "Service Provider"
    }

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when messages update
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Mark messages read as soon as user opens chat
    LaunchedEffect(canonicalJobId) {
        viewModel.markMessagesAsRead(canonicalJobId)
    }

    val quickReplies = remember(language) {
        if (language == AppLanguage.URDU) {
            listOf(
                "میں باہر موجود ہوں",
                "بیل بجا دیں",
                "راستے میں ہوں (5 منٹ)",
                "گیٹ نمبر بتائیں",
                "میں پہنچ گیا ہوں"
            )
        } else {
            listOf(
                "I'm outside",
                "Please ring the bell",
                "On my way (5 mins)",
                "Gate code is...",
                "I have arrived"
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE5E5EA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = targetName.take(1).uppercase(Locale.getDefault()),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007AFF),
                                fontSize = 16.sp
                            )
                        }

                        Column {
                            Text(
                                text = targetName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34C759))
                                )
                                Text(
                                    text = "$targetRole • Online",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF007AFF)
                        )
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick = onStartCall,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF34C759),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(38.dp)
                            .testTag("chat_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Start Voice Call",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF2F2F7))
        ) {
            // Context Job Card (Apple Inset Grouped style)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x1F000000))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Handyman,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = job.serviceTitle.ifBlank { "Home Service" },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Rs ${if (job.agreedPriceRs > 0) job.agreedPriceRs else job.budgetRs} • ${job.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Active Job #${job.id}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Messages LazyColumn
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = if (language == AppLanguage.URDU) "پیغامات یہاں ظاہر ہوں گے" else "Direct job chat with $targetName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (language == AppLanguage.URDU) "تیز رابطہ کے لیے نیچے دیے گئے فوری جوابات پر کلک کریں" else "Messages are synced in realtime via Supabase",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.senderId == currentPhone || (isProvider && msg.senderType == "provider") || (!isProvider && msg.senderType == "customer")
                    MessageBubble(
                        message = msg,
                        isMine = isMine
                    )
                }
            }

            // Quick reply chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickReplies) { reply ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                viewModel.sendJobMessage(canonicalJobId, reply)
                            },
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x26000000))
                    ) {
                        Text(
                            text = reply,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1C1C1E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom Input Bar (iOS iMessage capsule)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x1F000000))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = if (language == AppLanguage.URDU) "پیغام لکھیں..." else "iMessage",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF8E8E93)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input"),
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0x24000000),
                            focusedContainerColor = Color(0xFFF2F2F7),
                            unfocusedContainerColor = Color(0xFFF2F2F7),
                            focusedTextColor = Color(0xFF1C1C1E),
                            unfocusedTextColor = Color(0xFF1C1C1E)
                        ),
                        maxLines = 4
                    )

                    FilledIconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendJobMessage(canonicalJobId, textInput.trim())
                                textInput = ""
                            }
                        },
                        enabled = textInput.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (textInput.isNotBlank()) Color(0xFF007AFF) else Color(0xFFE5E5EA),
                            contentColor = if (textInput.isNotBlank()) Color.White else Color(0xFF8E8E93),
                            disabledContainerColor = Color(0xFFE5E5EA),
                            disabledContentColor = Color(0xFF8E8E93)
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: JobMessageEntity,
    isMine: Boolean,
    modifier: Modifier = Modifier
) {
    val timeFormatted = remember(message.createdAtEpochMs) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.createdAtEpochMs))
    }

    val bubbleColor = if (isMine) {
        Color(0xFF007AFF) // Apple System Blue
    } else {
        Color(0xFFE9E9EB) // Apple iMessage gray
    }

    val textColor = if (isMine) {
        Color.White
    } else {
        Color(0xFF1C1C1E)
    }

    val timeColor = if (isMine) {
        Color.White.copy(alpha = 0.75f)
    } else {
        Color(0xFF8E8E93)
    }

    val bubbleShape = if (isMine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            modifier = Modifier.widthIn(min = 64.dp, max = 280.dp),
            shadowElevation = 0.5.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                if (!isMine) {
                    Text(
                        text = if (message.senderType == "provider") "Provider" else "Customer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = timeColor
                    )

                    if (isMine) {
                        if (message.readAtEpochMs != null) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = "Read",
                                tint = Color(0xFF4DD0E1), // Teal/Cyan double check
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Sent",
                                tint = timeColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
