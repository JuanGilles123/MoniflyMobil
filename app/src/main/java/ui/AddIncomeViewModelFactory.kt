package com.juangilles123.monifly.ui // O el paquete que prefieras para factories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.juangilles123.monifly.data.repository.TransactionRepository

@Suppress("UNCHECKED_CAST")
class AddIncomeViewModelFactory(
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddIncomeViewModel::class.java)) {
            return AddIncomeViewModel(transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}