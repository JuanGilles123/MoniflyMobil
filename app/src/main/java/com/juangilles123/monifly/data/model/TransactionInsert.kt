package com.juangilles123.monifly.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionInsert(
    @SerialName("user_id") val userId: String, // uuid
    @SerialName("type") val type: String, // "income" o "expense"
    @SerialName("amount") val amount: Double, // numeric
    @SerialName("description") val description: String? = null, // text
    @SerialName("category") val category: String? = null, // text
    @SerialName("account") val account: String? = null // text
    // Nota: NO incluimos id, created_at ni date para que se auto-generen en la BD
    // La tabla usa created_at como timestamp automático, no un campo date separado
) {
    companion object {
        const val TYPE_INCOME = "income"
        const val TYPE_EXPENSE = "expense"
    }
}