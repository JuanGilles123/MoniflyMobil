package com.juangilles123.monifly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.juangilles123.monifly.data.repository.DebtRepository // Importar el objeto DebtRepository

// DebtRepository es ahora un objeto (Singleton), por lo que no necesita ser pasado en el constructor.
class DebtsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebtsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Pasamos la instancia Singleton de DebtRepository al ViewModel
            return DebtsViewModel(DebtRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}