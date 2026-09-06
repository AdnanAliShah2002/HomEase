package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.JobOfferEntity
import com.example.data.db.ServiceRequestEntity
import com.example.data.db.UserEntity
import com.example.data.localization.AppLanguage
import com.example.data.model.UserRole
import com.example.data.remote.HomEaseSupabaseClient
import com.example.data.remote.OtpRemoteService
import com.example.data.remote.ProviderRemoteService
import com.example.data.remote.SupabaseSession
import com.example.data.repository.HomeaseRepository
import com.example.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    PROVIDER_JOB_ACCEPT
}

class HomeaseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HomeaseRepository(AppDatabase.getDatabase(application))
    private val sessionManager = SessionManager(application)
    private val otpService = OtpRemoteService()
    private val providerRemoteService = ProviderRemoteService(application)
    private val supabaseClient = HomEaseSupabaseClient.getInstance(application)

    private val _currentDestination = MutableStateFlow(AppNavDestination.SPLASH)
    val currentDestination: StateFlow<AppNavDestination> = _currentDestination.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _activeRole = MutableStateFlow(UserRole.CUSTOMER)
    val activeRole: StateFlow<UserRole> = _activeRole.asStateFlow()

    private val _isSignInMode = MutableStateFlow(false)
    val isSignInMode: StateFlow<Boolean> = _isSignInMode.asStateFlow()

    private val _currentPhoneNumber = MutableStateFlow("+923001234567")
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

    // Customer live requests
    val customerRequests = repository.getCustomerRequests("+923001234567")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Provider available jobs
    val availableJobs = repository.getAvailableJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Provider active job
    val providerActiveJob = repository.getActiveJobForProvider("+923217654321")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Provider past / completed jobs (including awaiting rating)
    val providerPastJobs = repository.getPastJobsForProvider("+923217654321")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Provider completed jobs
    val providerCompletedJobs = repository.getCompletedJobsForProvider("+923217654321")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customer job awaiting rating / confirmation
    val customerAwaitingRatingJob = repository.getAwaitingConfirmationForCustomer("+923001234567")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setLanguage(newLanguage: AppLanguage) {
        _language.value = newLanguage
    }

    fun toggleLanguage() {
        _language.value = if (_language.value == AppLanguage.ENGLISH) AppLanguage.URDU else AppLanguage.ENGLISH
    }

    fun selectRole(role: UserRole) {
        _activeRole.value = role
        _currentPhoneNumber.value = if (role == UserRole.PROVIDER) "+923217654321" else "+923001234567"
        _currentDestination.value = AppNavDestination.AUTH_CHOICE
    }

    fun toggleRole() {
        if (_activeRole.value == UserRole.CUSTOMER) {
            _activeRole.value = UserRole.PROVIDER
            _currentPhoneNumber.value = "+923217654321"
            viewModelScope.launch {
                val provider = repository.getUser("+923217654321")
                if (provider != null) {
                    _currentUser.value = provider
                }
            }
            _currentDestination.value = AppNavDestination.PROVIDER_HOME
        } else {
            _activeRole.value = UserRole.CUSTOMER
            _currentPhoneNumber.value = "+923001234567"
            viewModelScope.launch {
                val customer = repository.getUser("+923001234567")
                if (customer != null) {
                    _currentUser.value = customer
                }
            }
            _currentDestination.value = AppNavDestination.CUSTOMER_HOME
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
                        name = if (savedRole == UserRole.PROVIDER) "Ustad Muhammad Rashid" else "Adnan Shah",
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
                repository.getOffersForRequest(requestId).collect { offers ->
                    _incomingOffers.value = offers
                }
            }
        }
        _currentDestination.value = AppNavDestination.CUSTOMER_REQUEST_FLOW
    }

    fun submitServiceRequest(request: ServiceRequestEntity) {
        viewModelScope.launch {
            val id = repository.createServiceRequest(request)
            val updated = request.copy(id = id)
            _activeLiveRequest.value = updated
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
            _activeLiveRequest.value = req.copy(
                status = "ACCEPTED",
                selectedProviderName = offer.providerName,
                selectedProviderPhone = offer.providerPhone,
                agreedPriceRs = offer.counterPriceRs
            )
        }
    }

    fun acceptJobAsProvider(job: ServiceRequestEntity) {
        viewModelScope.launch {
            val provider = _currentUser.value ?: repository.getUser("+923217654321")
            repository.acceptJobByProvider(
                requestId = job.id,
                providerPhone = provider?.phone ?: "+923217654321",
                providerName = provider?.name ?: "Ustad Muhammad Rashid",
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
            val provider = _currentUser.value ?: repository.getUser("+923217654321")
            repository.submitProviderCounter(
                requestId = job.id,
                providerPhone = provider?.phone ?: "+923217654321",
                providerName = provider?.name ?: "Ustad Muhammad Rashid",
                counterPrice = counterPrice,
                note = note
            )
            _fullscreenPingJob.value = null
            _currentDestination.value = AppNavDestination.PROVIDER_HOME
        }
    }

    fun completeActiveJob(jobId: Long) {
        viewModelScope.launch {
            repository.markJobAwaitingConfirmation(jobId)
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
        notifPref: String,
        savedAddressesCsv: String
    ) {
        viewModelScope.launch {
            val phone = _currentPhoneNumber.value
            repository.updateCustomerProfile(phone, name, cityArea, notifPref, savedAddressesCsv)
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
