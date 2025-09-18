package com.juangilles123.monifly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.juangilles123.monifly.data.repository.SupabaseTransactionRepository
import com.juangilles123.monifly.data.repository.TransactionRepository

class DashboardViewModelFactory : ViewModelProvider.Factory {

    // Podrías pasar el repositorio como argumento si ya lo tienes instanciado en otro lugar,
    // pero para este caso, lo crearemos aquí directamente.
    // En una app más grande, usarías Inyección de Dependencias (Hilt, Koin).
    private val transactionRepository: TransactionRepository by lazy {
        SupabaseTransactionRepository()
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}