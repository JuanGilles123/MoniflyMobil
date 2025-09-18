package com.juangilles123.monifly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.juangilles123.monifly.data.repository.TransactionRepository

@Suppress("UNCHECKED_CAST")
class AddExpenseViewModelFactory(
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddExpenseViewModel::class.java)) {
            return AddExpenseViewModel(transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}