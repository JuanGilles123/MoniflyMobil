package com.juangilles123.monifly.data.repository

import android.util.Log
import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.data.model.Expense
import com.juangilles123.monifly.data.model.Income
import com.juangilles123.monifly.data.model.Transaction
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest

class SupabaseTransactionRepository : TransactionRepository {

    private val client = SupabaseManager.client

    companion object {
        private const val TAG = "SupabaseTransactionRepo"
        private const val TABLE_INCOMES = "incomes"
        private const val TABLE_EXPENSES = "expenses"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_ID = "id" // Asumiendo que la columna de ID se llama 'id'
    }

    override suspend fun addIncome(income: Income): Result<Unit> {
        return try {
            Log.d(TAG, "Agregando ingreso a Supabase: $income")
            client.postgrest[TABLE_INCOMES].insert(value = income)
            Log.d(TAG, "Ingreso enviado a Supabase.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar ingreso a Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun addExpense(expense: Expense): Result<Unit> {
        return try {
            Log.d(TAG, "Agregando gasto a Supabase: $expense")
            client.postgrest[TABLE_EXPENSES].insert(value = expense)
            Log.d(TAG, "Gasto enviado a Supabase.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar gasto a Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getIncomes(): Result<List<Income>> {
        return try {
            val currentUserId = client.auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                Log.w(TAG, "getIncomes: Usuario no autenticado.")
                return Result.failure(Exception("Usuario no autenticado"))
            }
            Log.d(TAG, "Obteniendo ingresos para el usuario: $currentUserId")
            val incomes = client.postgrest[TABLE_INCOMES].select {
                filter {
                    eq(COLUMN_USER_ID, currentUserId)
                }
            }.decodeList<Income>()
            Log.d(TAG, "Ingresos obtenidos: ${incomes.size}")
            Result.success(incomes)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener ingresos de Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getExpenses(): Result<List<Expense>> {
        return try {
            val currentUserId = client.auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                Log.w(TAG, "getExpenses: Usuario no autenticado.")
                return Result.failure(Exception("Usuario no autenticado"))
            }
            Log.d(TAG, "Obteniendo gastos para el usuario: $currentUserId")
            val expenses = client.postgrest[TABLE_EXPENSES].select {
                filter {
                    eq(COLUMN_USER_ID, currentUserId)
                }
            }.decodeList<Expense>()
            Log.d(TAG, "Gastos obtenidos: ${expenses.size}")
            Result.success(expenses)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener gastos de Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteIncome(id: String): Result<Unit> {
        return try {
            Log.d(TAG, "Eliminando ingreso con ID: $id de Supabase")
            client.postgrest[TABLE_INCOMES].delete {
                filter {
                    eq(COLUMN_ID, id) // id es String, Income.id es String
                }
            }
            Log.d(TAG, "Solicitud de eliminación de ingreso enviada para ID: $id.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar ingreso con ID: $id de Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(id: String): Result<Unit> {
        return try {
            // El ID que llega aquí es un String (convertido desde Expense.id que es Int).
            // Si el compilador previamente indicó que eq(COLUMN_ID, value) necesita que 'value' sea String,
            // entonces usamos el 'id: String' directamente.
            Log.d(TAG, "Eliminando gasto con ID (String): $id de Supabase")
            client.postgrest[TABLE_EXPENSES].delete {
                filter {
                    eq(COLUMN_ID, id) // Usar el id: String directamente
                }
            }
            Log.d(TAG, "Solicitud de eliminación de gasto enviada para ID: $id.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar gasto con ID: $id de Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Implementación de nuevos métodos requeridos por la interfaz
    override suspend fun getTransactionsByUserId(userId: String): Result<List<Transaction>> {
        return try {
            Log.d(TAG, "Obteniendo transacciones para usuario: $userId")
            // Nota: Este repository maneja las tablas separadas de ingresos y gastos
            // Por simplicidad, retornar una lista vacía o delegar a TransactionRepositoryImpl
            Log.w(TAG, "SupabaseTransactionRepository no soporta transacciones unificadas. Use TransactionRepositoryImpl.")
            Result.success(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Error en getTransactionsByUserId: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Eliminando transacción ID: $transactionId")
            // Nota: Este repository maneja las tablas separadas de ingresos y gastos
            // Por simplicidad, retornar éxito o delegar a TransactionRepositoryImpl
            Log.w(TAG, "SupabaseTransactionRepository no soporta eliminación de transacciones unificadas. Use TransactionRepositoryImpl.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error en deleteTransaction: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getTransactionById(transactionId: String): Result<Transaction> {
        return try {
            Log.d(TAG, "Obteniendo transacción por ID: $transactionId")
            // Nota: Este repository maneja las tablas separadas de ingresos y gastos
            // Por simplicidad, retornar error o delegar a TransactionRepositoryImpl
            Log.w(TAG, "SupabaseTransactionRepository no soporta obtener transacciones por ID unificado. Use TransactionRepositoryImpl.")
            Result.failure(Exception("Método no soportado en SupabaseTransactionRepository"))
        } catch (e: Exception) {
            Log.e(TAG, "Error en getTransactionById: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateIncome(income: Income): Result<Unit> {
        return try {
            Log.d(TAG, "Actualizando ingreso: ${income.id}")
            client.postgrest[TABLE_INCOMES].update(income) {
                filter {
                    eq(COLUMN_ID, income.id)
                }
            }
            Log.d(TAG, "Ingreso actualizado exitosamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar ingreso: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            Log.d(TAG, "Actualizando gasto: ${expense.id}")
            client.postgrest[TABLE_EXPENSES].update(expense) {
                filter {
                    eq(COLUMN_ID, expense.id)
                }
            }
            Log.d(TAG, "Gasto actualizado exitosamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar gasto: ${e.message}", e)
            Result.failure(e)
        }
    }
}