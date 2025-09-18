package com.juangilles123.monifly.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible // Importación necesaria para isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.juangilles123.monifly.data.repository.SupabaseTransactionRepository
import com.juangilles123.monifly.databinding.FragmentAddIncomeBinding
import java.util.Calendar

class AddIncomeFragment : Fragment() {

    private var _binding: FragmentAddIncomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddIncomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddIncomeBinding.inflate(inflater, container, false)

        // Inicializar Repositorio (esto podría mejorarse con Inyección de Dependencias - Hilt/Koin)
        // Por ahora, lo instanciamos directamente aquí.
        val transactionRepository = SupabaseTransactionRepository()
        val viewModelFactory = AddIncomeViewModelFactory(transactionRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[AddIncomeViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.buttonSaveIncome.setOnClickListener {
            val amountText = binding.editTextAmount.text.toString()
            val description = binding.editTextDescription.text.toString()
            val date = binding.editTextDate.text.toString()

            // Limpiar errores previos de los TextInputLayouts antes de llamar al ViewModel
            binding.textFieldAmount.error = null
            binding.textFieldDate.error = null
            // binding.textFieldDescription.error = null // Si tuviera validación específica

            viewModel.saveIncome(amountText, description, date)
        }

        binding.editTextDate.setOnClickListener {
            showDatePickerDialog()
        }
        // Si tienes un ícono de fin en textFieldDate y quieres que también abra el diálogo:
        // binding.textFieldDate.setEndIconOnClickListener {
        //     showDatePickerDialog()
        // }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            // Deshabilitar campos y botón mientras se carga para evitar múltiples envíos o ediciones
            binding.buttonSaveIncome.isEnabled = !isLoading
            binding.editTextAmount.isEnabled = !isLoading
            binding.editTextDescription.isEnabled = !isLoading
            binding.editTextDate.isEnabled = !isLoading
            // Si tienes un ícono en textFieldDate, también podrías deshabilitar su click
            // binding.textFieldDate.isEndIconCheckable = !isLoading
        }

        viewModel.saveOperationResult.observe(viewLifecycleOwner) { result ->
            // Asegurarse de que isLoading no esté activo después de una respuesta (éxito o fallo)
            // El ViewModel ya lo hace en su finally, pero es una buena práctica estar seguro.
            // binding.progressBar.isVisible = false // O gestionarlo desde el observer de isLoading

            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Ingreso guardado exitosamente!", Toast.LENGTH_LONG).show() // Mensaje corregido
                    findNavController().popBackStack() // Volver al Dashboard
                },
                onFailure = { error ->
                    Log.e("AddIncomeFragment", "Error al guardar ingreso: ${error.message}", error)
                    Toast.makeText(requireContext(), "Error al guardar: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }

        viewModel.validationError.observe(viewLifecycleOwner) { errorMessage ->
            // Limpiar errores previos en caso de una nueva validación
            binding.textFieldAmount.error = null
            binding.textFieldDate.error = null
            // binding.textFieldDescription.error = null

            if (errorMessage != null) {
                // Heuristic simple para determinar qué campo mostrar el error.
                // Podrías hacer esto más robusto si el ViewModel emitiera errores más estructurados.
                when {
                    errorMessage.contains("monto", ignoreCase = true) -> {
                        binding.textFieldAmount.error = errorMessage
                        binding.textFieldAmount.requestFocus() // Pedir foco al campo con error
                    }
                    errorMessage.contains("fecha", ignoreCase = true) -> {
                        binding.textFieldDate.error = errorMessage
                        // No tiene sentido pedir foco a un campo no focusable, pero el error se muestra.
                    }
                    else -> {
                        // Para errores de validación no específicos de un campo, o si la heurística falla
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showDatePickerDialog() {
        // Asegurarse de que el contexto no sea nulo
        context ?: return

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(), // Usar requireContext() para asegurar que no sea nulo
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
                binding.editTextDate.setText(formattedDate)
                binding.textFieldDate.error = null // Limpiar error de fecha al seleccionar una
            },
            year, month, day
        )
        // Opcional: Establecer fecha máxima (ej. no permitir fechas futuras)
        // datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Muy importante para evitar memory leaks con ViewBinding en Fragmentos
    }
}