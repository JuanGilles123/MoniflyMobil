package com.juangilles123.monifly.data.model

import kotlinx.serialization.SerialName // Importación añadida
import kotlinx.serialization.Serializable

@Serializable
data class Income(
    val id: String, // Podría ser un UUID generado. El ViewModel lo está proporcionando actualmente.
    @SerialName("user_id") // Anotación añadida ASUMIENDO que tu columna en Supabase es "user_id"
    val userId: String, // ID del usuario de Supabase Auth
    val amount: Double,
    val description: String? = null, // Opcional
    val date: String, // Formato "yyyy-MM-dd" como lo establecimos
    val category: String? = null, // Categoría del ingreso - nullable para compatibilidad
    val account: String? = null, // Cuenta/método de pago - nullable para compatibilidad
    val createdAt: Long? = null // Modificado: Hecho nulable, valor por defecto null. La BD debería usar DEFAULT now().
)
