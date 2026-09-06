package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.db.UserEntity
import com.example.ui.screens.AuthChoiceScreen
import com.example.ui.screens.CustomerHomeScreen
import com.example.ui.screens.CustomerRegistrationScreen
import com.example.ui.screens.CustomerRequestFlowScreen
import com.example.ui.screens.LanguageSelectScreen
import com.example.ui.screens.OtpVerificationScreen
import com.example.ui.screens.PhoneEntryScreen
import com.example.ui.screens.ProviderHomeScreen
import com.example.ui.screens.ProviderJobAcceptScreen
import com.example.ui.screens.ProviderRegistrationScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.HomEaseTheme
import com.example.ui.viewmodel.AppNavDestination
import com.example.ui.viewmodel.HomeaseViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: HomeaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomEaseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundLight
                ) {
                    HomEaseApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun HomEaseApp(viewModel: HomeaseViewModel) {
    val destination by viewModel.currentDestination.collectAsState()
    val language by viewModel.language.collectAsState()
    val activeRole by viewModel.activeRole.collectAsState()
    val isSignInMode by viewModel.isSignInMode.collectAsState()
    val currentPhoneNumber by viewModel.currentPhoneNumber.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeLiveRequest by viewModel.activeLiveRequest.collectAsState()
    val initialCatId by viewModel.initialCategoryForRequest.collectAsState()
    val incomingOffers by viewModel.incomingOffers.collectAsState()
    val customerRequests by viewModel.customerRequests.collectAsState()
    val customerAwaitingRatingJob by viewModel.customerAwaitingRatingJob.collectAsState()
    val availableJobs by viewModel.availableJobs.collectAsState()
    val providerActiveJob by viewModel.providerActiveJob.collectAsState()
    val providerPastJobs by viewModel.providerPastJobs.collectAsState()
    val providerCompletedJobs by viewModel.providerCompletedJobs.collectAsState()
    val fullscreenPingJob by viewModel.fullscreenPingJob.collectAsState()
    val isSendingOtp by viewModel.isSendingOtp.collectAsState()
    val otpSendError by viewModel.otpSendError.collectAsState()
    val isVerifyingOtp by viewModel.isVerifyingOtp.collectAsState()
    val otpVerifyError by viewModel.otpVerifyError.collectAsState()

    val fallbackCustomer = UserEntity(
        phone = currentPhoneNumber,
        role = "CUSTOMER",
        name = "Adnan Shah",
        cityArea = "Lahore - Gulberg III",
        homeAddress = "House 42-B, Main Boulevard, Gulberg III",
        status = "ACTIVE"
    )

    val fallbackProvider = UserEntity(
        phone = currentPhoneNumber,
        role = "PROVIDER",
        name = "Ustad Muhammad Rashid",
        cityArea = "Lahore - Gulberg II",
        categoriesCsv = "plumbing,electrical",
        yearsExperience = "8 years",
        serviceRadiusKm = 12,
        cnicNumber = "35201-8492019-3",
        shopName = "Rashid Sanitary Works",
        status = "PENDING",
        isOnline = true
    )

    when (destination) {
        AppNavDestination.SPLASH -> {
            SplashScreen(
                language = language,
                onTimeout = { viewModel.onSplashFinished() }
            )
        }

        AppNavDestination.LANGUAGE_SELECT -> {
            LanguageSelectScreen(
                currentLanguage = language,
                onLanguageSelected = { viewModel.onLanguageSelected(it) },
                onSkip = { viewModel.onSkipLanguage() }
            )
        }

        AppNavDestination.ROLE_SELECT -> {
            RoleSelectionScreen(
                language = language,
                onRoleSelected = { viewModel.selectRole(it) }
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
                onComplete = { user -> viewModel.completeProviderRegistration(user) }
            )
        }

        AppNavDestination.CUSTOMER_HOME -> {
            CustomerHomeScreen(
                user = currentUser ?: fallbackCustomer,
                activeRequests = customerRequests,
                awaitingRatingJob = customerAwaitingRatingJob,
                language = language,
                onToggleRole = { viewModel.toggleRole() },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onStartNewRequest = { catId -> viewModel.startNewRequestFlow(catId) },
                onOpenRequestDetails = { reqId -> viewModel.openRequestDetails(reqId) },
                onSubmitRating = { jobId, rating, comment ->
                    viewModel.submitCustomerRating(jobId, rating, comment)
                },
                onReportIssue = { jobId, category, description ->
                    viewModel.reportCustomerIssue(jobId, category, description)
                },
                onAutoCompleteJob = { jobId ->
                    viewModel.autoCompleteJobWithoutRating(jobId)
                },
                onUpdateProfile = { name, cityArea, notifPref, savedAddresses ->
                    viewModel.updateCustomerProfile(name, cityArea, notifPref, savedAddresses)
                },
                onLogout = { viewModel.logout() }
            )
        }

        AppNavDestination.PROVIDER_HOME -> {
            val currentProvider = currentUser ?: fallbackProvider
            val isApproved = currentProvider.status == "APPROVED"
            ProviderHomeScreen(
                provider = currentProvider,
                incomingJobs = if (isApproved) availableJobs else emptyList(),
                activeJob = if (isApproved) providerActiveJob else null,
                pastJobs = providerPastJobs,
                completedJobs = providerCompletedJobs,
                language = language,
                onToggleRole = { viewModel.toggleRole() },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onToggleOnline = { isOnline -> viewModel.toggleProviderOnline(isOnline) },
                onToggleVerification = { viewModel.toggleProviderVerificationStatus() },
                onAcceptJob = { job -> viewModel.acceptJobAsProvider(job) },
                onRejectJob = { job -> viewModel.rejectJobAsProvider(job) },
                onCounterJob = { job, counterPrice, note -> viewModel.counterJobAsProvider(job, counterPrice, note) },
                onCompleteActiveJob = { jobId -> viewModel.completeActiveJob(jobId) },
                onUpdateProfile = { name, cityArea, categoriesCsv, yearsExp, radius, bio, shopName, payoutMethod, payoutAcc ->
                    viewModel.updateProviderProfile(
                        name, cityArea, categoriesCsv, yearsExp, radius, bio, shopName, payoutMethod, payoutAcc
                    )
                },
                onLogout = { viewModel.logout() }
            )
        }

        AppNavDestination.CUSTOMER_REQUEST_FLOW -> {
            CustomerRequestFlowScreen(
                initialCategoryId = initialCatId,
                customerPhone = currentUser?.phone ?: currentPhoneNumber,
                customerName = currentUser?.name ?: "Adnan Shah",
                savedAddress = currentUser?.homeAddress ?: "House 42-B, Main Boulevard, Gulberg III, Lahore",
                cityArea = currentUser?.cityArea ?: "Lahore - Gulberg",
                language = language,
                activeLiveRequest = activeLiveRequest,
                incomingOffers = incomingOffers,
                onBack = { viewModel.navigateBack() },
                onSubmitRequest = { req -> viewModel.submitServiceRequest(req) },
                onSelectOffer = { offer -> viewModel.selectOfferForRequest(offer) },
                onDoneViewingConfirmed = { viewModel.navigateToHome() }
            )
        }

        AppNavDestination.PROVIDER_JOB_ACCEPT -> {
            val pingJob = fullscreenPingJob ?: availableJobs.firstOrNull()
            if (pingJob != null) {
                ProviderJobAcceptScreen(
                    job = pingJob,
                    language = language,
                    onAccept = { job -> viewModel.acceptJobAsProvider(job) },
                    onReject = { job -> viewModel.rejectJobAsProvider(job) },
                    onCounter = { job, price, note -> viewModel.counterJobAsProvider(job, price, note) }
                )
            } else {
                viewModel.navigateToHome()
            }
        }
    }
}
