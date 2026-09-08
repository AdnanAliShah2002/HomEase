package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    fun getUserByPhoneFlow(phone: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isOnline = :isOnline WHERE phone = :phone")
    suspend fun updateProviderOnlineStatus(phone: String, isOnline: Boolean)

    @Query("UPDATE users SET avgRating = :avgRating, totalJobs = :totalJobs WHERE phone = :phone")
    suspend fun updateProviderRatingStats(phone: String, avgRating: Double, totalJobs: Int)

    @Query("UPDATE users SET name = :name, cityArea = :cityArea, savedAddressesCsv = :savedAddresses WHERE phone = :phone")
    suspend fun updateCustomerProfile(phone: String, name: String, cityArea: String, savedAddresses: String)

    @Query("UPDATE users SET name = :name, cityArea = :cityArea, categoriesCsv = :categories, yearsExperience = :years, serviceRadiusKm = :radius, bio = :bio, shopName = :shopName, payoutMethod = :payoutMethod, payoutAccountNumber = :payoutAccount WHERE phone = :phone")
    suspend fun updateProviderProfile(
        phone: String,
        name: String,
        cityArea: String,
        categories: String,
        years: String,
        radius: Int,
        bio: String,
        shopName: String,
        payoutMethod: String,
        payoutAccount: String
    )
}

@Dao
interface ServiceRequestDao {
    @Query("SELECT * FROM service_requests ORDER BY createdAt DESC")
    fun getAllRequestsFlow(): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_requests WHERE customerPhone = :phone ORDER BY createdAt DESC")
    fun getCustomerRequestsFlow(phone: String): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_requests WHERE status = 'SEARCHING' ORDER BY createdAt DESC")
    fun getAvailableJobsFlow(): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_requests WHERE selectedProviderPhone = :providerPhone AND status IN ('ACCEPTED', 'IN_PROGRESS') ORDER BY createdAt DESC LIMIT 1")
    fun getActiveJobForProviderFlow(providerPhone: String): Flow<ServiceRequestEntity?>

    @Query("SELECT * FROM service_requests WHERE selectedProviderPhone = :providerPhone AND status IN ('COMPLETED', 'AWAITING_CUSTOMER_CONFIRMATION') ORDER BY createdAt DESC")
    fun getPastJobsForProviderFlow(providerPhone: String): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_requests WHERE selectedProviderPhone = :providerPhone AND status = 'COMPLETED' ORDER BY createdAt DESC")
    fun getCompletedJobsForProviderFlow(providerPhone: String): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_requests WHERE selectedProviderPhone = :providerPhone ORDER BY createdAt DESC")
    fun getAllJobsForProviderFlow(providerPhone: String): Flow<List<ServiceRequestEntity>>

    @Query("SELECT * FROM service_requests WHERE customerPhone = :phone AND status = 'AWAITING_CUSTOMER_CONFIRMATION' ORDER BY createdAt DESC LIMIT 1")
    fun getAwaitingConfirmationForCustomerFlow(phone: String): Flow<ServiceRequestEntity?>

    @Query("SELECT * FROM service_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: Long): ServiceRequestEntity?

    @Query("SELECT * FROM service_requests WHERE id = :id LIMIT 1")
    fun getRequestByIdFlow(id: Long): Flow<ServiceRequestEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: ServiceRequestEntity): Long

    @Update
    suspend fun updateRequest(request: ServiceRequestEntity)

    @Query("UPDATE service_requests SET status = :status, selectedProviderPhone = :providerPhone, selectedProviderName = :providerName, agreedPriceRs = :price WHERE id = :id")
    suspend fun acceptJob(id: Long, status: String, providerPhone: String, providerName: String, price: Int)

    @Query("UPDATE service_requests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE service_requests SET status = 'AWAITING_CUSTOMER_CONFIRMATION' WHERE id = :id")
    suspend fun markJobAwaitingConfirmation(id: Long)

    @Query("UPDATE service_requests SET status = 'COMPLETED', ratingGiven = :rating, ratingComment = :comment, completedAt = :completedAt WHERE id = :id")
    suspend fun completeJobWithRating(id: Long, rating: Int, comment: String, completedAt: Long)

    @Query("UPDATE service_requests SET status = 'COMPLETED', completedAt = :completedAt WHERE id = :id")
    suspend fun autoCompleteJobWithoutRating(id: Long, completedAt: Long)

    @Query("UPDATE service_requests SET issueCategory = :category, issueDescription = :description WHERE id = :id")
    suspend fun reportJobIssue(id: Long, category: String, description: String)

    @Query("UPDATE service_requests SET budgetRs = 1500 WHERE budgetRs > 100000")
    suspend fun sanitizeCorruptedBudgets()

    @Query("UPDATE service_requests SET agreedPriceRs = 1500 WHERE agreedPriceRs > 100000")
    suspend fun sanitizeCorruptedAgreedPrices()
}

@Dao
interface JobOfferDao {
    @Query("SELECT * FROM job_offers WHERE requestId = :requestId ORDER BY createdAt DESC")
    fun getOffersForRequestFlow(requestId: Long): Flow<List<JobOfferEntity>>

    @Query("SELECT * FROM job_offers WHERE requestId = :requestId ORDER BY createdAt DESC")
    suspend fun getOffersForRequest(requestId: Long): List<JobOfferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: JobOfferEntity): Long

    @Query("SELECT * FROM job_offers WHERE id = :id LIMIT 1")
    suspend fun getOfferById(id: Long): JobOfferEntity?

    @Query("UPDATE job_offers SET status = :status WHERE id = :id")
    suspend fun updateOfferStatus(id: Long, status: String)

    @Query("UPDATE job_offers SET status = 'accepted' WHERE id = :offerId")
    suspend fun markOfferAccepted(offerId: Long)

    @Query("UPDATE job_offers SET status = 'expired' WHERE requestId = :requestId AND id != :acceptedOfferId AND status = 'pending'")
    suspend fun expireOtherOffersForRequest(requestId: Long, acceptedOfferId: Long)

    @Query("UPDATE job_offers SET counterPriceRs = 1500 WHERE counterPriceRs > 100000")
    suspend fun sanitizeCorruptedOffers()
}

@Dao
interface ServiceCategoryDao {
    @Query("SELECT * FROM service_categories ORDER BY id ASC")
    fun getAllCategoriesFlow(): Flow<List<ServiceCategoryEntity>>

    @Query("SELECT * FROM service_categories ORDER BY id ASC")
    suspend fun getAllCategories(): List<ServiceCategoryEntity>

    @Query("SELECT * FROM service_categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): ServiceCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<ServiceCategoryEntity>)

    @Query("UPDATE service_categories SET priceMin = :min, priceMax = :max WHERE id = :id")
    suspend fun updatePriceRange(id: String, min: Int, max: Int)

    @Query("SELECT COUNT(*) FROM service_categories")
    suspend fun getCategoryCount(): Int
}

@Dao
interface JobRatingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(rating: JobRatingEntity): Long

    @Query("SELECT * FROM job_ratings WHERE providerPhone = :providerPhone ORDER BY createdAt DESC")
    fun getRatingsForProviderFlow(providerPhone: String): Flow<List<JobRatingEntity>>

    @Query("SELECT * FROM job_ratings WHERE jobId = :jobId LIMIT 1")
    suspend fun getRatingForJob(jobId: Long): JobRatingEntity?

    @Query("SELECT AVG(rating) FROM job_ratings WHERE providerPhone = :providerPhone")
    suspend fun getAverageRatingForProvider(providerPhone: String): Double?

    @Query("SELECT COUNT(*) FROM job_ratings WHERE providerPhone = :providerPhone")
    suspend fun getRatingCountForProvider(providerPhone: String): Int
}
