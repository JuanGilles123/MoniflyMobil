package com.juangilles123.monifly.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.juangilles123.monifly.R
import com.juangilles123.monifly.data.model.Transaction
import com.juangilles123.monifly.data.repository.TransactionRepositoryImpl
import com.juangilles123.monifly.databinding.FragmentAddIncomeBinding
import com.juangilles123.monifly.util.TransactionEventBus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddIncomeFragment : Fragment() {

    private var _binding: FragmentAddIncomeBinding? = null
    private val binding get() = _binding!!

    private val navArgs: AddIncomeFragmentArgs by navArgs()

    private lateinit var viewModel: AddIncomeViewModel
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddIncomeBinding.inflate(inflater, container, false)

        // Inicializar Repositorio (esto podría mejorarse con Inyección de Dependencias - Hilt/Koin)
        val transactionRepository = TransactionRepositoryImpl()
        val viewModelFactory = AddIncomeViewModelFactory(transactionRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[AddIncomeViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verificar si estamos en modo edición
        val transactionId = navArgs.transactionId
        val isEditMode = !transactionId.isNullOrEmpty()

        if (isEditMode) {
            // Cargar los datos de la transacción para editar
            viewModel.loadTransactionForEdit(transactionId!!)
            // Cambiar el texto del botón para indicar modo edición
            binding.buttonSaveIncome.text = "Actualizar Ingreso"
        }

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.buttonSaveIncome.setOnClickListener {
            val amountText = binding.editTextAmount.text.toString()
            val description = binding.editTextDescription.text.toString()
            val date = binding.editTextDate.text.toString()
            val category = getSelectedIncomeCategory()
            val account = getSelectedIncomeAccount()

            // Limpiar errores previos de los TextInputLayouts antes de llamar al ViewModel
            binding.textFieldAmount.error = null
            binding.textFieldDate.error = null

            // Verificar si estamos en modo edición
            val transactionId = navArgs.transactionId
            val isEditMode = !transactionId.isNullOrEmpty()

            if (isEditMode) {
                // Actualizar la transacción existente
                viewModel.updateIncome(transactionId!!, amountText, description, date, category, account)
            } else {
                // Crear nueva transacción
                viewModel.saveIncome(amountText, description, date, category, account)
            }
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.editTextDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun getSelectedIncomeCategory(): String {
        return when (binding.chipGroupIncomeCategories.checkedChipId) {
            binding.chipSalary.id -> "Salario"
            binding.chipFreelance.id -> "Freelance"
            binding.chipSales.id -> "Ventas"
            binding.chipInvestments.id -> "Inversiones"
            binding.chipBonus.id -> "Bonificación"
            binding.chipOtherIncome.id -> "Otros"
            else -> "Otros" // Default fallback
        }
    }

    private fun getSelectedIncomeAccount(): String {
        return when (binding.chipGroupIncomeAccounts.checkedChipId) {
            binding.chipCash.id -> "Efectivo"
            binding.chipDebitCard.id -> "Cuenta Principal (Débito)"
            binding.chipTransfer.id -> "Transferencia"
            else -> "Efectivo" // Default fallback
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.buttonSaveIncome.isEnabled = !isLoading
        }

        viewModel.saveOperationResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Ingreso guardado exitosamente!", Toast.LENGTH_LONG).show()
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

            if (errorMessage != null) {
                when {
                    errorMessage.contains("monto", ignoreCase = true) -> {
                        binding.textFieldAmount.error = errorMessage
                    }
                    errorMessage.contains("fecha", ignoreCase = true) -> {
                        binding.textFieldDate.error = errorMessage
                    }
                    else -> {
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Observar transacción cargada para modo edición
        viewModel.loadedTransaction.observe(viewLifecycleOwner) { transaction ->
            if (transaction != null) {
                populateFieldsWithTransaction(transaction)
            }
        }
    }

    private fun showDatePickerDialog() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateInView() {
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding.editTextDate.setText(sdf.format(calendar.time))
    }

    private fun populateFieldsWithTransaction(transaction: Transaction) {
        // Llenar monto
        binding.editTextAmount.setText(transaction.amount.toString())
        
        // Llenar descripción
        binding.editTextDescription.setText(transaction.description ?: "")
        
        // Convertir y mostrar fecha
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = dateFormat.parse(transaction.createdAt.substringBefore("T"))
            if (date != null) {
                binding.editTextDate.setText(displayFormat.format(date))
                calendar.time = date
            }
        } catch (e: Exception) {
            Log.e("AddIncomeFragment", "Error al parsear fecha: ${transaction.createdAt}", e)
        }
        
        // Seleccionar categoría
        when (transaction.category) {
            "Salario" -> binding.chipSalary.isChecked = true
            "Freelance" -> binding.chipFreelance.isChecked = true
            "Ventas" -> binding.chipSales.isChecked = true
            "Inversiones" -> binding.chipInvestments.isChecked = true
            "Bonificación" -> binding.chipBonus.isChecked = true
            else -> binding.chipOtherIncome.isChecked = true
        }
        
        // Seleccionar cuenta
        when (transaction.account) {
            "Efectivo" -> binding.chipCash.isChecked = true
            "Cuenta Principal (Débito)" -> binding.chipDebitCard.isChecked = true
            "Transferencia" -> binding.chipTransfer.isChecked = true
            else -> binding.chipCash.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}