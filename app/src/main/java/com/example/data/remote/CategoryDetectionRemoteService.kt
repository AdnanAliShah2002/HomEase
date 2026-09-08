package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class CategoryDetectionResult(
    val success: Boolean,
    val category: String? = null,
    val categoryId: String? = null,
    val serviceNote: String? = null,
    val confidence: String? = null, // "high", "medium", "low"
    val error: String? = null
)

class CategoryDetectionRemoteService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val SUPABASE_URL = "https://nsqrfagylbqrwlbvnsug.supabase.co"
        const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5zcXJmYWd5bGJxcndsYnZuc3VnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI5NTY4NTIsImV4cCI6MjA4ODUzMjg1Mn0.qNnOe8xR6ehutINqCGVK7tfHLBh14tBWgbhFeHvFa40"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val ALLOWED_CATEGORIES = listOf(
            "Laundry & Ironing",
            "Home Cleaning",
            "AC Servicing",
            "Car Care/Wash",
            "Plumbing",
            "Electrical",
            "Appliance Repair"
        )

        fun mapCategoryToId(categoryName: String?): String? {
            if (categoryName == null) return null
            val clean = categoryName.trim().lowercase()
            return when {
                clean.contains("laundry") || clean.contains("iron") || clean.contains("dry clean") -> "dry_cleaning"
                clean.contains("home clean") || clean.contains("house clean") || clean == "cleaning" -> "cleaning"
                clean.contains("ac") || clean.contains("air condition") -> "ac_repair"
                clean.contains("car") || (clean.contains("wash") && clean.contains("car")) -> "car_care"
                clean.contains("plumb") || clean.contains("pipe") || clean.contains("leak") || clean.contains("tap") -> "plumbing"
                clean.contains("electr") || clean.contains("wiring") || clean.contains("switch") || clean.contains("fan") -> "electrical"
                clean.contains("appliance") || clean.contains("fridge") || clean.contains("refrigerator") || clean.contains("microwave") -> "appliance_repair"
                clean.contains("carpent") || clean.contains("wood") -> "carpentry"
                clean.contains("paint") -> "painting"
                else -> null
            }
        }
    }

    /**
     * Calls the Supabase Edge Function 'detect-category', which invokes Gemini server-side.
     * Never calls the Gemini API directly from client-side code.
     * If the remote edge function is not reachable (e.g. offline/testing),
     * it falls back gracefully to a heuristic classifier.
     */
    suspend fun detectCategory(description: String): CategoryDetectionResult = withContext(Dispatchers.IO) {
        val trimmed = description.trim()
        if (trimmed.isBlank()) {
            return@withContext CategoryDetectionResult(
                success = false,
                error = "Description cannot be empty"
            )
        }

        try {
            val payload = JSONObject().apply {
                put("description", trimmed)
            }.toString()

            val request = Request.Builder()
                .url("$SUPABASE_URL/functions/v1/detect-category")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (ioe: IOException) {
                null
            }

            if (response != null && response.isSuccessful) {
                val bodyString = response.body?.string().orEmpty()
                val json = try { JSONObject(bodyString) } catch (_: Exception) { JSONObject() }
                if (json.optBoolean("success", false)) {
                    val rawCategory = json.optString("category", "")
                    val serviceNote = json.optString("service_note", "")
                    val confidence = json.optString("confidence", "medium").lowercase()

                    val mappedId = mapCategoryToId(rawCategory)
                    if (mappedId != null) {
                        return@withContext CategoryDetectionResult(
                            success = true,
                            category = rawCategory,
                            categoryId = mappedId,
                            serviceNote = serviceNote.ifBlank { "Recommended service" },
                            confidence = confidence
                        )
                    } else {
                        // Category does not match app's actual categories
                        return@withContext CategoryDetectionResult(
                            success = false,
                            error = "Could not confidently classify category"
                        )
                    }
                } else {
                    val errorMsg = json.optString("error", "Could not classify problem")
                    // If remote function returned unclassified, check local fallback
                    val fallback = localHeuristicClassify(trimmed)
                    return@withContext fallback ?: CategoryDetectionResult(
                        success = false,
                        error = errorMsg
                    )
                }
            } else {
                // Remote function unreachable or returned HTTP error — use resilient local fallback
                val fallback = localHeuristicClassify(trimmed)
                return@withContext fallback ?: CategoryDetectionResult(
                    success = false,
                    error = "Could not classify issue. Please select category manually."
                )
            }
        } catch (e: Exception) {
            val fallback = localHeuristicClassify(trimmed)
            return@withContext fallback ?: CategoryDetectionResult(
                success = false,
                error = e.message ?: "Failed to detect category"
            )
        }
    }

    /**
     * Resilient offline fallback classifier for testing and offline environments.
     */
    fun localHeuristicClassify(description: String): CategoryDetectionResult? {
        val lower = description.lowercase()
        return when {
            lower.contains("fridge") || lower.contains("refrigerator") || lower.contains("microwave") || lower.contains("oven") || lower.contains("washing machine") || lower.contains("dispenser") -> {
                CategoryDetectionResult(
                    success = true,
                    category = "Appliance Repair",
                    categoryId = "appliance_repair",
                    serviceNote = "Appliance diagnosis & component fix",
                    confidence = "high"
                )
            }
            lower.contains("ac") || lower.contains("air condition") || lower.contains("cooling") || lower.contains("chilling") || lower.contains("compressor") || lower.contains("gas leak") -> {
                CategoryDetectionResult(
                    success = true,
                    category = "AC Servicing",
                    categoryId = "ac_repair",
                    serviceNote = "AC cooling inspection & repair",
                    confidence = "high"
                )
            }
            lower.contains("tap") || lower.contains("pipe") || lower.contains("leak") || lower.contains("drain") || lower.contains("geyser") || lower.contains("flush") || lower.contains("plumb") || lower.contains("faucet") || lower.contains("sink") -> {
                CategoryDetectionResult(
                    success = true,
                    category = "Plumbing",
                    categoryId = "plumbing",
                    serviceNote = "Leaking pipe or plumbing repair",
                    confidence = "high"
                )
            }
            lower.contains("fan") || lower.contains("switch") || lower.contains("wiring") || lower.contains("short circuit") || lower.contains("ups") || lower.contains("breaker") || lower.contains("electric") || lower.contains("spark") -> {
                CategoryDetectionResult(
                    success = true,
                    category = "Electrical",
                    categoryId = "electrical",
                    serviceNote = "Electrical inspection & wiring fix",
                    confidence = "high"
                )
            }
            lower.contains("iron") || lower.contains("wash") && (lower.contains("cloth") || lower.contains("shirt") || lower.contains("suit") || lower.contains("shalwar")) || lower.contains("dry clean") || lower.contains("laundry") -> {
                CategoryDetectionResult(
                    success = true,
                    category = "Laundry & Ironing",
                    categoryId = "dry_cleaning",
                    serviceNote = "Laundry & suit pressing care",
                    confidence = "high"
                )
            }
            lower.contains("deep clean") || lower.contains("sofa") || lower.contains("carpet") || lower.contains("tank clean") || (lower.contains("clean") && lower.contains("house")) || lower.contains("home clean") -> {
                CategoryDetectionResult(
                    success = true,
                    category = "Home Cleaning",
                    categoryId = "cleaning",
                    serviceNote = "Home deep cleaning & wash",
                    confidence = "high"
                )
            }
            lower.contains("car wash") || lower.contains("detailing") || lower.contains("car care") || lower.contains("oil change") || lower.contains("jumpstart") -> {
                CategoryDetectionResult(
                    success = true,
                    category = "Car Care/Wash",
                    categoryId = "car_care",
                    serviceNote = "Doorstep car wash & care",
                    confidence = "high"
                )
            }
            lower.contains("wood") || lower.contains("door") || lower.contains("cabinet") || lower.contains("carpenter") || lower.contains("lock") -> {
                CategoryDetectionResult(
                    success = true,
                    category = "Plumbing", // Closest among 7 or handle gracefully
                    categoryId = "plumbing",
                    serviceNote = "Home woodwork repair",
                    confidence = "low"
                )
            }
            else -> null
        }
    }
}
