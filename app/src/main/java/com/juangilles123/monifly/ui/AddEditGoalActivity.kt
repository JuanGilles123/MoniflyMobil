package com.juangilles123.monifly.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.data.model.Goal
import com.juangilles123.monifly.databinding.ActivityAddEditGoalBinding
import com.juangilles123.monifly.presentation.viewmodel.GoalsViewModel
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class AddEditGoalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditGoalBinding
    private val goalsViewModel: GoalsViewModel by viewModels()
    
    private var selectedDate: LocalDate? = null
    private var isEditMode = false
    private var goalToEdit: Goal? = null
    private var isUpdating = false // Flag to track when we're updating
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

    companion object {
        const val EXTRA_GOAL_ID = "extra_goal_id"
        const val EXTRA_GOAL_NAME = "extra_goal_name"
        const val EXTRA_GOAL_DESCRIPTION = "extra_goal_description"
        const val EXTRA_GOAL_TARGET_AMOUNT = "extra_goal_target_amount"
        const val EXTRA_GOAL_CURRENT_AMOUNT = "extra_goal_current_amount"
        const val EXTRA_GOAL_TARGET_DATE = "extra_goal_target_date"
        const val EXTRA_IS_EDIT_MODE = "extra_is_edit_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        setupObservers()
        handleIntent()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveGoal()
        }

        binding.etTargetDate.setOnClickListener {
            showDatePicker()
        }

        binding.tilTargetDate.setStartIconOnClickListener {
            showDatePicker()
        }

        // Delete button listener
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        // Add text watchers for real-time progress calculation
        binding.etTargetAmount.setOnFocusChangeListener { _, _ ->
            updateProgressPreview()
        }

        binding.etCurrentAmount.setOnFocusChangeListener { _, _ ->
            updateProgressPreview()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            goalsViewModel.isLoading.collect { isLoading ->
                binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnSave.isEnabled = !isLoading
                binding.btnCancel.isEnabled = !isLoading
            }
        }

        lifecycleScope.launch {
            goalsViewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(this@AddEditGoalActivity, it, Toast.LENGTH_LONG).show()
                    goalsViewModel.clearErrorMessage()
                }
            }
        }

        // Listen for successful goal creation/delete
        lifecycleScope.launch {
            goalsViewModel.goals.collect { goals ->
                if (isEditMode && goalToEdit != null && !isUpdating) {
                    // Check if goal was deleted
                    val goalExists = goals.any { it.id == goalToEdit!!.id }
                    if (!goalExists) {
                        Toast.makeText(this@AddEditGoalActivity, "Meta eliminada correctamente", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else if (!isEditMode) {
                    // New goal was created - check if list has new items
                    if (goals.isNotEmpty()) {
                        Toast.makeText(this@AddEditGoalActivity, "Meta creada correctamente", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun handleIntent() {
        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false)
        
        if (isEditMode) {
            binding.toolbar.title = "Editar Meta"
            binding.btnSave.text = "Actualizar Meta"
            binding.cardProgressPreview.visibility = View.VISIBLE
            binding.btnDelete.visibility = View.VISIBLE
            
            // Load goal data from intent
            val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: ""
            val goalName = intent.getStringExtra(EXTRA_GOAL_NAME) ?: ""
            val goalDescription = intent.getStringExtra(EXTRA_GOAL_DESCRIPTION) ?: ""
            val targetAmount = intent.getDoubleExtra(EXTRA_GOAL_TARGET_AMOUNT, 0.0)
            val currentAmount = intent.getDoubleExtra(EXTRA_GOAL_CURRENT_AMOUNT, 0.0)
            val targetDate = intent.getStringExtra(EXTRA_GOAL_TARGET_DATE)
            
            // Create goal object for editing
            goalToEdit = Goal(
                id = goalId,
                userId = getCurrentUserId() ?: "",
                name = goalName,
                description = goalDescription.ifEmpty { null },
                targetAmount = targetAmount,
                currentSaved = currentAmount,
                targetDate = targetDate,
                status = "active",
                createdAt = null,
                updatedAt = null
            )
            
            // Populate fields
            binding.etGoalTitle.setText(goalName)
            binding.etGoalDescription.setText(goalDescription)
            binding.etTargetAmount.setText(targetAmount.toString())
            binding.etCurrentAmount.setText(currentAmount.toString())
            
            // Handle date
            targetDate?.let {
                try {
                    selectedDate = LocalDate.parse(it)
                    binding.etTargetDate.setText(selectedDate!!.format(dateFormatter))
                } catch (e: Exception) {
                    // Invalid date format, ignore
                }
            }
            
            updateProgressPreview()
        } else {
            binding.toolbar.title = "Nueva Meta"
            binding.btnSave.text = "Crear Meta"
            binding.cardProgressPreview.visibility = View.GONE
            binding.btnDelete.visibility = View.GONE
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                binding.etTargetDate.setText(selectedDate!!.format(dateFormatter))
            },
            year,
            month,
            day
        ).apply {
            // Only allow future dates for new goals
            if (!isEditMode) {
                datePicker.minDate = System.currentTimeMillis()
            }
            show()
        }
    }

    private fun updateProgressPreview() {
        if (!isEditMode) return
        
        try {
            val targetText = binding.etTargetAmount.text.toString()
            val currentText = binding.etCurrentAmount.text.toString()
            
            val targetAmount = if (targetText.isNotEmpty()) targetText.toDouble() else 0.0
            val currentAmount = if (currentText.isNotEmpty()) currentText.toDouble() else 0.0
            
            val progress = if (targetAmount > 0) {
                ((currentAmount / targetAmount) * 100).toInt().coerceIn(0, 100)
            } else 0
            
            binding.tvProgressPercentage.text = "$progress%"
            binding.progressBarGoal.progress = progress
        } catch (e: Exception) {
            // Invalid numbers, show 0%
            binding.tvProgressPercentage.text = "0%"
            binding.progressBarGoal.progress = 0
        }
    }

    private fun saveGoal() {
        val title = binding.etGoalTitle.text.toString().trim()
        val description = binding.etGoalDescription.text.toString().trim()
        val targetAmountText = binding.etTargetAmount.text.toString().trim()
        val currentAmountText = binding.etCurrentAmount.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            binding.tilGoalTitle.error = "El título es requerido"
            binding.etGoalTitle.requestFocus()
            return
        } else {
            binding.tilGoalTitle.error = null
        }

        if (targetAmountText.isEmpty()) {
            binding.tilTargetAmount.error = "El monto objetivo es requerido"
            binding.etTargetAmount.requestFocus()
            return
        } else {
            binding.tilTargetAmount.error = null
        }

        val targetAmount = try {
            targetAmountText.toDouble()
        } catch (e: NumberFormatException) {
            binding.tilTargetAmount.error = "Ingresa un monto válido"
            binding.etTargetAmount.requestFocus()
            return
        }

        if (targetAmount <= 0) {
            binding.tilTargetAmount.error = "El monto debe ser mayor a 0"
            binding.etTargetAmount.requestFocus()
            return
        } else {
            binding.tilTargetAmount.error = null
        }

        val currentAmount = try {
            if (currentAmountText.isNotEmpty()) currentAmountText.toDouble() else 0.0
        } catch (e: NumberFormatException) {
            binding.tilCurrentAmount.error = "Ingresa un monto válido"
            binding.etCurrentAmount.requestFocus()
            return
        }

        if (currentAmount < 0) {
            binding.tilCurrentAmount.error = "El monto no puede ser negativo"
            binding.etCurrentAmount.requestFocus()
            return
        } else {
            binding.tilCurrentAmount.error = null
        }

        val userId = getCurrentUserId()
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        if (isEditMode && goalToEdit != null) {
            // Update existing goal
            val updatedGoal = goalToEdit!!.copy(
                name = title,
                description = description.ifEmpty { null },
                targetAmount = targetAmount,
                currentSaved = currentAmount,
                targetDate = selectedDate?.toString() // This is correct - converts LocalDate to string or null
            )
            // Show loading and update
            isUpdating = true
            binding.loadingOverlay.visibility = View.VISIBLE
            goalsViewModel.updateGoal(updatedGoal)
            
            // Use a coroutine to wait briefly and then finish the activity
            lifecycleScope.launch {
                kotlinx.coroutines.delay(1000) // Wait 1 second for the update to complete
                Toast.makeText(this@AddEditGoalActivity, "Meta actualizada correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            // Create new goal
            val newGoal = Goal(
                id = "", // Will be ignored in repository
                userId = userId,
                name = title,
                description = description.ifEmpty { null },
                targetAmount = targetAmount,
                currentSaved = currentAmount,
                targetDate = selectedDate?.toString(), // This is correct - converts LocalDate to string or null
                status = "active",
                createdAt = null,
                updatedAt = null
            )
            goalsViewModel.createGoal(newGoal)
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Meta")
            .setMessage("¿Estás seguro de que quieres eliminar esta meta? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteGoal()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteGoal() {
        goalToEdit?.let { goal ->
            binding.loadingOverlay.visibility = View.VISIBLE
            goalsViewModel.deleteGoal(goal.id)
        }
    }

    private fun getCurrentUserId(): String? {
        return try {
            SupabaseManager.client.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }
}