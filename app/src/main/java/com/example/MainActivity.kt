package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.localization.AppLanguage
import com.example.data.model.UserRole
import com.example.ui.screens.*
import com.example.ui.theme.HomEaseTheme
import com.example.ui.viewmodel.AppNavDestination
import com.example.ui.viewmodel.HomeaseViewModel
import com.example.ui.viewmodel.IncomingCallInfo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels for local notifications
        com.example.service.HomEaseFirebaseMessagingService.createNotificationChannels(this)

        val notifJobId = intent?.getStringExtra("job_id")
        val notifType = intent?.getStringExtra("notification_type")

        setContent {
            val viewModel: HomeaseViewModel = viewModel()
            val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()
            val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
            val isSignInMode by viewModel.isSignInMode.collectAsStateWithLifecycle()
            val currentPhoneNumber by viewModel.currentPhoneNumber.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val isSendingOtp by viewModel.isSendingOtp.collectAsStateWithLifecycle()
            val otpSendError by viewModel.otpSendError.collectAsStateWithLifecycle()
            val isVerifyingOtp by viewModel.isVerifyingOtp.collectAsStateWithLifecycle()
            val otpVerifyError by viewModel.otpVerifyError.collectAsStateWithLifecycle()
            val activeLiveRequest by viewModel.activeLiveRequest.collectAsStateWithLifecycle()
            val initialCategoryForRequest by viewModel.initialCategoryForRequest.collectAsStateWithLifecycle()
            val incomingOffers by viewModel.incomingOffers.collectAsStateWithLifecycle()
            val fullscreenPingJob by viewModel.fullscreenPingJob.collectAsStateWithLifecycle()
            val isSubmittingRequest by viewModel.isSubmittingRequest.collectAsStateWithLifecycle()
            val requestSubmissionError by viewModel.requestSubmissionError.collectAsStateWithLifecycle()
            val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
            val trackingJob by viewModel.trackingJob.collectAsStateWithLifecycle()
            val activeChatJob by viewModel.activeChatJob.collectAsStateWithLifecycle()

            val customerRequests by viewModel.customerRequests.collectAsStateWithLifecycle()
            val availableJobs by viewModel.availableJobs.collectAsStateWithLifecycle()
            val providerActiveJob by viewModel.providerActiveJob.collectAsStateWithLifecycle()
            val providerPastJobs by viewModel.providerPastJobs.collectAsStateWithLifecycle()
            val providerCompletedJobs by viewModel.providerCompletedJobs.collectAsStateWithLifecycle()
            val customerAwaitingRatingJob by viewModel.customerAwaitingRatingJob.collectAsStateWithLifecycle()

            val providerRegistrationLoading by viewModel.providerRegistrationLoading.collectAsStateWithLifecycle()
            val providerRegistrationError by viewModel.providerRegistrationError.collectAsStateWithLifecycle()
            val isRefreshingStatus by viewModel.isRefreshingStatus.collectAsStateWithLifecycle()
            val statusCheckMessage by viewModel.statusCheckMessage.collectAsStateWithLifecycle()
            val providerActionError by viewModel.providerActionError.collectAsStateWithLifecycle()
            val providerJobWonConfirmation by viewModel.providerJobWonConfirmation.collectAsStateWithLifecycle()
            val incomingCall by viewModel.incomingCall.collectAsStateWithLifecycle()
            val inAppMessageNotification by viewModel.inAppMessageNotification.collectAsStateWithLifecycle()

            // Handle notification clicks once ViewModel is ready
            LaunchedEffectOnce(notifJobId) {
                if (notifJobId != null) {
                    viewModel.handleNotificationClick(notifJobId, notifType)
                }
            }

            HomEaseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // System back button handling
                    BackHandler(enabled = currentDestination != AppNavDestination.SPLASH &&
                            currentDestination != AppNavDestination.CUSTOMER_HOME &&
                            currentDestination != AppNavDestination.PROVIDER_HOME) {
                        viewModel.navigateBack()
                    }

                    when (currentDestination) {
                        AppNavDestination.SPLASH -> {
                            SplashScreen(
                                language = language,
                                onTimeout = { viewModel.onSplashFinished() }
                            )
                        }

                        AppNavDestination.LANGUAGE_SELECT -> {
                            LanguageSelectScreen(
                                currentLanguage = language,
                                onLanguageSelected = { lang -> viewModel.onLanguageSelected(lang) },
                                onSkip = { viewModel.onSkipLanguage() }
                            )
                        }

                        AppNavDestination.ROLE_SELECT -> {
                            RoleSelectionScreen(
                                language = language,
                                onRoleSelected = { role -> viewModel.selectRole(role) }
                            )
                        }

                        AppNavDestination.AUTH_CHOICE -> {
                            AuthChoiceScreen(
                                role = activeRole,
                                language = language,
                                onCreateAccount = { viewModel.startCreateAccount() },
                                onSignIn = { viewModel.startSignIn() }
                            )
                        }

                        AppNavDestination.PHONE_ENTRY -> {
                            PhoneEntryScreen(
                                role = activeRole,
                                isSignIn = isSignInMode,
                                language = language,
                                isLoading = isSendingOtp,
                                errorMessage = otpSendError,
                                onBack = { viewModel.navigateBack() },
                                onSendCode = { phone -> viewModel.onSendCode(phone) }
                            )
                        }

                        AppNavDestination.OTP_VERIFICATION -> {
                            OtpVerificationScreen(
                                phoneNumber = currentPhoneNumber,
                                language = language,
                                isLoading = isVerifyingOtp,
                                errorMessage = otpVerifyError,
                                onBack = { viewModel.navigateBack() },
                                onResend = { viewModel.resendOtpCode() },
                                onVerified = { code -> viewModel.onOtpVerified(code) }
                            )
                        }

                        AppNavDestination.CUSTOMER_REGISTRATION -> {
                            CustomerRegistrationScreen(
                                phoneNumber = currentPhoneNumber,
                                language = language,
                                onComplete = { user -> viewModel.completeCustomerRegistration(user) }
                            )
                        }

                        AppNavDestination.PROVIDER_REGISTRATION -> {
                            ProviderRegistrationScreen(
                                phoneNumber = currentPhoneNumber,
                                language = language,
                                isSubmitting = providerRegistrationLoading,
                                registrationError = providerRegistrationError,
                                onSubmit = { user, onSuccess ->
                                    viewModel.completeProviderRegistration(user, onSuccess)
                                },
                                onGoToDashboard = { viewModel.proceedToProviderDashboard() },
                                onClearError = { viewModel.clearProviderRegistrationError() }
                            )
                        }

                        AppNavDestination.CUSTOMER_HOME -> {
                            val user = currentUser ?: com.example.data.db.UserEntity(
                                phone = currentPhoneNumber.ifBlank { "" },
                                role = "CUSTOMER",
                                name = "Customer",
                                cityArea = ""
                            )
                            CustomerHomeScreen(
                                user = user,
                                activeRequests = customerRequests,
                                awaitingRatingJob = customerAwaitingRatingJob,
                                language = language,
                                onToggleRole = { viewModel.toggleRole() },
                                onToggleLanguage = { viewModel.toggleLanguage() },
                                onStartNewRequest = { catId -> viewModel.startNewRequestFlow(catId) },
                                onOpenRequestDetails = { reqId -> viewModel.openRequestDetails(reqId) },
                                onOpenLiveTracking = { job -> viewModel.openLiveTracking(job) },
                                onSubmitRating = { jobId, rating, comment -> viewModel.submitCustomerRating(jobId, rating, comment) },
                                onReportIssue = { jobId, category, description -> viewModel.reportCustomerIssue(jobId, category, description) },
                                onAutoCompleteJob = { jobId -> viewModel.autoCompleteJobWithoutRating(jobId) },
                                onUpdateProfile = { name, cityArea, savedAddresses -> viewModel.updateCustomerProfile(name, cityArea, savedAddresses) },
                                onLogout = { viewModel.logout() }
                            )
                        }

                        AppNavDestination.PROVIDER_HOME -> {
                            val provider = currentUser ?: com.example.data.db.UserEntity(
                                phone = currentPhoneNumber.ifBlank { "" },
                                role = "PROVIDER",
                                name = "Service Provider",
                                cityArea = "",
                                isOnline = true
                            )
                            ProviderHomeScreen(
                                provider = provider,
                                incomingJobs = availableJobs,
                                activeJob = providerActiveJob,
                                pastJobs = providerPastJobs,
                                completedJobs = providerCompletedJobs,
                                language = language,
                                isRefreshingStatus = isRefreshingStatus,
                                statusCheckMessage = statusCheckMessage,
                                providerActionError = providerActionError,
                                onClearProviderActionError = { viewModel.clearProviderActionError() },
                                onToggleRole = { viewModel.toggleRole() },
                                onToggleLanguage = { viewModel.toggleLanguage() },
                                onToggleOnline = { online -> viewModel.toggleProviderOnline(online) },
                                onCheckVerificationStatus = { viewModel.refreshProviderStatus() },
                                onClearStatusMessage = { viewModel.clearStatusCheckMessage() },
                                jobWonConfirmation = providerJobWonConfirmation,
                                onDismissJobWonConfirmation = { viewModel.clearProviderJobWonConfirmation() },
                                onAcceptJob = { job -> viewModel.acceptJobAsProvider(job) },
                                onRejectJob = { job -> viewModel.rejectJobAsProvider(job) },
                                onCounterJob = { job, counterPrice, note -> viewModel.counterJobAsProvider(job, counterPrice, note) },
                                onStartTrip = { job -> viewModel.startProviderJobTrip(job) },
                                onArrived = { job -> viewModel.markProviderJobArrived(job) },
                                onStartWork = { job -> viewModel.startProviderJobWork(job) },
                                onCompleteActiveJob = { jobId -> viewModel.completeActiveJob(jobId) },
                                onOpenChat = { job -> viewModel.openJobChat(job) },
                                onStartCall = { job -> viewModel.startVoiceCall(job) },
                                onUpdateProfile = { name, cityArea, categoriesCsv, exp, radiusKm, bio, shopName, payoutMethod, payoutAccountNumber ->
                                    viewModel.updateProviderProfile(name, cityArea, categoriesCsv, exp, radiusKm, bio, shopName, payoutMethod, payoutAccountNumber)
                                },
                                onCancelJob = { job, reason -> viewModel.cancelJobByProvider(job, reason) },
                                onLogout = { viewModel.logout() }
                            )
                        }

                        AppNavDestination.CUSTOMER_REQUEST_FLOW -> {
                            val user = currentUser
                            CustomerRequestFlowScreen(
                                initialCategoryId = initialCategoryForRequest,
                                customerPhone = user?.phone ?: currentPhoneNumber,
                                customerName = user?.name ?: "Customer",
                                savedAddress = user?.homeAddress ?: "",
                                cityArea = user?.cityArea ?: "",
                                initialLat = user?.lat,
                                initialLng = user?.lng,
                                language = language,
                                activeLiveRequest = activeLiveRequest,
                                incomingOffers = incomingOffers,
                                isSubmitting = isSubmittingRequest,
                                submissionError = requestSubmissionError,
                                onDismissError = { viewModel.clearRequestSubmissionError() },
                                onBack = { viewModel.navigateBack() },
                                onSubmitRequest = { req -> viewModel.submitServiceRequest(req) },
                                onSelectOffer = { offer -> viewModel.selectOfferForRequest(offer) },
                                onDoneViewingConfirmed = { viewModel.navigateToHome() },
                                onOpenLiveTracking = { job -> viewModel.openLiveTracking(job) },
                                onCancelRequest = { job, reason -> viewModel.cancelJobByCustomer(job, reason) }
                            )
                        }

                        AppNavDestination.PROVIDER_JOB_ACCEPT -> {
                            val pingJob = fullscreenPingJob ?: availableJobs.firstOrNull()
                            if (pingJob != null) {
                                ProviderJobAcceptScreen(
                                    job = pingJob,
                                    language = language,
                                    actionError = providerActionError,
                                    onDismissError = { viewModel.clearProviderActionError() },
                                    onAccept = { job -> viewModel.acceptJobAsProvider(job) },
                                    onReject = { job -> viewModel.rejectJobAsProvider(job) },
                                    onCounter = { job, price, note -> viewModel.counterJobAsProvider(job, price, note) }
                                )
                            } else {
                                viewModel.navigateToHome()
                            }
                        }

                        AppNavDestination.CUSTOMER_LIVE_TRACKING -> {
                            val job = trackingJob ?: customerRequests.firstOrNull { it.status in listOf("ACCEPTED", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS") }
                            if (job != null) {
                                LiveTrackingMapScreen(
                                    job = job,
                                    viewModel = viewModel,
                                    theme = currentTheme,
                                    language = language,
                                    onBack = { viewModel.navigateBack() },
                                    onOpenChat = { viewModel.openJobChat(job) },
                                    onStartCall = { viewModel.startVoiceCall(job) }
                                )
                            } else {
                                viewModel.navigateToHome()
                            }
                        }

                        AppNavDestination.JOB_CHAT -> {
                            val job = activeChatJob
                            if (job != null) {
                                JobChatScreen(
                                    job = job,
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() },
                                    onStartCall = { viewModel.startVoiceCall(job) }
                                )
                            } else {
                                viewModel.navigateBack()
                            }
                        }

                        AppNavDestination.IN_CALL -> {
                            InCallScreen(
                                viewModel = viewModel,
                                onMinimize = { viewModel.closeCallScreen() },
                                onCallClosed = { viewModel.closeCallScreen() }
                            )
                        }
                    }

                    incomingCall?.let { callInfo ->
                        val context = LocalContext.current
                        DisposableEffect(callInfo.callSessionId) {
                            var ringtone: android.media.Ringtone? = null
                            try {
                                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                                ringtone = android.media.RingtoneManager.getRingtone(context, uri)
                                ringtone?.play()
                            } catch (_: Exception) {}
                            onDispose {
                                try {
                                    ringtone?.stop()
                                } catch (_: Exception) {}
                            }
                        }

                        IncomingCallDialog(
                            callInfo = callInfo,
                            language = language,
                            onAccept = { viewModel.acceptIncomingCall(callInfo) },
                            onDecline = { viewModel.declineIncomingCall(callInfo) }
                        )
                    }

                    inAppMessageNotification?.let { notif ->
                        com.example.ui.components.InAppMessageBanner(
                            notification = notif,
                            language = language,
                            onOpenChat = {
                                viewModel.openChatForJobId(notif.jobId)
                            },
                            onDismiss = {
                                viewModel.dismissInAppMessageNotification()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingCallDialog(
    callInfo: IncomingCallInfo,
    language: AppLanguage,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E293B),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top caller avatar with pulsating ring
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size((80 * pulseScale).dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C759).copy(alpha = 0.25f))
                    )
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Caller name & role
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = callInfo.callerName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = callInfo.callerRole,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (language == AppLanguage.URDU) "آنے والی وائس کال..." else "Incoming Voice Call...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF34C759),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions: Decline (Red) and Accept (Green)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onDecline,
                            modifier = Modifier.size(60.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "مسترد کریں" else "Decline",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Accept
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onAccept,
                            modifier = Modifier.size(60.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF34C759),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Accept",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == AppLanguage.URDU) "قبول کریں" else "Accept",
                            color = Color(0xFF34C759),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun LaunchedEffectOnce(key: Any?, block: suspend () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key) {
        block()
    }
}
