package com.juangilles123.monifly

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.juangilles123.monifly.databinding.FragmentAddDebtBinding
import com.juangilles123.monifly.data.model.DebtType
import com.juangilles123.monifly.data.model.Debt
import com.juangilles123.monifly.ui.DebtsViewModel
import com.juangilles123.monifly.ui.DebtsViewModelFactory
import com.juangilles123.monifly.data.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import java.util.Date
import java.util.Locale
import com.google.android.material.datepicker.MaterialDatePicker
import android.util.Log

class AddDebtFragment : Fragment() {

    private var _binding: FragmentAddDebtBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var debtsViewModel: DebtsViewModel
    private var debtToEdit: Debt? = null
    private var selectedDueDate: String? = null // Variable para almacenar la fecha seleccionada
    
    // Obtener argumentos de navegación
    private val args: AddDebtFragmentArgs by navArgs()
    
    // Función para generar timestamp compatible con API 24+
    private fun getCurrentTimestamp(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }
    
    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDebtBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Configurar ViewModel
        val factory = DebtsViewModelFactory()
        debtsViewModel = ViewModelProvider(this, factory)[DebtsViewModel::class.java]
        
        setupClickListeners()
        setupFormValidation()
        
        // Verificar si estamos editando una deuda existente
        args.debtId?.let { debtId ->
            loadDebtForEditing(debtId)
        } ?: run {
            // Modo creación - configurar tipo de deuda por defecto
            setupDefaultDebtType()
        }
    }

    private fun setupClickListeners() {
        binding.buttonSaveDebt.setOnClickListener {
            saveDebt()
        }
        
        binding.buttonCancel.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Configurar listener para el campo de fecha de vencimiento
        binding.editTextDueDate.setOnClickListener {
            showDatePicker()
        }
    }
    
    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha de vencimiento")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        
        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = dateFormat.format(calendar.time)
            
            val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val displayDate = displayFormat.format(calendar.time)
            
            binding.editTextDueDate.setText(displayDate)
            
            // Guardar la fecha en formato ISO para la base de datos
            selectedDueDate = formattedDate
            
            Log.d("AddDebtFragment", "Fecha seleccionada: $formattedDate (mostrada como: $displayDate)")
        }
        
        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun setupFormValidation() {
        // Configurar validación de formulario básica
        // Por ahora simplificado para que compile
    }

    private fun saveDebt() {
        // Obtener datos del formulario
        val title = binding.editTextDescription.text.toString().trim()
        val amountText = binding.editTextAmount.text.toString().trim()
        val creditorDebtorName = binding.editTextPersonName.text.toString().trim()
        val notes = binding.editTextNotes.text.toString().trim()

        // Validaciones básicas
        if (title.isBlank()) {
            binding.editTextDescription.error = "El título es obligatorio"
            return
        }

        if (creditorDebtorName.isBlank()) {
            binding.editTextPersonName.error = "El nombre es obligatorio"
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.editTextAmount.error = "Ingrese un monto válido"
            return
        }

        // Obtener tipo de deuda seleccionado y convertir a formato de la base de datos
        val selectedDebtTypeId = binding.radioGroupDebtType.checkedRadioButtonId
        val debtType = if (selectedDebtTypeId == R.id.radioButtonIOwe) {
            "debt_owing"  // Formato correcto para la base de datos web
        } else {
            "debt_owed"   // Formato correcto para la base de datos web
        }

        // Obtener tipo de pago según el chip seleccionado
        val selectedPaymentTypeId = binding.chipGroupPaymentType.checkedChipId
        val paymentType = if (selectedPaymentTypeId == R.id.chipSinglePayment) {
            "lump_sum"    // Pago único
        } else {
            "installment" // En cuotas
        }
        
        // Obtener categoría seleccionada
        val selectedCategoryId = binding.chipGroupCategory.checkedChipId
        val category = when (selectedCategoryId) {
            R.id.chipLoan -> "loan"
            R.id.chipPurchase -> "purchase"
            R.id.chipService -> "service"
            R.id.chipOther -> "other"
            else -> "loan" // Por defecto
        }
        
        // Obtener estado seleccionado
        val selectedStatusId = binding.chipGroupInitialStatus.checkedChipId
        val status = when (selectedStatusId) {
            R.id.chipPaid -> "paid"
            R.id.chipOverdue -> "overdue"
            R.id.chipCancelled -> "cancelled"
            R.id.chipActive -> "active"
            else -> "active" // Por defecto
        }

        lifecycleScope.launch {
            try {
                // Obtener usuario actual
                val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
                if (currentUserId == null) {
                    Log.e("AddDebtFragment", "Error: No hay usuario autenticado")
                    Toast.makeText(context, "Error: No hay usuario autenticado.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                if (debtToEdit != null) {
                    // Modo edición - actualizar deuda existente
                    updateExistingDebt(currentUserId, title, debtType, amount, creditorDebtorName, notes, paymentType, category, status)
                } else {
                    // Modo creación - crear nueva deuda
                    createNewDebt(currentUserId, title, debtType, amount, creditorDebtorName, notes, paymentType, category, status)
                }

            } catch (e: Exception) {
                Log.e("AddDebtFragment", "Error al guardar deuda", e)
                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                // Rehabilitar el botón en caso de error
                binding.buttonSaveDebt.isEnabled = true
                binding.buttonSaveDebt.text = "Guardar Deuda"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private fun loadDebtForEditing(debtId: String) {
        // Buscar la deuda en el ViewModel
        debtsViewModel.allDebts.observe(viewLifecycleOwner) { debts ->
            val debt = debts.find { it.id == debtId }
            debt?.let {
                debtToEdit = it
                populateFormWithDebt(it)
                // Cambiar el título del fragmento
                activity?.title = "Editar Deuda"
            }
        }
    }
    
    private fun populateFormWithDebt(debt: Debt) {
        binding.editTextDescription.setText(debt.title)
        binding.editTextAmount.setText(debt.originalAmount.toString())
        binding.editTextPersonName.setText(debt.creditorDebtorName)
        binding.editTextNotes.setText(debt.description ?: "")
        
        // Configurar tipo de deuda
        if (debt.type == "debt_owing") {
            binding.radioButtonIOwe.isChecked = true
        } else {
            binding.radioButtonTheyOweMe.isChecked = true
        }
        
        // Configurar tipo de pago
        if (debt.paymentType == "lump_sum") {
            binding.chipSinglePayment.isChecked = true
        } else {
            binding.chipInstallments.isChecked = true
        }
        
        // Configurar categoría de la deuda
        setupCategorySelection(debt)
        
        // Configurar estado de la deuda
        setupStatusSelection(debt)
        
        // Configurar fecha de vencimiento si existe
        debt.dueDate?.let { dueDate ->
            selectedDueDate = dueDate
            // Formatear fecha para mostrar (de yyyy-MM-dd a dd/MM/yyyy)
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                val date = inputFormat.parse(dueDate)
                date?.let {
                    binding.editTextDueDate.setText(outputFormat.format(it))
                }
            } catch (e: Exception) {
                binding.editTextDueDate.setText(dueDate)
            }
        }
        
        // Cambiar el texto del botón para indicar que estamos editando
        binding.buttonSaveDebt.text = "Actualizar Deuda"
    }
    
    private fun setupCategorySelection(debt: Debt) {
        // Por defecto seleccionar "Préstamo" si no hay categoría específica
        binding.chipLoan.isChecked = true
    }
    
    private fun setupStatusSelection(debt: Debt) {
        // Configurar el estado actual de la deuda
        when (debt.status?.lowercase()) {
            "paid" -> binding.chipPaid.isChecked = true
            "overdue" -> binding.chipOverdue.isChecked = true
            "cancelled" -> binding.chipCancelled.isChecked = true
            else -> binding.chipActive.isChecked = true // "active" o cualquier otro valor
        }
    }
    
    private fun setupDefaultDebtType() {
        // Configurar el tipo de deuda según el argumento
        when (args.debtTypeArg) {
            DebtType.I_OWE -> binding.radioButtonIOwe.isChecked = true
            DebtType.THEY_OWE_ME -> binding.radioButtonTheyOweMe.isChecked = true
        }
    }
    
    private fun createNewDebt(currentUserId: String, title: String, debtType: String, amount: Double, creditorDebtorName: String, notes: String, paymentType: String, category: String, status: String) {
        Log.d("AddDebtFragment", "=== CREANDO NUEVA DEUDA ===")
        Log.d("AddDebtFragment", "Usuario: $currentUserId")
        Log.d("AddDebtFragment", "Título: $title")
        Log.d("AddDebtFragment", "Categoría: $category")
        Log.d("AddDebtFragment", "Estado: $status")
        
        // Crear nueva deuda con campos mínimos para evitar errores de esquema
        val debtId = UUID.randomUUID().toString()
        val currentTime = getCurrentTimestamp()
        val currentDate = getCurrentDate()
        
        val newDebt = Debt(
            id = debtId,
            userId = currentUserId,
            title = title,
            description = notes.ifBlank { null },
            type = debtType,
            originalAmount = amount,
            remainingAmount = if (status == "paid") 0.0 else amount,
            paymentType = paymentType,
            totalInstallments = null,
            paidInstallments = 0,
            installmentAmount = null,
            paymentFrequency = null,
            createdDate = currentDate,
            dueDate = selectedDueDate,
            nextPaymentDate = null,
            interestRate = null,
            hasInterest = false,
            status = status,
            creditorDebtorName = creditorDebtorName,
            notes = notes.ifBlank { null },
            createdAt = currentTime,
            updatedAt = currentTime
        )
        
        Log.d("AddDebtFragment", "Llamando a ViewModel.addDebt...")
        
        // Deshabilitar el botón para evitar múltiples envíos
        binding.buttonSaveDebt.isEnabled = false
        binding.buttonSaveDebt.text = "Guardando..."
        
        debtsViewModel.addDebt(newDebt)
        
        lifecycleScope.launch {
            // Esperar un momento antes de navegar para evitar cancelación
            delay(1000)
            
            Log.d("AddDebtFragment", "Deuda enviada al ViewModel, navegando de vuelta...")
            Toast.makeText(context, "Deuda creada correctamente", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }
    
    private fun updateExistingDebt(currentUserId: String, title: String, debtType: String, amount: Double, creditorDebtorName: String, notes: String, paymentType: String, category: String, status: String) {
        Log.d("AddDebtFragment", "=== ACTUALIZANDO DEUDA EXISTENTE ===")
        Log.d("AddDebtFragment", "ID: ${debtToEdit?.id}")
        Log.d("AddDebtFragment", "Usuario: $currentUserId")
        Log.d("AddDebtFragment", "Título: $title")
        Log.d("AddDebtFragment", "Categoría: $category")
        Log.d("AddDebtFragment", "Estado: $status")
        
        debtToEdit?.let { originalDebt ->
            val currentTime = getCurrentTimestamp()
            
            val updatedDebt = originalDebt.copy(
                title = title,
                description = notes.ifBlank { null },
                type = debtType,
                originalAmount = amount,
                remainingAmount = if (status == "paid") 0.0 else amount, // Si está pagado, monto restante es 0
                paymentType = paymentType,
                creditorDebtorName = creditorDebtorName,
                notes = notes.ifBlank { null },
                dueDate = selectedDueDate,
                status = status,
                updatedAt = currentTime
            )
            
            Log.d("AddDebtFragment", "Llamando a ViewModel.updateDebt...")
            
            // Deshabilitar el botón para evitar múltiples envíos
            binding.buttonSaveDebt.isEnabled = false
            binding.buttonSaveDebt.text = "Actualizando..."
            
            debtsViewModel.updateDebt(updatedDebt)
            
            lifecycleScope.launch {
                // Esperar un momento antes de navegar
                delay(1000)
                
                Log.d("AddDebtFragment", "Deuda actualizada, navegando de vuelta...")
                Toast.makeText(context, "Deuda actualizada correctamente", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }
}