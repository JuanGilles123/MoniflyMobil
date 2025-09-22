package com.juangilles123.monifly.presentation.dialog

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.juangilles123.monifly.R
import com.juangilles123.monifly.data.model.Goal
import com.juangilles123.monifly.databinding.DialogCreateGoalBinding
import com.juangilles123.monifly.presentation.viewmodel.GoalsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
class CreateGoalDialog : DialogFragment() {

    private var _binding: DialogCreateGoalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalsViewModel by viewModels({ requireParentFragment() })

    private var selectedDate: LocalDate? = null

    // Callback para obtener el userId del fragment padre
    var getUserId: (() -> String?)? = null

    companion object {
        fun newInstance(): CreateGoalDialog {
            return CreateGoalDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupDialog()
    }

    private fun setupDialog() {
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreate.setOnClickListener {
            createGoal()
        }

        binding.etTargetDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                binding.etTargetDate.setText(selectedDate!!.format(formatter))
            },
            year,
            month,
            day
        ).apply {
            // Solo permitir fechas futuras
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }

    private fun createGoal() {
        val name = binding.etGoalName.text.toString().trim()
        val description = binding.etGoalDescription.text.toString().trim()
        val targetAmountText = binding.etTargetAmount.text.toString().trim()

        // Validaciones
        if (name.isEmpty()) {
            binding.etGoalName.error = "El nombre es requerido"
            return
        }

        if (targetAmountText.isEmpty()) {
            binding.etTargetAmount.error = "El monto objetivo es requerido"
            return
        }

        val targetAmount = try {
            targetAmountText.toDouble()
        } catch (e: NumberFormatException) {
            binding.etTargetAmount.error = "Ingresa un monto válido"
            return
        }

        if (targetAmount <= 0) {
            binding.etTargetAmount.error = "El monto debe ser mayor a 0"
            return
        }

        val userId = getUserId?.invoke()
        if (userId == null) {
            Toast.makeText(context, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear el objeto Goal
        val goal = Goal(
            id = "", // Se genera automáticamente en la base de datos
            userId = userId,
            name = name,
            description = description.ifEmpty { null },
            targetAmount = targetAmount,
            currentSaved = 0.0,
            targetDate = selectedDate?.toString(), // Formato YYYY-MM-DD
            status = "active",
            createdAt = null,
            updatedAt = null
        )

        // Crear la meta usando el ViewModel
        viewModel.createGoal(goal)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}