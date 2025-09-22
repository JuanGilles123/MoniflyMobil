package com.juangilles123.monifly.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Goal(
    @SerialName("id") val id: String, // uuid
    @SerialName("user_id") val userId: String, // uuid
    @SerialName("name") val name: String, // text
    @SerialName("description") val description: String? = null, // text
    @SerialName("target_amount") val targetAmount: Double, // numeric
    @SerialName("current_saved") val currentSaved: Double, // numeric
    @SerialName("target_date") val targetDate: String? = null, // date
    @SerialName("status") val status: String? = null, // text
    @SerialName("created_at") val createdAt: String? = null, // timestamp with time zone
    @SerialName("updated_at") val updatedAt: String? = null // timestamp with time zone
) {
    // Propiedades computadas para compatibilidad con código anterior
    val title: String
        get() = name
        
    val currentAmount: Double
        get() = currentSaved
        
    val deadline: String?
        get() = targetDate
        
    val category: String?
        get() = status
        
    val isCompleted: Boolean
        get() = status?.lowercase() == "completed" || currentSaved >= targetAmount
        
    val progressPercentage: Int
        get() = if (targetAmount > 0) {
            ((currentSaved / targetAmount) * 100).toInt().coerceIn(0, 100)
        } else 0
        
    val remainingAmount: Double
        get() = (targetAmount - currentSaved).coerceAtLeast(0.0)
}