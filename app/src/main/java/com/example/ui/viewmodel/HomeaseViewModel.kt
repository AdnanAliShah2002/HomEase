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
            val localJob = repository.getRequestByRemoteId(jobId) ?: repository.getRequestById(jobId.toLongOrNull() ?: -1L)
            if (localJob != null) {
                when (notificationType) {
                    "job_ping" -> {
                        _fullscreenPingJob.value = localJob
                        _currentDestination.value = AppNavDestination.PROVIDER_JOB_ACCEPT
                    }
                    "offer_received" -> {
                        openRequestDetails(localJob.id)
                    }
                    "offer_accepted", "status_update" -> {
                        openLiveTracking(localJob)
                    }
                    "new_message" -> {
                        openJobChat(localJob)
                    }
                    else -> {
                        openLiveTracking(localJob)
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

        viewModelScope.launch {
            refreshActiveTheme()
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
        val formattedPhone = if (phone.startsWith("+")) phone else "+$phone"
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
        val phone = _currentPhoneNumber.value
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
        val phone = _currentPhoneNumber.value
        _isVerifyingOtp.value = true
        _otpVerifyError.value = null
        viewModelScope.launch {
            val result = otpService.verifyOtp(phone, code)
            _isVerifyingOtp.value = false
            if (result.isSuccess) {
                val verifyData = result.getOrNull()!!
                val sessionUserId = verifyData.userId ?: UUID.randomUUID().toString()
                val sessionToken = verifyData.accessToken ?: UUID.randomUUID().toString()
                supabaseClient.setSession(
                    SupabaseSession(
                        userId = sessionUserId,
                        phone = phone,
                        accessToken = sessionToken,
                        role = _activeRole.value.name.lowercase()
                    )
                )

                val localUser = repository.getUser(phone)
                val isProvider = _activeRole.value == UserRole.PROVIDER

                if (isProvider) {
                    // Check remote service_providers table or completed local provider entity
                    var remoteProviderProfile: JSONObject? = null
                    if (sessionUserId.isNotBlank()) {
                        val profileRes = supabaseClient.getProviderOwnProfile(sessionUserId)
                        remoteProviderProfile = profileRes.getOrNull()
                    }

                    val hasValidRemoteProfile = remoteProviderProfile != null &&
                            remoteProviderProfile.optString("cnic_number").isNotBlank() &&
                            remoteProviderProfile.optString("cnic_number") != "PENDING"

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
                        val currentStatus = remoteStatus ?: localUser?.status ?: "PENDING"
                        val user = (localUser ?: UserEntity(
                            phone = phone,
                            role = "PROVIDER",
                            name = remoteProviderProfile?.optString("full_name")?.ifBlank { "Service Provider" } ?: "Service Provider",
                            cityArea = remoteProviderProfile?.optString("city_area")?.ifBlank { "Islamabad" } ?: "Islamabad",
                            status = currentStatus
                        )).copy(status = currentStatus)

                        repository.saveUser(user)
                        _currentUser.value = user
                        sessionManager.saveSession(user.phone, "PROVIDER")
                        _activeRole.value = UserRole.PROVIDER
                        _currentDestination.value = AppNavDestination.PROVIDER_HOME
                    } else {
                        // Incomplete or new provider: route to Provider Registration screen
                        _currentDestination.value = AppNavDestination.PROVIDER_REGISTRATION
                    }
                } else {
                    // Customer: check if complete profile exists (name is not default placeholder)
                    val hasCompleteCustomerProfile = localUser != null &&
                            localUser.role == "CUSTOMER" &&
                            localUser.name.isNotBlank() &&
                            localUser.name != "Valued Customer" &&
                            localUser.name != "Customer" &&
                            !verifyData.isNewUser

                    if (hasCompleteCustomerProfile) {
                        _currentUser.value = localUser
                        sessionManager.saveSession(localUser!!.phone, "CUSTOMER")
                        _activeRole.value = UserRole.CUSTOMER
                        _currentDestination.value = AppNavDestination.CUSTOMER_HOME
                    } else {
                        // Incomplete or new customer: route to Customer Registration screen
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
                onSuccess()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Provider registration failed on server"
                _providerRegistrationError.value = error
            }
        }
    }

    fun proceedToProviderDashboard() {
        _currentDestination.value = AppNavDestination.PROVIDER_HOME
    }

    fun logout() {
        sessionManager.clearSession()
        supabaseClient.clearSession()
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
    }

    fun openChat(job: ServiceRequestEntity) = openJobChat(job)

    fun openJobChat(job: ServiceRequestEntity) {
        _activeChatJob.value = job
        _currentDestination.value = AppNavDestination.JOB_CHAT
        val currentUserId = _currentUser.value?.phone ?: _currentPhoneNumber.value
        viewModelScope.launch {
            repository.markMessagesAsRead(job.id.toString(), currentUserId)
            supabaseClient.markMessagesAsRead(job.id.toString(), currentUserId)
        }
    }

    fun startVoiceCall(job: ServiceRequestEntity) {
        val currentPhone = _currentUser.value?.phone ?: _currentPhoneNumber.value
        val isProvider = _activeRole.value == UserRole.PROVIDER
        val targetName = if (isProvider) job.customerName.ifBlank { "Customer" } else (job.selectedProviderName ?: "Service Provider")
        val targetRole = if (isProvider) "Customer" else "Service Provider"
        val targetPhone = if (isProvider) job.customerPhone else (job.selectedProviderPhone ?: "")

        agoraVoiceManager.startCall(
            jobId = job.id.toString(),
            targetName = targetName,
            targetRole = targetRole,
            targetPhone = targetPhone,
            currentUserId = currentPhone,
            repository = repository,
            supabaseClient = supabaseClient
        )
        _currentDestination.value = AppNavDestination.IN_CALL
    }

    fun toggleCallMute() {
        agoraVoiceManager.toggleMute()
    }

    fun toggleCallSpeaker() {
        agoraVoiceManager.toggleSpeaker()
    }

    fun endVoiceCall() {
        agoraVoiceManager.endCall(repository, supabaseClient)
    }

    fun closeCallScreen() {
        agoraVoiceManager.resetToIdle()
        navigateBack()
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

            // Realtime subscription
            val channel = supabaseClient.realtime.channel("job-chat-$jobId")
            try {
                channel.subscribe()
                channel.postgresChangeFlow<JobMessage>(schema = "public") {
                    table = "job_messages"
                    filter = "job_id=eq.$jobId"
                }.collect { action ->
                    val m = action.record
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
            val result = supabaseClient.sendJobMessage(jobId, currentUserId, senderType, text.trim())
            if (result.isSuccess) {
                val sent = result.getOrNull()
                if (sent != null && sent.id != tempId) {
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
    fun getLiveTrackingLocationFlow(jobId: Long): Flow<ProviderLocation> = flow {
        // 1. Initial cached position if available
        val initialLocal = repository.getProviderLocationForJob(jobId.toString())
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
        val channel = supabaseClient.realtime.channel("job-tracking-$jobId")
        try {
            channel.subscribe()
            channel.postgresChangeFlow<ProviderLocation>(schema = "public") {
                table = "provider_locations"
                filter = "job_id=eq.$jobId"
            }.collect { change ->
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
            remoteId = remoteId
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
            remoteOfferId = remoteOfferId
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
                budgetRs = request.budgetRs
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
                providerId = null,
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

            // Primary blocking write to Supabase
            val acceptResult = supabaseClient.acceptJobDirectly(
                jobId = remoteJobId,
                providerId = provider?.phone,
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

            // Primary blocking write to Supabase public.job_offers
            val offerResult = supabaseClient.submitJobOffer(
                jobId = remoteJobId,
                providerId = provider?.phone,
                providerPhone = resolvedPhone,
                providerName = resolvedName,
                offerPriceRs = counterPrice,
                counterPriceRs = counterPrice,
                distanceKm = 1.5,
                providerRating = provider?.avgRating ?: 4.8,
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
                distanceKm = 1.5,
                providerRating = provider?.avgRating ?: 4.8,
                status = "pending",
                offerNote = note,
                remoteOfferId = remoteOfferId
            )
            repository.syncRemoteOffer(offerWithRemote)

            _isProviderActionLoading.value = false
            _fullscreenPingJob.value = null
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
                providerId = pPhone
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
        }
    }

    private var providerJobsRealtimeChannel: com.example.data.remote.RealtimeChannel? = null
    private var providerJobRealtimeJob: kotlinx.coroutines.Job? = null
    private var providerJobPollingJob: kotlinx.coroutines.Job? = null

    fun toggleProviderOnline(isOnline: Boolean) {
        viewModelScope.launch {
            val phone = _currentPhoneNumber.value
            repository.setProviderOnline(phone, isOnline)

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
                // 1. Instant Realtime WebSocket subscription for open jobs
                val channel = supabaseClient.realtime.channel("provider-open-jobs")
                providerJobsRealtimeChannel = channel
                channel.subscribe()

                providerJobRealtimeJob = launch {
                    channel.postgresChangeFlow<JSONObject>("public") {
                        table = "jobs"
                        filter = ""
                    }.collect { action ->
                        try {
                            val record = action.record
                            val status = record.optString("status", "")
                            if (status.equals("SEARCHING", ignoreCase = true)) {
                                val entity = jsonToServiceRequestEntity(record)
                                val localId = repository.syncRemoteJob(entity)
                                val savedEntity = entity.copy(id = localId)
                                _fullscreenPingJob.value = savedEntity
                            }
                        } catch (_: Exception) {}
                    }
                }

                // 2. Initial fetch & 25s low-frequency safety check (preserves battery & quota)
                providerJobPollingJob = launch {
                    val seenJobIds = mutableSetOf<String>()
                    while (isActive) {
                        try {
                            val category = categories.firstOrNull()
                            val jobsResult = supabaseClient.getOpenJobs(category)
                            if (jobsResult.isSuccess) {
                                val jobList = jobsResult.getOrThrow()
                                for (jobObj in jobList) {
                                    val remoteId = jobObj.optString("id")
                                    val isNew = remoteId.isNotBlank() && seenJobIds.add(remoteId)
                                    val entity = jsonToServiceRequestEntity(jobObj)
                                    val localId = repository.syncRemoteJob(entity)
                                    val savedEntity = entity.copy(id = localId)

                                    if (isNew && seenJobIds.size > 1) {
                                        _fullscreenPingJob.value = savedEntity
                                    }
                                }
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
