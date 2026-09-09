package com.example.data.model

enum class UserRole {
    CUSTOMER,
    PROVIDER
}

enum class AccountStatus {
    PENDING,
    ACTIVE,
    REJECTED
}

enum class NotificationPreference {
    WHATSAPP,
    SMS,
    BOTH
}

enum class RequestStatus {
    SEARCHING,
    ACCEPTED,
    ON_THE_WAY,
    ARRIVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

@Serializable
data class ProviderLocation(
    @SerialName("job_id") val jobId: String,
    @SerialName("provider_id") val providerId: String,
    val lat: Double,
    val lng: Double,
    val heading: Double? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    companion object {
        fun fromJson(obj: org.json.JSONObject): ProviderLocation {
            return ProviderLocation(
                jobId = obj.optString("job_id", ""),
                providerId = obj.optString("provider_id", ""),
                lat = obj.optDouble("lat", 0.0),
                lng = obj.optDouble("lng", 0.0),
                heading = if (obj.has("heading") && !obj.isNull("heading")) obj.optDouble("heading") else null,
                updatedAt = if (obj.has("updated_at") && !obj.isNull("updated_at")) obj.optString("updated_at") else null
            )
        }
    }
}

data class JobMessage(
    val id: String,
    val jobId: String,
    val senderId: String,
    val senderType: String, // "customer" | "provider"
    val message: String,
    val createdAt: String,
    val readAt: String? = null
) {
    companion object {
        fun fromJson(obj: org.json.JSONObject): JobMessage {
            return JobMessage(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                jobId = obj.optString("job_id", ""),
                senderId = obj.optString("sender_id", ""),
                senderType = obj.optString("sender_type", "customer"),
                message = obj.optString("message", ""),
                createdAt = obj.optString("created_at", ""),
                readAt = if (obj.has("read_at") && !obj.isNull("read_at")) obj.optString("read_at") else null
            )
        }
    }
}

data class CallLog(
    val id: String,
    val jobId: String,
    val callerId: String,
    val startedAt: String,
    val endedAt: String? = null,
    val durationSeconds: Int? = null
) {
    companion object {
        fun fromJson(obj: org.json.JSONObject): CallLog {
            return CallLog(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                jobId = obj.optString("job_id", ""),
                callerId = obj.optString("caller_id", ""),
                startedAt = obj.optString("started_at", ""),
                endedAt = if (obj.has("ended_at") && !obj.isNull("ended_at")) obj.optString("ended_at") else null,
                durationSeconds = if (obj.has("duration_seconds") && !obj.isNull("duration_seconds")) obj.optInt("duration_seconds") else null
            )
        }
    }
}

enum class OfferStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

data class ServiceCategoryItem(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val iconType: String,
    val popularServices: List<String>,
    val emoji: String = "🛠️",
    val pastelBgColor: Long = 0xFFEFF6FF,
    val priceMin: Int = 300,
    val priceMax: Int = 2000,
    val priceBasis: String = "per job",
    val isEstimateOnly: Boolean = false
)

object ServiceCatalog {
    val categories = listOf(
        ServiceCategoryItem(
            id = "dry_cleaning",
            nameKey = "cat_dry_cleaning",
            descKey = "cat_dry_cleaning_desc",
            iconType = "dry_cleaning",
            popularServices = listOf("Ironing Only (Rs 40–60)", "Wash Only (Rs 60–90)", "Wash & Iron (Rs 90–150)", "Heavy Blanket / Duvet"),
            emoji = "👔",
            pastelBgColor = 0xFFFDF0ED, // coral-50
            priceMin = 40,
            priceMax = 150,
            priceBasis = "per suit (ironing / wash / wash & iron)",
            isEstimateOnly = false
        ),
        ServiceCategoryItem(
            id = "cleaning",
            nameKey = "cat_cleaning",
            descKey = "cat_cleaning_desc",
            iconType = "cleaning",
            popularServices = listOf("Deep Home Cleaning", "Sofa & Carpet Shampoo", "Kitchen Degreasing", "Water Tank Clean"),
            emoji = "🧼",
            pastelBgColor = 0xFFECFDF5, // emerald-50
            priceMin = 1500,
            priceMax = 3500,
            priceBasis = "per visit by home size",
            isEstimateOnly = false
        ),
        ServiceCategoryItem(
            id = "ac_repair",
            nameKey = "cat_ac_repair",
            descKey = "cat_ac_repair_desc",
            iconType = "ac_repair",
            popularServices = listOf("AC General Service", "Gas Leakage & Refill", "Master Deep Cleaning", "Inverter PCB Repair"),
            emoji = "❄️",
            pastelBgColor = 0xFFFFF7ED, // orange-50
            priceMin = 1200,
            priceMax = 2000,
            priceBasis = "per unit",
            isEstimateOnly = false
        ),
        ServiceCategoryItem(
            id = "car_care",
            nameKey = "cat_car_care",
            descKey = "cat_car_care_desc",
            iconType = "car_care",
            popularServices = listOf("Home Car Wash & Wax", "Oil & Filter Service", "Battery Jumpstart", "Interior Detailing"),
            emoji = "🚗",
            pastelBgColor = 0xFFFAF5FF, // purple-50
            priceMin = 500,
            priceMax = 1500,
            priceBasis = "per service tier",
            isEstimateOnly = false
        ),
        ServiceCategoryItem(
            id = "plumbing",
            nameKey = "cat_plumbing",
            descKey = "cat_plumbing_desc",
            iconType = "plumbing",
            popularServices = listOf("Leaking Tap / Pipe", "Water Tank Motor", "Drain Blockage", "Geyser Installation"),
            emoji = "💧",
            pastelBgColor = 0xFFEFF6FF, // blue-50
            priceMin = 300,
            priceMax = 2000,
            priceBasis = "varies by issue — indicative only",
            isEstimateOnly = true
        ),
        ServiceCategoryItem(
            id = "electrical",
            nameKey = "cat_electrical",
            descKey = "cat_electrical_desc",
            iconType = "electrical",
            popularServices = listOf("Ceiling Fan Repair", "UPS / Battery Wiring", "Short Circuit Fix", "Switchboard Replacement"),
            emoji = "⚡",
            pastelBgColor = 0xFFFFFBEB, // amber-50
            priceMin = 300,
            priceMax = 2000,
            priceBasis = "varies by issue — indicative only",
            isEstimateOnly = true
        ),
        ServiceCategoryItem(
            id = "appliance_repair",
            nameKey = "cat_appliance_repair",
            descKey = "cat_appliance_repair_desc",
            iconType = "ac_repair",
            popularServices = listOf("Refrigerator Cooling Fix", "Washing Machine Motor", "Microwave Repair", "Water Dispenser"),
            emoji = "🛠️",
            pastelBgColor = 0xFFFFF1F2, // rose-50
            priceMin = 400,
            priceMax = 2500,
            priceBasis = "varies by issue — indicative only",
            isEstimateOnly = true
        ),
        ServiceCategoryItem(
            id = "carpentry",
            nameKey = "cat_carpentry",
            descKey = "cat_carpentry_desc",
            iconType = "carpentry",
            popularServices = listOf("Furniture Assembly", "Door Lock & Hinges", "Wooden Wardrobe Fix", "Bed Repair"),
            emoji = "🪵",
            pastelBgColor = 0xFFFFF1F2, // rose-50
            priceMin = 400,
            priceMax = 2500,
            priceBasis = "varies by issue — indicative only",
            isEstimateOnly = true
        ),
        ServiceCategoryItem(
            id = "painting",
            nameKey = "cat_painting",
            descKey = "cat_painting_desc",
            iconType = "painting",
            popularServices = listOf("Single Room Wall Paint", "Full House Painting", "Wood & Door Polish", "Water Seepage Patch"),
            emoji = "🎨",
            pastelBgColor = 0xFFF0FDF4, // green-50
            priceMin = 1500,
            priceMax = 6000,
            priceBasis = "varies by room/area — indicative only",
            isEstimateOnly = true
        )
    )
}
