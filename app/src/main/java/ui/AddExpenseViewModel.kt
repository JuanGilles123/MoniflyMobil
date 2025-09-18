package com.juangilles123.monifly.ui

import android.util.Log // Importación añadida
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juangilles123.monifly.data.SupabaseManager // Importación añadida
import com.juangilles123.monifly.data.model.Expense
import com.juangilles123.monifly.data.repository.TransactionRepository
import io.github.jan.supabase.gotrue.auth // Importación añadida
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat // Importación añadida
import java.util.Date // Importación añadida
import java.util.Locale // Importación añadida
import java.util.UUID

class AddExpenseViewModel(private val transactionRepository: TransactionRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveOperationResult = MutableLiveData<Result<Unit>>()
    val saveOperationResult: LiveData<Result<Unit>> = _saveOperationResult

    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError

    // Acceder al cliente de Supabase
    private val supabaseClient = SupabaseManager.client // Línea añadida

    fun saveExpense(amountStr: String, description: String?, category: String, date: String) {
        if (amountStr.isBlank()) {
            _validationError.value = "El monto no puede estar vacío."
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _validationError.value = "Por favor, ingresa un monto válido."
            return
        }
        if (category.isBlank()) {
            _validationError.value = "La categoría no puede estar vacía."
            return
        }
        if (date.isBlank()) {
            _validationError.value = "La fecha no puede estar vacía."
            return
        }
        _validationError.value = null // Limpiar error de validación

        // --- INICIO DE LA MODIFICACIÓN DE FECHA ---
        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateObject: java.util.Date? = try {
            inputFormat.parse(date)
        } catch (e: Exception) {
            Log.e("AddExpenseVM", "Error parseando la fecha de entrada: $date", e)
            _validationError.value = "Formato de fecha inválido. Use dd/MM/yyyy."
            _saveOperationResult.value = Result.failure(IllegalArgumentException("Formato de fecha inválido: $date", e))
            return
        }

        if (dateObject == null) {
            _validationError.value = "No se pudo interpretar la fecha."
            _saveOperationResult.value = Result.failure(IllegalArgumentException("No se pudo interpretar la fecha: $date"))
            return
        }
        val formattedDateForSupabase = outputFormat.format(dateObject)
        // --- FIN DE LA MODIFICACIÓN DE FECHA ---

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // --- OBTENER EL USER ID REAL ---
                val currentSupabaseUser = supabaseClient.auth.currentUserOrNull()
                if (currentSupabaseUser == null) {
                    Log.e("AddExpenseVM", "Usuario no autenticado. No se puede guardar el gasto.")
                    _saveOperationResult.value = Result.failure(Exception("Usuario no autenticado."))
                    _isLoading.value = false // Detener el indicador de carga
                    return@launch
                }
                val actualUserId = currentSupabaseUser.id
                // --- FIN OBTENER USER ID REAL ---

                val expenseId = UUID.randomUUID().toString()

                val newExpense = Expense(
                    id = expenseId,
                    userId = actualUserId, // <--- USER ID REAL
                    amount = amount,
                    description = description?.takeIf { it.isNotBlank() },
                    category = category,
                    date = formattedDateForSupabase // <--- FECHA FORMATEADA
                    // createdAt = null (por defecto en la data class Expense)
                )

                Log.d("AddExpenseVM", "Guardando gasto con userId: $actualUserId, detalles: $newExpense")
                val result = transactionRepository.addExpense(newExpense)
                _saveOperationResult.value = result
            } catch (e: Exception) {
                Log.e("AddExpenseVM", "Excepción al guardar gasto: ${e.message}", e)
                _saveOperationResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}