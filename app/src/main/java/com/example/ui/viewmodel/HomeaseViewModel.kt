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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    // Active job being tracked on the Live Map (Customer or Provider view)
    private val _trackingJob = MutableStateFlow<ServiceRequestEntity?>(null)
    val trackingJob: StateFlow<ServiceRequestEntity?> = _trackingJob.asStateFlow()

    // Customer live requests
    @OptIn(ExperimentalCoroutinesApi::class)
    val customerRequests = _currentPhoneNumber.flatMapLatest { phone ->
        if (phone.isNotBlank()) repository.getCustomerRequests(phone) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Provider available jobs
    val availableJobs = repository.getAvailableJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                        cityArea = "Lahore - Gulberg",
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
                val isExisting = !verifyData.isNewUser || localUser != null

                if (isExisting) {
                    // Existing number: log straight in to home screen — no registration form shown
                    val user = localUser ?: UserEntity(
                        phone = phone,
                        role = _activeRole.value.name,
                        name = if (_activeRole.value == UserRole.PROVIDER) "Service Provider" else "Customer",
                        cityArea = "Lahore - Gulberg",
                        status = "ACTIVE"
                    )
                    if (localUser == null) {
                        repository.saveUser(user)
                    }
                    _currentUser.value = user
                    sessionManager.saveSession(user.phone, user.role)
                    if (user.role == "PROVIDER") {
                        _activeRole.value = UserRole.PROVIDER
                        _currentDestination.value = AppNavDestination.PROVIDER_HOME
                    } else {
                        _activeRole.value = UserRole.CUSTOMER
                        _currentDestination.value = AppNavDestination.CUSTOMER_HOME
                    }
                } else {
                    // New number: route to registration form based on the selected role
                    if (_activeRole.value == UserRole.PROVIDER) {
                        _currentDestination.value = AppNavDestination.PROVIDER_REGISTRATION
                    } else {
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

    fun completeProviderRegistration(user: UserEntity) {
        viewModelScope.launch {
            val pendingUser = user.copy(status = "PENDING")
            repository.saveUser(pendingUser)
            sessionManager.saveSession(pendingUser.phone, "PROVIDER")
            _currentUser.value = pendingUser
            _activeRole.value = UserRole.PROVIDER
            _currentDestination.value = AppNavDestination.PROVIDER_HOME
            // Background remote sync to Supabase storage and table
            providerRemoteService.submitProviderRegistration(pendingUser)
        }
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

    fun submitServiceRequest(request: ServiceRequestEntity) {
        viewModelScope.launch {
            val id = repository.createServiceRequest(request)
            val updated = request.copy(id = id)
            _activeLiveRequest.value = updated
            _trackingJob.value = updated
            // Listen for incoming offers reactively
            repository.getOffersForRequest(id).collect { offers ->
                _incomingOffers.value = offers
            }
        }
    }

    fun selectOfferForRequest(offer: JobOfferEntity) {
        viewModelScope.launch {
            val req = _activeLiveRequest.value ?: return@launch
            repository.customerSelectOffer(req.id, offer)
            val accepted = req.copy(
                status = "ACCEPTED",
                selectedProviderName = offer.providerName,
                selectedProviderPhone = offer.providerPhone,
                agreedPriceRs = offer.counterPriceRs
            )
            _activeLiveRequest.value = accepted
            _trackingJob.value = accepted
        }
    }

    fun acceptJobAsProvider(job: ServiceRequestEntity) {
        viewModelScope.launch {
            val phone = _currentPhoneNumber.value
            val provider = _currentUser.value ?: if (phone.isNotBlank()) repository.getUser(phone) else null
            val resolvedPhone = provider?.phone ?: phone
            val resolvedName = provider?.name?.ifBlank { null } ?: "Service Provider"
            repository.acceptJobByProvider(
                requestId = job.id,
                providerPhone = resolvedPhone,
                providerName = resolvedName,
                agreedPrice = job.budgetRs
            )
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
            val phone = _currentPhoneNumber.value
            val provider = _currentUser.value ?: if (phone.isNotBlank()) repository.getUser(phone) else null
            val resolvedPhone = provider?.phone ?: phone
            val resolvedName = provider?.name?.ifBlank { null } ?: "Service Provider"
            repository.submitProviderCounter(
                requestId = job.id,
                providerPhone = resolvedPhone,
                providerName = resolvedName,
                counterPrice = counterPrice,
                note = note
            )
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
            repository.markJobOnTheWay(job.id)
            supabaseClient.updateJobStatus(job.id.toString(), "ON_THE_WAY")
            val pPhone = job.selectedProviderPhone ?: _currentPhoneNumber.value
            ProviderLocationService.start(
                context = getApplication(),
                jobId = job.id.toString(),
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
            repository.markJobArrived(job.id)
            supabaseClient.updateJobStatus(job.id.toString(), "ARRIVED")
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
            repository.markJobInProgress(job.id)
            supabaseClient.updateJobStatus(job.id.toString(), "IN_PROGRESS")
            ProviderLocationService.stop(getApplication())
        }
    }

    fun completeActiveJob(jobId: Long) {
        viewModelScope.launch {
            ProviderLocationService.stop(getApplication())
            repository.markJobAwaitingConfirmation(jobId)
            supabaseClient.updateJobStatus(jobId.toString(), "AWAITING_CUSTOMER_CONFIRMATION")
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
        savedAddressesCsv: String
    ) {
        viewModelScope.launch {
            val phone = _currentPhoneNumber.value
            repository.updateCustomerProfile(phone, name, cityArea, savedAddressesCsv)
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
        payoutAccountNumber: String
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
                payoutAccountNumber = payoutAccountNumber
            )
            val updated = repository.getUser(phone)
            if (updated != null) {
                _currentUser.value = updated
            }
        }
    }

    fun toggleProviderOnline(isOnline: Boolean) {
        viewModelScope.launch {
            val phone = _currentPhoneNumber.value
            repository.setProviderOnline(phone, isOnline)
        }
    }

    fun toggleProviderVerificationStatus() {
        viewModelScope.launch {
            val current = _currentUser.value ?: return@launch
            if (current.role == "PROVIDER") {
                val newStatus = if (current.status == "APPROVED") "PENDING" else "APPROVED"
                val updated = current.copy(status = newStatus)
                repository.saveUser(updated)
                _currentUser.value = updated
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
