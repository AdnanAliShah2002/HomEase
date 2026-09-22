package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.CallLogEntity
import com.example.data.db.JobMessageEntity
import com.example.data.db.JobOfferEntity
import com.example.data.db.JobRatingEntity
import com.example.data.db.ProviderLocationEntity
import com.example.data.db.ServiceCategoryEntity
import com.example.data.db.ServiceRequestEntity
import com.example.data.db.UserEntity
import com.example.data.model.ServiceCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HomeaseRepository(private val database: AppDatabase) {
    private val userDao = database.userDao()
    private val requestDao = database.serviceRequestDao()
    private val offerDao = database.jobOfferDao()
    private val jobRatingDao = database.jobRatingDao()
    private val categoryDao = database.serviceCategoryDao()
    private val locationDao = database.providerLocationDao()
    private val jobMessageDao = database.jobMessageDao()
    private val callLogDao = database.callLogDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfEmpty()
        }
    }

    suspend fun seedCategoriesIfEmpty() {
        if (categoryDao.getCategoryCount() == 0) {
            val entities = ServiceCatalog.categories.map { cat ->
                ServiceCategoryEntity(
                    id = cat.id,
                    name = cat.nameKey,
                    urduName = cat.nameKey,
                    priceMin = cat.priceMin,
                    priceMax = cat.priceMax,
                    priceBasis = cat.priceBasis,
                    isEstimateOnly = cat.isEstimateOnly,
                    popularServicesCsv = cat.popularServices.joinToString("|")
                )
            }
            categoryDao.insertCategories(entities)
        }
    }

    private suspend fun seedInitialDataIfEmpty() {
        // Sanitize any previously corrupted concatenated numbers (> 100,000 PKR)
        requestDao.sanitizeCorruptedBudgets()
        requestDao.sanitizeCorruptedAgreedPrices()
        offerDao.sanitizeCorruptedOffers()

        // Seed service categories with reference pricing ranges
        seedCategoriesIfEmpty()

        // Clean up any legacy demo / sandbox accounts or requests to maintain clean production state
        userDao.deleteDemoUsers()
        requestDao.deleteDemoRequests()
        offerDao.deleteDemoOffers()
        jobRatingDao.deleteDemoRatings()

    }

    // User Operations
    fun getUserFlow(phone: String): Flow<UserEntity?> = userDao.getUserByPhoneFlow(phone)
    suspend fun getUser(phone: String): UserEntity? = userDao.getUserByPhone(phone)
    suspend fun saveUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun setProviderOnline(phone: String, isOnline: Boolean) =
        userDao.updateProviderOnlineStatus(phone, isOnline)

    // Request Operations
    fun getCustomerRequests(customerPhone: String): Flow<List<ServiceRequestEntity>> =
        requestDao.getCustomerRequestsFlow(customerPhone)

    fun getAvailableJobs(): Flow<List<ServiceRequestEntity>> =
        requestDao.getAvailableJobsFlow()

    fun getActiveJobForProvider(providerPhone: String): Flow<ServiceRequestEntity?> =
        requestDao.getActiveJobForProviderFlow(providerPhone)

    fun getCompletedJobsForProvider(providerPhone: String): Flow<List<ServiceRequestEntity>> =
        requestDao.getCompletedJobsForProviderFlow(providerPhone)

    fun getRequestByIdFlow(id: Long): Flow<ServiceRequestEntity?> =
        requestDao.getRequestByIdFlow(id)

    suspend fun getRequestById(id: Long): ServiceRequestEntity? =
        requestDao.getRequestById(id)

    suspend fun getRequestByRemoteId(remoteId: String): ServiceRequestEntity? =
        requestDao.getRequestByRemoteId(remoteId)

    suspend fun reconcileOpenJobs(openRemoteIds: List<String>) {
        requestDao.clearObsoleteSearchingJobs(openRemoteIds)
    }

    suspend fun syncRemoteJob(job: ServiceRequestEntity): Long {
        val remoteId = job.remoteId
        if (!remoteId.isNullOrBlank()) {
            val existing = requestDao.getRequestByRemoteId(remoteId)
            if (existing != null) {
                val updated = job.copy(id = existing.id)
                requestDao.updateRequest(updated)
                return existing.id
            }
        }
        return requestDao.insertRequest(job)
    }

    suspend fun syncRemoteOffer(offer: JobOfferEntity): Long {
        val remoteOfferId = offer.remoteOfferId
        if (!remoteOfferId.isNullOrBlank()) {
            val existing = offerDao.getOfferByRemoteOfferId(remoteOfferId)
            if (existing != null) {
                val updated = offer.copy(id = existing.id)
                offerDao.updateOfferStatus(existing.id, offer.status)
                return existing.id
            }
        }
        return offerDao.insertOffer(offer)
    }

    suspend fun getOffersForRequestSync(requestId: Long): List<JobOfferEntity> =
        offerDao.getOffersForRequest(requestId)

    suspend fun createServiceRequest(request: ServiceRequestEntity): Long {
        return requestDao.insertRequest(request)
    }

    suspend fun acceptJobByProvider(requestId: Long, providerPhone: String, providerName: String, agreedPrice: Int) {
        requestDao.acceptJob(
            id = requestId,
            status = "ACCEPTED",
            providerPhone = providerPhone,
            providerName = providerName,
            price = agreedPrice
        )
    }

    suspend fun customerSelectOffer(requestId: Long, offer: JobOfferEntity) {
        val agreedPrice = if (offer.offerPriceRs > 0) offer.offerPriceRs else offer.counterPriceRs
        requestDao.acceptJob(
            id = requestId,
            status = "ACCEPTED",
            providerPhone = offer.providerPhone,
            providerName = offer.providerName,
            price = agreedPrice
        )
        offerDao.markOfferAccepted(offer.id)
        offerDao.expireOtherOffersForRequest(requestId, offer.id)

        // Seed initial provider location nearby the customer so live tracking map works immediately
        try {
            val req = requestDao.getRequestById(requestId)
            val baseLat = req?.lat ?: 31.5204
            val baseLng = req?.lng ?: 74.3587
            locationDao.upsertLocation(
                ProviderLocationEntity(
                    jobId = requestId.toString(),
                    providerId = offer.providerPhone,
                    lat = baseLat + 0.012,
                    lng = baseLng + 0.009,
                    heading = 45.0,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {
            // Ignore if error
        }
    }

    suspend fun customerSelectOffer(offer: JobOfferEntity) {
        customerSelectOffer(offer.requestId, offer)
    }

    suspend fun submitProviderCounter(
        requestId: Long,
        providerPhone: String,
        providerName: String,
        counterPrice: Int,
        note: String? = null
    ) {
        offerDao.insertOffer(
            JobOfferEntity(
                requestId = requestId,
                providerPhone = providerPhone,
                providerName = providerName,
                counterPriceRs = counterPrice,
                offerPriceRs = counterPrice,
                distanceKm = 1.5,
                providerRating = 4.9,
                status = "pending",
                offerNote = note
            )
        )
    }

    fun getAllCategoriesFlow(): Flow<List<ServiceCategoryEntity>> =
        categoryDao.getAllCategoriesFlow()

    suspend fun getAllServiceCategories(): List<ServiceCategoryEntity> =
        categoryDao.getAllCategories()

    suspend fun getCategoryById(id: String): ServiceCategoryEntity? =
        categoryDao.getCategoryById(id)

    fun getProviderLocationForJobFlow(jobId: String): Flow<ProviderLocationEntity?> =
        locationDao.getLocationForJobFlow(jobId)

    suspend fun getProviderLocationForJob(jobId: String): ProviderLocationEntity? =
        locationDao.getLocationForJob(jobId)

    suspend fun saveProviderLocation(location: ProviderLocationEntity) {
        locationDao.upsertLocation(location)
    }

    suspend fun updateJobStatus(requestId: Long, status: String) {
        requestDao.updateJobStatusWithTimestamp(requestId, status, System.currentTimeMillis())
    }

    suspend fun markJobOnTheWay(requestId: Long) {
        requestDao.updateJobStatusWithTimestamp(requestId, "ON_THE_WAY", System.currentTimeMillis())
    }

    suspend fun markJobArrived(requestId: Long) {
        requestDao.updateJobStatusWithTimestamp(requestId, "ARRIVED", System.currentTimeMillis())
    }

    suspend fun markJobInProgress(requestId: Long) {
        requestDao.updateJobStatusWithTimestamp(requestId, "IN_PROGRESS", System.currentTimeMillis())
    }

    suspend fun completeJob(requestId: Long) {
        requestDao.updateStatus(requestId, "COMPLETED")
    }

    suspend fun markJobAwaitingConfirmation(requestId: Long) {
        requestDao.markJobAwaitingConfirmation(requestId)
    }

    suspend fun completeJobWithRating(requestId: Long, rating: Int, comment: String) {
        val job = requestDao.getRequestById(requestId) ?: return
        val now = System.currentTimeMillis()
        requestDao.completeJobWithRating(requestId, rating, comment, now)

        val providerPhone = job.selectedProviderPhone ?: return
        // Insert rating entity
        jobRatingDao.insertRating(
            JobRatingEntity(
                jobId = requestId,
                providerPhone = providerPhone,
                customerPhone = job.customerPhone,
                rating = rating,
                comment = comment,
                createdAt = now
            )
        )

        // Recalculate provider average rating and total jobs count
        val avg = jobRatingDao.getAverageRatingForProvider(providerPhone) ?: rating.toDouble()
        val count = jobRatingDao.getRatingCountForProvider(providerPhone)
        val roundedAvg = Math.round(avg * 10.0) / 10.0
        userDao.updateProviderRatingStats(providerPhone, roundedAvg, count)
    }

    suspend fun autoCompleteJobWithoutRating(requestId: Long) {
        val job = requestDao.getRequestById(requestId) ?: return
        val now = System.currentTimeMillis()
        requestDao.autoCompleteJobWithoutRating(requestId, now)
        val providerPhone = job.selectedProviderPhone ?: return
        val count = jobRatingDao.getRatingCountForProvider(providerPhone)
        val currentProvider = userDao.getUserByPhone(providerPhone)
        val currentAvg = currentProvider?.avgRating ?: 5.0
        userDao.updateProviderRatingStats(providerPhone, currentAvg, count + 1)
    }

    suspend fun reportJobIssue(requestId: Long, category: String, description: String) {
        requestDao.reportJobIssue(requestId, category, description)
    }

    fun getPastJobsForProvider(providerPhone: String): Flow<List<ServiceRequestEntity>> =
        requestDao.getPastJobsForProviderFlow(providerPhone)

    fun getAllJobsForProvider(providerPhone: String): Flow<List<ServiceRequestEntity>> =
        requestDao.getAllJobsForProviderFlow(providerPhone)

    fun getAwaitingConfirmationForCustomer(customerPhone: String): Flow<ServiceRequestEntity?> =
        requestDao.getAwaitingConfirmationForCustomerFlow(customerPhone)

    fun getRatingsForProvider(providerPhone: String): Flow<List<JobRatingEntity>> =
        jobRatingDao.getRatingsForProviderFlow(providerPhone)

    suspend fun updateCustomerProfile(
        phone: String,
        name: String,
        cityArea: String,
        savedAddressesCsv: String,
        lat: Double? = null,
        lng: Double? = null
    ) {
        userDao.updateCustomerProfile(phone, name, cityArea, savedAddressesCsv, lat, lng)
    }

    suspend fun updateProviderProfile(
        phone: String,
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
        userDao.updateProviderProfile(
            phone = phone,
            name = name,
            cityArea = cityArea,
            categories = categoriesCsv,
            years = yearsExperience,
            radius = serviceRadiusKm,
            bio = bio,
            shopName = shopName,
            payoutMethod = payoutMethod,
            payoutAccount = payoutAccountNumber,
            lat = lat,
            lng = lng
        )
    }

    fun getOffersForRequest(requestId: Long): Flow<List<JobOfferEntity>> =
        offerDao.getOffersForRequestFlow(requestId)

    // ========================================================================
    // In-App Messaging & Calling
    // ========================================================================

    fun getMessagesForJobFlow(jobId: String): Flow<List<JobMessageEntity>> =
        jobMessageDao.getMessagesForJobFlow(jobId)

    suspend fun getMessagesForJob(jobId: String): List<JobMessageEntity> =
        jobMessageDao.getMessagesForJob(jobId)

    suspend fun insertMessage(message: JobMessageEntity) =
        jobMessageDao.insertMessage(message)

    suspend fun insertMessages(messages: List<JobMessageEntity>) =
        jobMessageDao.insertMessages(messages)

    suspend fun markMessagesAsRead(jobId: String, currentUserId: String) =
        jobMessageDao.markMessagesAsRead(jobId, currentUserId)

    fun getUnreadMessageCountFlow(jobId: String, currentUserId: String): Flow<Int> =
        jobMessageDao.getUnreadCountFlow(jobId, currentUserId)

    suspend fun getUnreadMessageCount(jobId: String, currentUserId: String): Int =
        jobMessageDao.getUnreadCount(jobId, currentUserId)

    suspend fun insertCallLog(callLog: CallLogEntity) =
        callLogDao.insertCallLog(callLog)

    suspend fun updateCallLog(id: String, endedAt: Long, durationSeconds: Int) =
        callLogDao.updateCallLog(id, endedAt, durationSeconds)

    fun getCallLogsForJobFlow(jobId: String): Flow<List<CallLogEntity>> =
        callLogDao.getCallLogsForJobFlow(jobId)
}

