package com.example.data.model

import org.json.JSONObject

/**
 * Annotation matching @Serializable for compatibility with Kotlin serialization frameworks
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Serializable

/**
 * Annotation matching @SerialName for mapping JSON column names
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class SerialName(val value: String)

/**
 * Data model matching the `app_themes` Supabase table.
 * Supports dynamic remote runtime theming configured via the admin dashboard.
 */
@Serializable
data class MobileAppTheme(
    val id: String = "default_coral_sunset",
    val name: String = "Coral Sunset",
    @SerialName("primary_color") val primaryColor: String = "#DC5F45",
    @SerialName("accent_color") val accentColor: String = "#2A9D8F",
    @SerialName("background_color") val backgroundColor: String = "#FFFBF7",
    @SerialName("text_color") val textColor: String = "#292524",
    @SerialName("is_active") val isActive: Boolean = true
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("primary_color", primaryColor)
            put("accent_color", accentColor)
            put("background_color", backgroundColor)
            put("text_color", textColor)
            put("is_active", isActive)
        }
    }

    companion object {
        val DEFAULT_FALLBACK_THEME = MobileAppTheme(
            id = "default_coral_sunset",
            name = "Coral Sunset",
            primaryColor = "#DC5F45",
            accentColor = "#2A9D8F",
            backgroundColor = "#FFFBF7",
            textColor = "#292524",
            isActive = true
        )

        fun fromJson(json: JSONObject): MobileAppTheme {
            return MobileAppTheme(
                id = json.optString("id", DEFAULT_FALLBACK_THEME.id),
                name = json.optString("name", DEFAULT_FALLBACK_THEME.name),
                primaryColor = json.optString("primary_color", DEFAULT_FALLBACK_THEME.primaryColor),
                accentColor = json.optString("accent_color", DEFAULT_FALLBACK_THEME.accentColor),
                backgroundColor = json.optString("background_color", DEFAULT_FALLBACK_THEME.backgroundColor),
                textColor = json.optString("text_color", DEFAULT_FALLBACK_THEME.textColor),
                isActive = json.optBoolean("is_active", true)
            )
        }
    }
}
