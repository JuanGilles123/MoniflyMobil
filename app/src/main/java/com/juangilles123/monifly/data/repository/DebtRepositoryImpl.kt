package com.juangilles123.monifly.data.repository

import android.util.Log
import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.data.model.Debt
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class DebtStatusUpdate(
    @SerialName("status") val status: String,
    @SerialName("remaining_amount") val remainingAmount: Double,
    @SerialName("updated_at") val updatedAt: String
)

class DebtRepositoryImpl : DebtRepositoryInterface {

    private val supabaseClient = SupabaseManager.client
    private val tableName = "debts" // Nombre correcto de la tabla en la app web
    
    // Formateo de fechas compatible con API 24+
    private fun getCurrentTimestamp(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }

    override suspend fun getDebtsByUserId(userId: String): Result<List<Debt>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DebtRepositoryImpl", "=== INICIANDO CONSULTA DE DEUDAS ===")
                Log.d("DebtRepositoryImpl", "Usuario ID: $userId")
                Log.d("DebtRepositoryImpl", "Tabla: $tableName")
                
                // Primero intentar una consulta simple para verificar conectividad
                try {
                    Log.d("DebtRepositoryImpl", "Probando conectividad a tabla '$tableName'...")
                    supabaseClient.postgrest.from(tableName)
                        .select {
                            limit(1)
                        }
                        .decodeList<Debt>()
                    Log.d("DebtRepositoryImpl", "Conectividad a tabla '$tableName' exitosa")
                } catch (testE: Exception) {
                    Log.e("DebtRepositoryImpl", "Error de conectividad a tabla '$tableName': ${testE.message}")
                    throw testE
                }
                
                Log.d("DebtRepositoryImpl", "Ejecutando consulta filtrada por user_id...")
                val debts = supabaseClient.postgrest.from(tableName)
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<Debt>()
                
                Log.d("DebtRepositoryImpl", "=== RESULTADO CONSULTA ===")
                Log.d("DebtRepositoryImpl", "Deudas encontradas: ${debts.size}")
                if (debts.isNotEmpty()) {
                    debts.forEachIndexed { index, debt ->
                        Log.d("DebtRepositoryImpl", "Deuda $index: id=${debt.id}, title=${debt.title}, type=${debt.type}, amount=${debt.remainingAmount}")
                    }
                } else {
                    Log.w("DebtRepositoryImpl", "No se encontraron deudas para el usuario $userId")
                    
                    // Verificar si hay datos en la tabla en general
                    try {
                        val allDebts = supabaseClient.postgrest.from(tableName)
                            .select {
                                limit(5)
                            }
                            .decodeList<Debt>()
                        Log.d("DebtRepositoryImpl", "Total de deudas en la tabla (muestra): ${allDebts.size}")
                        if (allDebts.isNotEmpty()) {
                            val userIds = allDebts.map { debt -> debt.userId }.distinct()
                            Log.d("DebtRepositoryImpl", "Ejemplo de user_ids existentes: $userIds")
                        }
                    } catch (verifyE: Exception) {
                        Log.e("DebtRepositoryImpl", "Error al verificar datos generales: ${verifyE.message}")
                    }
                }
                
                Result.success(debts)
            } catch (e: RestException) {
                Log.e("DebtRepositoryImpl", "=== ERROR REST ===")
                Log.e("DebtRepositoryImpl", "Código de estado: ${e.statusCode}")
                Log.e("DebtRepositoryImpl", "Error: ${e.error}")
                Log.e("DebtRepositoryImpl", "Mensaje: ${e.message}")
                Result.failure(Exception("Error al obtener deudas: ${e.error}"))
            } catch (e: Exception) {
                Log.e("DebtRepositoryImpl", "=== ERROR GENERAL ===")
                Log.e("DebtRepositoryImpl", "Tipo: ${e::class.simpleName}")
                Log.e("DebtRepositoryImpl", "Mensaje: ${e.message}")
                Log.e("DebtRepositoryImpl", "Stack trace: ", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun insertDebt(debt: Debt): Result<Debt> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DebtRepositoryImpl", "=== INICIANDO INSERCIÓN EN REPOSITORIO ===")
                Log.d("DebtRepositoryImpl", "Tabla destino: $tableName")
                Log.d("DebtRepositoryImpl", "ID de deuda: ${debt.id}")
                Log.d("DebtRepositoryImpl", "Usuario: ${debt.userId}")
                Log.d("DebtRepositoryImpl", "Título: ${debt.title}")
                Log.d("DebtRepositoryImpl", "Tipo: ${debt.type}")
                Log.d("DebtRepositoryImpl", "Monto original: ${debt.originalAmount}")
                Log.d("DebtRepositoryImpl", "Monto restante: ${debt.remainingAmount}")
                Log.d("DebtRepositoryImpl", "Persona: ${debt.creditorDebtorName}")
                Log.d("DebtRepositoryImpl", "Payment type: ${debt.paymentType}")
                Log.d("DebtRepositoryImpl", "Status: ${debt.status}")
                
                // Asegurar que los timestamps estén actualizados
                val currentTime = getCurrentTimestamp()
                val debtToInsert = debt.copy(
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
                
                Log.d("DebtRepositoryImpl", "Timestamp actualizado: $currentTime")
                Log.d("DebtRepositoryImpl", "Enviando a Supabase...")
                
                // Crear un mapa con solo los campos esenciales para la primera prueba
                val essentialData = mapOf(
                    "id" to debtToInsert.id,
                    "user_id" to debtToInsert.userId,
                    "title" to debtToInsert.title,
                    "type" to debtToInsert.type,
                    "original_amount" to debtToInsert.originalAmount,
                    "remaining_amount" to debtToInsert.remainingAmount,
                    "creditor_debtor_name" to debtToInsert.creditorDebtorName,
                    "status" to debtToInsert.status,
                    "created_at" to debtToInsert.createdAt,
                    "updated_at" to debtToInsert.updatedAt
                )
                
                Log.d("DebtRepositoryImpl", "Datos esenciales a insertar: $essentialData")
                
                val insertedDebt = supabaseClient.postgrest.from(tableName)
                    .insert(debtToInsert) {
                        select()
                    }
                    .decodeSingle<Debt>()
                
                Log.d("DebtRepositoryImpl", "=== INSERCIÓN EXITOSA ===")
                Log.d("DebtRepositoryImpl", "Deuda insertada con ID: ${insertedDebt.id}")
                Log.d("DebtRepositoryImpl", "Título insertado: ${insertedDebt.title}")
                Log.d("DebtRepositoryImpl", "Tipo insertado: ${insertedDebt.type}")
                Result.success(insertedDebt)
            } catch (e: RestException) {
                Log.e("DebtRepositoryImpl", "=== ERROR REST EN INSERCIÓN ===")
                Log.e("DebtRepositoryImpl", "Código de estado: ${e.statusCode}")
                Log.e("DebtRepositoryImpl", "Error: ${e.error}")
                Log.e("DebtRepositoryImpl", "Mensaje: ${e.message}")
                Log.e("DebtRepositoryImpl", "Detalles de la deuda que falló:")
                Log.e("DebtRepositoryImpl", "  - ID: ${debt.id}")
                Log.e("DebtRepositoryImpl", "  - Usuario: ${debt.userId}")
                Log.e("DebtRepositoryImpl", "  - Título: ${debt.title}")
                Log.e("DebtRepositoryImpl", "  - Tipo: ${debt.type}")
                Result.failure(Exception("Error al crear deuda: ${e.error}"))
            } catch (e: Exception) {
                Log.e("DebtRepositoryImpl", "=== ERROR GENERAL EN INSERCIÓN ===")
                Log.e("DebtRepositoryImpl", "Tipo de excepción: ${e::class.simpleName}")
                Log.e("DebtRepositoryImpl", "Mensaje: ${e.message}")
                Log.e("DebtRepositoryImpl", "Error al insertar deuda", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateDebt(debt: Debt): Result<Debt> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DebtRepositoryImpl", "Actualizando deuda: ${debt.id}")
                
                // Asegurar que el timestamp de actualización esté actualizado
                val debtToUpdate = debt.copy(
                    updatedAt = getCurrentTimestamp()
                )
                
                val updatedDebt = supabaseClient.postgrest.from(tableName)
                    .update(debtToUpdate) {
                        filter {
                            eq("id", debt.id)
                            eq("user_id", debt.userId)
                        }
                        select()
                    }
                    .decodeSingle<Debt>()
                
                Log.d("DebtRepositoryImpl", "Deuda actualizada exitosamente: ${updatedDebt.id}")
                Result.success(updatedDebt)
            } catch (e: RestException) {
                Log.e("DebtRepositoryImpl", "Error REST al actualizar deuda: ${e.statusCode} - ${e.error}")
                Result.failure(Exception("Error al actualizar deuda: ${e.error}"))
            } catch (e: Exception) {
                Log.e("DebtRepositoryImpl", "Error al actualizar deuda", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteDebt(debtId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DebtRepositoryImpl", "Eliminando deuda: $debtId")
                
                supabaseClient.postgrest.from(tableName)
                    .delete {
                        filter {
                            eq("id", debtId)
                        }
                    }
                
                Log.d("DebtRepositoryImpl", "Deuda eliminada exitosamente: $debtId")
                Result.success(Unit)
            } catch (e: RestException) {
                Log.e("DebtRepositoryImpl", "Error REST al eliminar deuda: ${e.statusCode} - ${e.error}")
                Result.failure(Exception("Error al eliminar deuda: ${e.error}"))
            } catch (e: Exception) {
                Log.e("DebtRepositoryImpl", "Error al eliminar deuda", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun markDebtAsPaid(debtId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DebtRepositoryImpl", "Marcando deuda como pagada: $debtId")
                
                val updateData = DebtStatusUpdate(
                    status = "paid",
                    remainingAmount = 0.0,
                    updatedAt = getCurrentTimestamp()
                )
                
                supabaseClient.postgrest.from(tableName)
                    .update(updateData) {
                        filter {
                            eq("id", debtId)
                        }
                    }
                
                Log.d("DebtRepositoryImpl", "Deuda marcada como pagada: $debtId")
                Result.success(Unit)
            } catch (e: RestException) {
                Log.e("DebtRepositoryImpl", "Error REST al marcar deuda como pagada: ${e.statusCode} - ${e.error}")
                Result.failure(Exception("Error al marcar deuda como pagada: ${e.error}"))
            } catch (e: Exception) {
                Log.e("DebtRepositoryImpl", "Error al marcar deuda como pagada", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateDebtProgress(debtId: String, paidAmount: Double): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DebtRepositoryImpl", "Actualizando progreso de deuda: $debtId, monto pagado: $paidAmount")
                
                // Primero obtener la deuda actual para calcular el monto restante
                val currentDebt = supabaseClient.postgrest.from(tableName)
                    .select {
                        filter {
                            eq("id", debtId)
                        }
                    }
                    .decodeSingle<Debt>()
                
                val newRemainingAmount = maxOf(0.0, currentDebt.remainingAmount - paidAmount)
                val newStatus = if (newRemainingAmount <= 0.0) "paid" else "active"
                
                supabaseClient.postgrest.from(tableName)
                    .update(
                        mapOf(
                            "remaining_amount" to newRemainingAmount,
                            "status" to newStatus,
                            "updated_at" to getCurrentTimestamp()
                        )
                    ) {
                        filter {
                            eq("id", debtId)
                        }
                    }
                
                Log.d("DebtRepositoryImpl", "Progreso de deuda actualizado: $debtId")
                Result.success(Unit)
            } catch (e: RestException) {
                Log.e("DebtRepositoryImpl", "Error REST al actualizar progreso: ${e.statusCode} - ${e.error}")
                Result.failure(Exception("Error al actualizar progreso: ${e.error}"))
            } catch (e: Exception) {
                Log.e("DebtRepositoryImpl", "Error al actualizar progreso de deuda", e)
                Result.failure(e)
            }
        }
    }
}