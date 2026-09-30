package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.JobMessageEntity
import com.example.data.db.JobOfferEntity
import com.example.data.db.ServiceRequestEntity
import com.example.data.db.UserEntity
import com.example.data.localization.AppLanguage
import com.example.data.model.JobMessage
import com.example.data.model.MobileAppTheme
import com.example.data.model.ProviderLocation
import com.example.data.model.UserRole
import com.example.data.remote.CategoryDetectionRemoteService
import com.example.data.remote.CategoryDetectionResult
import com.example.data.remote.HomEaseSupabaseClient
import com.example.data.remote.OtpRemoteService
import com.example.data.remote.ProviderRemoteService
import com.example.data.remote.RealtimeChannel
import com.example.data.remote.SupabaseSession
import com.example.data.repository.HomeaseRepository
import com.example.data.theme.LocalThemeStore
import com.example.service.AgoraVoiceManager
import com.example.service.CallState
import com.example.service.ProviderLocationService
import com.example.util.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import com.google.firebase.messaging.FirebaseMessaging

data class IncomingCallInfo(
    val jobId: String,
    val channelName: String,
    val callerName: String,
    val callerRole: String,
    val callerPhone: String,
    val callSessionId: String = ""
)

data class InAppMessageNotification(
    val id: String = UUID.randomUUID().toString(),
    val jobId: String,
    val senderName: String,
    val senderRole: String,
    val messageText: String,
    val timestampMs: Long = System.currentTimeMillis()
)

enum class AppNavDestination {
    SPLASH,
    LANGUAGE_SELECT,
    ROLE_SELECT,
    AUTH_CHOICE,
    PHONE_ENTRY,
    OTP_VERIFICATION,
    CUSTOMER_REGISTRATION,
    PROVIDER_REGISTRATION,
    CUSTOMER_HOME,
    PROVIDER_HOME,
    CUSTOMER_REQUEST_FLOW,
    PROVIDER_JOB_ACCEPT,
    CUSTOMER_LIVE_TRACKING,
    JOB_CHAT,
    IN_CALL
}

class HomeaseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HomeaseRepository(AppDatabase.getDatabase(application))
    private val sessionManager = SessionManager(application)
    private val otpService = OtpRemoteService()
    private val providerRemoteService = ProviderRemoteService(application)
    private val supabaseClient = HomEaseSupabaseClient.getInstance(application)
    private val categoryDetectionService = CategoryDetectionRemoteService()
    private val localThemeStore = LocalThemeStore(application)
    private val agoraVoiceManager = AgoraVoiceManager.getInstance(application)

    val callState: StateFlow<CallState> = agoraVoiceManager.callState

    private val _incomingCall = MutableStateFlow<IncomingCallInfo?>(null)
    val incomingCall: StateFlow<IncomingCallInfo?> = _incomingCall.asStateFlow()

    private val _inAppMessageNotification = MutableStateFlow<InAppMessageNotification?>(null)
    val inAppMessageNotification: StateFlow<InAppMessageNotification?> = _inAppMessageNotification.asStateFlow()

    private val _activeChatJob = MutableStateFlow<ServiceRequestEntity?>(null)
    val activeChatJob: StateFlow<ServiceRequestEntity?> = _activeChatJob.asStateFlow()

    // Dynamic Remote Theming State
    private val _currentTheme = MutableStateFlow<MobileAppTheme>(
        localThemeStore.getCachedTheme() ?: MobileAppTheme.DEFAULT_FALLBACK_THEME
    )
    val currentTheme: StateFlow<MobileAppTheme> = _currentTheme.asStateFlow()

    private val _currentDestination = MutableStateFlow(AppNavDestination.SPLASH)
    val currentDestination: StateFlow<AppNavDestination> = _currentDestination.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _activeRole = MutableStateFlow(UserRole.CUSTOMER)
    val activeRole: StateFlow<UserRole> = _activeRole.asStateFlow()

    private val _isSignInMode = MutableStateFlow(false)
    val isSignInMode: StateFlow<Boolean> = _isSignInMode.asStateFlow()

    private val _currentPhoneNumber = MutableStateFlow("")
    val currentPhoneNumber: StateFlow<String> = _currentPhoneNumber.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // OTP Network States
    private val _isSendingOtp = MutableStateFlow(false)
    val isSendingOtp: StateFlow<Boolean> = _isSendingOtp.asStateFlow()

    private val _otpSendError = MutableStateFlow<String?>(null)
    val otpSendError: StateFlow<String?> = _otpSendError.asStateFlow()

    private val _isVerifyingOtp = MutableStateFlow(false)
    val isVerifyingOtp: StateFlow<Boolean> = _isVerifyingOtp.asStateFlow()

    private val _otpVerifyError = MutableStateFlow<String?>(null)
    val otpVerifyError: StateFlow<String?> = _otpVerifyError.asStateFlow()

    // Active customer request being created or viewed
    private val _activeLiveRequest = MutableStateFlow<ServiceRequestEntity?>(null)
    val activeLiveRequest: StateFlow<ServiceRequestEntity?> = _activeLiveRequest.asStateFlow()

    private val _initialCategoryForRequest = MutableStateFlow<String?>(null)
    val initialCategoryForRequest: StateFlow<String?> = _initialCategoryForRequest.asStateFlow()

    private val _incomingOffers = MutableStateFlow<List<JobOfferEntity>>(emptyList())
    val incomingOffers: StateFlow<List<JobOfferEntity>> = _incomingOffers.asStateFlow()

    // Full-screen incoming ping job for Provider
    private val _fullscreenPingJob = MutableStateFlow<ServiceRequestEntity?>(null)
    val fullscreenPingJob: StateFlow<ServiceRequestEntity?> = _fullscreenPingJob.asStateFlow()

    // Provider notification when their offer or application wins/gets accepted
    private val _providerJobWonConfirmation = MutableStateFlow<ServiceRequestEntity?>(null)
    val providerJobWonConfirmation: StateFlow<ServiceRequestEntity?> = _providerJobWonConfirmation.asStateFlow()
    private val acknowledgedJobWonIds = mutableSetOf<String>()

    fun clearProviderJobWonConfirmation() {
        _providerJobWonConfirmation.value?.remoteId?.let { acknowledgedJobWonIds.add(it) }
        _providerJobWonConfirmation.value = null
    }

    // Request Submission State (Customer)
    private val _isSubmittingRequest = MutableStateFlow(false)
    val isSubmittingRequest: StateFlow<Boolean> = _isSubmittingRequest.asStateFlow()

    private val _requestSubmissionError = MutableStateFlow<String?>(null)
    val requestSubmissionError: StateFlow<String?> = _requestSubmissionError.asStateFlow()

    // Provider Action State (Accept / Counter)
    private val _isProviderActionLoading = MutableStateFlow(false)
    val isProviderActionLoading: StateFlow<Boolean> = _isProviderActionLoading.asStateFlow()

    private val _providerActionError = MutableStateFlow<String?>(null)
    val providerActionError: StateFlow<String?> = _providerActionError.asStateFlow()

    private val _fcmToken = MutableStateFlow("")
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()

    /**
     * Fetches real token from FirebaseMessaging.getInstance().token, caches it, and registers device.
     */
    fun fetchFcmTokenAndRegister() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isNullOrBlank()) {
                    val token = task.result
                    val prefs = getApplication<Application>().getSharedPreferences("homease_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("fcm_token", token).apply()
                    updateFcmToken(token)
                }
            }
        } catch (_: Exception) {
            // Best effort if Google Play Services / Firebase is not initialized
        }
    }

    fun updateFcmToken(token: String) {
        _fcmToken.value = token
        viewModelScope.launch {
            val phone = _currentPhoneNumber.value
            val role = if (_activeRole.value == UserRole.PROVIDER) "provider" else "customer"
            if (phone.isNotBlank()) {
                val user = _currentUser.value ?: repository.getUser(phone)
                val categories = user?.categoriesCsv?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                val isOnline = user?.isOnline ?: true
                supabaseClient.registerDeviceToken(
                    phone = phone,
                    role = role,
                    fcmToken = token,
                    categories = categories,
                    isOnline = isOnline
                )
            }
        }
    }

    fun handleNotificationClick(jobId: String, notificationType: String?) {
        viewModelScope.launch {
            var localJob = repository.getRequestByRemoteId(jobId) ?: repository.getRequestById(jobId.toLongOrNull() ?: -1L)

            // If job isn't in local Room DB yet, fetch from Supabase and sync
            if (localJob == null && jobId.isNotBlank()) {
                try {
                    val result = supabaseClient.getJobById(jobId)
                    if (result.isSuccess) {
                        result.getOrNull()?.let { remoteObj ->
                            val entity = jsonToServiceRequestEntity(remoteObj)
                            val localId = repository.syncRemoteJob(entity)
                            localJob = entity.copy(id = localId)
                        }
                    }
                } catch (_: Exception) {}
            }

            if (localJob != null) {
                when (notificationType) {
                    "job_ping" -> {
                        _fullscreenPingJob.value = localJob
                        _currentDestination.value = AppNavDestination.PROVIDER_JOB_ACCEPT
                    }
                    "offer_received" -> {
                        openRequestDetails(localJob!!.id)
                    }
                    "offer_accepted", "status_update" -> {
                        openLiveTracking(localJob!!)
                    }
                    "new_message" -> {
                        openJobChat(localJob!!)
                    }
                    else -> {
                        openLiveTracking(localJob!!)
                    }
                }
            }
        }
    }

    private val _providerRegistrationLoading = MutableStateFlow(false)
    val providerRegistrationLoading: StateFlow<Boolean> = _providerRegistrationLoading.asStateFlow()

    private val _providerRegistrationError = MutableStateFlow<String?>(null)
    val providerRegistrationError: StateFlow<String?> = _providerRegistrationError.asStateFlow()

    private val _isRefreshingStatus = MutableStateFlow(false)
    val isRefreshingStatus: StateFlow<Boolean> = _isRefreshingStatus.asStateFlow()

    private val _statusCheckMessage = MutableStateFlow<String?>(null)
    val statusCheckMessage: StateFlow<String?> = _statusCheckMessage.asStateFlow()

    fun clearProviderRegistrationError() {
        _providerRegistrationError.value = null
    }

    fun clearStatusCheckMessage() {
        _statusCheckMessage.value = null
    }

    fun clearRequestSubmissionError() {
        _requestSubmissionError.value = null
    }

    fun clearProviderActionError() {
        _providerActionError.value = null
    }

    // Active job being tracked on the Live Map (Customer or Provider view)
    private val _trackingJob = MutableStateFlow<ServiceRequestEntity?>(null)
    val trackingJob: StateFlow<ServiceRequestEntity?> = _trackingJob.asStateFlow()

    // Customer live requests
    @OptIn(ExperimentalCoroutinesApi::class)
    val customerRequests = _currentPhoneNumber.flatMapLatest { phone ->
        if (phone.isNotBlank()) repository.getCustomerRequests(phone) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Provider available jobs (filtered by provider's configured service radius & location)
    val availableJobs = combine(
        repository.getAvailableJobs(),
        _currentUser
    ) { jobs, user ->
        if (user == null || user.role != "PROVIDER") {
            jobs
        } else {
            val providerRadiusKm = if (user.serviceRadiusKm > 0) user.serviceRadiusKm.toDouble() else 15.0
            val providerLat = user.lat
            val providerLng = user.lng
            val providerCityArea = user.cityArea.trim().lowercase()

            jobs.filter { job ->
                val jobLat = job.lat
                val jobLng = job.lng

                if (providerLat != null && providerLng != null && jobLat != null && jobLng != null) {
                    // Haversine distance in kilometers
                    val earthRadiusKm = 6371.0
                    val dLat = Math.toRadians(jobLat - providerLat)
                    val dLng = Math.toRadians(jobLng - providerLng)
                    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                            Math.cos(Math.toRadians(providerLat)) * Math.cos(Math.toRadians(jobLat)) *
                            Math.sin(dLng / 2) * Math.sin(dLng / 2)
                    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                    val distanceKm = earthRadiusKm * c
                    distanceKm <= providerRadiusKm
                } else if (providerCityArea.isNotBlank() && job.cityArea.isNotBlank()) {
                    // Fallback to city/area match if GPS coordinates are not yet available
                    val jobArea = job.cityArea.trim().lowercase()
                    jobArea.contains(providerCityArea) || providerCityArea.contains(jobArea)
                } else {
                    true
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Provider active job
    @OptIn(ExperimentalCoroutinesApi::class)
    val providerActiveJob = _currentPhoneNumber.flatMapLatest { phone ->
        if (phone.isNotBlank()) repository.getActiveJobForProvider(phone) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Provider past / completed jobs (including awaiting rating)
    @OptIn(ExperimentalCoroutinesApi::class)
    val providerPastJobs = _currentPhoneNumber.flatMapLatest { phone ->
        if (phone.isNotBlank()) repository.getPastJobsForProvider(phone) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Provider completed jobs
    @OptIn(ExperimentalCoroutinesApi::class)
    val providerCompletedJobs = _currentPhoneNumber.flatMapLatest { phone ->
        if (phone.isNotBlank()) repository.getCompletedJobsForProvider(phone) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customer job awaiting rating / confirmation
    @OptIn(ExperimentalCoroutinesApi::class)
    val customerAwaitingRatingJob = _currentPhoneNumber.flatMapLatest { phone ->
        if (phone.isNotBlank()) repository.getAwaitingConfirmationForCustomer(phone) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Initialize token from SharedPreferences or generate stable local device token if FCM is unavailable
        val prefs = getApplication<Application>().getSharedPreferences("homease_prefs", android.content.Context.MODE_PRIVATE)
        val existingToken = prefs.getString("fcm_token", null) ?: run {
            val fallback = "dev_token_" + UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString("fcm_token", fallback).apply()
            fallback
        }
        _fcmToken.value = existingToken
        fetchFcmTokenAndRegister()

        viewModelScope.launch {
            refreshActiveTheme()
        }

        // Auto-subscribe to call signals for any active job (provider or customer)
        viewModelScope.launch {
            providerActiveJob.collect { job ->
                val rId = job?.remoteId
                if (!rId.isNullOrBlank()) {
                    watchCallSignals(rId)
                }
            }
        }
        viewModelScope.launch {
            customerRequests.collect { list ->
                val job = list.firstOrNull { it.status in listOf("ACCEPTED", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS") }
                val rId = job?.remoteId
                if (!rId.isNullOrBlank()) {
                    watchCallSignals(rId)
                }
            }
        }

        // Broadcast END signal whenever call ends or errors out
        viewModelScope.launch {
            agoraVoiceManager.callState.collect { state ->
                if (state is CallState.Ended) {
                    val sessionId = activeCallSessionId
                    if (!sessionId.isNullOrBlank()) {
                        endedCallSessionIds.add(sessionId)
                        val channelName = "job_${state.jobId.replace("-", "").take(16)}"
                        val currentPhone = _currentUser.value?.phone ?: _currentPhoneNumber.value
                        val senderType = if (_activeRole.value == UserRole.PROVIDER) "provider" else "customer"
                        val signalPayload = "[[CALL_SIGNAL:END:$channelName:$currentPhone:$sessionId]]"
                        if (state.jobId.isNotBlank()) {
                            supabaseClient.sendJobMessage(state.jobId, currentPhone, senderType, signalPayload)
                        }
                    }
                }
            }
        }

        // Continuous customer active jobs sync loop (detects provider arrival, cancellation, completion)
        viewModelScope.launch {
            while (isActive) {
                try {
                    val activeJobs = customerRequests.value.filter {
                        it.status in listOf("SEARCHING", "ACCEPTED", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS")
                    }
                    for (job in activeJobs) {
                        val rId = job.remoteId ?: continue
                        val jobRes = supabaseClient.getJobById(rId)
                        if (jobRes.isSuccess) {
                            val jobJson = jobRes.getOrNull()
                            if (jobJson != null) {
                                val remoteStatus = jobJson.optString("status", "").uppercase()
                                if (remoteStatus == "CANCELLED" && job.status != "CANCELLED") {
                                    val cancelledBy = jobJson.optString("cancelled_by").ifBlank { "provider" }
                                    val reason = jobJson.optString("cancellation_reason").ifBlank { "Cancelled" }
                                    repository.cancelJobByRemoteId(rId, cancelledBy, reason)
                                    if (job.id > 0) {
                                        repository.cancelJob(job.id, cancelledBy, reason)
                                    }
                                    if (_trackingJob.value?.remoteId == rId || _trackingJob.value?.id == job.id) {
                                        _trackingJob.value = null
                                    }
                                    if (_activeLiveRequest.value?.remoteId == rId || _activeLiveRequest.value?.id == job.id) {
                                        _activeLiveRequest.value = null
                                    }
                                    if (_currentDestination.value == AppNavDestination.CUSTOMER_LIVE_TRACKING) {
                                        _currentDestination.value = AppNavDestination.CUSTOMER_HOME
                                    }
                                } else if (remoteStatus.isNotBlank() && remoteStatus != job.status) {
                                    val entity = jsonToServiceRequestEntity(jobJson).copy(id = job.id)
                                    repository.syncRemoteJob(entity)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(2500L)
            }
        }
    }

    /**
     * Refreshes active theme from Supabase with local caching and offline fallback.
     */
    fun refreshActiveTheme() {
        viewModelScope.launch {
            val theme = fetchActiveTheme(supabaseClient)
            if (theme != null) {
                _currentTheme.value = theme
            }
        }
    }

    /**
     * Fetches active theme from Supabase with local caching fallback matching the requested signature.
     */
    suspend fun fetchActiveTheme(client: HomEaseSupabaseClient): MobileAppTheme? {
        return try {
            val theme = client.fetchActiveThemeRemote()
            theme?.let { localThemeStore.saveTheme(it) }
            theme ?: localThemeStore.getCachedTheme() ?: MobileAppTheme.DEFAULT_FALLBACK_THEME
        } catch (e: Exception) {
            localThemeStore.getCachedTheme() ?: MobileAppTheme.DEFAULT_FALLBACK_THEME
        }
    }

    fun setLanguage(newLanguage: AppLanguage) {
        _language.value = newLanguage
    }

    fun toggleLanguage() {
        _language.value = if (_language.value == AppLanguage.ENGLISH) AppLanguage.URDU else AppLanguage.ENGLISH
    }

    fun selectRole(role: UserRole) {
        _activeRole.value = role
        _currentDestination.value = AppNavDestination.AUTH_CHOICE
    }

    fun toggleRole() {
        val newRole = if (_activeRole.value == UserRole.CUSTOMER) UserRole.PROVIDER else UserRole.CUSTOMER
        _activeRole.value = newRole
        val phone = _currentPhoneNumber.value
        if (phone.isNotBlank()) {
            viewModelScope.launch {
                val user = repository.getUser(phone)
                if (user != null) {
                    _currentUser.value = user
                }
            }
            if (newRole == UserRole.PROVIDER) {
                watchProviderJobsAndOffers(phone)
            }
        }
        _currentDestination.value = if (newRole == UserRole.PROVIDER) {
            AppNavDestination.PROVIDER_HOME
        } else {
            AppNavDestination.CUSTOMER_HOME
        }
    }

    fun onSplashFinished() {
        viewModelScope.launch {
            val session = sessionManager.getSession()
            if (session != null) {
                val savedPhone = session.phone
                val savedRole = if (session.role == "PROVIDER") UserRole.PROVIDER else UserRole.CUSTOMER
                _activeRole.value = savedRole
                _currentPhoneNumber.value = savedPhone

                // Synchronize Supabase Auth session state
                val supabaseSession = supabaseClient.getSession()
                if (supabaseSession != null) {
                    val tokenValid = supabaseClient.isTokenValid(supabaseSession.accessToken)
                    val hasRefresh = !supabaseSession.refreshToken.isNullOrBlank()
                    if (!tokenValid && hasRefresh) {
                        try {
                            supabaseClient.refreshSession()
                        } catch (_: Exception) {}
                    }
                }

                val user = repository.getUser(savedPhone)
                if (user != null) {
                    _currentUser.value = user
                } else {
                    val defaultUser = UserEntity(
                        phone = savedPhone,
                        role = session.role,
                        name = if (savedRole == UserRole.PROVIDER) "Service Provider" else "Customer",
                        cityArea = "",
                        status = "ACTIVE"
                    )
                    repository.saveUser(defaultUser)
                    _currentUser.value = defaultUser
                }

                _currentDestination.value = if (savedRole == UserRole.PROVIDER) {
                    watchProviderJobsAndOffers(savedPhone)
                    // Resume open-jobs Realtime listener so new job pings arrive instantly
                    val wasOnline = user?.isOnline ?: true
                    if (wasOnline) {
                        toggleProviderOnline(true)
                    }
                    AppNavDestination.PROVIDER_HOME
                } else {
                    AppNavDestination.CUSTOMER_HOME
                }
            } else {
                _currentDestination.value = AppNavDestination.LANGUAGE_SELECT
            }
        }
    }

    fun onLanguageSelected(lang: AppLanguage) {
        _language.value = lang
        _currentDestination.value = AppNavDestination.ROLE_SELECT
    }

    fun onSkipLanguage() {
        _currentDestination.value = AppNavDestination.ROLE_SELECT
    }

    fun startCreateAccount() {
        _isSignInMode.value = false
        _otpSendError.value = null
        _currentDestination.value = AppNavDestination.PHONE_ENTRY
    }

    fun startSignIn() {
        _isSignInMode.value = true
        _otpSendError.value = null
        _currentDestination.value = AppNavDestination.PHONE_ENTRY
    }

    fun onSendCode(phone: String) {
        val formattedPhone = OtpRemoteService.normalizePhone(phone)
        _currentPhoneNumber.value = formattedPhone
        _isSendingOtp.value = true
        _otpSendError.value = null
        viewModelScope.launch {
            val result = otpService.sendOtp(formattedPhone)
            _isSendingOtp.value = false
            if (result.isSuccess) {
                _otpVerifyError.value = null
                _currentDestination.value = AppNavDestination.OTP_VERIFICATION
            } else {
                _otpSendError.value = result.exceptionOrNull()?.message ?: "Failed to send verification code via WhatsApp"
            }
        }
    }

    fun resendOtpCode() {
        val phone = OtpRemoteService.normalizePhone(_currentPhoneNumber.value)
        _isSendingOtp.value = true
        _otpVerifyError.value = null
        viewModelScope.launch {
            val result = otpService.sendOtp(phone)
            _isSendingOtp.value = false
            if (result.isFailure) {
                _otpVerifyError.value = result.exceptionOrNull()?.message ?: "Failed to resend code"
            }
        }
    }

    fun onOtpVerified(code: String) {
        val phone = OtpRemoteService.normalizePhone(_currentPhoneNumber.value)
        _isVerifyingOtp.value = true
        _otpVerifyError.value = null
        viewModelScope.launch {
            val result = otpService.verifyOtp(phone, code)
            _isVerifyingOtp.value = false
            if (result.isSuccess) {
                val verifyData = result.getOrNull()!!
                val sessionUserId = verifyData.userId ?: UUID.randomUUID().toString()
                val sessionToken = verifyData.accessToken
                val sessionRefreshToken = verifyData.refreshToken
                val finalToken = if (!sessionToken.isNullOrBlank() && supabaseClient.isTokenValid(sessionToken)) {
                    sessionToken
                } else {
                    HomEaseSupabaseClient.SUPABASE_ANON_KEY
                }
                supabaseClient.setSession(
                    SupabaseSession(
                        userId = sessionUserId,
                        phone = phone,
                        accessToken = finalToken,
                        role = _activeRole.value.name.lowercase(),
                        refreshToken = sessionRefreshToken
                    )
                )

                val localUser = repository.getUser(phone)
                val isProvider = _activeRole.value == UserRole.PROVIDER

                if (isProvider) {
                    // 1. Check remote service_providers table by phone (or sessionUserId as fallback)
                    val remoteProfileRes = supabaseClient.getProviderByPhone(phone)
                    var remoteProviderProfile = remoteProfileRes.getOrNull()
                    if (remoteProviderProfile == null && sessionUserId.isNotBlank()) {
                        remoteProviderProfile = supabaseClient.getProviderOwnProfile(sessionUserId).getOrNull()
                    }

                    val hasValidRemoteProfile = remoteProviderProfile != null &&
                            remoteProviderProfile.optString("full_name").isNotBlank() &&
                            remoteProviderProfile.optString("full_name") != "Service Provider"

                    val hasValidLocalProfile = localUser != null &&
                            localUser.role == "PROVIDER" &&
                            localUser.cnicNumber.isNotBlank() &&
                            localUser.cnicNumber != "PENDING" &&
                            localUser.name.isNotBlank() &&
                            localUser.name != "Service Provider"

                    val isProviderRegistered = hasValidRemoteProfile || hasValidLocalProfile

                    if (isProviderRegistered) {
                        // Provider has registered profile; sync status from remote if available
                        val remoteStatus = remoteProviderProfile?.optString("status")?.uppercase()
                        val currentStatus = remoteStatus ?: localUser?.status ?: "APPROVED"

                        // Map remote profile fields (bio, years_of_experience, service_categories, shop_name, etc.)
                        val remoteCategories = remoteProviderProfile?.optJSONArray("service_categories")?.let { arr ->
                            val list = mutableListOf<String>()
                            for (i in 0 until arr.length()) list.add(arr.getString(i))
                            list.joinToString(",")
                        } ?: localUser?.categoriesCsv ?: ""

                        val remoteExp = remoteProviderProfile?.optString("years_of_experience")?.ifBlank { null }
                            ?: localUser?.yearsExperience ?: ""
                        val remoteBio = remoteProviderProfile?.optString("bio")?.ifBlank { null }
                            ?: localUser?.bio ?: ""
                        val remoteShopName = remoteProviderProfile?.optString("business_name")?.ifBlank { null }
                            ?: localUser?.shopName ?: ""
                        val remoteCnic = remoteProviderProfile?.optString("cnic_number")?.ifBlank { null }
                            ?: localUser?.cnicNumber ?: ""
                        val remoteCity = remoteProviderProfile?.optString("city_area")?.ifBlank { null }
                            ?: localUser?.cityArea ?: "Islamabad"

                        val user = (localUser ?: UserEntity(
                            phone = phone,
                            role = "PROVIDER",
                            name = remoteProviderProfile?.optString("full_name")?.ifBlank { "Service Provider" } ?: "Service Provider",
                            cityArea = remoteCity,
                            status = currentStatus
                        )).copy(
                            status = currentStatus,
                            bio = remoteBio,
                            yearsExperience = remoteExp,
                            categoriesCsv = remoteCategories,
                            shopName = remoteShopName,
                            cnicNumber = remoteCnic,
                            cityArea = remoteCity
                        )

                        repository.saveUser(user)
                        _currentUser.value = user
                        sessionManager.saveSession(user.phone, "PROVIDER")
                        _activeRole.value = UserRole.PROVIDER
                        fetchFcmTokenAndRegister()
                        watchProviderJobsAndOffers(user.phone)
                        toggleProviderOnline(true)
                        _currentDestination.value = AppNavDestination.PROVIDER_HOME
                    } else {
                        // Incomplete or new provider: route to Provider Registration screen
                        fetchFcmTokenAndRegister()
                        _currentDestination.value = AppNavDestination.PROVIDER_REGISTRATION
                    }
                } else {
                    // Customer: check remote Supabase users table and jobs table first
                    val remoteUserResult = supabaseClient.getUserByPhone(phone)
                    val remoteUser = remoteUserResult.getOrNull()
                    val remoteName = remoteUser?.optString("full_name")?.ifBlank { null }
                    val remoteCity = remoteUser?.optString("city")?.ifBlank { null }

                    // Also check jobs table if customer posted jobs before
                    var lastJobName: String? = null
                    var lastJobCity: String? = null
                    if (remoteName.isNullOrBlank()) {
                        val lastJobResult = supabaseClient.getCustomerLastJob(phone)
                        val lastJob = lastJobResult.getOrNull()
                        lastJobName = lastJob?.optString("customer_name")?.ifBlank { null }
                        lastJobCity = lastJob?.optString("city_area")?.ifBlank { null }
                    }

                    val resolvedName = remoteName ?: lastJobName ?: localUser?.name?.takeIf {
                        it.isNotBlank() && it != "Valued Customer" && it != "Customer"
                    }
                    val resolvedCity = remoteCity ?: lastJobCity ?: localUser?.cityArea?.takeIf { it.isNotBlank() } ?: "Islamabad"

                    val hasExistingCustomerAccount = !resolvedName.isNullOrBlank()

                    if (hasExistingCustomerAccount) {
                        val customerUser = (localUser ?: UserEntity(
                            phone = phone,
                            role = "CUSTOMER",
                            name = resolvedName!!,
                            cityArea = resolvedCity,
                            status = "ACTIVE"
                        )).copy(
                            name = resolvedName!!,
                            cityArea = resolvedCity,
                            status = "ACTIVE"
                        )

                        repository.saveUser(customerUser)
                        _currentUser.value = customerUser
                        sessionManager.saveSession(phone, "CUSTOMER")
                        _activeRole.value = UserRole.CUSTOMER
                        fetchFcmTokenAndRegister()
                        _currentDestination.value = AppNavDestination.CUSTOMER_HOME
                    } else {
                        // Incomplete or new customer: route to Customer Registration screen
                        fetchFcmTokenAndRegister()
                        _currentDestination.value = AppNavDestination.CUSTOMER_REGISTRATION
                    }
                }
            } else {
                _otpVerifyError.value = result.exceptionOrNull()?.message ?: "Incorrect code. Please try again."
            }
        }
    }

    fun completeCustomerRegistration(user: UserEntity) {
        viewModelScope.launch {
            repository.saveUser(user)
            sessionManager.saveSession(user.phone, "CUSTOMER")
            _currentUser.value = user
            _activeRole.value = UserRole.CUSTOMER
            supabaseClient.upsertUser(user)
            fetchFcmTokenAndRegister()
            _currentDestination.value = AppNavDestination.CUSTOMER_HOME
        }
    }

    fun completeProviderRegistration(user: UserEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _providerRegistrationLoading.value = true
            _providerRegistrationError.value = null

            val pendingUser = user.copy(status = "PENDING")
            val result = providerRemoteService.submitProviderRegistration(pendingUser)

            _providerRegistrationLoading.value = false
            if (result.isSuccess) {
                repository.saveUser(pendingUser)
                sessionManager.saveSession(pendingUser.phone, "PROVIDER")
                _currentUser.value = pendingUser
                _activeRole.value = UserRole.PROVIDER
                fetchFcmTokenAndRegister()
                onSuccess()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Provider registration failed on server"
                _providerRegistrationError.value = error
            }
        }
    }

    fun proceedToProviderDashboard() {
        val phone = _currentPhoneNumber.value
        if (phone.isNotBlank()) {
            watchProviderJobsAndOffers(phone)
        }
        _currentDestination.value = AppNavDestination.PROVIDER_HOME
    }

    fun logout() {
        sessionManager.clearSession()
        supabaseClient.clearSession()
        acknowledgedJobWonIds.clear()
        _currentUser.value = null
        _currentDestination.value = AppNavDestination.ROLE_SELECT
    }

    suspend fun detectCategory(description: String): CategoryDetectionResult {
        return categoryDetectionService.detectCategory(description)
    }

    fun startNewRequestFlow(categoryId: String?) {
        _initialCategoryForRequest.value = categoryId
        _activeLiveRequest.value = null
        _incomingOffers.value = emptyList()
        _currentDestination.value = AppNavDestination.CUSTOMER_REQUEST_FLOW
    }

    fun openRequestDetails(requestId: Long) {
        viewModelScope.launch {
            val req = repository.getRequestById(requestId)
            if (req != null) {
                _activeLiveRequest.value = req
                _trackingJob.value = req
                repository.getOffersForRequest(requestId).collect { offers ->
                    _incomingOffers.value = offers
                }
            }
        }
        _currentDestination.value = AppNavDestination.CUSTOMER_REQUEST_FLOW
    }

    fun openLiveTracking(job: ServiceRequestEntity) {
        _trackingJob.value = job
        _activeLiveRequest.value = job
        _currentDestination.value = AppNavDestination.CUSTOMER_LIVE_TRACKING
        val rId = job.remoteId ?: job.id.toString()
        if (rId.isNotBlank()) {
            watchCallSignals(rId)
        }
    }

    fun openChat(job: ServiceRequestEntity) = openJobChat(job)

    fun openJobChat(job: ServiceRequestEntity) {
        _activeChatJob.value = job
        _currentDestination.value = AppNavDestination.JOB_CHAT
        val currentUserId = _currentUser.value?.phone ?: _currentPhoneNumber.value
        val canonicalJobId = job.remoteId ?: job.id.toString()
        watchCallSignals(canonicalJobId)
        viewModelScope.launch {
            repository.markMessagesAsRead(canonicalJobId, currentUserId)
            supabaseClient.markMessagesAsRead(canonicalJobId, currentUserId)
        }
    }

    fun dismissInAppMessageNotification() {
        _inAppMessageNotification.value = null
    }

    fun openChatForJobId(jobId: String) {
        _inAppMessageNotification.value = null
        viewModelScope.launch {
            val activeProvider = providerActiveJob.value
            val activeCustomer = customerRequests.value.firstOrNull { it.remoteId == jobId || it.id.toString() == jobId }
            val targetJob: ServiceRequestEntity? = if (activeProvider?.remoteId == jobId || activeProvider?.id.toString() == jobId) {
                activeProvider
            } else if (activeCustomer != null) {
                activeCustomer
            } else {
                repository.getRequestByRemoteId(jobId) ?: repository.getRequestById(jobId.toLongOrNull() ?: -1L)
            }
            if (targetJob != null) {
                openJobChat(targetJob)
            }
        }
    }

    private var activeWatchedCallSignalJobId: String? = null
    private var callSignalsJob: kotlinx.coroutines.Job? = null
    private var activeCallSessionId: String? = null
    private val handledSignalTexts = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val endedCallSessionIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun startVoiceCall(job: ServiceRequestEntity) {
        val currentPhone = _currentUser.value?.phone ?: _currentPhoneNumber.value
        val isProvider = _activeRole.value == UserRole.PROVIDER
        val targetName = if (isProvider) job.customerName.ifBlank { "Customer" } else (job.selectedProviderName ?: "Service Provider")
        val targetRole = if (isProvider) "Customer" else "Service Provider"
        val targetPhone = if (isProvider) job.customerPhone else (job.selectedProviderPhone ?: "")
        val canonicalJobId = job.remoteId ?: job.id.toString()
        val cleanJobId = canonicalJobId.replace("-", "").take(16)
        val channelName = "job_$cleanJobId"
        val callSessionId = "call_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        activeCallSessionId = callSessionId

        agoraVoiceManager.startCall(
            jobId = canonicalJobId,
            targetName = targetName,
            targetRole = targetRole,
            targetPhone = targetPhone,
            currentUserId = currentPhone,
            repository = repository,
            supabaseClient = supabaseClient
        )
        _currentDestination.value = AppNavDestination.IN_CALL

        // Send call signal to callee
        viewModelScope.launch {
            val callerName = _currentUser.value?.name ?: if (isProvider) "Service Provider" else "Customer"
            val nowMs = System.currentTimeMillis()
            val signalPayload = "[[CALL_SIGNAL:START:$channelName:$callerName:$currentPhone:$targetPhone:$callSessionId:$nowMs]]"
            val senderType = if (isProvider) "provider" else "customer"
            supabaseClient.sendJobMessage(canonicalJobId, currentPhone, senderType, signalPayload)
        }
    }

    fun acceptIncomingCall(info: IncomingCallInfo) {
        val currentPhone = _currentUser.value?.phone ?: _currentPhoneNumber.value
        activeCallSessionId = info.callSessionId
        _incomingCall.value = null
        agoraVoiceManager.joinIncomingCall(
            jobId = info.jobId,
            channelName = info.channelName,
            targetName = info.callerName,
            targetRole = info.callerRole,
            targetPhone = info.callerPhone,
            currentUserId = currentPhone,
            repository = repository,
            supabaseClient = supabaseClient
        )
        agoraVoiceManager.markRemoteUserConnected()
        _currentDestination.value = AppNavDestination.IN_CALL

        viewModelScope.launch {
            val senderType = if (_activeRole.value == UserRole.PROVIDER) "provider" else "customer"
            val signalPayload = "[[CALL_SIGNAL:ACCEPT:${info.channelName}:$currentPhone:${info.callSessionId}]]"
            supabaseClient.sendJobMessage(info.jobId, currentPhone, senderType, signalPayload)
        }
    }

    fun declineIncomingCall(info: IncomingCallInfo) {
        val currentPhone = _currentUser.value?.phone ?: _currentPhoneNumber.value
        if (info.callSessionId.isNotBlank()) {
            endedCallSessionIds.add(info.callSessionId)
        }
        _incomingCall.value = null
        viewModelScope.launch {
            val senderType = if (_activeRole.value == UserRole.PROVIDER) "provider" else "customer"
            val signalPayload = "[[CALL_SIGNAL:DECLINE:${info.channelName}:$currentPhone:${info.callSessionId}]]"
            supabaseClient.sendJobMessage(info.jobId, currentPhone, senderType, signalPayload)
        }
    }

    fun toggleCallMute() {
        agoraVoiceManager.toggleMute()
    }

    fun toggleCallSpeaker() {
        agoraVoiceManager.toggleSpeaker()
    }

    fun endVoiceCall() {
        val current = agoraVoiceManager.callState.value
        val jobId = when (current) {
            is CallState.Connecting -> current.jobId
            is CallState.Connected -> current.jobId
            is CallState.Ended -> current.jobId
            else -> null
        }
        val channelName = when (current) {
            is CallState.Connecting -> current.channelName
            is CallState.Connected -> current.channelName
            else -> null
        }
        val sessionId = activeCallSessionId.orEmpty()
        if (sessionId.isNotBlank()) {
            endedCallSessionIds.add(sessionId)
        }
        _incomingCall.value = null
        agoraVoiceManager.endCall(repository, supabaseClient)

        if (jobId != null && channelName != null) {
            viewModelScope.launch {
                val currentPhone = _currentUser.value?.phone ?: _currentPhoneNumber.value
                val senderType = if (_activeRole.value == UserRole.PROVIDER) "provider" else "customer"
                val signalPayload = "[[CALL_SIGNAL:END:$channelName:$currentPhone:$sessionId]]"
                supabaseClient.sendJobMessage(jobId, currentPhone, senderType, signalPayload)
            }
        }
    }

    fun closeCallScreen() {
        agoraVoiceManager.resetToIdle()
        navigateBack()
    }

    fun watchCallSignals(jobId: String) {
        if (jobId.isBlank()) return
        if (activeWatchedCallSignalJobId == jobId && callSignalsJob?.isActive == true) {
            // Already listening for this job's call signals
            return
        }
        activeWatchedCallSignalJobId = jobId
        callSignalsJob?.cancel()
        callSignalsJob = viewModelScope.launch {
            val channel = supabaseClient.realtime.channel("call-signals-$jobId")
            try {
                val flow = channel.postgresChangeFlow<JobMessage>("public") {
                    table = "job_messages"
                    filter = "job_id=eq.$jobId"
                }
                channel.subscribe()
                flow.collect { action ->
                    val msg = action.record
                    if (msg.message.startsWith("[[CALL_SIGNAL:")) {
                        handleIncomingSignalIfNeeded(jobId, msg.message, msg.senderId)
                    } else {
                        handleIncomingChatMessage(jobId, msg)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("HomeaseViewModel", "Error in call signals listener", e)
            } finally {
                channel.unsubscribe()
                if (activeWatchedCallSignalJobId == jobId) {
                    activeWatchedCallSignalJobId = null
                }
            }
        }
    }

    private fun cleanPhoneForCompare(p: String): String {
        return p.replace("+", "").replace("-", "").replace(" ", "").trimStart('0')
    }

    private fun handleIncomingChatMessage(jobId: String, msg: JobMessage) {
        val myPhone = _currentUser.value?.phone ?: _currentPhoneNumber.value
        val myUserId = supabaseClient.getSession()?.userId
        val isSelf = (msg.senderId.isNotBlank() && cleanPhoneForCompare(msg.senderId) == cleanPhoneForCompare(myPhone)) ||
                (myUserId != null && msg.senderId == myUserId)
        if (isSelf) return

        viewModelScope.launch {
            val entity = JobMessageEntity(
                id = msg.id,
                jobId = jobId,
                senderId = msg.senderId,
                senderType = msg.senderType,
                message = msg.message,
                createdAtEpochMs = parseIsoOrNow(msg.createdAt),
                createdAtIso = msg.createdAt,
                readAtEpochMs = if (msg.readAt != null) parseIsoOrNow(msg.readAt) else null
            )
            repository.insertMessage(entity)

            val isViewingThisChat = _currentDestination.value == AppNavDestination.JOB_CHAT &&
                    (_activeChatJob.value?.remoteId == jobId || _activeChatJob.value?.id.toString() == jobId)
            if (isViewingThisChat) return@launch

            val nowMs = System.currentTimeMillis()
            val msgAge = nowMs - parseIsoOrNow(msg.createdAt)
            if (msgAge > 60_000L) return@launch

            val isProvider = _activeRole.value == UserRole.PROVIDER
            val senderRole = if (isProvider) "Customer" else "Service Provider"
            val senderName = if (isProvider) {
                providerActiveJob.value?.customerName?.ifBlank { "Customer" } ?: "Customer"
            } else {
                val req = customerRequests.value.firstOrNull { it.remoteId == jobId || it.id.toString() == jobId }
                req?.selectedProviderName?.ifBlank { "Service Provider" } ?: "Service Provider"
            }

            _inAppMessageNotification.value = InAppMessageNotification(
                id = msg.id,
                jobId = jobId,
                senderName = senderName,
                senderRole = senderRole,
                messageText = msg.message
            )

            try {
                com.example.service.HomEaseFirebaseMessagingService.showChatNotification(
                    context = getApplication(),
                    jobId = jobId,
                    senderName = senderName,
                    messageText = msg.message
                )
            } catch (e: Exception) {
                android.util.Log.w("HomeaseViewModel", "Could not trigger system notification: ${e.message}")
            }
        }
    }

    private fun handleIncomingSignalIfNeeded(jobId: String, text: String, senderId: String) {
        if (!text.startsWith("[[CALL_SIGNAL:")) return

        if (!handledSignalTexts.add(text)) {
            // Already processed this exact signal string once
            return
        }
        if (handledSignalTexts.size > 200) {
            handledSignalTexts.clear()
            handledSignalTexts.add(text)
        }

        val myPhone = _currentUser.value?.phone ?: _currentPhoneNumber.value
        val myUserId = supabaseClient.getSession()?.userId

        // Never self-signal
        if (senderId.isNotBlank()) {
            if (cleanPhoneForCompare(senderId) == cleanPhoneForCompare(myPhone)) return
            if (myUserId != null && senderId == myUserId) return
        }

        val content = text.removePrefix("[[CALL_SIGNAL:").removeSuffix("]]")
        val parts = content.split(":")
        val action = parts.getOrNull(0) ?: ""
        val channelName = parts.getOrNull(1) ?: ""

        when (action) {
            "START" -> {
                val callerName: String
                val callerPhone: String
                val targetPhone: String
                val callSessionId: String
                val timestampMs: Long

                if (parts.size >= 7) {
                    callerName = parts[2]
                    callerPhone = parts[3]
                    targetPhone = parts[4]
                    callSessionId = parts[5]
                    timestampMs = parts[6].toLongOrNull() ?: 0L
                } else {
                    callerName = parts.getOrNull(2) ?: "User"
                    callerPhone = senderId
                    targetPhone = parts.getOrNull(3) ?: ""
                    callSessionId = parts.getOrNull(4) ?: ""
                    timestampMs = parts.getOrNull(5)?.toLongOrNull() ?: 0L
                }

                // Never self-signal
                if (callerPhone.isNotBlank() && cleanPhoneForCompare(callerPhone) == cleanPhoneForCompare(myPhone)) {
                    return
                }

                // Freshness check: Require a valid recent timestamp (within last 35 seconds)
                val now = System.currentTimeMillis()
                if (timestampMs <= 0L || (now - timestampMs) > 35_000L) {
                    return
                }

                // Never re-display a dismissed / ended session
                if (callSessionId.isNotBlank() && endedCallSessionIds.contains(callSessionId)) {
                    return
                }

                // If targetPhone is specified, ensure it's addressed to this user
                if (targetPhone.isNotBlank() && myPhone.isNotBlank()) {
                    if (cleanPhoneForCompare(targetPhone) != cleanPhoneForCompare(myPhone)) {
                        return
                    }
                }

                val currentCallState = agoraVoiceManager.callState.value
                // Allow call if Idle OR if previously Ended (reset to Idle)
                if (currentCallState is CallState.Idle || currentCallState is CallState.Ended) {
                    if (currentCallState is CallState.Ended) {
                        agoraVoiceManager.resetToIdle()
                    }
                    val isProvider = _activeRole.value == UserRole.PROVIDER
                    val callerRole = if (isProvider) "Customer" else "Service Provider"
                    _incomingCall.value = IncomingCallInfo(
                        jobId = jobId,
                        channelName = channelName,
                        callerName = callerName,
                        callerRole = callerRole,
                        callerPhone = if (callerPhone.isNotBlank()) callerPhone else senderId,
                        callSessionId = callSessionId
                    )
                }
            }
            "ACCEPT" -> {
                val senderPhone = parts.getOrNull(2) ?: ""
                if (senderPhone.isNotBlank() && cleanPhoneForCompare(senderPhone) == cleanPhoneForCompare(myPhone)) return
                agoraVoiceManager.markRemoteUserConnected()
            }
            "DECLINE" -> {
                val senderPhone = parts.getOrNull(2) ?: ""
                val callSessionId = parts.getOrNull(3) ?: parts.getOrNull(2) ?: ""
                if (senderPhone.isNotBlank() && cleanPhoneForCompare(senderPhone) == cleanPhoneForCompare(myPhone)) return
                if (callSessionId.isNotBlank()) {
                    endedCallSessionIds.add(callSessionId)
                }
                _incomingCall.value = null
                val current = agoraVoiceManager.callState.value
                if (current is CallState.Connecting || current is CallState.Connected) {
                    agoraVoiceManager.endCall(repository, supabaseClient)
                }
            }
            "END" -> {
                val senderPhone = parts.getOrNull(2) ?: ""
                val callSessionId = parts.getOrNull(3) ?: parts.getOrNull(2) ?: ""
                if (senderPhone.isNotBlank() && cleanPhoneForCompare(senderPhone) == cleanPhoneForCompare(myPhone)) return
                if (callSessionId.isNotBlank()) {
                    endedCallSessionIds.add(callSessionId)
                }
                _incomingCall.value = null
                val current = agoraVoiceManager.callState.value
                if (current is CallState.Connecting || current is CallState.Connected) {
                    agoraVoiceManager.endCall(repository, supabaseClient)
                }
            }
        }
    }

    fun getJobMessagesFlow(jobId: String): Flow<List<JobMessageEntity>> {
        val currentUserId = _currentUser.value?.phone ?: _currentPhoneNumber.value
        viewModelScope.launch {
            // Initial sync from remote Supabase
            try {
                val remoteMessages = supabaseClient.getJobMessages(jobId)
                if (remoteMessages.isNotEmpty()) {
                    val entities = remoteMessages.map { m ->
                        JobMessageEntity(
                            id = m.id,
                            jobId = m.jobId,
                            senderId = m.senderId,
                            senderType = m.senderType,
                            message = m.message,
                            createdAtEpochMs = parseIsoOrNow(m.createdAt),
                            createdAtIso = m.createdAt,
                            readAtEpochMs = if (m.readAt != null) parseIsoOrNow(m.readAt) else null
                        )
                    }
                    repository.insertMessages(entities)
                }
            } catch (_: Exception) {}

            // Realtime subscription (Configure filter BEFORE subscribe)
            val channel = supabaseClient.realtime.channel("job-chat-$jobId")
            try {
                val msgFlow = channel.postgresChangeFlow<JobMessage>(schema = "public") {
                    table = "job_messages"
                    filter = "job_id=eq.$jobId"
                }
                channel.subscribe()
                msgFlow.collect { action ->
                    val m = action.record
                    handleIncomingSignalIfNeeded(jobId, m.message, m.senderId)
                    val entity = JobMessageEntity(
                        id = m.id,
                        jobId = m.jobId,
                        senderId = m.senderId,
                        senderType = m.senderType,
                        message = m.message,
                        createdAtEpochMs = parseIsoOrNow(m.createdAt),
                        createdAtIso = m.createdAt,
                        readAtEpochMs = if (m.readAt != null) parseIsoOrNow(m.readAt) else null
                    )
                    repository.insertMessage(entity)
                }
            } finally {
                channel.unsubscribe()
            }
        }
        return repository.getMessagesForJobFlow(jobId)
    }

    fun sendJobMessage(jobId: String, text: String) {
        if (text.isBlank()) return
        val currentUserId = _currentUser.value?.phone ?: _currentPhoneNumber.value
        val senderType = if (_activeRole.value == UserRole.PROVIDER) "provider" else "customer"
        val tempId = UUID.randomUUID().toString()
        val nowMs = System.currentTimeMillis()
        val isoTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date(nowMs))

        val localEntity = JobMessageEntity(
            id = tempId,
            jobId = jobId,
            senderId = currentUserId,
            senderType = senderType,
            message = text.trim(),
            createdAtEpochMs = nowMs,
            createdAtIso = isoTime,
            readAtEpochMs = null
        )

        viewModelScope.launch {
            repository.insertMessage(localEntity)
            val result = supabaseClient.sendJobMessage(
                jobId = jobId,
                senderId = currentUserId,
                senderType = senderType,
                messageText = text.trim(),
                messageId = tempId
            )
            if (result.isSuccess) {
                val sent = result.getOrNull()
                if (sent != null) {
                    val updated = localEntity.copy(id = sent.id, createdAtIso = sent.createdAt)
                    repository.insertMessage(updated)
                }
            }
        }
    }

    fun markMessagesAsRead(jobId: String) {
        val currentUserId = _currentUser.value?.phone ?: _currentPhoneNumber.value
        viewModelScope.launch {
            repository.markMessagesAsRead(jobId, currentUserId)
            supabaseClient.markMessagesAsRead(jobId, currentUserId)
        }
    }

    fun getUnreadCountFlow(jobId: String): Flow<Int> {
        val currentUserId = _currentUser.value?.phone ?: _currentPhoneNumber.value
        return repository.getUnreadMessageCountFlow(jobId, currentUserId)
    }

    private fun parseIsoOrNow(iso: String): Long {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            sdf.parse(iso.take(19))?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    /**
     * Subscribes to Realtime location updates for the active job.
     * Continuously emits smooth moving coordinates as the provider moves.
     */
    fun getLiveTrackingLocationFlow(jobId: String): Flow<ProviderLocation> = flow {
        // 1. Initial cached position if available
        val initialLocal = repository.getProviderLocationForJob(jobId)
        if (initialLocal != null) {
            emit(
                ProviderLocation(
                    jobId = initialLocal.jobId,
                    providerId = initialLocal.providerId,
                    lat = initialLocal.lat,
                    lng = initialLocal.lng,
                    heading = initialLocal.heading,
                    updatedAt = initialLocal.updatedAt.toString()
                )
            )
        }

        // 2. Realtime subscription to Supabase provider_locations table
        // IMPORTANT: Configure the filter BEFORE subscribing so the WebSocket
        // join message includes the correct job_id filter and polling fallback starts
        val channel = supabaseClient.realtime.channel("job-tracking-$jobId")
        try {
            val locationFlow = channel.postgresChangeFlow<ProviderLocation>(schema = "public") {
                table = "provider_locations"
                filter = "job_id=eq.$jobId"
            }
            channel.subscribe()
            locationFlow.collect { change ->
                emit(change.record)
            }
        } finally {
            channel.unsubscribe()
        }
    }

    private fun jsonToServiceRequestEntity(obj: JSONObject): ServiceRequestEntity {
        val remoteId = obj.optString("id")
        val customerPhone = obj.optString("customer_phone", "")
        val customerName = obj.optString("customer_name", "Customer")
        val category = obj.optString("category", "General")
        val categoryId = obj.optString("category_id", "general")
        val serviceTitle = obj.optString("service_title", "Service Request")
        val description = obj.optString("description", "")
        val cityArea = obj.optString("city_area", "")
        val fullAddress = obj.optString("full_address", "")
        val budgetRs = obj.optInt("budget_rs", 1500)
        val statusRaw = obj.optString("status", "searching").uppercase()
        val status = when (statusRaw) {
            "SEARCHING" -> "SEARCHING"
            "ACCEPTED" -> "ACCEPTED"
            "ON_THE_WAY" -> "ON_THE_WAY"
            "ARRIVED" -> "ARRIVED"
            "IN_PROGRESS" -> "IN_PROGRESS"
            "AWAITING_CUSTOMER_CONFIRMATION" -> "AWAITING_CUSTOMER_CONFIRMATION"
            "COMPLETED" -> "COMPLETED"
            "CANCELLED" -> "CANCELLED"
            else -> statusRaw
        }
        val providerPhone = obj.optString("provider_phone").ifBlank { null }
        val providerName = obj.optString("provider_name").ifBlank { null }
        val agreedPriceRs = obj.optInt("agreed_price_rs", budgetRs)
        val lat = if (obj.has("lat") && !obj.isNull("lat")) obj.optDouble("lat").takeIf { !it.isNaN() } else null
        val lng = if (obj.has("lng") && !obj.isNull("lng")) obj.optDouble("lng").takeIf { !it.isNaN() } else null
        val cancelledBy = obj.optString("cancelled_by").ifBlank { null }
        val cancellationReason = obj.optString("cancellation_reason").ifBlank { null }
        val cancelledAt = if (obj.has("cancelled_at") && !obj.isNull("cancelled_at")) {
            try {
                val iso = obj.optString("cancelled_at")
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(iso.take(19))?.time
            } catch (_: Exception) { null }
        } else null

        return ServiceRequestEntity(
            id = 0,
            customerPhone = customerPhone,
            customerName = customerName,
            categoryId = categoryId,
            categoryTitle = category,
            serviceTitle = serviceTitle,
            description = description,
            cityArea = cityArea,
            fullAddress = fullAddress,
            budgetRs = budgetRs,
            customerAskingPrice = budgetRs,
            status = status,
            selectedProviderPhone = providerPhone,
            selectedProviderName = providerName,
            agreedPriceRs = agreedPriceRs,
            remoteId = remoteId,
            lat = lat,
            lng = lng,
            cancelledBy = cancelledBy,
            cancellationReason = cancellationReason,
            cancelledAt = cancelledAt
        )
    }

    private fun jsonToJobOfferEntity(obj: JSONObject, localRequestId: Long): JobOfferEntity {
        val remoteOfferId = obj.optString("id")
        val providerPhone = obj.optString("provider_phone", "")
        val providerName = obj.optString("provider_name", "Provider")
        val offerPrice = obj.optInt("offer_price_rs", 0)
        val counterPrice = obj.optInt("counter_price_rs", offerPrice)
        val distanceKm = obj.optDouble("distance_km", 1.5)
        val rating = obj.optDouble("provider_rating", 4.8)
        val status = obj.optString("status", "pending")
        val offerNote = obj.optString("offer_note").ifBlank { null }
        val providerId = obj.optString("provider_id").ifBlank { null }

        return JobOfferEntity(
            id = 0,
            requestId = localRequestId,
            providerPhone = providerPhone,
            providerName = providerName,
            counterPriceRs = counterPrice,
            offerPriceRs = offerPrice,
            distanceKm = distanceKm,
            providerRating = rating,
            status = status,
            offerNote = offerNote,
            remoteOfferId = remoteOfferId,
            providerId = providerId
        )
    }

    fun submitServiceRequest(request: ServiceRequestEntity) {
        viewModelScope.launch {
            _isSubmittingRequest.value = true
            _requestSubmissionError.value = null

            // 1. Primary write to Supabase must happen first and be awaited
            val createResult = supabaseClient.createJob(
                customerPhone = request.customerPhone,
                customerName = request.customerName,
                category = request.categoryTitle,
                categoryId = request.categoryId,
                serviceTitle = request.serviceTitle,
                description = request.description,
                cityArea = request.cityArea,
                fullAddress = request.fullAddress,
                budgetRs = request.budgetRs,
                lat = request.lat,
                lng = request.lng
            )

            // 2. If the Supabase write fails, show a visible error and DO NOT fall back to local-only
            if (createResult.isFailure) {
                val err = createResult.exceptionOrNull()?.message ?: "Failed to post job to network"
                _isSubmittingRequest.value = false
                _requestSubmissionError.value = "Connection error: Could not submit request. Check your internet connection and try again ($err)."
                return@launch
            }

            val remoteJobId = createResult.getOrThrow()

            // 3. Only save to local Room cache AFTER successful Supabase write
            val syncedEntity = request.copy(
                remoteId = remoteJobId,
                status = "SEARCHING"
            )
            val localId = repository.syncRemoteJob(syncedEntity)
            val activeEntity = syncedEntity.copy(id = localId)

            _isSubmittingRequest.value = false
            _activeLiveRequest.value = activeEntity
            _trackingJob.value = activeEntity

            // Instant Realtime WebSocket + fast-poll listener for job acceptance on customer device
            watchCustomerJobAccepted(remoteJobId, localId)

            // Listen for incoming offers reactively from local Room
            launch {
                repository.getOffersForRequest(localId).collect { offers ->
                    _incomingOffers.value = offers
                }
            }

            // Realtime WebSocket channel for instant offer push (0ms delay)
            val offersChannel = supabaseClient.realtime.channel("job-offers-$remoteJobId")
            offersChannel.subscribe()
            val offersRealtimeJob = launch {
                offersChannel.postgresChangeFlow<JSONObject>("public") {
                    table = "job_offers"
                    filter = "job_id=eq.$remoteJobId"
                }.collect { action ->
                    try {
                        val offerEntity = jsonToJobOfferEntity(action.record, localId)
                        repository.syncRemoteOffer(offerEntity)
                    } catch (_: Exception) {}
                }
            }

            // Dispatch notification event to notify matching providers via Edge Function / FCM
            launch {
                try {
                    val jobJson = JSONObject().apply {
                        put("id", remoteJobId)
                        put("category", request.categoryTitle)
                        put("category_id", request.categoryId)
                        put("service_title", request.serviceTitle)
                        put("budget_rs", request.budgetRs)
                        put("city_area", request.cityArea)
                        put("customer_phone", request.customerPhone)
                        put("status", "searching")
                    }
                    supabaseClient.dispatchNotificationEvent("INSERT", "jobs", jobJson)
                } catch (_: Exception) {}
            }

            // Gentle fallback sync (every 15s instead of busy-polling) to safeguard in case of network drops
            launch {
                while (_activeLiveRequest.value?.status == "SEARCHING" && _activeLiveRequest.value?.remoteId == remoteJobId) {
                    try {
                        val offersResult = supabaseClient.getOffersForJob(remoteJobId)
                        if (offersResult.isSuccess) {
                            val offersList = offersResult.getOrThrow()
                            for (offerJson in offersList) {
                                val offerEntity = jsonToJobOfferEntity(offerJson, localId)
                                repository.syncRemoteOffer(offerEntity)
                            }
                        }
                    } catch (_: Exception) {}
                    delay(15000L)
                }
                offersChannel.unsubscribe()
                offersRealtimeJob.cancel()
            }
        }
    }

    fun selectOfferForRequest(offer: JobOfferEntity) {
        viewModelScope.launch {
            val req = _activeLiveRequest.value ?: return@launch
            _isSubmittingRequest.value = true
            _requestSubmissionError.value = null

            val remoteJobId = req.remoteId ?: req.id.toString()
            val remoteOfferId = offer.remoteOfferId ?: offer.id.toString()
            val agreedPrice = if (offer.offerPriceRs > 0) offer.offerPriceRs else offer.counterPriceRs

            // Primary blocking update to Supabase
            val acceptResult = supabaseClient.acceptJobOffer(
                jobId = remoteJobId,
                offerId = remoteOfferId,
                providerId = offer.providerId,
                providerPhone = offer.providerPhone,
                providerName = offer.providerName,
                agreedPriceRs = agreedPrice
            )

            if (acceptResult.isFailure) {
                val err = acceptResult.exceptionOrNull()?.message ?: "Failed to accept offer on network"
                _isSubmittingRequest.value = false
                _requestSubmissionError.value = "Failed to accept offer on network: $err"
                return@launch
            }

            // Sync to local Room after remote success
            repository.customerSelectOffer(req.id, offer)
            val accepted = req.copy(
                status = "ACCEPTED",
                selectedProviderName = offer.providerName,
                selectedProviderPhone = offer.providerPhone,
                agreedPriceRs = agreedPrice
            )
            _activeLiveRequest.value = accepted
            _trackingJob.value = accepted
            _isSubmittingRequest.value = false
        }
    }

    fun acceptJobAsProvider(job: ServiceRequestEntity) {
        viewModelScope.launch {
            _isProviderActionLoading.value = true
            _providerActionError.value = null

            val phone = _currentPhoneNumber.value
            val provider = _currentUser.value ?: if (phone.isNotBlank()) repository.getUser(phone) else null
            val resolvedPhone = provider?.phone ?: phone
            val resolvedName = provider?.name?.ifBlank { null } ?: "Service Provider"
            val remoteJobId = job.remoteId ?: job.id.toString()
            val sessionUserId = supabaseClient.getSession()?.userId

            // Primary blocking write to Supabase
            val acceptResult = supabaseClient.acceptJobDirectly(
                jobId = remoteJobId,
                providerId = sessionUserId,
                providerPhone = resolvedPhone,
                providerName = resolvedName,
                priceRs = job.budgetRs
            )

            if (acceptResult.isFailure) {
                val err = acceptResult.exceptionOrNull()?.message ?: "Failed to accept job on network"
                _isProviderActionLoading.value = false
                _providerActionError.value = "Failed to accept job: $err"
                return@launch
            }

            // Update local Room
            repository.acceptJobByProvider(
                requestId = job.id,
                providerPhone = resolvedPhone,
                providerName = resolvedName,
                agreedPrice = job.budgetRs
            )
            _isProviderActionLoading.value = false
            _fullscreenPingJob.value = null
            _currentDestination.value = AppNavDestination.PROVIDER_HOME
        }
    }

    fun rejectJobAsProvider(job: ServiceRequestEntity) {
        _fullscreenPingJob.value = null
        _currentDestination.value = AppNavDestination.PROVIDER_HOME
    }

    fun counterJobAsProvider(job: ServiceRequestEntity, counterPrice: Int, note: String? = null) {
        viewModelScope.launch {
            _isProviderActionLoading.value = true
            _providerActionError.value = null

            val phone = _currentPhoneNumber.value
            val provider = _currentUser.value ?: if (phone.isNotBlank()) repository.getUser(phone) else null
            val resolvedPhone = provider?.phone ?: phone
            val resolvedName = provider?.name?.ifBlank { null } ?: "Service Provider"
            val remoteJobId = job.remoteId ?: job.id.toString()

            // Dynamically calculate Haversine distance in km between provider and job location if GPS is available
            val providerLat = provider?.lat
            val providerLng = provider?.lng
            val jobLat = job.lat
            val jobLng = job.lng
            val calculatedDistanceKm: Double = if (providerLat != null && providerLng != null && jobLat != null && jobLng != null) {
                val earthRadiusKm = 6371.0
                val dLat = Math.toRadians(jobLat - providerLat)
                val dLng = Math.toRadians(jobLng - providerLng)
                val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(providerLat)) * Math.cos(Math.toRadians(jobLat)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2)
                val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                val dist = earthRadiusKm * c
                (Math.round(dist * 10.0) / 10.0).coerceAtLeast(0.5)
            } else {
                1.5 // Default area estimate when GPS is not yet acquired
            }
            val resolvedRating = if ((provider?.avgRating ?: 0.0) > 0.0) provider!!.avgRating else 4.8
            val sessionUserId = supabaseClient.getSession()?.userId

            // Primary blocking write to Supabase public.job_offers
            val offerResult = supabaseClient.submitJobOffer(
                jobId = remoteJobId,
                providerId = sessionUserId,
                providerPhone = resolvedPhone,
                providerName = resolvedName,
                offerPriceRs = counterPrice,
                counterPriceRs = counterPrice,
                distanceKm = calculatedDistanceKm,
                providerRating = resolvedRating,
                offerNote = note
            )

            if (offerResult.isFailure) {
                val err = offerResult.exceptionOrNull()?.message ?: "Failed to submit counter offer"
                _isProviderActionLoading.value = false
                _providerActionError.value = "Failed to submit counter offer: $err"
                return@launch
            }

            val remoteOfferId = offerResult.getOrThrow()

            // Save to local Room
            val offerWithRemote = JobOfferEntity(
                requestId = job.id,
                providerPhone = resolvedPhone,
                providerName = resolvedName,
                counterPriceRs = counterPrice,
                offerPriceRs = counterPrice,
                distanceKm = calculatedDistanceKm,
                providerRating = resolvedRating,
                status = "pending",
                offerNote = note,
                remoteOfferId = remoteOfferId
            )
            repository.syncRemoteOffer(offerWithRemote)

            _isProviderActionLoading.value = false
            _fullscreenPingJob.value = null

            // Ensure provider is listening for acceptance of this offer
            watchProviderJobsAndOffers(resolvedPhone)

            _currentDestination.value = AppNavDestination.PROVIDER_HOME
        }
    }

    /**
     * Provider taps "On the way":
     * - Changes job status to ON_THE_WAY
     * - Launches ProviderLocationService (Foreground Service with persistent notification)
     * - Updates Supabase status and status_updated_at
     */
    fun startProviderJobTrip(job: ServiceRequestEntity) {
        viewModelScope.launch {
            val remoteId = job.remoteId ?: job.id.toString()
            repository.markJobOnTheWay(job.id)
            supabaseClient.updateJobStatus(remoteId, "on_the_way")
            val pPhone = job.selectedProviderPhone ?: _currentPhoneNumber.value
            ProviderLocationService.start(
                context = getApplication(),
                jobId = remoteId,
                providerId = pPhone,
                destLat = job.lat ?: 33.6844,
                destLng = job.lng ?: 73.0479
            )
        }
    }

    /**
     * Provider taps "Arrived":
     * - Changes job status to ARRIVED
     * - Stops ProviderLocationService
     * - Updates Supabase status and status_updated_at
     */
    fun markProviderJobArrived(job: ServiceRequestEntity) {
        viewModelScope.launch {
            val remoteId = job.remoteId ?: job.id.toString()
            repository.markJobArrived(job.id)
            supabaseClient.updateJobStatus(remoteId, "arrived")
            ProviderLocationService.stop(getApplication())

            // Alert customer via chat message and notification event
            val currentPhone = _currentUser.value?.phone ?: _currentPhoneNumber.value
            supabaseClient.sendJobMessage(
                remoteId,
                currentPhone,
                "provider",
                "I have arrived at your doorstep! Please receive me."
            )
            val record = JSONObject().apply {
                put("id", remoteId)
                put("status", "arrived")
                put("customer_phone", job.customerPhone)
                put("provider_phone", job.selectedProviderPhone ?: currentPhone)
            }
            supabaseClient.dispatchNotificationEvent("UPDATE", "jobs", record)
        }
    }

    /**
     * Provider taps "Start Job":
     * - Changes job status to IN_PROGRESS
     * - Updates Supabase status and status_updated_at
     */
    fun startProviderJobWork(job: ServiceRequestEntity) {
        viewModelScope.launch {
            val remoteId = job.remoteId ?: job.id.toString()
            repository.markJobInProgress(job.id)
            supabaseClient.updateJobStatus(remoteId, "in_progress")
            ProviderLocationService.stop(getApplication())
        }
    }

    fun completeActiveJob(jobId: Long) {
        viewModelScope.launch {
            val job = repository.getRequestById(jobId)
            val remoteId = job?.remoteId ?: jobId.toString()
            ProviderLocationService.stop(getApplication())
            repository.markJobAwaitingConfirmation(jobId)
            supabaseClient.updateJobStatus(remoteId, "awaiting_customer_confirmation")
        }
    }

    fun submitCustomerRating(jobId: Long, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.completeJobWithRating(jobId, rating, comment)
            // Refresh current user if provider is the current user
            val phone = _currentPhoneNumber.value
            val updated = repository.getUser(phone)
            if (updated != null) {
                _currentUser.value = updated
            }
        }
    }

    fun reportCustomerIssue(jobId: Long, category: String, description: String) {
        viewModelScope.launch {
            repository.reportJobIssue(jobId, category, description)
        }
    }

    fun autoCompleteJobWithoutRating(jobId: Long) {
        viewModelScope.launch {
            repository.autoCompleteJobWithoutRating(jobId)
        }
    }

    fun cancelJobByCustomer(job: ServiceRequestEntity, reason: String) {
        viewModelScope.launch {
            val remoteId = job.remoteId
            val localId = job.id

            // End voice call if active
            val currentCall = agoraVoiceManager.callState.value
            if (currentCall !is CallState.Idle) {
                endVoiceCall()
            }

            // 1. Update Room DB
            if (localId > 0) {
                repository.cancelJob(localId, cancelledBy = "customer", reason = reason)
            }
            if (!remoteId.isNullOrBlank()) {
                repository.cancelJobByRemoteId(remoteId, cancelledBy = "customer", reason = reason)
            }

            // 2. Clear customer active/tracking states
            if (_activeLiveRequest.value?.id == localId || _activeLiveRequest.value?.remoteId == remoteId) {
                _activeLiveRequest.value = null
            }
            if (_trackingJob.value?.id == localId || _trackingJob.value?.remoteId == remoteId) {
                _trackingJob.value = null
            }

            // 3. Update Supabase
            if (!remoteId.isNullOrBlank()) {
                supabaseClient.cancelJob(remoteId, cancelledBy = "customer", cancellationReason = reason)
                val currentPhone = _currentUser.value?.phone ?: _currentPhoneNumber.value
                supabaseClient.sendJobMessage(
                    jobId = remoteId,
                    senderId = currentPhone,
                    senderType = "customer",
                    messageText = "🚫 Booking cancelled by customer. Reason: $reason"
                )
            }

            // Terminate customer watchers
            customerJobRealtimeChannel?.unsubscribe()
            customerJobRealtimeJob?.cancel()
            customerJobPollingJob?.cancel()

            // Navigate back to customer home
            _currentDestination.value = AppNavDestination.CUSTOMER_HOME
        }
    }

    fun cancelJobByProvider(job: ServiceRequestEntity, reason: String) {
        viewModelScope.launch {
            val remoteId = job.remoteId
            val localId = job.id

            // End voice call if active
            val currentCall = agoraVoiceManager.callState.value
            if (currentCall !is CallState.Idle) {
                endVoiceCall()
            }

            // Stop location service if running
            ProviderLocationService.stop(getApplication())

            // 1. Update Room DB
            if (localId > 0) {
                repository.cancelJob(localId, cancelledBy = "provider", reason = reason)
            }
            if (!remoteId.isNullOrBlank()) {
                repository.cancelJobByRemoteId(remoteId, cancelledBy = "provider", reason = reason)
            }

            // 2. Clear ping / won confirmations
            if (_fullscreenPingJob.value?.id == localId || _fullscreenPingJob.value?.remoteId == remoteId) {
                _fullscreenPingJob.value = null
            }
            if (_providerJobWonConfirmation.value?.id == localId || _providerJobWonConfirmation.value?.remoteId == remoteId) {
                _providerJobWonConfirmation.value = null
            }

            // 3. Update Supabase
            if (!remoteId.isNullOrBlank()) {
                supabaseClient.cancelJob(remoteId, cancelledBy = "provider", cancellationReason = reason)
                val currentPhone = _currentUser.value?.phone ?: _currentPhoneNumber.value
                supabaseClient.sendJobMessage(
                    jobId = remoteId,
                    senderId = currentPhone,
                    senderType = "provider",
                    messageText = "🚫 Booking cancelled by provider. Reason: $reason"
                )
            }
        }
    }

    fun updateCustomerProfile(
        name: String,
        cityArea: String,
        savedAddressesCsv: String,
        lat: Double? = null,
        lng: Double? = null
    ) {
        viewModelScope.launch {
            val phone = _currentPhoneNumber.value
            repository.updateCustomerProfile(phone, name, cityArea, savedAddressesCsv, lat, lng)
            val updated = repository.getUser(phone)
            if (updated != null) {
                _currentUser.value = updated
            }
        }
    }

    fun updateProviderProfile(
        name: String,
        cityArea: String,
        categoriesCsv: String,
        yearsExperience: String,
        serviceRadiusKm: Int,
        bio: String,
        shopName: String,
        payoutMethod: String,
        payoutAccountNumber: String,
        lat: Double? = null,
        lng: Double? = null
    ) {
        viewModelScope.launch {
            val phone = _currentPhoneNumber.value
            repository.updateProviderProfile(
                phone = phone,
                name = name,
                cityArea = cityArea,
                categoriesCsv = categoriesCsv,
                yearsExperience = yearsExperience,
                serviceRadiusKm = serviceRadiusKm,
                bio = bio,
                shopName = shopName,
                payoutMethod = payoutMethod,
                payoutAccountNumber = payoutAccountNumber,
                lat = lat,
                lng = lng
            )
            val updated = repository.getUser(phone)
            if (updated != null) {
                _currentUser.value = updated
            }

            // Sync profile changes to Supabase service_providers table via PATCH
            val catList = categoriesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
            supabaseClient.updateProviderRemoteProfile(
                phone = phone,
                name = name,
                cityArea = cityArea,
                categories = catList,
                yearsExperience = yearsExperience,
                serviceRadiusKm = serviceRadiusKm,
                bio = bio,
                shopName = shopName,
                payoutMethod = payoutMethod,
                payoutAccountNumber = payoutAccountNumber
            )
        }
    }

    // Customer job acceptance realtime & fast-poll watchers
    private var customerJobRealtimeChannel: RealtimeChannel? = null
    private var customerJobRealtimeJob: kotlinx.coroutines.Job? = null
    private var customerJobPollingJob: kotlinx.coroutines.Job? = null

    // Provider assigned jobs & won offers realtime & fast-poll watchers
    private var providerAssignedRealtimeChannel: RealtimeChannel? = null
    private var providerAssignedRealtimeJob: kotlinx.coroutines.Job? = null
    private var providerOffersRealtimeChannel: RealtimeChannel? = null
    private var providerOffersRealtimeJob: kotlinx.coroutines.Job? = null
    private var providerAcceptedPollingJob: kotlinx.coroutines.Job? = null

    fun watchCustomerJobAccepted(remoteJobId: String, localJobId: Long) {
        if (remoteJobId.isBlank()) return

        customerJobRealtimeChannel?.unsubscribe()
        customerJobRealtimeJob?.cancel()
        customerJobPollingJob?.cancel()

        // 1. Direct Realtime WebSocket listener on 'jobs' table filtered by id
        val channel = supabaseClient.realtime.channel("customer-job-$remoteJobId")
        customerJobRealtimeChannel = channel

        val flow = channel.postgresChangeFlow<JSONObject>("public") {
            table = "jobs"
            filter = "id=eq.$remoteJobId"
        }
        channel.subscribe()

        customerJobRealtimeJob = viewModelScope.launch {
            flow.collect { action ->
                try {
                    val record = action.record
                    val status = record.optString("status", "").uppercase()
                    if (status.isNotBlank() && status != "SEARCHING") {
                        android.util.Log.d("HomeaseViewModel", "Realtime customer job update: status=$status")
                        handleJobAcceptedOnCustomerSide(remoteJobId, localJobId, record)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("HomeaseViewModel", "Error in customer realtime job listener", e)
                }
            }
        }

        // 2. High-speed dual-engine polling (every 1.5 seconds) while status == "SEARCHING"
        customerJobPollingJob = viewModelScope.launch {
            while (isActive && _activeLiveRequest.value?.status == "SEARCHING" && _activeLiveRequest.value?.remoteId == remoteJobId) {
                try {
                    val jobResult = supabaseClient.getJobById(remoteJobId)
                    if (jobResult.isSuccess) {
                        val jobJson = jobResult.getOrNull()
                        if (jobJson != null) {
                            val status = jobJson.optString("status", "").uppercase()
                            if (status.isNotBlank() && status != "SEARCHING") {
                                android.util.Log.d("HomeaseViewModel", "Polling customer job acceptance: status=$status")
                                handleJobAcceptedOnCustomerSide(remoteJobId, localJobId, jobJson)
                                break
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(1500L)
            }
        }
    }

    private suspend fun handleJobAcceptedOnCustomerSide(
        remoteJobId: String,
        localJobId: Long,
        record: JSONObject
    ) {
        val statusRaw = record.optString("status", "accepted").uppercase()
        val providerPhone = record.optString("provider_phone").ifBlank { null }
        val providerName = record.optString("provider_name").ifBlank { "Service Provider" }
        val budgetRs = _activeLiveRequest.value?.budgetRs ?: record.optInt("budget_rs", 1500)
        val agreedPrice = record.optInt("agreed_price_rs", budgetRs)

        // Update local Room database immediately
        if (!providerPhone.isNullOrBlank()) {
            repository.acceptJobByProvider(
                requestId = localJobId,
                providerPhone = providerPhone,
                providerName = providerName,
                agreedPrice = agreedPrice
            )
        }
        val entity = jsonToServiceRequestEntity(record).copy(id = localJobId)
        repository.syncRemoteJob(entity)

        val current = _activeLiveRequest.value
        val updated = (current ?: entity).copy(
            status = statusRaw,
            selectedProviderPhone = providerPhone ?: current?.selectedProviderPhone,
            selectedProviderName = providerName ?: current?.selectedProviderName,
            agreedPriceRs = agreedPrice
        )
        _activeLiveRequest.value = updated
        _trackingJob.value = updated

        // Watch incoming call signals for this active job
        watchCallSignals(remoteJobId)

        // Only terminate watcher when job is completed or cancelled
        if (statusRaw == "CANCELLED") {
            _trackingJob.value = null
            _activeLiveRequest.value = null
            customerJobRealtimeChannel?.unsubscribe()
            customerJobRealtimeJob?.cancel()
            customerJobPollingJob?.cancel()
            if (_currentDestination.value == AppNavDestination.CUSTOMER_LIVE_TRACKING) {
                _currentDestination.value = AppNavDestination.CUSTOMER_HOME
            }
        } else if (statusRaw == "COMPLETED") {
            customerJobRealtimeChannel?.unsubscribe()
            customerJobRealtimeJob?.cancel()
            customerJobPollingJob?.cancel()
        }
    }

    fun watchProviderJobsAndOffers(providerPhone: String) {
        if (providerPhone.isBlank()) return

        providerAssignedRealtimeChannel?.unsubscribe()
        providerAssignedRealtimeJob?.cancel()
        providerOffersRealtimeChannel?.unsubscribe()
        providerOffersRealtimeJob?.cancel()
        providerAcceptedPollingJob?.cancel()

        // 1. Direct Realtime WebSocket listener on 'jobs' table for this provider
        val channel = supabaseClient.realtime.channel("provider-jobs-$providerPhone")
        providerAssignedRealtimeChannel = channel

        val jobsFlow = channel.postgresChangeFlow<JSONObject>("public") {
            table = "jobs"
            filter = "provider_phone=eq.$providerPhone"
        }
        channel.subscribe()

        providerAssignedRealtimeJob = viewModelScope.launch {
            jobsFlow.collect { action ->
                try {
                    val record = action.record
                    val status = record.optString("status", "").uppercase()
                    if (status in listOf("ACCEPTED", "ON_THE_WAY", "ARRIVED", "IN_PROGRESS")) {
                        android.util.Log.d("HomeaseViewModel", "Provider assigned job realtime event: status=$status")
                        handleJobWonOnProviderSide(record)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("HomeaseViewModel", "Error in provider assigned jobs flow", e)
                }
            }
        }

        // 2. Realtime WebSocket listener on 'job_offers' table for this provider
        val offersChannel = supabaseClient.realtime.channel("provider-offers-$providerPhone")
        providerOffersRealtimeChannel = offersChannel

        val offersFlow = offersChannel.postgresChangeFlow<JSONObject>("public") {
            table = "job_offers"
            filter = "provider_phone=eq.$providerPhone"
        }
        offersChannel.subscribe()

        providerOffersRealtimeJob = viewModelScope.launch {
            offersFlow.collect { action ->
                try {
                    val record = action.record
                    val status = record.optString("status", "").lowercase()
                    if (status == "accepted") {
                        android.util.Log.d("HomeaseViewModel", "Provider offer accepted realtime event: $record")
                        val jobId = record.optString("job_id")
                        if (jobId.isNotBlank()) {
                            val jobResult = supabaseClient.getJobById(jobId)
                            if (jobResult.isSuccess) {
                                jobResult.getOrNull()?.let { handleJobWonOnProviderSide(it) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("HomeaseViewModel", "Error in provider offers flow", e)
                }
            }
        }

        // 3. High-reliability companion polling (every 2.5 seconds)
        providerAcceptedPollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val jobsResult = supabaseClient.getProviderAcceptedJobs(providerPhone)
                    if (jobsResult.isSuccess) {
                        val jobsList = jobsResult.getOrThrow()
                        for (jobJson in jobsList) {
                            handleJobWonOnProviderSide(jobJson)
                        }

                        // Check if provider's active job was cancelled remotely
                        val currentActive = providerActiveJob.value
                        if (currentActive != null && !currentActive.remoteId.isNullOrBlank()) {
                            val activeRemoteId = currentActive.remoteId
                            val isStillInAcceptedList = jobsList.any { it.optString("id") == activeRemoteId }
                            if (!isStillInAcceptedList) {
                                val checkRes = supabaseClient.getJobById(activeRemoteId)
                                if (checkRes.isSuccess) {
                                    val checkJson = checkRes.getOrNull()
                                    if (checkJson != null && checkJson.optString("status").uppercase() == "CANCELLED") {
                                        val rReason = checkJson.optString("cancellation_reason").ifBlank { "Cancelled by customer" }
                                        val rBy = checkJson.optString("cancelled_by").ifBlank { "customer" }
                                        repository.cancelJobByRemoteId(activeRemoteId, rBy, rReason)
                                    }
                                }
                            }
                        }
                    }

                    val offersResult = supabaseClient.getProviderAcceptedOffers(providerPhone)
                    if (offersResult.isSuccess) {
                        val offersList = offersResult.getOrThrow()
                        for (offerJson in offersList) {
                            val jobId = offerJson.optString("job_id")
                            if (jobId.isNotBlank()) {
                                val jobResult = supabaseClient.getJobById(jobId)
                                if (jobResult.isSuccess) {
                                    jobResult.getOrNull()?.let { handleJobWonOnProviderSide(it) }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(2500L)
            }
        }
    }

    private suspend fun handleJobWonOnProviderSide(record: JSONObject) {
        val entity = jsonToServiceRequestEntity(record)
        val localId = repository.syncRemoteJob(entity)
        val saved = entity.copy(id = localId)

        val statusUpper = entity.status.uppercase()
        val rId = entity.remoteId

        if (statusUpper == "CANCELLED") {
            if (!rId.isNullOrBlank()) {
                repository.cancelJobByRemoteId(rId, entity.cancelledBy ?: "customer", entity.cancellationReason ?: "Cancelled by customer")
            }
            if (_fullscreenPingJob.value?.remoteId == rId || _fullscreenPingJob.value?.id == localId) {
                _fullscreenPingJob.value = null
            }
            if (_providerJobWonConfirmation.value?.remoteId == rId || _providerJobWonConfirmation.value?.id == localId) {
                _providerJobWonConfirmation.value = null
            }
            return
        }

        // Clear ping if this job was currently showing on screen
        if (_fullscreenPingJob.value?.remoteId == entity.remoteId || _fullscreenPingJob.value?.id == localId) {
            _fullscreenPingJob.value = null
        }

        // Trigger confirmation modal if status is ACCEPTED and not previously acknowledged
        if (!rId.isNullOrBlank()) {
            watchCallSignals(rId)
        }
        if (statusUpper == "ACCEPTED") {
            if (!rId.isNullOrBlank() && rId !in acknowledgedJobWonIds) {
                if (_providerJobWonConfirmation.value?.remoteId != rId) {
                    _providerJobWonConfirmation.value = saved
                }
            }
        }
    }

    private var providerJobsRealtimeChannel: com.example.data.remote.RealtimeChannel? = null
    private var providerJobRealtimeJob: kotlinx.coroutines.Job? = null
    private var providerJobPollingJob: kotlinx.coroutines.Job? = null

    fun toggleProviderOnline(isOnline: Boolean) {
        viewModelScope.launch {
            val phone = _currentPhoneNumber.value
            repository.setProviderOnline(phone, isOnline)

            // Update is_online in Supabase service_providers table directly
            supabaseClient.updateProviderOnlineStatus(phone, isOnline)

            // Sync online status and FCM token to device_tokens
            val token = _fcmToken.value
            val user = _currentUser.value ?: repository.getUser(phone)
            val categories = user?.categoriesCsv?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            if (phone.isNotBlank()) {
                supabaseClient.registerDeviceToken(
                    phone = phone,
                    role = "provider",
                    fcmToken = token,
                    categories = categories,
                    isOnline = isOnline
                )
            }

            providerJobsRealtimeChannel?.unsubscribe()
            providerJobRealtimeJob?.cancel()
            providerJobPollingJob?.cancel()

            if (isOnline) {
                // Watch for this provider's assigned jobs and won offers in realtime
                watchProviderJobsAndOffers(phone)

                // 1. Instant Realtime WebSocket subscription for open jobs
                val channel = supabaseClient.realtime.channel("provider-open-jobs")
                providerJobsRealtimeChannel = channel

                val openJobsFlow = channel.postgresChangeFlow<JSONObject>("public") {
                    table = "jobs"
                    filter = ""
                }
                channel.subscribe()

                providerJobRealtimeJob = launch {
                    openJobsFlow.collect { action ->
                        try {
                            val record = action.record
                            val status = record.optString("status", "")
                            val entity = jsonToServiceRequestEntity(record)
                            val localId = repository.syncRemoteJob(entity)
                            val savedEntity = entity.copy(id = localId)

                            if (status.equals("SEARCHING", ignoreCase = true)) {
                                _fullscreenPingJob.value = savedEntity
                            } else {
                                // Status changed away from searching (e.g. accepted, cancelled, on_the_way)
                                // If this was currently pinging the provider on screen, dismiss it immediately
                                if (_fullscreenPingJob.value?.remoteId == entity.remoteId || _fullscreenPingJob.value?.id == localId) {
                                    _fullscreenPingJob.value = null
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // 2. Initial fetch & 25s low-frequency safety check (preserves battery & quota)
                providerJobPollingJob = launch {
                    val seenJobIds = mutableSetOf<String>()
                    var initialLoadDone = false
                    while (isActive) {
                        try {
                            val category = categories.firstOrNull()
                            val jobsResult = supabaseClient.getOpenJobs(category)
                            if (jobsResult.isSuccess) {
                                val jobList = jobsResult.getOrThrow()
                                val openRemoteIds = jobList.mapNotNull { it.optString("id").takeIf { id -> id.isNotBlank() } }
                                repository.reconcileOpenJobs(openRemoteIds)

                                for (jobObj in jobList) {
                                    val remoteId = jobObj.optString("id")
                                    val isNew = remoteId.isNotBlank() && seenJobIds.add(remoteId)
                                    val entity = jsonToServiceRequestEntity(jobObj)
                                    val localId = repository.syncRemoteJob(entity)
                                    val savedEntity = entity.copy(id = localId)

                                    if (isNew && initialLoadDone) {
                                        _fullscreenPingJob.value = savedEntity
                                    }
                                }
                                initialLoadDone = true
                            }
                        } catch (_: Exception) {}
                        delay(25000L) // 25 second safety net
                    }
                }
            }
        }
    }

    fun refreshProviderStatus() {
        viewModelScope.launch {
            val current = _currentUser.value ?: return@launch
            if (current.role != "PROVIDER") return@launch

            _isRefreshingStatus.value = true
            _statusCheckMessage.value = null

            val sessionUserId = supabaseClient.getSession()?.userId ?: ""
            if (sessionUserId.isBlank()) {
                _statusCheckMessage.value = "Session expired. Please sign in again."
                _isRefreshingStatus.value = false
                return@launch
            }

            val result = supabaseClient.getProviderOwnProfile(sessionUserId)
            _isRefreshingStatus.value = false

            if (result.isSuccess) {
                val profile = result.getOrNull()
                if (profile != null) {
                    val remoteStatus = profile.optString("status", "pending").uppercase()
                    val updated = current.copy(status = remoteStatus)
                    repository.saveUser(updated)
                    _currentUser.value = updated

                    if (remoteStatus == "APPROVED" || remoteStatus == "ACTIVE") {
                        _statusCheckMessage.value = "Your application has been approved by admin!"
                    } else if (remoteStatus == "REJECTED") {
                        _statusCheckMessage.value = "Application was not approved by admin."
                    } else {
                        _statusCheckMessage.value = "Status: Under review by admin."
                    }
                } else {
                    _statusCheckMessage.value = "No provider profile found on server."
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to query server"
                _statusCheckMessage.value = "Status check error: $err"
            }
        }
    }

    fun navigateBack() {
        when (_currentDestination.value) {
            AppNavDestination.OTP_VERIFICATION -> _currentDestination.value = AppNavDestination.PHONE_ENTRY
            AppNavDestination.PHONE_ENTRY -> _currentDestination.value = AppNavDestination.AUTH_CHOICE
            AppNavDestination.AUTH_CHOICE -> _currentDestination.value = AppNavDestination.ROLE_SELECT
            AppNavDestination.ROLE_SELECT -> _currentDestination.value = AppNavDestination.LANGUAGE_SELECT
            AppNavDestination.CUSTOMER_REQUEST_FLOW -> _currentDestination.value = AppNavDestination.CUSTOMER_HOME
            AppNavDestination.CUSTOMER_LIVE_TRACKING -> _currentDestination.value = AppNavDestination.CUSTOMER_HOME
            AppNavDestination.PROVIDER_JOB_ACCEPT -> _currentDestination.value = AppNavDestination.PROVIDER_HOME
            AppNavDestination.JOB_CHAT -> {
                _currentDestination.value = if (_activeRole.value == UserRole.PROVIDER) {
                    AppNavDestination.PROVIDER_HOME
                } else {
                    AppNavDestination.CUSTOMER_LIVE_TRACKING
                }
            }
            AppNavDestination.IN_CALL -> {
                _currentDestination.value = if (_activeChatJob.value != null) {
                    AppNavDestination.JOB_CHAT
                } else if (_activeRole.value == UserRole.PROVIDER) {
                    AppNavDestination.PROVIDER_HOME
                } else {
                    AppNavDestination.CUSTOMER_LIVE_TRACKING
                }
            }
            else -> {}
        }
    }

    fun navigateToHome() {
        _currentDestination.value = if (_activeRole.value == UserRole.PROVIDER) {
            AppNavDestination.PROVIDER_HOME
        } else {
            AppNavDestination.CUSTOMER_HOME
        }
    }
}
