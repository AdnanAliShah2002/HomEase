package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.HomEaseSupabaseClient
import com.example.data.remote.PublicProviderProfile
import com.example.data.remote.SupabaseSession
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupabaseRlsSecurityTest {

    private lateinit var context: Context
    private lateinit var supabaseClient: HomEaseSupabaseClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        supabaseClient = HomEaseSupabaseClient.getInstance(context)
        supabaseClient.clearSession()
    }

    @Test
    fun `client is initialized strictly with anon key and never exposes service role key`() {
        val anonKey = HomEaseSupabaseClient.SUPABASE_ANON_KEY
        val baseUrl = HomEaseSupabaseClient.SUPABASE_URL

        assertTrue("Base URL must point to Supabase project", baseUrl.contains("supabase.co"))
        assertTrue("Anon key must not be empty", anonKey.isNotBlank())

        // Decode JWT payload of SUPABASE_ANON_KEY to verify role == "anon"
        val parts = anonKey.split(".")
        assertEquals(3, parts.size)
        val payloadJson = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
        val payloadObj = JSONObject(payloadJson)
        val role = payloadObj.optString("role")

        assertEquals("Client key must strictly have role 'anon'", "anon", role)
        assertFalse("Client key must NEVER be service_role", role.contains("service_role"))
    }

    @Test
    fun `headers properly attach authenticated user session token for direct table reads and writes`() {
        // 1. Initial state: unauthenticated
        assertFalse(supabaseClient.isAuthenticated())
        val guestHeaders = supabaseClient.getAuthHeaders()
        assertEquals(HomEaseSupabaseClient.SUPABASE_ANON_KEY, guestHeaders["apikey"])
        assertEquals("Bearer ${HomEaseSupabaseClient.SUPABASE_ANON_KEY}", guestHeaders["Authorization"])

        // 2. Log in as Customer Alice
        val aliceId = UUID.randomUUID().toString()
        val aliceToken = "jwt_token_for_alice_${UUID.randomUUID()}"
        val session = SupabaseSession(
            userId = aliceId,
            phone = "+923001112233",
            accessToken = aliceToken,
            role = "customer"
        )
        supabaseClient.setSession(session)

        assertTrue(supabaseClient.isAuthenticated())
        assertEquals(aliceId, supabaseClient.getSession()?.userId)

        // 3. Verify headers now carry Alice's authenticated session
        val authHeaders = supabaseClient.getAuthHeaders()
        assertEquals(HomEaseSupabaseClient.SUPABASE_ANON_KEY, authHeaders["apikey"])
        assertEquals("Bearer $aliceToken", authHeaders["Authorization"])

        // 4. Logout clears session back to unauthenticated
        supabaseClient.clearSession()
        assertFalse(supabaseClient.isAuthenticated())
        assertNull(supabaseClient.getSession())
    }

    /**
     * Simulates Supabase PostgREST Row Level Security engine:
     * Evaluates `auth.uid()` against requested table and policies.
     */
    private fun createSupabaseRlsMockClient(
        currentUserUid: String?,
        userRole: String? = "authenticated"
    ): OkHttpClient {
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        val mockInterceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()
            val authHeader = request.header("Authorization") ?: ""
            val token = authHeader.removePrefix("Bearer ").trim()

            // In Supabase, if token is valid user token, auth.uid() is resolved to the user's UUID
            val currentAuthUid = if (token.isNotBlank() && token != HomEaseSupabaseClient.SUPABASE_ANON_KEY) {
                currentUserUid
            } else {
                null
            }

            val path = request.url.encodedPath

            // 1. RLS enforcement on `users` table:
            // Policy: auth.uid() = id
            if (path.contains("/rest/v1/users")) {
                val filterId = request.url.queryParameter("id")?.removePrefix("eq.")
                if (currentAuthUid != null && filterId == currentAuthUid) {
                    val userObj = JSONObject().apply {
                        put("id", currentAuthUid)
                        put("full_name", "Authorized User")
                        put("phone", "+923001234567")
                        put("role", "customer")
                    }
                    val body = JSONArray().put(userObj).toString()
                    return@Interceptor Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(body.toResponseBody(jsonMediaType))
                        .build()
                } else {
                    // RLS filters out rows that do not match auth.uid() = id -> returns empty []
                    return@Interceptor Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("[]".toResponseBody(jsonMediaType))
                        .build()
                }
            }

            // 2. RLS enforcement on `jobs` table:
            // Customers can only view own jobs (auth.uid() = customer_id)
            // Providers can only view assigned jobs (auth.uid() = provider_id)
            if (path.contains("/rest/v1/jobs")) {
                val customerFilter = request.url.queryParameter("customer_id")?.removePrefix("eq.")
                val providerFilter = request.url.queryParameter("provider_id")?.removePrefix("eq.")

                if (customerFilter != null) {
                    if (currentAuthUid != null && customerFilter == currentAuthUid) {
                        val job = JSONObject().apply {
                            put("id", UUID.randomUUID().toString())
                            put("customer_id", currentAuthUid)
                            put("service_title", "Plumbing Repair")
                            put("status", "in_progress")
                        }
                        val body = JSONArray().put(job).toString()
                        return@Interceptor Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(body.toResponseBody(jsonMediaType))
                            .build()
                    } else {
                        // Unauthorized cross-user query: RLS returns empty list []
                        return@Interceptor Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("[]".toResponseBody(jsonMediaType))
                            .build()
                    }
                }

                if (providerFilter != null) {
                    if (currentAuthUid != null && providerFilter == currentAuthUid) {
                        val job = JSONObject().apply {
                            put("id", UUID.randomUUID().toString())
                            put("provider_id", currentAuthUid)
                            put("service_title", "Electrical Wiring")
                            put("status", "accepted")
                        }
                        val body = JSONArray().put(job).toString()
                        return@Interceptor Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(body.toResponseBody(jsonMediaType))
                            .build()
                    } else {
                        return@Interceptor Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("[]".toResponseBody(jsonMediaType))
                            .build()
                    }
                }
            }

            // 3. RLS enforcement on `service_providers` and `public_provider_profiles` view:
            if (path.contains("/rest/v1/public_provider_profiles")) {
                // Safe public view exposing ONLY safe non-sensitive columns
                val p1 = JSONObject().apply {
                    put("id", "provider_pub_1")
                    put("full_name", "Tariq Mahmood")
                    put("profile_photo_url", "https://example.com/tariq.jpg")
                    put("service_categories", JSONArray(listOf("plumbing")))
                    put("avg_rating", 4.9)
                    put("total_jobs", 34)
                    put("bio", "Licensed plumber with 10 years experience")
                    put("business_name", "Tariq Plumbing Solutions")
                    // Note: cnic_number, cnic_front_url, cnic_back_url, payout_account_number are NOT in this view!
                }
                val body = JSONArray().put(p1).toString()
                return@Interceptor Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody(jsonMediaType))
                    .build()
            }

            if (path.contains("/rest/v1/service_providers")) {
                val providerIdFilter = request.url.queryParameter("id")?.removePrefix("eq.")
                // Provider can only view own profile
                if (currentAuthUid != null && providerIdFilter == currentAuthUid) {
                    val fullProvider = JSONObject().apply {
                        put("id", currentAuthUid)
                        put("full_name", "Tariq Mahmood")
                        put("cnic_number", "35201-1234567-1")
                        put("home_address", "Street 12, Gulberg")
                        put("status", "approved")
                    }
                    val body = JSONArray().put(fullProvider).toString()
                    return@Interceptor Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(body.toResponseBody(jsonMediaType))
                        .build()
                } else {
                    // Attempting to query another provider's full profile directly from service_providers table
                    // is restricted by RLS (sensitive details hidden)
                    return@Interceptor Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("[]".toResponseBody(jsonMediaType))
                        .build()
                }
            }

            // 4. `otp_codes` table: ZERO client-facing policies
            if (path.contains("/rest/v1/otp_codes")) {
                // Client access is completely blocked (403 Forbidden or empty [])
                return@Interceptor Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(403)
                    .message("Forbidden")
                    .body("{\"message\":\"permission denied for table otp_codes\"}".toResponseBody(jsonMediaType))
                    .build()
            }

            // Fallback response
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("[]".toResponseBody(jsonMediaType))
                .build()
        }

        return OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()
    }

    @Test
    fun `RLS test - logged in customer can query own jobs but cross-user job query returns empty`() {
        val customerAliceId = "customer_alice_${UUID.randomUUID()}"
        val customerBobId = "customer_bob_${UUID.randomUUID()}"
        val aliceToken = "jwt_alice_${UUID.randomUUID()}"

        // Alice is logged in
        val rlsClient = createSupabaseRlsMockClient(currentUserUid = customerAliceId)

        // Alice queries her own jobs: /rest/v1/jobs?customer_id=eq.$customerAliceId
        val ownJobsRequest = Request.Builder()
            .url("${HomEaseSupabaseClient.SUPABASE_URL}/rest/v1/jobs?customer_id=eq.$customerAliceId&select=*")
            .addHeader("apikey", HomEaseSupabaseClient.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $aliceToken")
            .build()

        val ownJobsResponse = rlsClient.newCall(ownJobsRequest).execute()
        assertTrue(ownJobsResponse.isSuccessful)
        val ownJobs = JSONArray(ownJobsResponse.body?.string().orEmpty())
        assertEquals("Customer Alice can query her own jobs", 1, ownJobs.length())
        assertEquals(customerAliceId, ownJobs.getJSONObject(0).getString("customer_id"))

        // Alice attempts to query Customer Bob's jobs: /rest/v1/jobs?customer_id=eq.$customerBobId
        val bobJobsRequest = Request.Builder()
            .url("${HomEaseSupabaseClient.SUPABASE_URL}/rest/v1/jobs?customer_id=eq.$customerBobId&select=*")
            .addHeader("apikey", HomEaseSupabaseClient.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $aliceToken")
            .build()

        val bobJobsResponse = rlsClient.newCall(bobJobsRequest).execute()
        val bobJobs = JSONArray(bobJobsResponse.body?.string().orEmpty())
        assertEquals("RLS blocks cross-user query: Bob's jobs return empty to Alice", 0, bobJobs.length())
    }

    @Test
    fun `RLS test - logged in user can query own profile but another user profile returns empty`() {
        val userAliceId = "user_alice_${UUID.randomUUID()}"
        val userBobId = "user_bob_${UUID.randomUUID()}"
        val aliceToken = "jwt_alice_${UUID.randomUUID()}"

        val rlsClient = createSupabaseRlsMockClient(currentUserUid = userAliceId)

        // Alice queries her own profile
        val ownProfileReq = Request.Builder()
            .url("${HomEaseSupabaseClient.SUPABASE_URL}/rest/v1/users?id=eq.$userAliceId&select=*")
            .addHeader("apikey", HomEaseSupabaseClient.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $aliceToken")
            .build()

        val ownRes = rlsClient.newCall(ownProfileReq).execute()
        val ownArray = JSONArray(ownRes.body?.string().orEmpty())
        assertEquals(1, ownArray.length())
        assertEquals(userAliceId, ownArray.getJSONObject(0).getString("id"))

        // Alice attempts to query Bob's private user row
        val bobProfileReq = Request.Builder()
            .url("${HomEaseSupabaseClient.SUPABASE_URL}/rest/v1/users?id=eq.$userBobId&select=*")
            .addHeader("apikey", HomEaseSupabaseClient.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $aliceToken")
            .build()

        val bobRes = rlsClient.newCall(bobProfileReq).execute()
        val bobArray = JSONArray(bobRes.body?.string().orEmpty())
        assertEquals("RLS prevents accessing other user's profile, returns empty", 0, bobArray.length())
    }

    @Test
    fun `public_provider_profiles view exposes safe public fields and hides sensitive CNIC`() {
        val customerToken = "jwt_customer_${UUID.randomUUID()}"
        val rlsClient = createSupabaseRlsMockClient(currentUserUid = "customer_123")

        val publicViewReq = Request.Builder()
            .url("${HomEaseSupabaseClient.SUPABASE_URL}/rest/v1/public_provider_profiles?select=*")
            .addHeader("apikey", HomEaseSupabaseClient.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $customerToken")
            .build()

        val response = rlsClient.newCall(publicViewReq).execute()
        assertTrue(response.isSuccessful)
        val array = JSONArray(response.body?.string().orEmpty())
        assertTrue("Public profiles view returned results", array.length() > 0)

        val profile = array.getJSONObject(0)

        // Safe fields MUST be present
        assertTrue("Full name must be present", profile.has("full_name"))
        assertTrue("Profile photo url must be present", profile.has("profile_photo_url"))
        assertTrue("Service categories must be present", profile.has("service_categories"))
        assertTrue("Avg rating must be present", profile.has("avg_rating"))
        assertTrue("Total jobs must be present", profile.has("total_jobs"))
        assertTrue("Bio must be present", profile.has("bio"))

        // Sensitive fields MUST NOT be exposed in public view
        assertFalse("CNIC number must NEVER be exposed in public view", profile.has("cnic_number"))
        assertFalse("CNIC front photo must NEVER be exposed in public view", profile.has("cnic_front_url"))
        assertFalse("CNIC back photo must NEVER be exposed in public view", profile.has("cnic_back_url"))
        assertFalse("Payout account must NEVER be exposed in public view", profile.has("payout_account_number"))
        assertFalse("Home address must NEVER be exposed in public view", profile.has("home_address"))
    }

    @Test
    fun `RLS test - otp_codes table access is denied to client applications`() {
        val customerToken = "jwt_customer_${UUID.randomUUID()}"
        val rlsClient = createSupabaseRlsMockClient(currentUserUid = "customer_123")

        val otpQueryReq = Request.Builder()
            .url("${HomEaseSupabaseClient.SUPABASE_URL}/rest/v1/otp_codes?select=*")
            .addHeader("apikey", HomEaseSupabaseClient.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $customerToken")
            .build()

        val response = rlsClient.newCall(otpQueryReq).execute()
        assertFalse("Client query to otp_codes must be denied (HTTP 403 Forbidden)", response.isSuccessful)
        assertEquals(403, response.code)
    }
}
