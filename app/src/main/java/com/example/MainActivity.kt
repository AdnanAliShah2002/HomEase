package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.localization.AppLanguage
import com.example.data.model.UserRole
import com.example.ui.screens.*
import com.example.ui.theme.HomEaseTheme
import com.example.ui.viewmodel.AppNavDestination
import com.example.ui.viewmodel.HomeaseViewModel

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
                                onOpenLiveTracking = { job -> viewModel.openLiveTracking(job) }
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
