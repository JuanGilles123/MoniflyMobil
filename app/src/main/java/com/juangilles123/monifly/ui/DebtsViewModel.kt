package com.juangilles123.monifly.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.juangilles123.monifly.data.model.Debt
import com.juangilles123.monifly.data.model.DebtType
import com.juangilles123.monifly.data.repository.DebtRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DebtsViewModel(private val debtRepository: DebtRepository) : ViewModel() {

    // LiveData existente para la lista completa de deudas
    private val _allDebts = MutableLiveData<List<Debt>>(emptyList())
    val allDebts: LiveData<List<Debt>> get() = _allDebts

    // LiveData existentes para listas filtradas (se mantienen como están)
    val iOweDebts: LiveData<List<Debt>> = _allDebts.map {
        it.filter { debt -> debt.debtType == DebtType.I_OWE }
    }

    val theyOweMeDebts: LiveData<List<Debt>> = _allDebts.map {
        it.filter { debt -> debt.debtType == DebtType.THEY_OWE_ME }
    }

    // StateFlows para los totales
    private val _totalOwedByMe = MutableStateFlow(0.0)
    val totalOwedByMe: StateFlow<Double> = _totalOwedByMe.asStateFlow()

    private val _totalOwedToMe = MutableStateFlow(0.0)
    val totalOwedToMe: StateFlow<Double> = _totalOwedToMe.asStateFlow()

    private val _netBalance = MutableStateFlow(0.0)
    val netBalance: StateFlow<Double> = _netBalance.asStateFlow()


    fun addDebt(debt: Debt) {
        viewModelScope.launch {
            debtRepository.addDebt(debt)
            // Los totales se actualizarán automáticamente por el colector de debtRepository.debts
        }
    }

    fun updateDebt(debt: Debt) {
        viewModelScope.launch {
            debtRepository.updateDebt(debt)
            // Los totales se actualizarán automáticamente
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            debtRepository.deleteDebt(debt)
            // Los totales se actualizarán automáticamente
        }
    }

    private fun loadInitialDebts() {
        viewModelScope.launch {
            debtRepository.fetchDebts()
            // La primera carga de totales se hará cuando se recolecte de debtRepository.debts
        }
    }

    init {
        loadInitialDebts() // Carga inicial

        // Observar cambios en el repositorio para actualizar LiveData y StateFlows de totales
        viewModelScope.launch {
            debtRepository.debts.collect { debtsFromRepo ->
                _allDebts.value = debtsFromRepo // Actualizar LiveData

                // Calcular y actualizar StateFlows de totales
                // Solo se consideran las deudas pendientes (isPaid no es true)
                _totalOwedByMe.value = debtsFromRepo
                    .filter { it.debtType == DebtType.I_OWE && it.isPaid != true }
                    .sumOf { it.amount }

                _totalOwedToMe.value = debtsFromRepo
                    .filter { it.debtType == DebtType.THEY_OWE_ME && it.isPaid != true }
                    .sumOf { it.amount }

                _netBalance.value = _totalOwedToMe.value - _totalOwedByMe.value
            }
        }
    }
}