package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.localization.AppLanguage
import com.example.service.CallState
import com.example.ui.viewmodel.HomeaseViewModel
import java.util.Locale

@Composable
fun InCallScreen(
    viewModel: HomeaseViewModel,
    onMinimize: () -> Unit,
    onCallClosed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_ripple")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    var lastTargetName by remember { mutableStateOf("Participant") }
    val targetName = when (val state = callState) {
        is CallState.Connected -> {
            lastTargetName = state.targetName
            state.targetName
        }
        is CallState.Connecting -> {
            lastTargetName = state.targetName
            state.targetName
        }
        else -> lastTargetName
    }

    var lastTargetRole by remember { mutableStateOf("Job Member") }
    val targetRole = when (val state = callState) {
        is CallState.Connected -> {
            lastTargetRole = state.targetRole
            state.targetRole
        }
        is CallState.Connecting -> {
            lastTargetRole = state.targetRole
            state.targetRole
        }
        else -> lastTargetRole
    }

    var lastTargetPhone by remember { mutableStateOf("") }
    val targetPhone = when (val state = callState) {
        is CallState.Connected -> {
            lastTargetPhone = state.targetPhone
            state.targetPhone
        }
        is CallState.Connecting -> {
            lastTargetPhone = state.targetPhone
            state.targetPhone
        }
        else -> lastTargetPhone
    }

    val durationSeconds = when (val state = callState) {
        is CallState.Connected -> state.durationSeconds
        is CallState.Ended -> state.durationSeconds
        else -> 0
    }

    val formattedDuration = remember(durationSeconds) {
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    val isMuted = (callState as? CallState.Connected)?.isMuted ?: false
    val isSpeaker = (callState as? CallState.Connected)?.isSpeaker ?: true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1C1C1E),
                        Color(0xFF0A0A0C)
                    )
                )
            )
    ) {
        // Top Minimize Action & Audio Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMinimize,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .testTag("call_minimize_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Minimize Call",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C759)) // Apple System Green
                    )
                    Text(
                        text = "FaceTime Audio",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Center Calling Animation & Details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulsing Avatar Box
            Box(
                modifier = Modifier.size(170.dp),
                contentAlignment = Alignment.Center
            ) {
                if (callState is CallState.Connected || callState is CallState.Connecting) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF).copy(alpha = 0.22f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2C2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = targetName.take(1).uppercase(Locale.getDefault()),
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = targetName,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = targetRole,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Call Status Chip
            val statusText = when (callState) {
                is CallState.Connecting -> if (language == AppLanguage.URDU) "کنکٹ ہو رہا ہے..." else "Connecting..."
                is CallState.Connected -> {
                    val isRemoteIn = (callState as CallState.Connected).remoteUserConnected
                    if (isRemoteIn) formattedDuration else (if (language == AppLanguage.URDU) "گھنٹی جا رہی ہے..." else "Ringing...")
                }
                is CallState.Ended -> if (language == AppLanguage.URDU) "کال ختم ہو گئی" else "Call Ended"
                CallState.Idle -> ""
            }

            Surface(
                color = if (callState is CallState.Ended) Color(0xFFFF3B30).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (callState is CallState.Connected && (callState as CallState.Connected).remoteUserConnected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759))
                        )
                    }
                    Text(
                        text = statusText,
                        color = if (callState is CallState.Ended) Color(0xFFFF453A) else Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (targetPhone.isNotBlank() && callState !is CallState.Ended) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    onClick = {
                        try {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$targetPhone"))
                            context.startActivity(dialIntent)
                        } catch (e: Exception) {
                            // Fallback
                        }
                    },
                    color = Color(0xFF34C759).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = "Cellular Call",
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (language == AppLanguage.URDU) "براہ راست فون کال" else "Direct Cellular Call",
                            color = Color(0xFF34C759),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Bottom Call Action Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (callState is CallState.Ended) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (language == AppLanguage.URDU) "کال کی مدت: $formattedDuration" else "Call Duration: $formattedDuration",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (targetPhone.isNotBlank()) {
                                FilledIconButton(
                                    onClick = {
                                        try {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$targetPhone"))
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            // Fallback
                                        }
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color(0xFF34C759),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .size(54.dp)
                                        .testTag("call_cellular_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Phone,
                                        contentDescription = "Cellular Call"
                                    )
                                }
                            }
                            FilledIconButton(
                                onClick = onCallClosed,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFF007AFF),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(54.dp)
                                    .testTag("call_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Close Call View"
                                    )
                                }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { viewModel.toggleCallMute() },
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.18f))
                                .testTag("call_mute_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = if (isMuted) Color.Black else Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isMuted) "Unmute" else "Mute",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // End Call Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { viewModel.endVoiceCall() },
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30)) // Apple System Red
                                .testTag("call_end_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "کال کاٹیں" else "End",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // Speaker Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { viewModel.toggleCallSpeaker() },
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(if (isSpeaker) Color.White else Color.White.copy(alpha = 0.18f))
                                .testTag("call_speaker_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeaker) Icons.Filled.VolumeUp else Icons.Filled.VolumeDown,
                                contentDescription = if (isSpeaker) "Earpiece" else "Speaker",
                                tint = if (isSpeaker) Color.Black else Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isSpeaker) "Speaker" else "Earpiece",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
