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

    val primaryColorInt: Int get() = try {
        android.graphics.Color.parseColor(primaryColor)
    } catch (e: Exception) {
        0xFFDC5F45.toInt()
    }

    val accentColorInt: Int get() = try {
        android.graphics.Color.parseColor(accentColor)
    } catch (e: Exception) {
        0xFF2A9D8F.toInt()
    }

    val backgroundColorInt: Int get() = try {
        android.graphics.Color.parseColor(backgroundColor)
    } catch (e: Exception) {
        0xFFFFFBF7.toInt()
    }

    val textColorInt: Int get() = try {
        android.graphics.Color.parseColor(textColor)
    } catch (e: Exception) {
        0xFF292524.toInt()
    }

    companion object {
        val DEFAULT_FALLBACK_THEME = MobileAppTheme(
            id = "default_apple_cupertino",
            name = "Apple Cupertino",
            primaryColor = "#007AFF",
            accentColor = "#FF9500",
            backgroundColor = "#F2F2F7",
            textColor = "#1C1C1E",
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
