package com.juangilles123.monifly.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juangilles123.monifly.data.model.Goal
import com.juangilles123.monifly.data.repository.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GoalsViewModel : ViewModel() {

    private val goalRepository = GoalRepository()

    companion object {
        private const val TAG = "GoalsViewModel"
    }

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showCompletedGoals = MutableStateFlow(false)
    val showCompletedGoals: StateFlow<Boolean> = _showCompletedGoals.asStateFlow()

    fun loadGoals(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = goalRepository.getGoalsByUserId(userId)

                result.fold(
                    onSuccess = { goalsList ->
                        Log.d(TAG, "Goals loaded successfully: ${goalsList.size} goals")
                        _goals.value = goalsList
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error loading goals", exception)
                        _errorMessage.value = "Error al cargar las metas: ${exception.message}"
                        _goals.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading goals", e)
                _errorMessage.value = "Error inesperado: ${e.message}"
                _goals.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleShowCompletedGoals(userId: String) {
        _showCompletedGoals.value = !_showCompletedGoals.value
        loadGoals(userId)
    }

    fun createGoal(goal: Goal) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = goalRepository.insertGoal(goal)
                result.fold(
                    onSuccess = { createdGoal ->
                        Log.d(TAG, "Goal created successfully: ${createdGoal.name}")
                        // Recargar la lista de metas
                        val currentGoals = _goals.value.toMutableList()
                        currentGoals.add(0, createdGoal)
                        _goals.value = currentGoals
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error creating goal", exception)
                        _errorMessage.value = "Error al crear la meta: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error creating goal", e)
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = goalRepository.updateGoal(goal)
                result.fold(
                    onSuccess = { updatedGoal ->
                        Log.d(TAG, "Goal updated successfully: ${updatedGoal.name}")
                        // Actualizar la lista local
                        val currentGoals = _goals.value.toMutableList()
                        val index = currentGoals.indexOfFirst { it.id == updatedGoal.id }
                        if (index != -1) {
                            currentGoals[index] = updatedGoal
                            _goals.value = currentGoals
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error updating goal", exception)
                        _errorMessage.value = "Error al actualizar la meta: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error updating goal", e)
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = goalRepository.deleteGoal(goalId)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Goal deleted successfully: $goalId")
                        // Remover de la lista local
                        val currentGoals = _goals.value.toMutableList()
                        currentGoals.removeAll { it.id == goalId }
                        _goals.value = currentGoals
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error deleting goal", exception)
                        _errorMessage.value = "Error al eliminar la meta: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error deleting goal", e)
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleGoalCompletion(goal: Goal) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = if (goal.isCompleted) {
                    goalRepository.reactivateGoal(goal.id)
                } else {
                    goalRepository.markGoalCompleted(goal.id)
                }

                result.fold(
                    onSuccess = { updatedGoal ->
                        Log.d(TAG, "Goal completion toggled: ${updatedGoal.name}")
                        // Actualizar la lista local
                        val currentGoals = _goals.value.toMutableList()
                        val index = currentGoals.indexOfFirst { it.id == updatedGoal.id }
                        if (index != -1) {
                            currentGoals[index] = updatedGoal
                            _goals.value = currentGoals
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error toggling goal completion", exception)
                        _errorMessage.value = "Error al cambiar estado de la meta: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error toggling goal completion", e)
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addContribution(goalId: String, amount: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = goalRepository.updateGoalProgress(goalId, amount)
                result.fold(
                    onSuccess = { updatedGoal ->
                        Log.d(TAG, "Contribution added successfully: $amount")
                        // Actualizar la lista local
                        val currentGoals = _goals.value.toMutableList()
                        val index = currentGoals.indexOfFirst { it.id == updatedGoal.id }
                        if (index != -1) {
                            currentGoals[index] = updatedGoal
                            _goals.value = currentGoals
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error adding contribution", exception)
                        _errorMessage.value = "Error al agregar contribución: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error adding contribution", e)
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Funciones utilitarias para estadísticas
    fun getTotalGoals(): Int = _goals.value.size

    fun getCompletedGoals(): Int = _goals.value.count { it.isCompleted }

    fun getActiveGoals(): Int = _goals.value.count { !it.isCompleted }

    fun getTotalSaved(): Double = _goals.value.sumOf { it.currentSaved }

    fun getTotalTarget(): Double = _goals.value.sumOf { it.targetAmount }

    fun getOverallProgress(): Int {
        val totalTarget = getTotalTarget()
        return if (totalTarget > 0) {
            ((getTotalSaved() / totalTarget) * 100).toInt()
        } else 0
    }
}