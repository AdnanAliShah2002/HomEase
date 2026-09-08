package com.example.data.theme

import com.example.data.model.MobileAppTheme
import com.example.data.remote.HomEaseSupabaseClient

/**
 * Service to fetch active dynamic theme on app launch with local fallback.
 */
class ThemeRemoteService(
    private val supabaseClient: HomEaseSupabaseClient,
    private val localThemeStore: LocalThemeStore
) {
    /**
     * Fetches the active theme from the Supabase `app_themes` table.
     * Caches the result locally on success, or falls back to the cached theme / default theme.
     */
    suspend fun fetchActiveTheme(): MobileAppTheme {
        return try {
            val remoteTheme = supabaseClient.fetchActiveThemeRemote()
            if (remoteTheme != null) {
                localThemeStore.saveTheme(remoteTheme)
                remoteTheme
            } else {
                localThemeStore.getCachedTheme() ?: MobileAppTheme.DEFAULT_FALLBACK_THEME
            }
        } catch (e: Exception) {
            localThemeStore.getCachedTheme() ?: MobileAppTheme.DEFAULT_FALLBACK_THEME
        }
    }
}

/**
 * Top-level convenience function matching the requested signature:
 * suspend fun fetchActiveTheme(supabaseClient: ...): MobileAppTheme?
 */
suspend fun fetchActiveTheme(
    supabaseClient: HomEaseSupabaseClient,
    localThemeStore: LocalThemeStore
): MobileAppTheme {
    return ThemeRemoteService(supabaseClient, localThemeStore).fetchActiveTheme()
}
