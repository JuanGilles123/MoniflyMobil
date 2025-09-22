package com.juangilles123.monifly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.juangilles123.monifly.data.repository.DebtRepositoryImpl

class DebtsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebtsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Crear una instancia del repositorio real
            return DebtsViewModel(DebtRepositoryImpl()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}