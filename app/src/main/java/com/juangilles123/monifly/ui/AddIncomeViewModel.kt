package com.juangilles123.monifly.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.data.model.Income
import com.juangilles123.monifly.data.model.Transaction
import com.juangilles123.monifly.data.repository.TransactionRepository
import com.juangilles123.monifly.util.TransactionEventBus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class AddIncomeViewModel(private val transactionRepository: TransactionRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveOperationResult = MutableLiveData<Result<Unit>>()
    val saveOperationResult: LiveData<Result<Unit>> = _saveOperationResult

    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError

    private val _loadedTransaction = MutableLiveData<Transaction?>()
    val loadedTransaction: LiveData<Transaction?> = _loadedTransaction

    private val supabaseClient = SupabaseManager.client

    fun loadTransactionForEdit(transactionId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val result = transactionRepository.getTransactionById(transactionId)
                if (result.isSuccess) {
                    _loadedTransaction.value = result.getOrNull()
                } else {
                    _saveOperationResult.value = Result.failure(result.exceptionOrNull() ?: Exception("Error desconocido"))
                }
                
            } catch (e: Exception) {
                _saveOperationResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateIncome(transactionId: String, amountStr: String, description: String?, date: String, category: String = "Otros", account: String = "Efectivo") {
        if (amountStr.isBlank()) {
            _validationError.value = "El monto no puede estar vacío"
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _validationError.value = "Por favor, ingresa un monto válido."
            return
        }
        if (date.isBlank()) {
            _validationError.value = "La fecha no puede estar vacía."
            return
        }
        _validationError.value = null

        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateObject: java.util.Date? = try {
            inputFormat.parse(date)
        } catch (e: Exception) {
            Log.e("AddIncomeVM", "Error parseando la fecha de entrada: $date", e)
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

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val currentSupabaseUser = supabaseClient.auth.currentUserOrNull()
                if (currentSupabaseUser == null) {
                    Log.e("AddIncomeVM", "Usuario no autenticado. No se puede actualizar el ingreso.")
                    _saveOperationResult.value = Result.failure(Exception("Usuario no autenticado."))
                    _isLoading.value = false
                    return@launch
                }
                val actualUserId = currentSupabaseUser.id

                val updatedIncome = Income(
                    id = transactionId,
                    userId = actualUserId,
                    amount = amount,
                    description = description?.takeIf { it.isNotBlank() },
                    date = formattedDateForSupabase,
                    category = category,
                    account = account
                )

                Log.d("AddIncomeVM", "Actualizando ingreso con ID: $transactionId")
                val result = transactionRepository.updateIncome(updatedIncome)
                _saveOperationResult.value = result

                if (result.isSuccess) {
                    Log.d("AddIncomeVM", "Ingreso actualizado exitosamente, publicando evento en TransactionEventBus.")
                    TransactionEventBus.postRefreshRequest()
                }

            } catch (e: Exception) {
                Log.e("AddIncomeVM", "Excepción al actualizar ingreso: ${e.message}", e)
                _saveOperationResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveIncome(amountStr: String, description: String?, date: String, category: String = "Otros", account: String = "Efectivo") {
        if (amountStr.isBlank()) {
            _validationError.value = "El monto no puede estar vacío"
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _validationError.value = "Por favor, ingresa un monto válido."
            return
        }
        if (date.isBlank()) {
            _validationError.value = "La fecha no puede estar vacía."
            return
        }
        _validationError.value = null

        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateObject: java.util.Date? = try {
            inputFormat.parse(date)
        } catch (e: Exception) {
            Log.e("AddIncomeVM", "Error parseando la fecha de entrada: $date", e)
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

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val currentSupabaseUser = supabaseClient.auth.currentUserOrNull()
                if (currentSupabaseUser == null) {
                    Log.e("AddIncomeVM", "Usuario no autenticado. No se puede guardar el ingreso.")
                    _saveOperationResult.value = Result.failure(Exception("Usuario no autenticado."))
                    _isLoading.value = false
                    return@launch
                }
                val actualUserId = currentSupabaseUser.id
                val incomeId = UUID.randomUUID().toString()

                val newIncome = Income(
                    id = incomeId,
                    userId = actualUserId,
                    amount = amount,
                    description = description?.takeIf { it.isNotBlank() },
                    date = formattedDateForSupabase,
                    category = category,
                    account = account
                )

                Log.d("AddIncomeVM", "Guardando ingreso con userId: $actualUserId, categoría: $category, cuenta: $account, detalles: $newIncome")
                val result = transactionRepository.addIncome(newIncome)
                _saveOperationResult.value = result

                if (result.isSuccess) {
                    Log.d("AddIncomeVM", "Ingreso guardado exitosamente, publicando evento en TransactionEventBus.")
                    TransactionEventBus.postRefreshRequest()
                }

            } catch (e: Exception) {
                Log.e("AddIncomeVM", "Excepción al guardar ingreso: ${e.message}", e)
                _saveOperationResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}