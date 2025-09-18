package com.juangilles123.monifly.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Serializable
data class Debt(
    @SerialName("id") val id: String,
    @SerialName("description") val description: String,
    @SerialName("amount") val amount: Double,
    @SerialName("person_name") val personName: String,
    @SerialName("debt_type") val debtType: DebtType,
    @SerialName("due_date") val dueDate: Instant? = null,

    // Cambiado a Boolean? para aceptar null desde Supabase.
    // El valor por defecto se usa al crear el objeto en Kotlin si no se especifica.
    @SerialName("is_paid") var isPaid: Boolean? = false, 

    @SerialName("creation_date") val creationDate: Instant = Clock.System.now(),
    @SerialName("user_id") val userId: String
)
