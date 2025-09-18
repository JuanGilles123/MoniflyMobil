package com.juangilles123.monifly.data.repository

import com.juangilles123.monifly.data.model.Expense
import com.juangilles123.monifly.data.model.Income

interface TransactionRepository {

    suspend fun addIncome(income: Income): Result<Unit>

    suspend fun addExpense(expense: Expense): Result<Unit>

    // Funciones para obtener transacciones del usuario actual
    suspend fun getIncomes(): Result<List<Income>>
    suspend fun getExpenses(): Result<List<Expense>>

    // Funciones para eliminar transacciones
    suspend fun deleteIncome(id: String): Result<Unit>
    suspend fun deleteExpense(id: String): Result<Unit>
}