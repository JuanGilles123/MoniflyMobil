package com.juangilles123.monifly.data.repository

import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.data.model.Transaction
import com.juangilles123.monifly.data.model.TransactionInsert
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransactionRepositoryImpl : TransactionRepository {
    
    private val supabaseClient = SupabaseManager.client
    
    // Implementación de métodos legacy para compatibilidad
    override suspend fun addIncome(income: com.juangilles123.monifly.data.model.Income): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val transactionInsert = TransactionInsert(
                    userId = income.userId,
                    type = TransactionInsert.TYPE_INCOME,
                    amount = income.amount,
                    description = income.description,
                    category = income.category,
                    account = income.account
                )
                
                supabaseClient
                    .from("transactions")
                    .insert(transactionInsert)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun addExpense(expense: com.juangilles123.monifly.data.model.Expense): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val transactionInsert = TransactionInsert(
                    userId = expense.userId,
                    type = TransactionInsert.TYPE_EXPENSE,
                    amount = expense.amount,
                    description = expense.description,
                    category = expense.category,
                    account = expense.account
                )
                
                supabaseClient
                    .from("transactions")
                    .insert(transactionInsert)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getIncomes(): Result<List<com.juangilles123.monifly.data.model.Income>> {
        return withContext(Dispatchers.IO) {
            try {
                // Para compatibilidad, convertir Transactions a Income
                val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                if (currentUserId == null) {
                    return@withContext Result.failure(Exception("Usuario no autenticado"))
                }
                
                val transactions = supabaseClient
                    .from("transactions")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("user_id", currentUserId)
                            eq("type", Transaction.TYPE_INCOME)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Transaction>()
                
                val incomes = transactions.map { transaction ->
                    com.juangilles123.monifly.data.model.Income(
                        id = transaction.id.toString(),
                        userId = transaction.userId,
                        amount = transaction.amount,
                        description = transaction.description,
                        date = transaction.createdAt.substringBefore("T"), // Extraer solo la fecha
                        category = transaction.category,
                        account = transaction.account,
                        createdAt = null
                    )
                }
                
                Result.success(incomes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getExpenses(): Result<List<com.juangilles123.monifly.data.model.Expense>> {
        return withContext(Dispatchers.IO) {
            try {
                // Para compatibilidad, convertir Transactions a Expense
                val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
                if (currentUserId == null) {
                    return@withContext Result.failure(Exception("Usuario no autenticado"))
                }
                
                val transactions = supabaseClient
                    .from("transactions")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("user_id", currentUserId)
                            eq("type", Transaction.TYPE_EXPENSE)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Transaction>()
                
                val expenses = transactions.map { transaction ->
                    com.juangilles123.monifly.data.model.Expense(
                        id = transaction.id.toString(),
                        userId = transaction.userId,
                        amount = transaction.amount,
                        description = transaction.description,
                        category = transaction.category,
                        date = transaction.createdAt.substringBefore("T"), // Extraer solo la fecha
                        account = transaction.account,
                        createdAt = null
                    )
                }
                
                Result.success(expenses)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteIncome(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabaseClient
                    .from("transactions")
                    .delete {
                        filter {
                            eq("id", id.toLong())
                            eq("type", Transaction.TYPE_INCOME)
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteExpense(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabaseClient
                    .from("transactions")
                    .delete {
                        filter {
                            eq("id", id.toLong())
                            eq("type", Transaction.TYPE_EXPENSE)
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Nuevos métodos para trabajar directamente con Transaction
    override suspend fun getTransactionsByUserId(userId: String): Result<List<Transaction>> {
        return withContext(Dispatchers.IO) {
            try {
                val transactions = supabaseClient
                    .from("transactions")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("user_id", userId)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Transaction>()
                
                Result.success(transactions)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabaseClient
                    .from("transactions")
                    .delete {
                        filter {
                            eq("id", transactionId.toLong())
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getTransactionById(transactionId: String): Result<Transaction> {
        return withContext(Dispatchers.IO) {
            try {
                val transaction = supabaseClient
                    .from("transactions")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("id", transactionId.toLong())
                        }
                    }
                    .decodeSingle<Transaction>()
                
                Result.success(transaction)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun updateIncome(income: com.juangilles123.monifly.data.model.Income): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val transactionUpdate = TransactionInsert(
                    userId = income.userId,
                    type = TransactionInsert.TYPE_INCOME,
                    amount = income.amount,
                    description = income.description,
                    category = income.category,
                    account = income.account
                )
                
                supabaseClient
                    .from("transactions")
                    .update(transactionUpdate) {
                        filter {
                            eq("id", income.id.toLong())
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun updateExpense(expense: com.juangilles123.monifly.data.model.Expense): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val transactionUpdate = TransactionInsert(
                    userId = expense.userId,
                    type = TransactionInsert.TYPE_EXPENSE,
                    amount = expense.amount,
                    description = expense.description,
                    category = expense.category,
                    account = expense.account
                )
                
                supabaseClient
                    .from("transactions")
                    .update(transactionUpdate) {
                        filter {
                            eq("id", expense.id.toLong())
                        }
                    }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}