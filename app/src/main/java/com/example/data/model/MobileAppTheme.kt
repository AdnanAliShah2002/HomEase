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
    val id: String = "default_apple_pro_liquid",
    val name: String = "Apple Pro Liquid",
    @SerialName("primary_color") val primaryColor: String = "#4F46E5",
    @SerialName("accent_color") val accentColor: String = "#D97706",
    @SerialName("background_color") val backgroundColor: String = "#F6F7FB",
    @SerialName("text_color") val textColor: String = "#1E293B",
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

    val primaryColorInt: Int get() = try {
        android.graphics.Color.parseColor(primaryColor)
    } catch (e: Exception) {
        0xFF4F46E5.toInt()
    }

    val accentColorInt: Int get() = try {
        android.graphics.Color.parseColor(accentColor)
    } catch (e: Exception) {
        0xFFD97706.toInt()
    }

    val backgroundColorInt: Int get() = try {
        android.graphics.Color.parseColor(backgroundColor)
    } catch (e: Exception) {
        0xFFF6F7FB.toInt()
    }

    val textColorInt: Int get() = try {
        android.graphics.Color.parseColor(textColor)
    } catch (e: Exception) {
        0xFF1E293B.toInt()
    }

    companion object {
        val DEFAULT_FALLBACK_THEME = MobileAppTheme(
            id = "default_apple_pro_liquid",
            name = "Apple Pro Liquid",
            primaryColor = "#4F46E5",
            accentColor = "#D97706",
            backgroundColor = "#F6F7FB",
            textColor = "#1E293B",
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
