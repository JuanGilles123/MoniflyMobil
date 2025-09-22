package com.juangilles123.monifly.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.juangilles123.monifly.data.model.Debt
import com.juangilles123.monifly.data.model.DebtType
import com.juangilles123.monifly.data.model.TransactionInsert
import com.juangilles123.monifly.data.repository.DebtRepositoryInterface
import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.util.TransactionEventBus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.juangilles123.monifly.utils.BalanceCalculator

class DebtsViewModel(private val debtRepository: DebtRepositoryInterface) : ViewModel() {

    // Exponer el repository para uso en DebtsFragment
    val repository: DebtRepositoryInterface get() = debtRepository

    // LiveData existente para la lista completa de deudas
    private val _allDebts = MutableLiveData<List<Debt>>(emptyList())
    val allDebts: LiveData<List<Debt>> get() = _allDebts

    // Estado de carga para el SwipeRefresh
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estado de error usando StateFlow
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // LiveData para listas filtradas usando el campo 'type' del nuevo esquema
    val iOweDebts: LiveData<List<Debt>> = _allDebts.map {
        it.filter { debt -> 
            (debt.type == "debt_owing" || debt.type == "owe") && 
            !(debt.status?.lowercase() == "paid" || debt.isPaid)
        }
    }

    val theyOweMeDebts: LiveData<List<Debt>> = _allDebts.map {
        it.filter { debt -> 
            (debt.type == "debt_owed" || debt.type == "lent") && 
            !(debt.status?.lowercase() == "paid" || debt.isPaid)
        }
    }

    // Nueva LiveData para deudas pagadas
    val paidDebts: LiveData<List<Debt>> = _allDebts.map {
        it.filter { debt -> 
            debt.status?.lowercase() == "paid" || debt.isPaid
        }
    }

    // StateFlows para los totales usando remainingAmount del nuevo esquema
    private val _totalOwedByMe = MutableStateFlow(0.0)
    val totalOwedByMe: StateFlow<Double> = _totalOwedByMe.asStateFlow()

    private val _totalOwedToMe = MutableStateFlow(0.0)
    val totalOwedToMe: StateFlow<Double> = _totalOwedToMe.asStateFlow()

    private val _netBalance = MutableStateFlow(0.0)
    val netBalance: StateFlow<Double> = _netBalance.asStateFlow()

    // NUEVO: Balance de TODAS las deudas (para pantalla de deudas)
    private val _allDebtsBalance = MutableStateFlow(0.0)
    val allDebtsBalance: StateFlow<Double> = _allDebtsBalance.asStateFlow()

    init {
        loadDebts()
    }

    private fun loadDebts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
                if (currentUserId == null) {
                    _error.value = "Usuario no autenticado"
                    _allDebts.value = emptyList()
                    return@launch
                }

                Log.d("DebtsViewModel", "Cargando deudas para usuario: $currentUserId")
                
                val result = debtRepository.getDebtsByUserId(currentUserId)
                result.fold(
                    onSuccess = { debts ->
                        Log.d("DebtsViewModel", "Deudas cargadas exitosamente: ${debts.size}")
                        // Log detallado de cada deuda
                        debts.forEachIndexed { index, debt ->
                            Log.d("DebtsViewModel", "Deuda $index: type='${debt.type}', title='${debt.title}', status='${debt.status}'")
                        }
                        _allDebts.value = debts
                        updateTotals(debts)
                    },
                    onFailure = { exception ->
                        Log.e("DebtsViewModel", "Error al cargar deudas", exception)
                        _error.value = "Error al cargar deudas: ${exception.message}"
                        _allDebts.value = emptyList()
                        updateTotals(emptyList())
                    }
                )
            } catch (e: Exception) {
                Log.e("DebtsViewModel", "Excepción al cargar deudas", e)
                _error.value = "Error inesperado: ${e.message}"
                _allDebts.value = emptyList()
                updateTotals(emptyList())
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateTotals(debts: List<Debt>) {
        Log.d("DebtsViewModel", "Actualizando totales con ${debts.size} deudas")
        
        viewModelScope.launch {
            // 1. Obtener datos de deudas activas (para Me deben/Yo debo)
            val activeDebtsData = BalanceCalculator.calculateActiveDebtsBalance(debtRepository)
            
            // 2. Obtener balance de TODAS las deudas (para balance general)
            val allDebtsBalance = BalanceCalculator.calculateAllDebtsBalance(debtRepository)
            
            // Actualizar valores de deudas activas (para gestión diaria)
            _totalOwedByMe.value = activeDebtsData.totalOwedByMe
            _totalOwedToMe.value = activeDebtsData.totalOwedToMe
            _netBalance.value = activeDebtsData.netBalance
            
            // Actualizar balance de todas las deudas
            _allDebtsBalance.value = allDebtsBalance
            
            Log.d("DebtsViewModel", 
                "✅ TOTALES ACTUALIZADOS:" +
                " | Me deben (activas): ${activeDebtsData.totalOwedToMe}" +
                " | Yo debo (activas): ${activeDebtsData.totalOwedByMe}" +
                " | Balance neto activo: ${activeDebtsData.netBalance}" +
                " | Balance TODAS las deudas: ${allDebtsBalance}"
            )
        }
    }

    fun addDebt(debt: Debt) {
        // Usar GlobalScope para evitar cancelación cuando el Fragment se destruye
        GlobalScope.launch {
            _isLoading.value = true
            try {
                Log.d("DebtsViewModel", "=== INICIANDO PROCESO DE INSERCIÓN ===")
                Log.d("DebtsViewModel", "Deuda recibida - ID: ${debt.id}")
                Log.d("DebtsViewModel", "Usuario: ${debt.userId}")
                Log.d("DebtsViewModel", "Título: ${debt.title}")
                Log.d("DebtsViewModel", "Tipo: ${debt.type}")
                Log.d("DebtsViewModel", "Monto original: ${debt.originalAmount}")
                Log.d("DebtsViewModel", "Monto restante: ${debt.remainingAmount}")
                Log.d("DebtsViewModel", "Persona: ${debt.creditorDebtorName}")
                Log.d("DebtsViewModel", "Llamando al repositorio...")
                
                val result = debtRepository.insertDebt(debt)
                result.fold(
                    onSuccess = { insertedDebt ->
                        Log.d("DebtsViewModel", "=== INSERCIÓN EXITOSA ===")
                        Log.d("DebtsViewModel", "Deuda insertada con ID: ${insertedDebt.id}")
                        Log.d("DebtsViewModel", "Recargando lista de deudas...")
                        loadDebts() // Recargar lista
                    },
                    onFailure = { exception ->
                        Log.e("DebtsViewModel", "=== ERROR EN INSERCIÓN ===")
                        Log.e("DebtsViewModel", "Error al añadir deuda", exception)
                        Log.e("DebtsViewModel", "Tipo de error: ${exception::class.simpleName}")
                        Log.e("DebtsViewModel", "Mensaje: ${exception.message}")
                        _error.value = "Error al añadir deuda: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e("DebtsViewModel", "=== EXCEPCIÓN INESPERADA ===")
                Log.e("DebtsViewModel", "Excepción al añadir deuda", e)
                Log.e("DebtsViewModel", "Tipo: ${e::class.simpleName}")
                Log.e("DebtsViewModel", "Mensaje: ${e.message}")
                _error.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateDebt(debt: Debt) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Obtener el estado anterior de la deuda para detectar cambios
                val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
                if (currentUserId == null) {
                    _error.value = "Usuario no autenticado"
                    return@launch
                }

                // Buscar la deuda actual en la lista
                val oldDebt = _allDebts.value?.find { it.id == debt.id }
                
                Log.d("DebtsViewModel", "Actualizando deuda: ${debt.id}, estado anterior: ${oldDebt?.status}, estado nuevo: ${debt.status}")
                
                val result = debtRepository.updateDebt(debt)
                result.fold(
                    onSuccess = { 
                        Log.d("DebtsViewModel", "Deuda actualizada exitosamente")
                        
                        // Detectar cambio de estado de "paid" a "active" y revertir transacción
                        if (oldDebt?.status?.lowercase() == "paid" && debt.status?.lowercase() == "active") {
                            Log.d("DebtsViewModel", "Detectado cambio de deuda pagada a activa - revirtiendo transacción automática")
                            revertAutomaticTransaction(debt)
                        }
                        // Detectar cambio de estado de "active" a "paid" y crear transacción
                        else if (oldDebt?.status?.lowercase() != "paid" && debt.status?.lowercase() == "paid") {
                            Log.d("DebtsViewModel", "Detectado cambio de deuda activa a pagada - creando transacción automática")
                            createAutomaticTransaction(debt)
                        }
                        
                        loadDebts() // Recargar lista
                    },
                    onFailure = { exception ->
                        Log.e("DebtsViewModel", "Error al actualizar deuda", exception)
                        _error.value = "Error al actualizar deuda: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e("DebtsViewModel", "Excepción al actualizar deuda", e)
                _error.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = debtRepository.deleteDebt(debt.id)
                result.fold(
                    onSuccess = { 
                        Log.d("DebtsViewModel", "Deuda eliminada exitosamente")
                        loadDebts() // Recargar lista
                    },
                    onFailure = { exception ->
                        Log.e("DebtsViewModel", "Error al eliminar deuda", exception)
                        _error.value = "Error al eliminar deuda: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e("DebtsViewModel", "Excepción al eliminar deuda", e)
                _error.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markDebtAsPaid(debtId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Primero obtener la deuda para saber su tipo y monto
                val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
                if (currentUserId == null) {
                    _error.value = "Usuario no autenticado"
                    return@launch
                }

                // Buscar la deuda en la lista actual
                val debt = _allDebts.value?.find { it.id == debtId }
                if (debt == null) {
                    _error.value = "Deuda no encontrada"
                    return@launch
                }

                Log.d("DebtsViewModel", "Marcando deuda como pagada: ${debt.title}, tipo: ${debt.type}, monto: ${debt.originalAmount}")

                // Marcar la deuda como pagada
                val markPaidResult = debtRepository.markDebtAsPaid(debtId)
                markPaidResult.fold(
                    onSuccess = { 
                        Log.d("DebtsViewModel", "Deuda marcada como pagada exitosamente")
                        
                        // Crear transacción automática para afectar el dinero disponible
                        createAutomaticTransaction(debt)
                        
                        loadDebts() // Recargar lista
                    },
                    onFailure = { exception ->
                        Log.e("DebtsViewModel", "Error al marcar deuda como pagada", exception)
                        _error.value = "Error al marcar como pagada: ${exception.message}"
                    }
                )
                
            } catch (e: Exception) {
                Log.e("DebtsViewModel", "Excepción al marcar deuda como pagada", e)
                _error.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun createAutomaticTransaction(debt: Debt) {
        try {
            val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                Log.e("DebtsViewModel", "Usuario no autenticado para crear transacción")
                return
            }

            // Determinar el tipo de transacción según el tipo de deuda
            val transactionType: String
            val description: String
            
            when (debt.type.lowercase()) {
                "debt_owing", "owe" -> {
                    // Yo debía → Pago de deuda (gasto porque utilicé mi dinero)
                    transactionType = TransactionInsert.TYPE_EXPENSE
                    description = "Pago de deuda: ${debt.title} [DEBT_ID:${debt.id}]"
                }
                "debt_owed", "lent" -> {
                    // Me debían → Cobro de deuda (ingreso porque recibí dinero)
                    transactionType = TransactionInsert.TYPE_INCOME
                    description = "Cobro de deuda: ${debt.title} [DEBT_ID:${debt.id}]"
                }
                else -> {
                    Log.w("DebtsViewModel", "Tipo de deuda desconocido: ${debt.type}")
                    return
                }
            }

            val transaction = TransactionInsert(
                userId = currentUserId,
                type = transactionType,
                amount = debt.originalAmount,
                description = description,
                category = "Deudas",
                account = "Principal"
            )

            Log.d("DebtsViewModel", "Creando transacción automática: tipo=$transactionType, monto=${debt.originalAmount}, descripción=$description")

            // Insertar la transacción en la base de datos
            SupabaseManager.client
                .from("transactions")
                .insert(transaction)

            Log.d("DebtsViewModel", "Transacción automática creada exitosamente")
            
            // Notificar que las transacciones han cambiado
            TransactionEventBus.postRefreshRequest()

        } catch (e: Exception) {
            Log.e("DebtsViewModel", "Error al crear transacción automática", e)
            // No falla el proceso completo si hay error en la transacción
            _error.value = "Deuda marcada como pagada, pero error al actualizar balance: ${e.message}"
        }
    }

    private suspend fun revertAutomaticTransaction(debt: Debt) {
        try {
            val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                Log.e("DebtsViewModel", "Usuario no autenticado para revertir transacción")
                return
            }

            Log.d("DebtsViewModel", "Buscando transacción automática para deuda: ${debt.id}")

            // Buscar la transacción automática asociada a esta deuda
            val transactions = SupabaseManager.client
                .from("transactions")
                .select {
                    filter {
                        eq("user_id", currentUserId)
                        eq("category", "Deudas")
                        ilike("description", "%[DEBT_ID:${debt.id}]%")
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(1)
                }
                .decodeList<com.juangilles123.monifly.data.model.Transaction>()

            if (transactions.isNotEmpty()) {
                val transactionToRevert = transactions.first()
                Log.d("DebtsViewModel", "Encontrada transacción a revertir: ${transactionToRevert.id}, tipo: ${transactionToRevert.type}, monto: ${transactionToRevert.amount}")

                // Crear transacción de reversión (tipo opuesto)
                val reversalType = when (transactionToRevert.type) {
                    TransactionInsert.TYPE_EXPENSE -> TransactionInsert.TYPE_INCOME
                    TransactionInsert.TYPE_INCOME -> TransactionInsert.TYPE_EXPENSE
                    else -> {
                        Log.w("DebtsViewModel", "Tipo de transacción desconocido: ${transactionToRevert.type}")
                        return
                    }
                }

                val reversalDescription = "Reversión por reactivación: ${debt.title} [DEBT_ID:${debt.id}]"

                val reversalTransaction = TransactionInsert(
                    userId = currentUserId,
                    type = reversalType,
                    amount = transactionToRevert.amount,
                    description = reversalDescription,
                    category = "Deudas",
                    account = "Principal"
                )

                // Insertar la transacción de reversión
                SupabaseManager.client
                    .from("transactions")
                    .insert(reversalTransaction)

                Log.d("DebtsViewModel", "Transacción de reversión creada exitosamente: tipo=$reversalType, monto=${transactionToRevert.amount}")
                
                // Notificar que las transacciones han cambiado
                TransactionEventBus.postRefreshRequest()

            } else {
                Log.w("DebtsViewModel", "No se encontró transacción automática para revertir para la deuda: ${debt.id}")
            }

        } catch (e: Exception) {
            Log.e("DebtsViewModel", "Error al revertir transacción automática", e)
            // No falla el proceso completo si hay error en la reversión
            _error.value = "Deuda reactivada, pero error al actualizar balance: ${e.message}"
        }
    }

    fun refreshDebts() {
        Log.d("DebtsViewModel", "Refrescando deudas")
        loadDebts()
    }
    
    fun clearError() {
        _error.value = null
    }
}