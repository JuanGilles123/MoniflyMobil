package com.juangilles123.monifly.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class DebtType {
    I_OWE, // Yo debo
    THEY_OWE_ME // Me deben
}