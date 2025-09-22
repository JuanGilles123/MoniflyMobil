package com.juangilles123.monifly.data.repository

import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.data.model.Goal
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class GoalInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("target_amount") val targetAmount: Double,
    @SerialName("current_saved") val currentSaved: Double = 0.0,
    @SerialName("target_date") val targetDate: String, // Not nullable as database requires it
    @SerialName("status") val status: String = "active"
)

@Serializable
data class GoalUpdate(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("target_amount") val targetAmount: Double,
    @SerialName("current_saved") val currentSaved: Double,
    @SerialName("target_date") val targetDate: String, // Not nullable as database requires it
    @SerialName("status") val status: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class GoalProgressUpdate(
    @SerialName("current_saved") val currentSaved: Double,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class GoalStatusUpdate(
    @SerialName("status") val status: String,
    @SerialName("updated_at") val updatedAt: String
)

class GoalRepository {
    
    private val supabaseClient = SupabaseManager.client
    
    suspend fun getGoalsByUserId(userId: String): Result<List<Goal>> {
        return withContext(Dispatchers.IO) {
            try {
                val goals = supabaseClient
                    .from("goals")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("user_id", userId)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Goal>()
                
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun insertGoal(goal: Goal): Result<Goal> {
        return withContext(Dispatchers.IO) {
            try {
                // Create a proper serializable object for insertion
                val goalInsert = GoalInsert(
                    userId = goal.userId,
                    name = goal.name,
                    description = if (goal.description.isNullOrBlank()) null else goal.description,
                    targetAmount = goal.targetAmount,
                    currentSaved = goal.currentSaved,
                    // If no target date is provided, use a far future date to indicate "no deadline"
                    targetDate = if (goal.targetDate.isNullOrBlank()) "2099-12-31" else goal.targetDate,
                    status = goal.status ?: "active"
                )
                
                val insertedGoal = supabaseClient
                    .from("goals")
                    .insert(goalInsert) {
                        select()
                    }
                    .decodeSingle<Goal>()
                
                Result.success(insertedGoal)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun updateGoal(goal: Goal): Result<Goal> {
        return withContext(Dispatchers.IO) {
            try {
                // Create a proper serializable object for update
                val goalUpdate = GoalUpdate(
                    name = goal.name,
                    description = if (goal.description.isNullOrBlank()) null else goal.description,
                    targetAmount = goal.targetAmount,
                    currentSaved = goal.currentSaved,
                    // If no target date is provided, use a far future date to indicate "no deadline"
                    targetDate = if (goal.targetDate.isNullOrBlank()) "2099-12-31" else goal.targetDate,
                    status = goal.status ?: "active",
                    updatedAt = java.time.Instant.now().toString()
                )
                
                val updatedGoal = supabaseClient
                    .from("goals")
                    .update(goalUpdate) {
                        filter {
                            eq("id", goal.id)
                        }
                        select()
                    }
                    .decodeSingle<Goal>()
                
                Result.success(updatedGoal)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun deleteGoal(goalId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabaseClient
                    .from("goals")
                    .delete {
                        filter {
                            eq("id", goalId)
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun updateGoalProgress(goalId: String, currentSaved: Double): Result<Goal> {
        return withContext(Dispatchers.IO) {
            try {
                val progressUpdate = GoalProgressUpdate(
                    currentSaved = currentSaved,
                    updatedAt = java.time.Instant.now().toString()
                )
                
                val updatedGoal = supabaseClient
                    .from("goals")
                    .update(progressUpdate) {
                        filter {
                            eq("id", goalId)
                        }
                        select()
                    }
                    .decodeSingle<Goal>()
                
                Result.success(updatedGoal)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun markGoalCompleted(goalId: String): Result<Goal> {
        return withContext(Dispatchers.IO) {
            try {
                val statusUpdate = GoalStatusUpdate(
                    status = "completed",
                    updatedAt = java.time.Instant.now().toString()
                )
                
                val updatedGoal = supabaseClient
                    .from("goals")
                    .update(statusUpdate) {
                        filter {
                            eq("id", goalId)
                        }
                        select()
                    }
                    .decodeSingle<Goal>()
                
                Result.success(updatedGoal)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun reactivateGoal(goalId: String): Result<Goal> {
        return withContext(Dispatchers.IO) {
            try {
                val statusUpdate = GoalStatusUpdate(
                    status = "active",
                    updatedAt = java.time.Instant.now().toString()
                )
                
                val updatedGoal = supabaseClient
                    .from("goals")
                    .update(statusUpdate) {
                        filter {
                            eq("id", goalId)
                        }
                        select()
                    }
                    .decodeSingle<Goal>()
                
                Result.success(updatedGoal)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getActiveGoalsByUserId(userId: String): Result<List<Goal>> {
        return withContext(Dispatchers.IO) {
            try {
                val goals = supabaseClient
                    .from("goals")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("user_id", userId)
                            eq("status", "active")
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Goal>()
                
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getCompletedGoalsByUserId(userId: String): Result<List<Goal>> {
        return withContext(Dispatchers.IO) {
            try {
                val goals = supabaseClient
                    .from("goals")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("user_id", userId)
                            eq("status", "completed")
                        }
                        order("updated_at", Order.DESCENDING)
                    }
                    .decodeList<Goal>()
                
                Result.success(goals)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}