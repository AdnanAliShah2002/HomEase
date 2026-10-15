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

data class OtpVerifyResult(
    val isNewUser: Boolean,
    val userId: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
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

        fun normalizePhone(phone: String): String {
            val digits = phone.filter { it.isDigit() }
            val nationalNumber = when {
                digits.startsWith("920") -> digits.removePrefix("920")
                digits.startsWith("92") -> digits.removePrefix("92")
                digits.startsWith("0") -> digits.removePrefix("0")
                else -> digits
            }
            return "+92$nationalNumber"
        }
    }

    /**
     * Sends a WhatsApp OTP request to the Supabase Edge Function.
     * The Edge Function generates the OTP, delivers it via WhatsApp, and returns success.
     * The OTP code is NEVER leaked to the client.
     */
    suspend fun sendOtp(phone: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val formattedPhone = normalizePhone(phone)
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
     * Validates the entered OTP code strictly against the secure Supabase Edge Function /verify-otp.
     */
    suspend fun verifyOtp(phone: String, code: String): Result<OtpVerifyResult> = withContext(Dispatchers.IO) {
        try {
            val formattedPhone = normalizePhone(phone)
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
                    val refreshToken = when {
                        json.has("refreshToken") && !json.isNull("refreshToken") -> json.optString("refreshToken")
                        json.has("refresh_token") && !json.isNull("refresh_token") -> json.optString("refresh_token")
                        else -> null
                    }
                    return@withContext Result.success(OtpVerifyResult(
                        isNewUser = isNewUser,
                        userId = userId,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        phone = formattedPhone
                    ))
                } else {
                    val errorMsg = json.optString("error", "Invalid verification code")
                    return@withContext Result.failure(IOException(errorMsg))
                }
            }

            val bodyString = response?.body?.string().orEmpty()
            val errorMsg = try {
                val json = JSONObject(bodyString)
                json.optString("error", json.optString("message", "Invalid verification code. Please check your message or request a new code."))
            } catch (_: Exception) {
                if (response?.code == 400 || response?.code == 401) {
                    "Incorrect verification code"
                } else if (response?.code == 429) {
                    "Too many incorrect attempts. Please request a new code."
                } else {
                    "Verification failed (${response?.code ?: "Network error"}). Please try again."
                }
            }
            Result.failure(IOException(errorMsg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
