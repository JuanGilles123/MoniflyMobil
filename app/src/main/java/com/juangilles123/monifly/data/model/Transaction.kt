package com.juangilles123.monifly.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    @SerialName("id") val id: Long, // bigint en la BD web
    @SerialName("created_at") val createdAt: String, // timestamp with time zone
    @SerialName("user_id") val userId: String, // uuid
    @SerialName("type") val type: String, // "income" o "expense"
    @SerialName("amount") val amount: Double, // numeric
    @SerialName("description") val description: String? = null, // text
    @SerialName("category") val category: String? = null, // text
    @SerialName("account") val account: String? = null // text
) {
    companion object {
        const val TYPE_INCOME = "income"
        const val TYPE_EXPENSE = "expense"
    }
}