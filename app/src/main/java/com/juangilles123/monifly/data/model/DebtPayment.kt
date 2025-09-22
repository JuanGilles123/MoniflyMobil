package com.juangilles123.monifly.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DebtPayment(
    @SerialName("id") val id: String, // uuid
    @SerialName("debt_id") val debtId: String, // uuid
    @SerialName("user_id") val userId: String, // uuid
    @SerialName("transaction_id") val transactionId: Long? = null, // bigint
    @SerialName("amount") val amount: Double, // numeric
    @SerialName("payment_date") val paymentDate: String, // date
    @SerialName("payment_method") val paymentMethod: String? = null, // character varying
    @SerialName("notes") val notes: String? = null, // text
    @SerialName("reference_number") val referenceNumber: String? = null, // character varying
    @SerialName("created_at") val createdAt: String? = null, // timestamp with time zone
    @SerialName("updated_at") val updatedAt: String? = null // timestamp with time zone
)