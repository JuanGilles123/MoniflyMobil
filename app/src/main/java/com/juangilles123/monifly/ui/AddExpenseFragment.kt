package com.juangilles123.monifly.ui

import android.app.DatePickerDialog
import android.os.Bundle
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
import com.juangilles123.monifly.databinding.FragmentAddExpenseBinding
import com.juangilles123.monifly.util.TransactionEventBus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!
    
    private val navArgs: AddExpenseFragmentArgs by navArgs()

    private lateinit var viewModel: AddExpenseViewModel
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        val transactionRepository = TransactionRepositoryImpl()
        val factory = AddExpenseViewModelFactory(transactionRepository)
        viewModel = ViewModelProvider(this, factory)[AddExpenseViewModel::class.java]
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
            binding.buttonSaveExpense.text = "Actualizar Gasto"
        }

        setupListeners()
        observeViewModel()

        binding.buttonSaveExpense.setOnClickListener {
            validateAndSaveExpense()
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun validateAndSaveExpense() {
        val amountString = binding.editTextAmount.text.toString()
        val description = binding.editTextDescription.text.toString()
        val category = getSelectedExpenseCategory()
        val account = getSelectedExpenseAccount()
        val date = binding.editTextDate.text.toString()

        var isValid = true

        if (amountString.isBlank()) {
            binding.textFieldAmount.error = getString(R.string.error_amount_required)
            isValid = false
        } else {
            try {
                amountString.toDouble()
                binding.textFieldAmount.error = null
            } catch (_: NumberFormatException) {
                binding.textFieldAmount.error = getString(R.string.error_invalid_amount)
                isValid = false
            }
        }

        if (category.isBlank()) {
            Toast.makeText(requireContext(), "Por favor selecciona una categoría", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (account.isBlank()) {
            Toast.makeText(requireContext(), "Por favor selecciona una cuenta", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (date.isBlank()) {
            binding.textFieldDate.error = getString(R.string.error_date_required)
            isValid = false
        } else {
            binding.textFieldDate.error = null
        }

        if (isValid) {
            // Verificar si estamos en modo edición
            val transactionId = navArgs.transactionId
            val isEditMode = !transactionId.isNullOrEmpty()

            if (isEditMode) {
                // Actualizar la transacción existente
                viewModel.updateExpense(transactionId!!, amountString, description, category, date, account)
            } else {
                // Crear nueva transacción
                viewModel.saveExpense(amountString, description, category, date, account)
            }
        }
    }

    private fun setupListeners() {
        binding.editTextDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.buttonSaveExpense.isEnabled = !isLoading
        }

        viewModel.validationError.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.saveOperationResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, getString(R.string.expense_saved_successfully), Toast.LENGTH_SHORT).show()
                TransactionEventBus.postRefreshRequest()
                findNavController().popBackStack(R.id.dashboardFragment, false)
            }.onFailure {
                Toast.makeText(context, "Error al guardar el gasto: ${it.message}", Toast.LENGTH_LONG).show()
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
        binding.textFieldDate.error = null
    }

    private fun getSelectedExpenseCategory(): String {
        return when (binding.chipGroupExpenseCategories.checkedChipId) {
            binding.chipFood.id -> "Comida"
            binding.chipTransport.id -> "Transporte"
            binding.chipShopping.id -> "Compras"
            binding.chipEntertainment.id -> "Entretenimiento"
            binding.chipBills.id -> "Servicios"
            binding.chipHealth.id -> "Salud"
            binding.chipOtherExpense.id -> "Otros"
            else -> "Otros" // Default fallback
        }
    }

    private fun getSelectedExpenseAccount(): String {
        return when (binding.chipGroupExpenseAccounts.checkedChipId) {
            binding.chipCashExpense.id -> "Efectivo"
            binding.chipDebitCardExpense.id -> "Cuenta Principal (Débito)"
            binding.chipTransferExpense.id -> "Transferencia"
            else -> "Efectivo" // Default fallback
        }
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
            android.util.Log.e("AddExpenseFragment", "Error al parsear fecha: ${transaction.createdAt}", e)
        }
        
        // Seleccionar categoría
        when (transaction.category) {
            "Comida" -> binding.chipFood.isChecked = true
            "Transporte" -> binding.chipTransport.isChecked = true
            "Compras" -> binding.chipShopping.isChecked = true
            "Entretenimiento" -> binding.chipEntertainment.isChecked = true
            "Servicios" -> binding.chipBills.isChecked = true
            "Salud" -> binding.chipHealth.isChecked = true
            else -> binding.chipOtherExpense.isChecked = true
        }
        
        // Seleccionar cuenta
        when (transaction.account) {
            "Efectivo" -> binding.chipCashExpense.isChecked = true
            "Cuenta Principal (Débito)" -> binding.chipDebitCardExpense.isChecked = true
            "Transferencia" -> binding.chipTransferExpense.isChecked = true
            else -> binding.chipCashExpense.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}