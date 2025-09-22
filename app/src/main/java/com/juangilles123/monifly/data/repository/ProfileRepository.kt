package com.juangilles123.monifly.data.repository

import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.data.model.Profile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ProfileInsert(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("current_streak") val currentStreak: Int? = null,
    @SerialName("max_streak") val maxStreak: Int? = null,
    @SerialName("last_activity_date") val lastActivityDate: String? = null,
    @SerialName("has_seen_welcome") val hasSeenWelcome: Boolean? = null,
    @SerialName("welcome_seen_at") val welcomeSeenAt: String? = null
)

@Serializable
data class ProfileUpdate(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("current_streak") val currentStreak: Int? = null,
    @SerialName("max_streak") val maxStreak: Int? = null,
    @SerialName("last_activity_date") val lastActivityDate: String? = null,
    @SerialName("has_seen_welcome") val hasSeenWelcome: Boolean? = null,
    @SerialName("welcome_seen_at") val welcomeSeenAt: String? = null,
    @SerialName("updated_at") val updatedAt: String
)

class ProfileRepository {
    
    private val supabaseClient = SupabaseManager.client
    
    suspend fun getProfile(userId: String): Result<Profile?> {
        return withContext(Dispatchers.IO) {
            try {
                val profiles = supabaseClient
                    .from("profiles")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeList<Profile>()
                
                Result.success(profiles.firstOrNull())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun createProfile(profile: Profile): Result<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                val profileInsert = ProfileInsert(
                    id = profile.id,
                    fullName = if (profile.fullName.isNullOrBlank()) null else profile.fullName,
                    countryCode = if (profile.countryCode.isNullOrBlank()) null else profile.countryCode,
                    currentStreak = profile.currentStreak,
                    maxStreak = profile.maxStreak,
                    lastActivityDate = if (profile.lastActivityDate.isNullOrBlank()) null else profile.lastActivityDate,
                    hasSeenWelcome = profile.hasSeenWelcome,
                    welcomeSeenAt = if (profile.welcomeSeenAt.isNullOrBlank()) null else profile.welcomeSeenAt
                )
                
                val createdProfile = supabaseClient
                    .from("profiles")
                    .insert(profileInsert) {
                        select()
                    }
                    .decodeSingle<Profile>()
                
                Result.success(createdProfile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun updateProfile(profile: Profile): Result<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                val profileUpdate = ProfileUpdate(
                    fullName = if (profile.fullName.isNullOrBlank()) null else profile.fullName,
                    countryCode = if (profile.countryCode.isNullOrBlank()) null else profile.countryCode,
                    currentStreak = profile.currentStreak,
                    maxStreak = profile.maxStreak,
                    lastActivityDate = if (profile.lastActivityDate.isNullOrBlank()) null else profile.lastActivityDate,
                    hasSeenWelcome = profile.hasSeenWelcome,
                    welcomeSeenAt = if (profile.welcomeSeenAt.isNullOrBlank()) null else profile.welcomeSeenAt,
                    updatedAt = java.time.Instant.now().toString()
                )
                
                val updatedProfile = supabaseClient
                    .from("profiles")
                    .update(profileUpdate) {
                        filter {
                            eq("id", profile.id)
                        }
                        select()
                    }
                    .decodeSingle<Profile>()
                
                Result.success(updatedProfile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun updateStreak(userId: String, currentStreak: Int, maxStreak: Int): Result<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                val updateData = mapOf(
                    "current_streak" to currentStreak,
                    "max_streak" to maxStreak,
                    "last_activity_date" to java.time.Instant.now().toString(),
                    "updated_at" to java.time.Instant.now().toString()
                )
                
                val updatedProfile = supabaseClient
                    .from("profiles")
                    .update(updateData) {
                        filter {
                            eq("id", userId)
                        }
                        select()
                    }
                    .decodeSingle<Profile>()
                
                Result.success(updatedProfile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun markWelcomeSeen(userId: String): Result<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                val updateData = mapOf(
                    "has_seen_welcome" to true,
                    "welcome_seen_at" to java.time.Instant.now().toString(),
                    "updated_at" to java.time.Instant.now().toString()
                )
                
                val updatedProfile = supabaseClient
                    .from("profiles")
                    .update(updateData) {
                        filter {
                            eq("id", userId)
                        }
                        select()
                    }
                    .decodeSingle<Profile>()
                
                Result.success(updatedProfile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun upsertProfile(profile: Profile): Result<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                val upsertedProfile = supabaseClient
                    .from("profiles")
                    .upsert(profile) {
                        select()
                    }
                    .decodeSingle<Profile>()
                
                Result.success(upsertedProfile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}