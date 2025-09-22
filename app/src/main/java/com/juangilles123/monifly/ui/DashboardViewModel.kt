package com.juangilles123.monifly.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.data.model.Transaction
import com.juangilles123.monifly.data.repository.TransactionRepository
import com.juangilles123.monifly.data.repository.DebtRepositoryInterface
import com.juangilles123.monifly.data.repository.DebtRepositoryImpl
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
// kotlinx.coroutines.flow imports for StateFlow are removed if no longer needed
import kotlinx.coroutines.launch
import com.juangilles123.monifly.utils.BalanceCalculator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Enum para los períodos de tiempo
enum class TimePeriod {
    DAY,
    WEEK,
    MONTH
}

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val debtRepository: DebtRepositoryInterface = DebtRepositoryImpl()
) : ViewModel() {

    private val _dashboardItems = MutableLiveData<List<DashboardListItem>>()
    val dashboardItems: LiveData<List<DashboardListItem>> = _dashboardItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _transactionDeletedEvent = MutableLiveData<Pair<Boolean, String?>>()
    val transactionDeletedEvent: LiveData<Pair<Boolean, String?>> = _transactionDeletedEvent

    private val _selectedTimePeriod = MutableLiveData<TimePeriod>(TimePeriod.MONTH) // Default

    // StateFlows para _periodTotalIncome, _periodTotalExpenses, _periodNetBalance han sido eliminados

    private val supabaseDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

    private val supabaseAuth = SupabaseManager.client.auth
    private var authHasTriggeredLoad = false
    private var currentLoadJob: Job? = null
    private var currentDeleteJob: Job? = null

    private data class SortableTransaction(
        val originalDate: Date,
        val originalCreatedAt: Long?,
        val transactionViewData: TransactionViewData
    )

    init {
        Log.d("DashboardVM_Lifecycle", "ViewModel INIT")
        observeAuthStatus()
    }

    private fun observeAuthStatus() {
        viewModelScope.launch {
            supabaseAuth.sessionStatus
                .collect { status ->
                    val defaultHeader = DashboardListItem.Header()
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            if (!authHasTriggeredLoad) {
                                Log.d("DashboardVM_Auth", "Autenticado. Disparando carga inicial de datos.")
                                loadDashboardData()
                                authHasTriggeredLoad = true
                            }
                        }
                        is SessionStatus.NotAuthenticated -> {
                            Log.d("DashboardVM_Auth", "No autenticado. Limpiando datos y UI.")
                            currentLoadJob?.cancel()
                            _dashboardItems.postValue(listOf(defaultHeader))
                            _error.postValue("Sesión cerrada. Por favor, inicia sesión.")
                            _isLoading.postValue(false)
                            authHasTriggeredLoad = false
                        }
                        else -> { /* No action needed */ }
                    }
                }
        }
    }
    
    private fun normalizeDateToMidnight(date: Date, calendar: Calendar): Date {
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun calculateStreak(transactions: List<SortableTransaction>): Int {
        if (transactions.isEmpty()) return 0
        val calendar = Calendar.getInstance()
        val uniqueTransactionDays = transactions
            .map { normalizeDateToMidnight(it.originalDate, calendar).time }
            .toSortedSet<Long>(compareByDescending { it })

        if (uniqueTransactionDays.isEmpty()) return 0
        var streakCount = 0
        var currentExpectedDayTime = -1L 
        val todayMidnight = normalizeDateToMidnight(Date(), calendar).time
        calendar.time = Date() 
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayMidnight = normalizeDateToMidnight(calendar.time, calendar).time
        
        if (!uniqueTransactionDays.contains(todayMidnight) && !uniqueTransactionDays.contains(yesterdayMidnight)) {
            return 0
        }
        for (dayTimestamp in uniqueTransactionDays) {
            if (streakCount == 0) {
                if (dayTimestamp == todayMidnight || dayTimestamp == yesterdayMidnight) {
                    streakCount = 1
                    currentExpectedDayTime = dayTimestamp
                } else { break }
            } else {
                calendar.timeInMillis = currentExpectedDayTime
                calendar.add(Calendar.DAY_OF_YEAR, -1) 
                val previousExpectedDayTime = normalizeDateToMidnight(calendar.time, calendar).time
                if (dayTimestamp == previousExpectedDayTime) {
                    streakCount++
                    currentExpectedDayTime = dayTimestamp
                } else { break }
            }
        }
        return streakCount
    }

    fun loadDashboardData() {
        if (currentLoadJob?.isActive == true) {
            Log.d("DashboardVM_Load", "Carga de datos ya en progreso. Omitiendo nueva solicitud.")
            return
        }
        val currentUserId = supabaseAuth.currentUserOrNull()?.id

        if (currentUserId == null) {
            if (supabaseAuth.sessionStatus.value !is SessionStatus.NotAuthenticated) {
                 _error.value = "Debes iniciar sesión para ver tus transacciones."
            }
            _dashboardItems.postValue(listOf(DashboardListItem.Header()))
            _isLoading.value = false
            return
        }

        Log.d("DashboardVM_Load", "Iniciando carga de datos del dashboard. Usuario: $currentUserId")
        _isLoading.value = true
        _error.value = null

        currentLoadJob = viewModelScope.launch {
            try {
                // Obtener transacciones
                val transactionsResult = transactionRepository.getTransactionsByUserId(currentUserId)
                
                val allSortableTransactions = mutableListOf<SortableTransaction>()
                var hasErrorOccurred = false

                transactionsResult.fold(
                    onSuccess = { transactions -> 
                        Log.d("DashboardVM_Load", "Transacciones obtenidas: ${transactions.size}")
                        allSortableTransactions.addAll(transactions.mapNotNull { it.toSortableTransaction() })
                    },
                    onFailure = {
                        Log.e("DashboardVM_Load", "Error al obtener transacciones: ${it.message}", it)
                        _error.value = "Error al obtener transacciones: ${it.message}"
                        hasErrorOccurred = true
                    }
                )

                // ===== NUEVA LÓGICA UNIFICADA DE BALANCES =====
                // PANTALLA INICIO: Disponible + Patrimonio Total
                
                // Convertir SortableTransactions a formato Transaction para BalanceCalculator
                val transactionsForCalculator = allSortableTransactions.mapNotNull { sortableTx ->
                    try {
                        Transaction(
                            id = sortableTx.transactionViewData.id.toLongOrNull() ?: 0L,
                            userId = "",
                            type = if (sortableTx.transactionViewData.isExpense) "expense" else "income",
                            amount = sortableTx.transactionViewData.rawAmount,
                            description = sortableTx.transactionViewData.description,
                            category = sortableTx.transactionViewData.categoryOrType,
                            createdAt = ""
                        )
                    } catch (e: Exception) {
                        Log.w("DashboardVM_Convert", "Error convirtiendo transacción: ${e.message}")
                        null
                    }
                }
                
                // CALCULAR TODOS LOS BALANCES SEGÚN ESPECIFICACIONES
                val wealthData = BalanceCalculator.calculateTotalWealth(transactionsForCalculator, debtRepository)
                
                Log.d("DashboardVM_Balances", 
                    "🏠 PANTALLA INICIO:" +
                    " | Disponible: ${wealthData.availableMoney}" +
                    " | Balance Deudas: ${wealthData.allDebtsBalance}" +
                    " | Patrimonio Total: ${wealthData.totalWealth}" +
                    " | (Transacciones: ${wealthData.transactionsBalance} + Todas las deudas: ${wealthData.allDebtsBalance})"
                )
                
                val formattedAvailable = currencyFormatter.format(wealthData.availableMoney)
                val formattedDebtBalance = currencyFormatter.format(wealthData.allDebtsBalance)
                val formattedTotalWealth = currencyFormatter.format(wealthData.totalWealth)
                
                val streakCount = calculateStreak(allSortableTransactions)

                val header = DashboardListItem.Header(
                    availableMoney = formattedAvailable,
                    debtBalance = formattedDebtBalance, // Ahora sí incluir balance de deudas
                    totalWealth = formattedTotalWealth,
                    streakCount = streakCount
                )
                
                val finalDashboardList = mutableListOf<DashboardListItem>(header)

                // Agregar título de Historial
                finalDashboardList.add(DashboardListItem.HistoryTitle)

                if (!hasErrorOccurred) {
                    val orderedTxs = allSortableTransactions
                        .sortedWith(compareByDescending<SortableTransaction> { it.originalDate }
                        .thenByDescending { it.originalCreatedAt ?: 0L })
                        .map { DashboardListItem.Transaction(it.transactionViewData) }
                    finalDashboardList.addAll(orderedTxs)
                }
                _dashboardItems.value = finalDashboardList
            } catch (e: Exception) {
                Log.e("DashboardVM_Load", "Excepción inesperada durante la carga de datos del dashboard", e)
                _error.value = "Error inesperado: ${e.message}"
                _dashboardItems.value = listOf(DashboardListItem.Header())
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun Transaction.toSortableTransaction(): SortableTransaction? {
        return try {
            // Usar createdAt y extraer solo la fecha si es necesario
            val dateString = this.createdAt.substringBefore("T") // Obtener YYYY-MM-DD
            val dateObj = supabaseDateFormat.parse(dateString) ?: run { 
                Log.e("DashboardVM_Parse", "Fecha nula o inválida para Transaction ID ${this.id}: $dateString")
                return null 
            }
            
            val isExpense = this.type == "expense"
            val prefix = if (isExpense) "- " else "+ "
            val categoryOrType = if (isExpense) this.category ?: "Otros" else "Ingreso"
            
            SortableTransaction(
                originalDate = dateObj,
                originalCreatedAt = java.time.Instant.parse(this.createdAt).toEpochMilli(),
                transactionViewData = TransactionViewData(
                    id = this.id.toString(),
                    description = this.description ?: if (isExpense) "Gasto" else "Ingreso",
                    categoryOrType = categoryOrType,
                    amountFormatted = "$prefix${currencyFormatter.format(this.amount)}",
                    isExpense = isExpense,
                    rawAmount = this.amount,
                    originalDate = dateObj,
                    dateFormatted = displayDateFormat.format(dateObj)
                )
            )
        } catch (e: Exception) {
            Log.e("DashboardVM_Parse", "Error al parsear Transaction ID ${this.id}. CreatedAt: ${this.createdAt}", e)
            null
        }
    }

    fun deleteTransaction(transactionId: String, isExpense: Boolean) {
        if (currentDeleteJob?.isActive == true) { 
            Log.d("DashboardVM_Delete", "Eliminación ya en curso. Omitiendo.")
            return 
        }
        Log.d("DashboardVM_Delete", "Solicitando eliminación para ID de transacción: '$transactionId', esGasto: $isExpense")
        currentDeleteJob = viewModelScope.launch {
            try {
                // Usar el método unificado de eliminación
                val result = transactionRepository.deleteTransaction(transactionId)
                result.fold(
                    onSuccess = { 
                        _transactionDeletedEvent.postValue(Pair(true, null))
                        Log.d("DashboardVM_Delete", "Eliminación exitosa. Recargando datos.")
                        loadDashboardData() 
                    },
                    onFailure = { 
                        Log.e("DashboardVM_Delete", "Error al eliminar: ${it.message}", it)
                        _transactionDeletedEvent.postValue(Pair(false, it.message)) 
                    }
                )
            } catch (e: Exception) { 
                Log.e("DashboardVM_Delete", "Excepción en eliminación", e)
                _transactionDeletedEvent.postValue(Pair(false, e.message)) 
            }
        }
    }

    override fun onCleared() {
        super.onCleared(); Log.d("DashboardVM_Lifecycle", "ViewModel CLEARED. Cancelando jobs."); currentLoadJob?.cancel(); currentDeleteJob?.cancel()
    }
}
