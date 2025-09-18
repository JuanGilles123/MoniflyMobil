package com.juangilles123.monifly.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.data.model.Expense
import com.juangilles123.monifly.data.model.Income
import com.juangilles123.monifly.data.repository.TransactionRepository
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
// kotlinx.coroutines.flow imports for StateFlow are removed if no longer needed
import kotlinx.coroutines.launch
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

class DashboardViewModel(private val transactionRepository: TransactionRepository) : ViewModel() {

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
        Log.d("DashboardVM_Lifecycle", "ViewModel INIT, período inicial: ${_selectedTimePeriod.value}")
        observeAuthStatus()
    }

    private fun observeAuthStatus() {
        viewModelScope.launch {
            supabaseAuth.sessionStatus
                .collect { status ->
                    val currentSelectedPeriod = _selectedTimePeriod.value ?: TimePeriod.MONTH
                    val defaultHeader = DashboardListItem.Header(activePeriod = currentSelectedPeriod)
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
                            // Reseteo de StateFlows de período eliminado
                            authHasTriggeredLoad = false
                        }
                        else -> { /* No action needed */ }
                    }
                }
        }
    }
    
    fun setTimePeriod(period: TimePeriod) {
        if (_selectedTimePeriod.value == period && currentLoadJob?.isActive == false && _dashboardItems.value?.isNotEmpty() == true && _error.value == null) {
            // Log.d("DashboardVM_Period", "Período ya seleccionado y datos cargados para: $period. Omitiendo recarga.")
            // return 
        }
        _selectedTimePeriod.value = period
        Log.d("DashboardVM_Period", "Período de tiempo cambiado a: $period. Disparando recarga de datos.")
        loadDashboardData()
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
        val currentSelectedPeriod = _selectedTimePeriod.value ?: TimePeriod.MONTH

        if (currentUserId == null) {
            if (supabaseAuth.sessionStatus.value !is SessionStatus.NotAuthenticated) {
                 _error.value = "Debes iniciar sesión para ver tus transacciones."
            }
            _dashboardItems.postValue(listOf(DashboardListItem.Header(activePeriod = currentSelectedPeriod)))
            _isLoading.value = false
            // Reseteo de StateFlows de período eliminado
            return
        }

        Log.d("DashboardVM_Load", "Iniciando carga de datos del dashboard. Usuario: $currentUserId, Período: $currentSelectedPeriod")
        _isLoading.value = true
        _error.value = null

        currentLoadJob = viewModelScope.launch {
            try {
                val incomesDeferred = async { transactionRepository.getIncomes() }
                val expensesDeferred = async { transactionRepository.getExpenses() }
                val incomesResult = incomesDeferred.await()
                val expensesResult = expensesDeferred.await()
                
                val allSortableTransactions = mutableListOf<SortableTransaction>()
                var hasErrorOccurred = false

                incomesResult.fold(
                    onSuccess = { incomes -> allSortableTransactions.addAll(incomes.mapNotNull { it.toSortableTransaction() }) },
                    onFailure = {
                        Log.e("DashboardVM_Load", "Error al obtener ingresos: ${it.message}", it)
                        _error.value = "Error al obtener ingresos: ${it.message}"; hasErrorOccurred = true
                    }
                )
                expensesResult.fold(
                    onSuccess = { expenses -> allSortableTransactions.addAll(expenses.mapNotNull { it.toSortableTransaction() }) },
                    onFailure = {
                        Log.e("DashboardVM_Load", "Error al obtener gastos: ${it.message}", it)
                        val currentError = _error.value ?: ""; _error.value = "$currentError\nError al obtener gastos: ${it.message}".trim(); hasErrorOccurred = true
                    }
                )

                val calendar = Calendar.getInstance()
                var currentPeriodIncomes = 0.0 // Sigue siendo necesario para periodBalanceForHeader
                var currentPeriodExpenses = 0.0 // Sigue siendo necesario para periodBalanceForHeader
                var overallTotalIncomes = 0.0
                var overallTotalExpenses = 0.0

                val (startDate, endDate) = getPeriodDateRange(currentSelectedPeriod, calendar)
                // Log.d("DashboardVM_Load", "Rango de fechas para período $currentSelectedPeriod: $startDate - $endDate")

                allSortableTransactions.forEach { sortableTx ->
                    if (!sortableTx.transactionViewData.isExpense) {
                        overallTotalIncomes += sortableTx.transactionViewData.rawAmount
                    } else {
                        overallTotalExpenses += sortableTx.transactionViewData.rawAmount
                    }
                    val txDate = sortableTx.originalDate
                    if (!txDate.before(startDate) && !txDate.after(endDate)) {
                        if (!sortableTx.transactionViewData.isExpense) {
                            currentPeriodIncomes += sortableTx.transactionViewData.rawAmount
                        } else {
                            currentPeriodExpenses += sortableTx.transactionViewData.rawAmount
                        }
                    }
                }
                
                // Actualizaciones de StateFlows de período eliminadas

                val periodBalanceForHeader = currentPeriodIncomes - currentPeriodExpenses
                val totalBalanceForHeader = overallTotalIncomes - overallTotalExpenses
                
                val formattedPeriodBalance = currencyFormatter.format(periodBalanceForHeader)
                val formattedTotalBalance = currencyFormatter.format(totalBalanceForHeader)
                val periodTitle = getTitleForPeriod(currentSelectedPeriod)
                val streakCount = calculateStreak(allSortableTransactions)

                // Log.d("DashboardVM_Streak", "Racha de días con transacciones calculada: $streakCount días")
                // Log de totales de período eliminado, ya que los StateFlows no existen

                val header = DashboardListItem.Header(
                    periodTitle = periodTitle,
                    periodBalance = formattedPeriodBalance,
                    totalBalance = formattedTotalBalance,
                    activePeriod = currentSelectedPeriod,
                    streakCount = streakCount
                )
                
                val finalDashboardList = mutableListOf<DashboardListItem>(header)

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
                _dashboardItems.value = listOf(DashboardListItem.Header(activePeriod = currentSelectedPeriod))
                // Reseteo de StateFlows de período eliminado
            } finally {
                _isLoading.value = false
                // Log.d("DashboardVM_Load", "Carga de datos del dashboard finalizada.")
            }
        }
    }

    private fun getPeriodDateRange(period: TimePeriod, calendar: Calendar): Pair<Date, Date> {
        calendar.time = Date()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val startDate: Date; val endDate: Date
        when (period) {
            TimePeriod.DAY -> {
                startDate = calendar.time
                calendar.add(Calendar.DAY_OF_MONTH, 1); calendar.add(Calendar.MILLISECOND, -1); endDate = calendar.time
            }
            TimePeriod.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek); startDate = calendar.time
                calendar.add(Calendar.WEEK_OF_YEAR, 1); calendar.add(Calendar.MILLISECOND, -1); endDate = calendar.time
            }
            TimePeriod.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1); startDate = calendar.time
                calendar.add(Calendar.MONTH, 1); calendar.add(Calendar.MILLISECOND, -1); endDate = calendar.time
            }
        }
        return Pair(startDate, endDate)
    }

    private fun getTitleForPeriod(period: TimePeriod): String {
        return when (period) {
            TimePeriod.DAY -> "Balance del Día"
            TimePeriod.WEEK -> "Balance de la Semana"
            TimePeriod.MONTH -> "Balance del Mes"
        }
    }
    
    private fun Income.toSortableTransaction(): SortableTransaction? {
        return try {
            val dateObj = supabaseDateFormat.parse(this.date) ?: run { Log.e("DashboardVM_Parse", "Fecha nula o inválida para Income ID ${this.id}: ${this.date}"); return null }
            SortableTransaction(
                originalDate = dateObj, originalCreatedAt = this.createdAt,
                transactionViewData = TransactionViewData(id = this.id, description = this.description ?: "Ingreso", categoryOrType = "Ingreso", amountFormatted = "+ ${currencyFormatter.format(this.amount)}", isExpense = false, rawAmount = this.amount, originalDate = dateObj, dateFormatted = displayDateFormat.format(dateObj))
            )
        } catch (e: Exception) { Log.e("DashboardVM_Parse", "Error al parsear Income ID ${this.id}. Fecha: ${this.date}", e); null }
    }

    private fun Expense.toSortableTransaction(): SortableTransaction? {
        return try {
            val dateObj = supabaseDateFormat.parse(this.date) ?: run { Log.e("DashboardVM_Parse", "Fecha nula o inválida para Expense ID ${this.id}: ${this.date}"); return null }
            SortableTransaction(
                originalDate = dateObj, originalCreatedAt = this.createdAt,
                transactionViewData = TransactionViewData(id = this.id, description = this.description ?: "Gasto", categoryOrType = this.category, amountFormatted = "- ${currencyFormatter.format(this.amount)}", isExpense = true, rawAmount = this.amount, originalDate = dateObj, dateFormatted = displayDateFormat.format(dateObj))
            )
        } catch (e: Exception) { Log.e("DashboardVM_Parse", "Error al parsear Expense ID ${this.id}. Fecha: ${this.date}", e); null }
    }

    fun deleteTransaction(transactionId: String, isExpense: Boolean) {
        if (currentDeleteJob?.isActive == true) { Log.d("DashboardVM_Delete", "Eliminación ya en curso. Omitiendo."); return }
        Log.d("DashboardVM_Delete", "Solicitando eliminación para ID de transacción: '$transactionId', esGasto: $isExpense")
        currentDeleteJob = viewModelScope.launch {
            try {
                val result = if (isExpense) transactionRepository.deleteExpense(transactionId) else transactionRepository.deleteIncome(transactionId)
                result.fold(
                    onSuccess = { _transactionDeletedEvent.postValue(Pair(true, null)); Log.d("DashboardVM_Delete", "Eliminación exitosa. Recargando datos."); loadDashboardData() },
                    onFailure = { Log.e("DashboardVM_Delete", "Error al eliminar: ${it.message}", it); _transactionDeletedEvent.postValue(Pair(false, it.message)) }
                )
            } catch (e: Exception) { Log.e("DashboardVM_Delete", "Excepción en eliminación", e); _transactionDeletedEvent.postValue(Pair(false, e.message)) }
        }
    }

    override fun onCleared() {
        super.onCleared(); Log.d("DashboardVM_Lifecycle", "ViewModel CLEARED. Cancelando jobs."); currentLoadJob?.cancel(); currentDeleteJob?.cancel()
    }
}
