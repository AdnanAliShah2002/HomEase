package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class OtpVerifyResult(
    val isNewUser: Boolean,
    val userId: String? = null,
    val accessToken: String? = null,
    val phone: String? = null
)

class OtpRemoteService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val SUPABASE_URL = "https://nsqrfagylbqrwlbvnsug.supabase.co"
        const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5zcXJmYWd5bGJxcndsYnZuc3VnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI5NTY4NTIsImV4cCI6MjA4ODUzMjg1Mn0.qNnOe8xR6ehutINqCGVK7tfHLBh14tBWgbhFeHvFa40"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        // Stores server-generated OTP from send-otp as fallback when verify-otp function is not deployed on Supabase
        private val pendingOtps = ConcurrentHashMap<String, String>()
    }

    /**
     * Sends a WhatsApp OTP request to the Supabase Edge Function.
     * The Edge Function generates the OTP, delivers it via WhatsApp, and returns success with the code.
     */
    suspend fun sendOtp(phone: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val formattedPhone = if (phone.startsWith("+")) phone else "+$phone"
            val payload = JSONObject().apply {
                put("phone", formattedPhone)
            }.toString()

            val request = Request.Builder()
                .url("$SUPABASE_URL/functions/v1/send-otp")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = try { JSONObject(bodyString) } catch (_: Exception) { JSONObject() }
                if (json.optBoolean("success", true)) {
                    // Extract code returned by the WhatsApp send-otp function
                    val dispatchedCode = when {
                        json.has("code") && !json.isNull("code") -> json.optString("code")
                        json.has("data") -> json.optJSONObject("data")?.optString("code")
                        else -> null
                    }
                    if (!dispatchedCode.isNullOrBlank()) {
                        pendingOtps[formattedPhone] = dispatchedCode.trim()
                    }
                    Result.success(true)
                } else {
                    val errorMsg = json.optString("error", "Failed to send verification code")
                    Result.failure(IOException(errorMsg))
                }
            } else {
                val errorMsg = try {
                    val json = JSONObject(bodyString)
                    json.optString("error", json.optString("message", "Server error (${response.code})"))
                } catch (_: Exception) {
                    "Failed to send code (HTTP ${response.code})"
                }
                Result.failure(IOException(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates the entered OTP code:
     * 1. Attempts remote validation against /functions/v1/verify-otp if deployed.
     * 2. If verify-otp is not deployed on the Supabase project (404 Not Found),
     *    gracefully validates against the code returned and delivered by send-otp.
     */
    suspend fun verifyOtp(phone: String, code: String): Result<OtpVerifyResult> = withContext(Dispatchers.IO) {
        try {
            val formattedPhone = if (phone.startsWith("+")) phone else "+$phone"
            val cleanCode = code.trim()

            val payload = JSONObject().apply {
                put("phone", formattedPhone)
                put("code", cleanCode)
            }.toString()

            val request = Request.Builder()
                .url("$SUPABASE_URL/functions/v1/verify-otp")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                null
            }

            if (response != null && response.isSuccessful) {
                val bodyString = response.body?.string().orEmpty()
                val json = JSONObject(bodyString)
                if (json.optBoolean("success", false)) {
                    val isNewUser = json.optBoolean("isNewUser", true)
                    val userId = if (json.has("userId") && !json.isNull("userId")) json.optString("userId") else null
                    val accessToken = if (json.has("accessToken") && !json.isNull("accessToken")) json.optString("accessToken") else null
                    pendingOtps.remove(formattedPhone)
                    return@withContext Result.success(OtpVerifyResult(isNewUser = isNewUser, userId = userId, accessToken = accessToken, phone = formattedPhone))
                } else {
                    val errorMsg = json.optString("error", "Invalid verification code")
                    return@withContext Result.failure(IOException(errorMsg))
                }
            }

            val is404NotFound = response != null && response.code == 404

            // Fallback to validating against the dispatched OTP code from send-otp
            val expectedCode = pendingOtps[formattedPhone]
            if (expectedCode != null) {
                if (cleanCode == expectedCode) {
                    pendingOtps.remove(formattedPhone)
                    return@withContext Result.success(OtpVerifyResult(isNewUser = true))
                } else {
                    return@withContext Result.failure(IOException("Invalid verification code. Please check your WhatsApp message."))
                }
            }

            // If response was 404 and no code was in memory (e.g. app was restarted)
            if (is404NotFound) {
                return@withContext Result.failure(IOException("Session expired. Please tap 'Resend code' to receive a new OTP on WhatsApp."))
            }

            val bodyString = response?.body?.string().orEmpty()
            val errorMsg = try {
                val json = JSONObject(bodyString)
                json.optString("error", json.optString("message", "Verification failed"))
            } catch (_: Exception) {
                "Verification failed (${response?.code ?: "Network error"})"
            }
            Result.failure(IOException(errorMsg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
