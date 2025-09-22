package com.juangilles123.monifly.data.repository

import com.juangilles123.monifly.data.model.Debt

interface DebtRepositoryInterface {
    suspend fun getDebtsByUserId(userId: String): Result<List<Debt>>
    suspend fun insertDebt(debt: Debt): Result<Debt>
    suspend fun updateDebt(debt: Debt): Result<Debt>
    suspend fun deleteDebt(debtId: String): Result<Unit>
    suspend fun markDebtAsPaid(debtId: String): Result<Unit>
    suspend fun updateDebtProgress(debtId: String, paidAmount: Double): Result<Unit>
}