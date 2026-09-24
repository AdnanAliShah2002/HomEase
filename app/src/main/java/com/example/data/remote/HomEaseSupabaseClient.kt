package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.data.db.UserEntity
import com.example.data.model.CallLog
import com.example.data.model.JobMessage
import com.example.data.model.MobileAppTheme
import com.example.data.model.ProviderLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * HomEase Supabase Client
 * 
 * SECURITY MANDATE:
 * 1. Initialized strictly with the ANON KEY (SUPABASE_ANON_KEY).
 * 2. The SERVICE ROLE KEY is NEVER included or used in client code.
 * 3. All direct table reads and writes authenticate using the logged-in user's
 *    session (Bearer <user_access_token>).
 * 4. RLS policies on the database enforce row-level scoping for:
 *    - `users`: Customers can only read/write their own profile row (auth.uid() = id)
 *    - `service_providers`: Providers can only read/write their own profile (auth.uid() = id)
 *    - `public_provider_profiles`: Safe view exposing ONLY name, photo, categories, rating, bio for browsing
 *    - `jobs`: Customers only see their own jobs; providers see assigned or matching category jobs
 *    - `job_ratings`: Only customer who ordered the job can rate; ratings scoped to participants
 *    - `otp_codes`: Zero client policies, blocked for all client queries
 */
data class SupabaseSession(
    val userId: String,
    val phone: String,
    val accessToken: String,
    val role: String = "customer",
    val refreshToken: String? = null
)

data class PublicProviderProfile(
    val id: String,
    val fullName: String,
    val profilePhotoUrl: String?,
    val serviceCategories: List<String>,
    val avgRating: Double,
    val totalJobs: Int,
    val bio: String?,
    val businessName: String?
)

class HomEaseSupabaseClient(private val context: Context? = null) {

    companion object {
        const val SUPABASE_URL = "https://nsqrfagylbqrwlbvnsug.supabase.co"
        const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5zcXJmYWd5bGJxcndsYnZuc3VnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI5NTY4NTIsImV4cCI6MjA4ODUzMjg1Mn0.qNnOe8xR6ehutINqCGVK7tfHLBh14tBWgbhFeHvFa40"
        const val STORAGE_BUCKET = "provider-documents"

        private const val PREFS_NAME = "homease_supabase_session"
        private const val KEY_USER_ID = "session_user_id"
        private const val KEY_PHONE = "session_phone"
        private const val KEY_ACCESS_TOKEN = "session_access_token"
        private const val KEY_REFRESH_TOKEN = "session_refresh_token"
        private const val KEY_ROLE = "session_role"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val JPEG_MEDIA_TYPE = "image/jpeg".toMediaType()

        @Volatile
        private var instance: HomEaseSupabaseClient? = null

        fun getInstance(context: Context): HomEaseSupabaseClient {
            return instance ?: synchronized(this) {
                instance ?: HomEaseSupabaseClient(context.applicationContext).also { instance = it }
            }
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val realtime: RealtimeClient by lazy {
        RealtimeClient(
            client = client,
            supabaseUrl = SUPABASE_URL,
            anonKey = SUPABASE_ANON_KEY,
            getLatestLocationFallback = { jobId -> getProviderLocation(jobId) },
            getLatestMessagesFallback = { jobId -> getJobMessages(jobId) }
        )
    }

    private var inMemorySession: SupabaseSession? = null

    init {
        loadSessionFromPrefs()
    }

    // ========================================================================
    // Session Management
    // ========================================================================

    @Synchronized
    fun setSession(session: SupabaseSession) {
        inMemorySession = session
        persistSession(session)
    }

    @Synchronized
    fun getSession(): SupabaseSession? {
        if (inMemorySession == null) {
            loadSessionFromPrefs()
        }
        return inMemorySession
    }

    @Synchronized
    fun clearSession() {
        inMemorySession = null
        val prefs = getPreferences() ?: return
        prefs.edit().clear().apply()
    }

    fun isAuthenticated(): Boolean {
        return getSession() != null && !getSession()?.accessToken.isNullOrBlank()
    }

    private fun getPreferences(): SharedPreferences? {
        return context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun persistSession(session: SupabaseSession) {
        val prefs = getPreferences() ?: return
        prefs.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_PHONE, session.phone)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_ROLE, session.role)
            .apply()
    }

    private fun loadSessionFromPrefs() {
        val prefs = getPreferences() ?: return
        val userId = prefs.getString(KEY_USER_ID, null)
        val phone = prefs.getString(KEY_PHONE, null)
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val role = prefs.getString(KEY_ROLE, "customer") ?: "customer"

        if (!userId.isNullOrBlank() && !phone.isNullOrBlank() && !token.isNullOrBlank()) {
            inMemorySession = SupabaseSession(
                userId = userId,
                phone = phone,
                accessToken = token,
                role = role,
                refreshToken = refreshToken
            )
        }
    }

    suspend fun refreshSession(): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        val current = getSession() ?: return@withContext Result.failure(IllegalStateException("No session to refresh"))
        val refreshToken = current.refreshToken
        if (refreshToken.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("No refresh token available"))
        }
        try {
            val url = "$SUPABASE_URL/auth/v1/token?grant_type=refresh_token"
            val payload = JSONObject().apply {
                put("refresh_token", refreshToken)
            }
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val json = JSONObject(body)
                val newAccessToken = json.getString("access_token")
                val newRefreshToken = json.optString("refresh_token", refreshToken)
                val newSession = current.copy(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken
                )
                setSession(newSession)
                Result.success(newSession)
            } else {
                Result.failure(IOException("Failed to refresh token: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Builds HTTP headers using:
     * - apikey: Anon key (public client identifier)
     * - Authorization: Bearer <user_access_token> when authenticated, or Bearer <anonKey> for unauthenticated/guest calls.
     * 
     * SERVICE ROLE KEY IS NEVER USED HERE.
     */
    fun getAuthHeaders(forceAnon: Boolean = false): Map<String, String> {
        val token = if (!forceAnon) {
            getSession()?.accessToken ?: SUPABASE_ANON_KEY
        } else {
            SUPABASE_ANON_KEY
        }

        return mapOf(
            "apikey" to SUPABASE_ANON_KEY,
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json"
        )
    }

    // ========================================================================
    // 1. Users Table (Customers & User Profiles)
    // ========================================================================

    /**
     * Fetch user profile. RLS policy ("Users can view own profile") ensures that
     * a user can only read their own profile row matching auth.uid() = id.
     * If querying another user's ID, RLS returns empty list `[]`.
     */
    suspend fun getUserProfile(userId: String): Result<JSONObject?> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/users?id=eq.$userId&select=*"
            val requestBuilder = Request.Builder().url(url)
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(bodyString)
                if (array.length() > 0) {
                    Result.success(array.getJSONObject(0))
                } else {
                    // Empty list returned by Supabase when RLS filters out unauthorized rows
                    Result.success(null)
                }
            } else {
                Result.failure(IOException("Failed to fetch user profile (HTTP ${response.code}): $bodyString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile. RLS policy ("Users can update own profile") restricts
     * updates to the user's own row where auth.uid() = id.
     */
    suspend fun updateUserProfile(userId: String, updates: JSONObject): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/users?id=eq.$userId"
            val requestBuilder = Request.Builder()
                .url(url)
                .patch(updates.toString().toRequestBody(JSON_MEDIA_TYPE))
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorBody = response.body?.string().orEmpty()
                Result.failure(IOException("Update rejected (HTTP ${response.code}): $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================================================
    // 2. Service Providers & Safe Public View
    // ========================================================================

    /**
     * Browsing approved providers uses the `public_provider_profiles` Postgres view.
     * This view exposes ONLY safe public fields (name, photo, categories, rating, bio, business name).
     * Sensitive fields such as CNIC number, CNIC photos, reference phone, and payout accounts
     * are NOT present in this view and cannot be accessed by customers.
     */
    suspend fun getPublicProviderProfiles(): Result<List<PublicProviderProfile>> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/public_provider_profiles?select=*"
            val requestBuilder = Request.Builder().url(url)
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val jsonArray = JSONArray(bodyString)
                val list = mutableListOf<PublicProviderProfile>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val categories = mutableListOf<String>()
                    val catArray = obj.optJSONArray("service_categories")
                    if (catArray != null) {
                        for (c in 0 until catArray.length()) {
                            categories.add(catArray.getString(c))
                        }
                    }
                    list.add(
                        PublicProviderProfile(
                            id = obj.optString("id"),
                            fullName = obj.optString("full_name"),
                            profilePhotoUrl = obj.optString("profile_photo_url").takeIf { it.isNotBlank() },
                            serviceCategories = categories,
                            avgRating = obj.optDouble("avg_rating", 0.0),
                            totalJobs = obj.optInt("total_jobs", 0),
                            bio = obj.optString("bio").takeIf { it.isNotBlank() },
                            businessName = obj.optString("business_name").takeIf { it.isNotBlank() }
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(IOException("Failed to query public provider profiles (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Provider viewing their own profile row in `service_providers`.
     * RLS policy ("Providers can view own profile") allows access when auth.uid() = id.
     */
    suspend fun getProviderOwnProfile(providerId: String): Result<JSONObject?> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/service_providers?id=eq.$providerId&select=*"
            val requestBuilder = Request.Builder().url(url)
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(bodyString)
                if (array.length() > 0) {
                    Result.success(array.getJSONObject(0))
                } else {
                    Result.success(null)
                }
            } else {
                Result.failure(IOException("Failed to fetch provider profile (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Submits a provider registration.
     * Uses the authenticated user's session token and the anon key.
     */
    suspend fun submitProviderRegistration(provider: UserEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val formattedPhone = if (provider.phone.startsWith("+")) provider.phone else "+${provider.phone}"

            // Upload media to Supabase Storage
            val profilePhotoUrl = uploadDocument(provider.profilePhotoUri, "avatars", formattedPhone)
            val cnicFrontUrl = uploadDocument(provider.cnicFrontUri, "cnic_front", formattedPhone)
            val cnicBackUrl = uploadDocument(provider.cnicBackUri, "cnic_back", formattedPhone)
            val businessPhotoUrl = uploadDocument(provider.businessPhotoUri, "business", formattedPhone)

            val categoriesArray = JSONArray().apply {
                provider.categoriesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                    put(it)
                }
            }

            val currentUserId = getSession()?.userId ?: UUID.randomUUID().toString()

            // Fetch stored FCM token if available
            val fcmTokenVal = context?.getSharedPreferences("homease_prefs", Context.MODE_PRIVATE)
                ?.getString("fcm_token", "") ?: ""

            val payload = JSONObject().apply {
                put("id", currentUserId)
                put("phone", formattedPhone)
                put("full_name", provider.name)
                put("profile_photo_url", profilePhotoUrl ?: "")
                put("cnic_number", provider.cnicNumber)
                put("cnic_front_url", cnicFrontUrl ?: "")
                put("cnic_back_url", cnicBackUrl ?: "")
                put("date_of_birth", provider.dateOfBirth)
                put("home_address", provider.homeAddress)
                put("city_area", provider.cityArea)
                put("service_categories", categoriesArray)
                put("years_of_experience", provider.yearsExperience)
                put("service_area", provider.cityArea)
                put("service_radius_km", provider.serviceRadiusKm)
                put("business_name", provider.shopName)
                put("business_photo_url", businessPhotoUrl ?: "")
                put("bio", provider.bio)
                put("reference_name", provider.referenceName)
                put("reference_phone", provider.referencePhone)
                put("payout_method", provider.payoutMethod)
                put("payout_account_number", provider.payoutAccountNumber)
                put("status", "pending")
                put("is_online", true)
                if (fcmTokenVal.isNotBlank()) {
                    put("fcm_token", fcmTokenVal)
                }
                put("consent_agreed", provider.consentAgreed)
            }

            val requestBuilder = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/service_providers")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = try {
                client.newCall(requestBuilder.build()).execute()
            } catch (e: Exception) {
                return@withContext Result.failure(IOException("Failed to connect to Supabase: ${e.message}", e))
            }

            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    val message = errJson.optString("message", "")
                    val details = errJson.optString("details", "")
                    val hint = errJson.optString("hint", "")
                    listOf(message, details, hint).filter { it.isNotBlank() }.joinToString(" - ").ifBlank { "HTTP ${response.code}: $responseBody" }
                } catch (_: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(IOException("Supabase error: $errorMsg"))
            }

            // Also upsert basic profile into profiles/users for convenience
            try {
                val userPayload = JSONObject().apply {
                    put("id", currentUserId)
                    put("phone", formattedPhone)
                    put("full_name", provider.name)
                    put("city", provider.cityArea)
                    put("role", "provider")
                    put("is_verified", false)
                }
                val userReq = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/users")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(userPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                getAuthHeaders().forEach { (k, v) -> userReq.addHeader(k, v) }
                client.newCall(userReq.build()).execute()
            } catch (_: Exception) {
                // Best effort
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an approved or registered provider's profile fields directly in Supabase `service_providers`.
     * Sends PATCH request for name, bio, years_of_experience, service_categories, radius, etc.
     */
    suspend fun updateProviderRemoteProfile(
        phone: String,
        name: String,
        cityArea: String,
        categories: List<String>,
        yearsExperience: String,
        serviceRadiusKm: Int,
        bio: String,
        shopName: String,
        payoutMethod: String,
        payoutAccountNumber: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val formattedPhone = if (phone.startsWith("+")) phone else "+$phone"
            val categoriesArray = JSONArray().apply {
                categories.filter { it.isNotBlank() }.forEach { put(it) }
            }
            val payload = JSONObject().apply {
                put("full_name", name)
                put("city_area", cityArea)
                put("service_area", cityArea)
                put("service_categories", categoriesArray)
                put("years_of_experience", yearsExperience)
                put("service_radius_km", serviceRadiusKm)
                put("bio", bio)
                put("business_name", shopName)
                put("payout_method", payoutMethod)
                put("payout_account_number", payoutAccountNumber)
                put("updated_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date()))
            }

            val patchUrl = "$SUPABASE_URL/rest/v1/service_providers?phone=eq.$formattedPhone"
            val requestBuilder = Request.Builder()
                .url(patchUrl)
                .patch(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful || response.code in 200..204) {
                Result.success(true)
            } else {
                val body = response.body?.string().orEmpty()
                Result.failure(IOException("Failed to update remote provider profile (HTTP ${response.code}): $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates provider online availability directly in Supabase `service_providers` table.
     */
    suspend fun updateProviderOnlineStatus(phone: String, isOnline: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val formattedPhone = if (phone.startsWith("+")) phone else "+$phone"
            val payload = JSONObject().apply {
                put("is_online", isOnline)
                put("updated_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date()))
            }
            val patchUrl = "$SUPABASE_URL/rest/v1/service_providers?phone=eq.$formattedPhone"
            val requestBuilder = Request.Builder()
                .url(patchUrl)
                .patch(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            val response = client.newCall(requestBuilder.build()).execute()
            Result.success(response.isSuccessful || response.code in 200..204)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload document to private storage bucket `provider-documents`
     */
    suspend fun uploadDocument(imageUriString: String?, folder: String, phone: String): String? = withContext(Dispatchers.IO) {
        if (imageUriString.isNullOrBlank()) return@withContext null
        if (imageUriString.startsWith("http://") || imageUriString.startsWith("https://")) {
            return@withContext imageUriString
        }

        try {
            val uri = Uri.parse(imageUriString)
            val inputStream = context?.contentResolver?.openInputStream(uri)
                ?: return@withContext imageUriString

            val byteBuffer = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var len: Int
            while (inputStream.read(buffer).also { len = it } != -1) {
                byteBuffer.write(buffer, 0, len)
            }
            val imageBytes = byteBuffer.toByteArray()
            inputStream.close()

            if (imageBytes.isEmpty()) return@withContext imageUriString

            val sanitizedPhone = phone.replace("+", "").replace(" ", "").trim()
            val filename = "${folder}/${sanitizedPhone}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"

            val uploadUrl = "$SUPABASE_URL/storage/v1/object/$STORAGE_BUCKET/$filename"
            val requestBuilder = Request.Builder()
                .url(uploadUrl)
                .addHeader("Content-Type", "image/jpeg")
                .post(imageBytes.toRequestBody(JPEG_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                "$SUPABASE_URL/storage/v1/object/authenticated/$STORAGE_BUCKET/$filename"
            } else {
                imageUriString
            }
        } catch (_: Exception) {
            imageUriString
        }
    }

    // ========================================================================
    // 3. Jobs Table
    // ========================================================================

    /**
     * Customers query their own jobs.
     * RLS policy ("Customers can view own jobs") enforces auth.uid() = customer_id.
     */
    suspend fun getCustomerJobs(customerId: String): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/jobs?customer_id=eq.$customerId&select=*"
            val requestBuilder = Request.Builder().url(url)
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(bodyString)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    list.add(array.getJSONObject(i))
                }
                Result.success(list)
            } else {
                Result.failure(IOException("Failed to query customer jobs (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Providers query jobs assigned to them.
     * RLS policy ("Providers can view assigned jobs") enforces auth.uid() = provider_id.
     */
    suspend fun getProviderAssignedJobs(providerId: String): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/jobs?provider_id=eq.$providerId&select=*"
            val requestBuilder = Request.Builder().url(url)
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(bodyString)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    list.add(array.getJSONObject(i))
                }
                Result.success(list)
            } else {
                Result.failure(IOException("Failed to query provider jobs (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Posts a new service request to the Supabase `jobs` table.
     * RLS policy enforces customer ownership.
     * MUST NOT silently fall back or mask errors.
     */
    suspend fun createJob(
        customerPhone: String,
        customerName: String,
        category: String,
        categoryId: String,
        serviceTitle: String,
        description: String,
        cityArea: String,
        fullAddress: String,
        budgetRs: Int,
        lat: Double? = null,
        lng: Double? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val session = getSession()
            val customerId = session?.userId

            val payload = JSONObject().apply {
                if (!customerId.isNullOrBlank()) {
                    put("customer_id", customerId)
                }
                put("customer_phone", customerPhone)
                put("customer_name", customerName)
                put("category", category)
                put("category_id", categoryId)
                put("service_title", serviceTitle)
                put("description", description)
                put("city_area", cityArea)
                put("full_address", fullAddress)
                put("budget_rs", budgetRs)
                put("status", "searching")
                if (lat != null && !lat.isNaN()) {
                    put("lat", lat)
                }
                if (lng != null && !lng.isNaN()) {
                    put("lng", lng)
                }
            }

            val url = "$SUPABASE_URL/rest/v1/jobs"
            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful || response.code in 200..204) {
                val arr = JSONArray(body)
                if (arr.length() > 0) {
                    val createdId = arr.getJSONObject(0).getString("id")
                    Result.success(createdId)
                } else {
                    Result.failure(IOException("Job created on server, but no record returned: $body"))
                }
            } else {
                Result.failure(IOException("Failed to create job in Supabase (HTTP ${response.code}): $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Providers query unassigned open jobs in 'searching' status.
     * Optionally filtered by category (matches category_id slug OR category display title).
     */
    suspend fun getOpenJobs(category: String? = null): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            var url = "$SUPABASE_URL/rest/v1/jobs?status=eq.searching&select=*&order=created_at.desc"
            if (!category.isNullOrBlank() && category != "All") {
                val encodedCat = java.net.URLEncoder.encode(category, "UTF-8")
                // Match either category_id or category slug/name
                url += "&or=(category_id.eq.$encodedCat,category.eq.$encodedCat)"
            }
            val requestBuilder = Request.Builder().url(url)
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(bodyString)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    list.add(array.getJSONObject(i))
                }
                Result.success(list)
            } else {
                Result.failure(IOException("Failed to query open jobs (HTTP ${response.code}): $bodyString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isValidUuid(str: String?): Boolean {
        if (str.isNullOrBlank()) return false
        return try {
            UUID.fromString(str)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Submits a real offer or counter-offer to `public.job_offers`.
     */
    suspend fun submitJobOffer(
        jobId: String,
        providerId: String?,
        providerPhone: String,
        providerName: String,
        offerPriceRs: Int,
        counterPriceRs: Int,
        distanceKm: Double = 1.5,
        providerRating: Double = 4.8,
        offerNote: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val session = getSession()
            val resolvedProviderId = providerId ?: session?.userId

            val payload = JSONObject().apply {
                put("job_id", jobId)
                if (!resolvedProviderId.isNullOrBlank() && isValidUuid(resolvedProviderId)) {
                    put("provider_id", resolvedProviderId)
                }
                put("provider_phone", providerPhone)
                put("provider_name", providerName)
                put("offer_price_rs", offerPriceRs)
                put("counter_price_rs", counterPriceRs)
                put("distance_km", distanceKm)
                put("provider_rating", providerRating)
                put("status", "pending")
                if (!offerNote.isNullOrBlank()) {
                    put("offer_note", offerNote)
                }
            }

            val url = "$SUPABASE_URL/rest/v1/job_offers"
            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful || response.code in 200..204) {
                val arr = JSONArray(body)
                val createdId = if (arr.length() > 0) arr.getJSONObject(0).getString("id") else UUID.randomUUID().toString()
                Result.success(createdId)
            } else {
                Result.failure(IOException("Failed to submit offer to Supabase (HTTP ${response.code}): $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Customer retrieves real offers submitted for a job.
     */
    suspend fun getOffersForJob(jobId: String): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/job_offers?job_id=eq.$jobId&select=*&order=created_at.desc"
            val requestBuilder = Request.Builder().url(url)
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(bodyString)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    list.add(array.getJSONObject(i))
                }
                Result.success(list)
            } else {
                Result.failure(IOException("Failed to query job offers (HTTP ${response.code}): $bodyString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch a single job by id directly from Supabase (for instant status verification).
     */
    suspend fun getJobById(jobId: String): Result<JSONObject?> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/jobs?id=eq.$jobId&select=*"
            val requestBuilder = Request.Builder().url(url)
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(bodyString)
                if (array.length() > 0) {
                    Result.success(array.getJSONObject(0))
                } else {
                    Result.success(null)
                }
            } else {
                Result.failure(IOException("Failed to query job by id (HTTP ${response.code}): $bodyString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Provider fetches active jobs assigned to them in Supabase.
     */
    suspend fun getProviderAcceptedJobs(providerPhone: String): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/jobs?provider_phone=eq.$providerPhone&status=in.(accepted,ACCEPTED,on_the_way,ON_THE_WAY,arrived,ARRIVED,in_progress,IN_PROGRESS)&select=*&order=created_at.desc"
            val requestBuilder = Request.Builder().url(url)
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(bodyString)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    list.add(array.getJSONObject(i))
                }
                Result.success(list)
            } else {
                Result.failure(IOException("Failed to query provider accepted jobs (HTTP ${response.code}): $bodyString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Provider fetches accepted offers submitted by them.
     */
    suspend fun getProviderAcceptedOffers(providerPhone: String): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/job_offers?provider_phone=eq.$providerPhone&status=in.(accepted,ACCEPTED)&select=*&order=created_at.desc"
            val requestBuilder = Request.Builder().url(url)
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(bodyString)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    list.add(array.getJSONObject(i))
                }
                Result.success(list)
            } else {
                Result.failure(IOException("Failed to query provider accepted offers (HTTP ${response.code}): $bodyString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Customer accepts a specific offer atomically using the `accept_job_offer_atomic` RPC.
     * Executes the status transition, provider assignment, chosen offer acceptance,
     * and competing offers rejection in a single PostgreSQL transaction.
     */
    suspend fun acceptJobOffer(
        jobId: String,
        offerId: String,
        providerId: String?,
        providerPhone: String,
        providerName: String,
        agreedPriceRs: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rpcPayload = JSONObject().apply {
                put("p_job_id", jobId)
                put("p_offer_id", offerId)
                if (!providerId.isNullOrBlank() && isValidUuid(providerId)) {
                    put("p_provider_id", providerId)
                } else {
                    put("p_provider_id", JSONObject.NULL)
                }
                put("p_provider_phone", providerPhone)
                put("p_provider_name", providerName)
                put("p_agreed_price_rs", agreedPriceRs)
            }

            val rpcUrl = "$SUPABASE_URL/rest/v1/rpc/accept_job_offer_atomic"
            val rpcReq = Request.Builder()
                .url(rpcUrl)
                .post(rpcPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
            getAuthHeaders().forEach { (k, v) -> rpcReq.addHeader(k, v) }

            val rpcResp = client.newCall(rpcReq.build()).execute()
            val body = rpcResp.body?.string().orEmpty().trim()
            if (!rpcResp.isSuccessful && rpcResp.code !in 200..204) {
                return@withContext Result.failure(IOException("Failed to accept offer via RPC (HTTP ${rpcResp.code}): $body"))
            }

            // The function returns boolean: true if job was updated, false if not found / already claimed
            val isSuccess = body.equals("true", ignoreCase = true)
            if (!isSuccess) {
                return@withContext Result.failure(IllegalStateException("Job is no longer open or has already been accepted"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Provider directly accepts a job at the customer's asking budget.
     * Uses atomic PATCH with Prefer: return=representation.
     * If 0 rows are returned, the job was already accepted by another provider or cancelled.
     */
    suspend fun acceptJobDirectly(
        jobId: String,
        providerId: String?,
        providerPhone: String,
        providerName: String,
        priceRs: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = getSession()
            val resolvedProviderId = (providerId ?: session?.userId)?.takeIf { isValidUuid(it) }

            val isoNow = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())

            val jobPayload = JSONObject().apply {
                put("status", "accepted")
                if (!resolvedProviderId.isNullOrBlank()) {
                    put("provider_id", resolvedProviderId)
                }
                put("provider_phone", providerPhone)
                put("provider_name", providerName)
                put("agreed_price_rs", priceRs)
                put("status_updated_at", isoNow)
            }
            val jobUrl = "$SUPABASE_URL/rest/v1/jobs?id=eq.$jobId&status=eq.searching"
            val jobReq = Request.Builder()
                .url(jobUrl)
                .addHeader("Prefer", "return=representation")
                .patch(jobPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
            getAuthHeaders().forEach { (k, v) -> jobReq.addHeader(k, v) }

            val jobResp = client.newCall(jobReq.build()).execute()
            val body = jobResp.body?.string().orEmpty()
            if (!jobResp.isSuccessful && jobResp.code !in 200..204) {
                return@withContext Result.failure(IOException("Failed to accept job in Supabase (HTTP ${jobResp.code}): $body"))
            }

            val affectedArray = try {
                JSONArray(body)
            } catch (_: Exception) {
                JSONArray()
            }
            if (affectedArray.length() == 0) {
                return@withContext Result.failure(IllegalStateException("Job is no longer open or was already claimed by another provider"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates status of a job in Supabase (e.g. 'on_the_way', 'arrived', 'in_progress', 'completed').
     */
    suspend fun updateJobStatus(jobId: String, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isoNow = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())

            val payload = JSONObject().apply {
                put("status", status.lowercase())
                put("status_updated_at", isoNow)
                if (status.equals("completed", ignoreCase = true)) {
                    put("completed_at", isoNow)
                }
            }
            val url = "$SUPABASE_URL/rest/v1/jobs?id=eq.$jobId"
            val requestBuilder = Request.Builder()
                .url(url)
                .patch(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful || response.code in 200..204) {
                Result.success(Unit)
            } else {
                val body = response.body?.string().orEmpty()
                Result.failure(IOException("Failed to update job status in Supabase (HTTP ${response.code}): $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================================================
    // 4. Job Ratings Table
    // ========================================================================

    /**
     * Submits a rating for a completed job.
     * RLS policy ("Customers can rate own jobs") ensures:
     * - auth.uid() = customer_id
     * - exists (select 1 from jobs where id = job_id and customer_id = auth.uid())
     */
    suspend fun submitJobRating(
        jobId: String,
        customerId: String,
        providerId: String,
        rating: Int,
        comment: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("job_id", jobId)
                put("customer_id", customerId)
                put("provider_id", providerId)
                put("rating", rating)
                put("comment", comment)
            }

            val url = "$SUPABASE_URL/rest/v1/job_ratings"
            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val body = response.body?.string().orEmpty()
                Result.failure(IOException("Rating submission failed (HTTP ${response.code}): $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================================================
    // 5. FCM Device Token Registration & Dispatch
    // ========================================================================

    /**
     * Captures and syncs device FCM token into public.device_tokens table
     * as well as fcm_token column on users / service_providers.
     */
    suspend fun registerDeviceToken(
        phone: String,
        role: String,
        fcmToken: String,
        categories: List<String> = emptyList(),
        isOnline: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val formattedPhone = if (phone.startsWith("+")) phone else "+$phone"
            val url = "$SUPABASE_URL/rest/v1/device_tokens"
            val categoriesJson = JSONArray().apply {
                categories.forEach { put(it) }
            }
            val payload = JSONObject().apply {
                put("phone", formattedPhone)
                put("role", role)
                put("fcm_token", fcmToken)
                put("categories", categoriesJson)
                put("is_online", isOnline)
                put("updated_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date()))
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            try {
                client.newCall(requestBuilder.build()).execute()
            } catch (_: Exception) {}

            // Also update direct column in users or service_providers
            try {
                val table = if (role == "provider") "service_providers" else "users"
                val patchUrl = "$SUPABASE_URL/rest/v1/$table?phone=eq.$formattedPhone"
                val patchBody = JSONObject().apply { put("fcm_token", fcmToken) }
                val patchReq = Request.Builder()
                    .url(patchUrl)
                    .patch(patchBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                getAuthHeaders().forEach { (k, v) -> patchReq.addHeader(k, v) }
                client.newCall(patchReq.build()).execute()
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Triggers the notify-job-event Edge Function directly.
     * Functions as an immediate trigger alongside Postgres Database Webhooks.
     */
    suspend fun dispatchNotificationEvent(
        eventType: String,
        table: String,
        record: JSONObject
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/functions/v1/notify-job-event"
            val payload = JSONObject().apply {
                put("type", eventType)
                put("table", table)
                put("record", record)
            }
            val reqBuilder = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            getAuthHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val response = client.newCall(reqBuilder.build()).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================================================
    // 5. OTP Codes (Restricted / Zero Client Access)
    // ========================================================================

    /**
     * Direct query to `otp_codes` from client application.
     * Because `otp_codes` has RLS enabled with ZERO policies for anon/authenticated roles,
     * this query MUST return empty or be denied (HTTP 401/403 or empty array).
     */
    suspend fun attemptQueryOtpCodes(): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/otp_codes?select=*"
            val requestBuilder = Request.Builder().url(url)
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val array = JSONArray(bodyString)
                val list = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    list.add(array.getJSONObject(i))
                }
                // Under RLS with zero policies, array is empty []
                Result.success(list)
            } else {
                // Denied by Supabase PostgREST
                Result.failure(IOException("Access denied to otp_codes (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the active theme record from the Supabase `app_themes` table.
     * Uses the anon key for public read access.
     */
    suspend fun fetchActiveThemeRemote(): MobileAppTheme? = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/app_themes?is_active=eq.true&select=*&limit=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful && bodyString.isNotBlank()) {
                val array = JSONArray(bodyString)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    return@withContext MobileAppTheme.fromJson(obj)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // ========================================================================
    // 6. Provider Live Location Tracking (InDrive Style)
    // ========================================================================

    /**
     * Upserts provider location for an active job into `provider_locations`.
     * Overwrites existing row for this job (job_id is primary key).
     */
    suspend fun upsertProviderLocation(
        jobId: String,
        providerId: String,
        lat: Double,
        lng: Double,
        heading: Double?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/provider_locations"
            val isoTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())

            val bodyObj = JSONObject().apply {
                put("job_id", jobId)
                put("provider_id", providerId)
                put("lat", lat)
                put("lng", lng)
                if (heading != null) put("heading", heading)
                put("updated_at", isoTime)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful || response.code in 200..204) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to upsert provider location (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves current provider location for a job.
     */
    suspend fun getProviderLocation(jobId: String): ProviderLocation? = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/provider_locations?job_id=eq.$jobId&select=*&limit=1"
            val requestBuilder = Request.Builder().url(url).get()
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()
            if (response.isSuccessful && bodyString.isNotBlank()) {
                val array = JSONArray(bodyString)
                if (array.length() > 0) {
                    return@withContext ProviderLocation.fromJson(array.getJSONObject(0))
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // ========================================================================
    // In-App Messaging
    // ========================================================================

    suspend fun getJobMessages(jobId: String): List<JobMessage> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/job_messages?job_id=eq.$jobId&order=created_at.asc"
            val requestBuilder = Request.Builder().url(url).get()
            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()
            if (response.isSuccessful && bodyString.isNotBlank()) {
                val array = JSONArray(bodyString)
                val list = mutableListOf<JobMessage>()
                for (i in 0 until array.length()) {
                    list.add(JobMessage.fromJson(array.getJSONObject(i)))
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendJobMessage(
        jobId: String,
        senderId: String,
        senderType: String,
        messageText: String
    ): Result<JobMessage> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/job_messages"
            val isoTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())
            val id = UUID.randomUUID().toString()

            val bodyObj = JSONObject().apply {
                put("id", id)
                put("job_id", jobId)
                put("sender_id", senderId)
                put("sender_type", senderType)
                put("message", messageText)
                put("created_at", isoTime)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("Prefer", "return=representation")
                .post(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()
            if (response.isSuccessful || response.code in 200..204) {
                val message = if (bodyString.isNotBlank() && bodyString.startsWith("[")) {
                    val arr = JSONArray(bodyString)
                    if (arr.length() > 0) JobMessage.fromJson(arr.getJSONObject(0))
                    else JobMessage(id, jobId, senderId, senderType, messageText, isoTime)
                } else {
                    JobMessage(id, jobId, senderId, senderType, messageText, isoTime)
                }
                Result.success(message)
            } else {
                // Return gracefully generated message to avoid breaking client offline
                Result.success(JobMessage(id, jobId, senderId, senderType, messageText, isoTime))
            }
        } catch (e: Exception) {
            val isoTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())
            Result.success(JobMessage(UUID.randomUUID().toString(), jobId, senderId, senderType, messageText, isoTime))
        }
    }

    suspend fun markMessagesAsRead(jobId: String, readerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isoTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())

            val url = "$SUPABASE_URL/rest/v1/job_messages?job_id=eq.$jobId&sender_id=neq.$readerId&read_at=is.null"
            val bodyObj = JSONObject().apply {
                put("read_at", isoTime)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful || response.code in 200..204) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to mark messages read (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================================================
    // Call Logs (Agora Voice History)
    // ========================================================================

    suspend fun logCallStart(jobId: String, callerId: String): Result<String> = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        try {
            val url = "$SUPABASE_URL/rest/v1/call_logs"
            val isoTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())

            val bodyObj = JSONObject().apply {
                put("id", id)
                put("job_id", jobId)
                put("caller_id", callerId)
                put("started_at", isoTime)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            client.newCall(requestBuilder.build()).execute()
            Result.success(id)
        } catch (e: Exception) {
            Result.success(id)
        }
    }

    suspend fun logCallEnd(callId: String, durationSeconds: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/call_logs?id=eq.$callId"
            val isoTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())

            val bodyObj = JSONObject().apply {
                put("ended_at", isoTime)
                put("duration_seconds", durationSeconds)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .patch(bodyObj.toString().toRequestBody(JSON_MEDIA_TYPE))

            getAuthHeaders().forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            client.newCall(requestBuilder.build()).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

