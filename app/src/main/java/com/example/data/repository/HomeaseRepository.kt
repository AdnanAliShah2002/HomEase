package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.JobOfferEntity
import com.example.data.db.JobRatingEntity
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

        val existingCustomer = userDao.getUserByPhone("+923001234567")
        if (existingCustomer == null) {
            // Seed demo customer
            userDao.insertUser(
                UserEntity(
                    phone = "+923001234567",
                    role = "CUSTOMER",
                    name = "Adnan Shah",
                    cityArea = "Lahore - Gulberg III",
                    homeAddress = "House 42-B, Main Boulevard, Gulberg III",
                    notifPref = "WHATSAPP",
                    savedAddressesCsv = "Home: House 42-B, Main Boulevard, Gulberg III|Office: 3rd Floor, Siddiq Trade Centre, Gulberg II",
                    status = "ACTIVE"
                )
            )
            // Seed demo provider
            userDao.insertUser(
                UserEntity(
                    phone = "+923217654321",
                    role = "PROVIDER",
                    name = "Ustad Muhammad Rashid",
                    cityArea = "Lahore - Gulberg II",
                    categoriesCsv = "plumbing,electrical",
                    yearsExperience = "8 years",
                    serviceRadiusKm = 12,
                    cnicNumber = "35201-8492019-3",
                    shopName = "Rashid Sanitary & Electric Store",
                    bio = "Experienced technician specializing in sanitary fittings, water heaters, AC repairs, and home wiring. Available across Gulberg & Model Town.",
                    payoutMethod = "JazzCash",
                    payoutAccountNumber = "03217654321",
                    status = "APPROVED",
                    isOnline = true,
                    avgRating = 4.9,
                    totalJobs = 14
                )
            )

            // Seed an initial sample active request so provider home screen shows real data
            val sampleRequestId = requestDao.insertRequest(
                ServiceRequestEntity(
                    customerPhone = "+923001234567",
                    customerName = "Adnan Shah",
                    categoryId = "plumbing",
                    categoryTitle = "Plumbing",
                    serviceTitle = "Kitchen Sink Pipe Leakage",
                    description = "Water leaking under kitchen sink trap. Needs quick seal or replacement pipe.",
                    cityArea = "Gulberg III, Lahore",
                    fullAddress = "House 42-B, Main Boulevard, Gulberg III, Lahore",
                    budgetRs = 1500,
                    status = "SEARCHING"
                )
            )

            // Seed a sample offer
            offerDao.insertOffer(
                JobOfferEntity(
                    requestId = sampleRequestId,
                    providerPhone = "+923217654321",
                    providerName = "Ustad Muhammad Rashid",
                    counterPriceRs = 1500,
                    distanceKm = 1.4,
                    providerRating = 4.9,
                    status = "PENDING"
                )
            )

            // Seed sample past bookings for customer & provider history
            val pastJobId = requestDao.insertRequest(
                ServiceRequestEntity(
                    customerPhone = "+923001234567",
                    customerName = "Adnan Shah",
                    categoryId = "electrical",
                    categoryTitle = "Electrical",
                    serviceTitle = "Ceiling Fan Capacitor & Bearing",
                    description = "Master bedroom ceiling fan slow speed and squeaking noise.",
                    cityArea = "Gulberg III, Lahore",
                    fullAddress = "House 42-B, Gulberg III, Lahore",
                    budgetRs = 1200,
                    agreedPriceRs = 1200,
                    status = "COMPLETED",
                    selectedProviderPhone = "+923217654321",
                    selectedProviderName = "Ustad Muhammad Rashid",
                    createdAt = System.currentTimeMillis() - 86400000L * 3, // 3 days ago
                    completedAt = System.currentTimeMillis() - 86400000L * 3 + 3600000L,
                    ratingGiven = 5,
                    ratingComment = "Very punctual and fixed the fan bearing smoothly. Highly recommended!"
                )
            )

            jobRatingDao.insertRating(
                JobRatingEntity(
                    jobId = pastJobId,
                    providerPhone = "+923217654321",
                    customerPhone = "+923001234567",
                    rating = 5,
                    comment = "Very punctual and fixed the fan bearing smoothly. Highly recommended!"
                )
            )

            val pastJobId2 = requestDao.insertRequest(
                ServiceRequestEntity(
                    customerPhone = "+923001234567",
                    customerName = "Adnan Shah",
                    categoryId = "appliances",
                    categoryTitle = "AC & Appliances",
                    serviceTitle = "Inverter AC Gas Refill & Cleaning",
                    description = "Split AC cooling decreased before summer. General service needed.",
                    cityArea = "Gulberg III, Lahore",
                    fullAddress = "House 42-B, Gulberg III, Lahore",
                    budgetRs = 3500,
                    agreedPriceRs = 3500,
                    status = "COMPLETED",
                    selectedProviderPhone = "+923217654321",
                    selectedProviderName = "Ustad Muhammad Rashid",
                    createdAt = System.currentTimeMillis() - 86400000L * 7,
                    completedAt = System.currentTimeMillis() - 86400000L * 7 + 7200000L,
                    ratingGiven = 5,
                    ratingComment = "Excellent AC service. Gas pressures verified and cools great."
                )
            )

            jobRatingDao.insertRating(
                JobRatingEntity(
                    jobId = pastJobId2,
                    providerPhone = "+923217654321",
                    customerPhone = "+923001234567",
                    rating = 5,
                    comment = "Excellent AC service. Gas pressures verified and cools great."
                )
            )
        }
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

    suspend fun createServiceRequest(request: ServiceRequestEntity): Long {
        val id = requestDao.insertRequest(request)
        val askingPrice = if (request.customerAskingPrice > 0) request.customerAskingPrice else request.budgetRs
        val isInspectionNeeded = request.categoryId in listOf("plumbing", "electrical", "appliance_repair", "carpentry", "painting")

        // Automatically simulate nearby provider responses for InDrive bidding experience
        CoroutineScope(Dispatchers.IO).launch {
            delay(1500)
            // Provider 1: Accepts directly at customer's listed asking price
            offerDao.insertOffer(
                JobOfferEntity(
                    requestId = id,
                    providerPhone = "+923334567890",
                    providerName = "Kashif Ali (Master Plumber)",
                    counterPriceRs = askingPrice,
                    offerPriceRs = askingPrice,
                    distanceKm = 1.2,
                    providerRating = 4.9,
                    status = "pending",
                    offerNote = "Accepted at your asking price of Rs $askingPrice"
                )
            )
            delay(2000)
            // Provider 2: Counter offer or inspection quote
            val provider2Price = if (isInspectionNeeded && askingPrice > 800) 300 else (askingPrice * 1.15).toInt().coerceAtLeast(300)
            val provider2Note = if (isInspectionNeeded && askingPrice > 800) "Rs 300 to inspect and quote" else "Available immediately in 20 mins"
            offerDao.insertOffer(
                JobOfferEntity(
                    requestId = id,
                    providerPhone = "+923456789012",
                    providerName = "Tariq Mahmood Services",
                    counterPriceRs = provider2Price,
                    offerPriceRs = provider2Price,
                    distanceKm = 2.5,
                    providerRating = 4.8,
                    status = "pending",
                    offerNote = provider2Note
                )
            )
            delay(2000)
            // Provider 3: Competitive counter offer
            val provider3Price = (askingPrice * 1.1).toInt().coerceAtLeast(250)
            offerDao.insertOffer(
                JobOfferEntity(
                    requestId = id,
                    providerPhone = "+923217654321",
                    providerName = "Ustad Muhammad Rashid",
                    counterPriceRs = provider3Price,
                    offerPriceRs = provider3Price,
                    distanceKm = 1.8,
                    providerRating = 4.9,
                    status = "pending",
                    offerNote = "Experienced technician with all original spare parts"
                )
            )
        }
        return id
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

        val providerPhone = job.selectedProviderPhone ?: "+923217654321"
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
        val providerPhone = job.selectedProviderPhone ?: "+923217654321"
        val count = jobRatingDao.getRatingCountForProvider(providerPhone)
        val currentProvider = userDao.getUserByPhone(providerPhone)
        val currentAvg = currentProvider?.avgRating ?: 4.9
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
        notifPref: String,
        savedAddressesCsv: String
    ) {
        userDao.updateCustomerProfile(phone, name, cityArea, notifPref, savedAddressesCsv)
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
        payoutAccountNumber: String
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
            payoutAccount = payoutAccountNumber
        )
    }

    fun getOffersForRequest(requestId: Long): Flow<List<JobOfferEntity>> =
        offerDao.getOffersForRequestFlow(requestId)
}
