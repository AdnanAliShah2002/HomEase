package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val phone: String,
    val role: String, // "CUSTOMER" or "PROVIDER"
    val name: String,
    val profilePhotoUri: String? = null,
    val cityArea: String = "Lahore - Gulberg",
    val homeAddress: String = "",
    val categoriesCsv: String = "", // e.g. "plumbing,electrical"
    val yearsExperience: String = "",
    val serviceRadiusKm: Int = 10,
    val cnicNumber: String = "",
    val cnicFrontUri: String? = null,
    val cnicBackUri: String? = null,
    val dateOfBirth: String = "",
    val shopName: String = "",
    val businessPhotoUri: String? = null,
    val bio: String = "",
    val referenceName: String = "",
    val referencePhone: String = "",
    val payoutMethod: String = "Cash", // Cash, JazzCash, EasyPaisa
    val payoutAccountNumber: String = "",
    val consentAgreed: Boolean = true,
    val status: String = "PENDING", // PENDING, ACTIVE, APPROVED
    val isOnline: Boolean = false,
    val avgRating: Double = 5.0,
    val totalJobs: Int = 0,
    val savedAddressesCsv: String = "",
    val lat: Double? = null,
    val lng: Double? = null
) {
    val latitude: Double? get() = lat
    val longitude: Double? get() = lng
}

@Entity(tableName = "service_requests")
data class ServiceRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerPhone: String,
    val customerName: String,
    val categoryId: String,
    val categoryTitle: String,
    val serviceTitle: String,
    val description: String,
    val cityArea: String,
    val fullAddress: String,
    val budgetRs: Int,
    val customerAskingPrice: Int = budgetRs,
    val status: String = "SEARCHING", // SEARCHING, ACCEPTED, ON_THE_WAY, ARRIVED, IN_PROGRESS, AWAITING_CUSTOMER_CONFIRMATION, COMPLETED, CANCELLED
    val selectedProviderPhone: String? = null,
    val selectedProviderName: String? = null,
    val agreedPriceRs: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val statusUpdatedAt: Long? = null,
    val completedAt: Long? = null,
    val ratingGiven: Int? = null,
    val ratingComment: String? = null,
    val issueCategory: String? = null,
    val issueDescription: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val remoteId: String? = null
) {
    val latitude: Double? get() = lat
    val longitude: Double? get() = lng
}

@Entity(tableName = "provider_locations")
data class ProviderLocationEntity(
    @PrimaryKey
    val jobId: String,
    val providerId: String,
    val lat: Double,
    val lng: Double,
    val heading: Double? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "job_offers")
data class JobOfferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val requestId: Long,
    val providerPhone: String,
    val providerName: String,
    val counterPriceRs: Int,
    val offerPriceRs: Int = counterPriceRs,
    val distanceKm: Double,
    val providerRating: Double = 4.8,
    val status: String = "pending", // pending, accepted, rejected, expired
    val offerNote: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val remoteOfferId: String? = null
)

@Entity(tableName = "service_categories")
data class ServiceCategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val urduName: String,
    val priceMin: Int,
    val priceMax: Int,
    val priceBasis: String,
    val isEstimateOnly: Boolean = false,
    val popularServicesCsv: String = ""
)

@Entity(tableName = "job_ratings")
data class JobRatingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val jobId: Long,
    val providerPhone: String,
    val customerPhone: String,
    val rating: Int, // 1 to 5
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "job_messages")
data class JobMessageEntity(
    @PrimaryKey
    val id: String,
    val jobId: String,
    val senderId: String,
    val senderType: String, // "customer" | "provider"
    val message: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val createdAtIso: String = "",
    val readAtEpochMs: Long? = null
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey
    val id: String,
    val jobId: String,
    val callerId: String,
    val startedAtEpochMs: Long = System.currentTimeMillis(),
    val endedAtEpochMs: Long? = null,
    val durationSeconds: Int? = null
)

