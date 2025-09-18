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
import com.juangilles123.monifly.R
import com.juangilles123.monifly.data.repository.TransactionRepository // For the variable type
import com.juangilles123.monifly.data.repository.SupabaseTransactionRepository // For instantiation
import com.juangilles123.monifly.databinding.FragmentAddExpenseBinding
import com.juangilles123.monifly.ui.AddExpenseViewModel // Added import
import com.juangilles123.monifly.ui.AddExpenseViewModelFactory // Added import
import com.juangilles123.monifly.util.TransactionEventBus // Ensure this import is correct
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddExpenseViewModel
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        val transactionRepository: TransactionRepository = SupabaseTransactionRepository()
        val factory = AddExpenseViewModelFactory(transactionRepository)
        viewModel = ViewModelProvider(this, factory)[AddExpenseViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()

        binding.buttonSaveExpense.setOnClickListener {
            validateAndSaveExpense()
        }
    }

    private fun validateAndSaveExpense() {
        val amountString = binding.editTextAmount.text.toString()
        val description = binding.editTextDescription.text.toString() 
        val category = binding.editTextCategory.text.toString()
        val date = binding.editTextDate.text.toString()

        var isValid = true

        if (amountString.isBlank()) {
            binding.textFieldAmount.error = getString(R.string.error_amount_required)
            isValid = false
        } else {
            try {
                amountString.toDouble()
                binding.textFieldAmount.error = null 
            } catch (_: NumberFormatException) { // Changed e to _
                binding.textFieldAmount.error = getString(R.string.error_invalid_amount)
                isValid = false
            }
        }

        if (category.isBlank()) {
            binding.textFieldCategory.error = getString(R.string.error_category_required)
            isValid = false
        } else {
            binding.textFieldCategory.error = null 
        }

        if (date.isBlank()) {
            binding.textFieldDate.error = getString(R.string.error_date_required) 
            isValid = false
        } else {
            binding.textFieldDate.error = null 
        }

        if (isValid) {
            viewModel.saveExpense(amountString, description, category, date)
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
                // Potentially display this error in a more prominent way if it's a validation error from saveExpense
                // For now, simple Toast as before
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.saveOperationResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, getString(R.string.expense_saved_successfully), Toast.LENGTH_SHORT).show()
                TransactionEventBus.postRefreshRequest() // Corrected method name
                findNavController().popBackStack(R.id.dashboardFragment, false)
            }.onFailure {
                // Handle failure, e.g., show a generic error message or log
                Toast.makeText(context, "Error al guardar el gasto: ${it.message}", Toast.LENGTH_LONG).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
