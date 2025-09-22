package com.juangilles123.monifly.data.model

import kotlinx.serialization.SerialName // Importación añadida
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String, // El ViewModel debería proporcionar esto
    @SerialName("user_id") // Anotación añadida ASUMIENDO que tu columna en Supabase es "user_id"
    val userId: String, // ID del usuario de Supabase Auth
    val amount: Double,
    val description: String? = null, // Opcional
    val category: String? = null, // Categoría - nullable para compatibilidad
    val date: String, // El ViewModel debería asegurar el formato "yyyy-MM-dd"
    val account: String? = null, // Cuenta/método de pago - nullable para compatibilidad
    val createdAt: Long? = null // Modificado: Hecho nulable, valor por defecto null. La BD debería usar DEFAULT now().
)
