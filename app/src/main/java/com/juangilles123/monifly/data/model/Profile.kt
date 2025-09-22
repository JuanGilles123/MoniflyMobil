package com.juangilles123.monifly.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    @SerialName("id") val id: String, // uuid
    @SerialName("full_name") val fullName: String? = null, // text
    @SerialName("country_code") val countryCode: String? = null, // text
    @SerialName("updated_at") val updatedAt: String? = null, // timestamp with time zone
    @SerialName("current_streak") val currentStreak: Int? = null, // integer
    @SerialName("max_streak") val maxStreak: Int? = null, // integer
    @SerialName("last_activity_date") val lastActivityDate: String? = null, // timestamp with time zone
    @SerialName("has_seen_welcome") val hasSeenWelcome: Boolean? = false, // boolean
    @SerialName("welcome_seen_at") val welcomeSeenAt: String? = null // timestamp with time zone
)