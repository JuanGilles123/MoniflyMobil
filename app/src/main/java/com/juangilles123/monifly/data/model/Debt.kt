package com.juangilles123.monifly.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Debt(
    @SerialName("id") val id: String, // uuid
    @SerialName("user_id") val userId: String, // uuid
    @SerialName("title") val title: String, // character varying
    @SerialName("description") val description: String? = null, // text
    @SerialName("type") val type: String, // text (debt_type en el esquema anterior)
    @SerialName("original_amount") val originalAmount: Double, // numeric
    @SerialName("remaining_amount") val remainingAmount: Double, // numeric
    @SerialName("payment_type") val paymentType: String? = null, // text
    @SerialName("total_installments") val totalInstallments: Int? = null, // integer
    @SerialName("paid_installments") val paidInstallments: Int? = null, // integer
    @SerialName("installment_amount") val installmentAmount: Double? = null, // numeric
    @SerialName("payment_frequency") val paymentFrequency: String? = null, // text
    @SerialName("created_date") val createdDate: String? = null, // date
    @SerialName("due_date") val dueDate: String? = null, // date
    @SerialName("next_payment_date") val nextPaymentDate: String? = null, // date
    @SerialName("interest_rate") val interestRate: Double? = null, // numeric
    @SerialName("has_interest") val hasInterest: Boolean? = null, // boolean
    @SerialName("status") val status: String? = null, // text
    @SerialName("creditor_debtor_name") val creditorDebtorName: String? = null, // character varying
    @SerialName("notes") val notes: String? = null, // text
    @SerialName("created_at") val createdAt: String? = null, // timestamp with time zone
    @SerialName("updated_at") val updatedAt: String? = null // timestamp with time zone
) {
    // Propiedades computadas para compatibilidad con lógica anterior
    val personName: String
        get() = creditorDebtorName ?: "Sin nombre"
    
    val amount: Double
        get() = remainingAmount
        
    val debtType: DebtType
        get() = when(type.lowercase()) {
            "owe", "debt_owing" -> DebtType.I_OWE
            "lent", "debt_owed" -> DebtType.THEY_OWE_ME
            else -> DebtType.I_OWE
        }
        
    val isPaid: Boolean
        get() = status?.lowercase() == "paid" || remainingAmount <= 0.0
}
