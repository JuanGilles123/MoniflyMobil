package com.juangilles123.monifly.data.repository

import android.util.Log
import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.data.model.Debt
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.exceptions.RestException 
// Quitado import de Ktor bodyAsText ya que e.response no se usa directamente
// import io.ktor.client.statement.bodyAsText 

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object DebtRepository {

    private val supabaseClient = SupabaseManager.client
    private const val TABLE_NAME = "debtss"

    private val _debts = MutableStateFlow<List<Debt>>(emptyList())
    val debts: Flow<List<Debt>> = _debts.asStateFlow()

    suspend fun fetchDebts() {
        withContext(Dispatchers.IO) {
            try {
                val currentUser = supabaseClient.auth.currentUserOrNull()
                if (currentUser == null) {
                    Log.w("DebtRepository", "FetchDebts: No hay usuario autenticado.")
                    _debts.value = emptyList()
                    return@withContext
                }
                Log.d("DebtRepository", "Fetching debts for user: ${currentUser.id} from table: $TABLE_NAME")
                val result = supabaseClient.postgrest.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("user_id", currentUser.id)
                        }
                    }
                    .decodeList<Debt>()
                _debts.value = result
                Log.d("DebtRepository", "Deudas obtenidas de Supabase: ${result.size}")
            } catch (e: Exception) {
                Log.e("DebtRepository", "Error al obtener deudas de Supabase", e)
                _debts.value = emptyList()
            }
        }
    }

    suspend fun addDebt(debt: Debt) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("DebtRepository", "AddDebt: Attempting to add debt to table: $TABLE_NAME. Data: $debt")
                
                supabaseClient.postgrest.from(TABLE_NAME)
                    .insert(listOf(debt)) 
                
                Log.i("DebtRepository", "AddDebt: Insert operation sent for debt ID: ${debt.id}. Fetching updated list...")
                fetchDebts() 
            } catch (e: RestException) {
                // Simplificado el logging de RestException
                Log.e("DebtRepository", "AddDebt: RestException - StatusCode: ${e.statusCode}, Error: ${e.error}, Message: ${e.message}. Full Exception: ${e.toString()}", e)
            } catch (e: Exception) {
                Log.e("DebtRepository", "AddDebt: Excepción genérica al añadir deuda", e)
            }
        }
    }

    suspend fun updateDebt(debt: Debt) {
        withContext(Dispatchers.IO) {
            try {
                val currentUser = supabaseClient.auth.currentUserOrNull()
                if (currentUser == null) {
                    Log.w("DebtRepository", "UpdateDebt: No hay usuario autenticado.")
                    return@withContext
                }
                Log.d("DebtRepository", "UpdateDebt: Attempting to update debt in table: $TABLE_NAME. Data: $debt")

                supabaseClient.postgrest.from(TABLE_NAME)
                    .update(
                        value = debt 
                    ) {
                        filter {
                            eq("id", debt.id)
                            eq("user_id", currentUser.id) 
                        }
                    }
                
                Log.i("DebtRepository", "UpdateDebt: Update operation sent for debt ID: ${debt.id}. Fetching updated list...")
                fetchDebts()
            } catch (e: RestException) {
                // Simplificado el logging de RestException
                Log.e("DebtRepository", "UpdateDebt: RestException - StatusCode: ${e.statusCode}, Error: ${e.error}, Message: ${e.message}. Full Exception: ${e.toString()}", e)
            } catch (e: Exception) {
                Log.e("DebtRepository", "UpdateDebt: Excepción genérica al actualizar deuda", e)
            }
        }
    }

    suspend fun deleteDebt(debt: Debt) {
        withContext(Dispatchers.IO) {
            try {
                val currentUser = supabaseClient.auth.currentUserOrNull()
                if (currentUser == null) {
                    Log.w("DebtRepository", "DeleteDebt: No hay usuario autenticado.")
                    return@withContext
                }
                Log.d("DebtRepository", "DeleteDebt: Attempting to delete debt from table: $TABLE_NAME. ID: ${debt.id}")
                supabaseClient.postgrest.from(TABLE_NAME)
                    .delete {
                        filter {
                            eq("id", debt.id)
                            eq("user_id", currentUser.id)
                        }
                    }
                Log.d("DebtRepository", "DeleteDebt: Operación de borrado enviada para la deuda: ${debt.id}.")
                fetchDebts()
            } catch (e: RestException) {
                // Simplificado el logging de RestException
                 Log.e("DebtRepository", "DeleteDebt: RestException - StatusCode: ${e.statusCode}, Error: ${e.error}, Message: ${e.message}. Full Exception: ${e.toString()}", e)
            } catch (e: Exception) {
                Log.e("DebtRepository", "DeleteDebt: Excepción genérica al eliminar deuda", e)
            }
        }
    }
}