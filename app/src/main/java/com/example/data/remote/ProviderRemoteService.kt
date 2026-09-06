package com.example.data.remote

import android.content.Context
import com.example.data.db.UserEntity

/**
 * ProviderRemoteService
 * 
 * Handles remote registration and storage uploads for service providers.
 * Delegates to HomEaseSupabaseClient which:
 * - Uses the anon key (SUPABASE_ANON_KEY), never the service role key.
 * - Authenticates table queries and storage uploads with the user's active session.
 */
class ProviderRemoteService(
    private val context: Context
) {
    companion object {
        const val SUPABASE_URL = HomEaseSupabaseClient.SUPABASE_URL
        const val SUPABASE_ANON_KEY = HomEaseSupabaseClient.SUPABASE_ANON_KEY
        const val STORAGE_BUCKET = HomEaseSupabaseClient.STORAGE_BUCKET
    }

    private val supabaseClient = HomEaseSupabaseClient.getInstance(context)

    /**
     * Uploads an image from a content URI to the private Supabase Storage bucket `provider-documents`.
     * Returns the remote storage path/URL if successful, or the local URI string as fallback.
     */
    suspend fun uploadDocument(imageUriString: String?, folder: String, phone: String): String? {
        return supabaseClient.uploadDocument(imageUriString, folder, phone)
    }

    /**
     * Submits the provider registration to Supabase `service_providers` table.
     * Authenticates using the user's session token and the anon key.
     */
    suspend fun submitProviderRegistration(provider: UserEntity): Result<Boolean> {
        return supabaseClient.submitProviderRegistration(provider)
    }
}
