package com.juangilles123.monifly

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.juangilles123.monifly.data.model.Debt
import com.juangilles123.monifly.data.model.DebtType
import com.juangilles123.monifly.databinding.FragmentAddDebtBinding
import com.juangilles123.monifly.ui.DebtsViewModel
import com.juangilles123.monifly.ui.DebtsViewModelFactory
import com.juangilles123.monifly.data.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import java.util.UUID
// Asegúrate de tener el import para Clock si lo usaras aquí, aunque ahora el modelo se encarga.
// import kotlinx.datetime.Clock 

class AddDebtFragment : Fragment() {

    private var _binding: FragmentAddDebtBinding? = null
    private val binding get() = _binding!!

    private val args: AddDebtFragmentArgs by navArgs()
    private var debtToEdit: Debt? = null

    private val debtsViewModel: DebtsViewModel by navGraphViewModels(R.id.nav_graph) {
        DebtsViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddDebtBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val debtIdFromArgs = args.debtId
        if (debtIdFromArgs != null) {
            // Modo Edición
            binding.textViewAddDebtTitle.text = "Editar Deuda"
            binding.buttonSaveDebt.text = "Actualizar Deuda"
            debtsViewModel.allDebts.observe(viewLifecycleOwner) { debts ->
                debtToEdit = debts.firstOrNull { it.id == debtIdFromArgs }
                debtToEdit?.let {
                    populateFormWithDebtData(it)
                }
            }
        } else {
            // Modo Creación: Preseleccionar tipo de deuda si se pasó el argumento
            binding.textViewAddDebtTitle.text = "Añadir Nueva Deuda"
            binding.buttonSaveDebt.text = "Guardar Deuda"
            when (args.debtTypeArg) {
                DebtType.I_OWE -> binding.radioGroupDebtType.check(R.id.radioButtonIOwe)
                DebtType.THEY_OWE_ME -> binding.radioGroupDebtType.check(R.id.radioButtonTheyOweMe)
            }
        }

        binding.buttonSaveDebt.setOnClickListener {
            saveDebt()
        }
    }

    private fun populateFormWithDebtData(debt: Debt) {
        binding.editTextDescription.setText(debt.description)
        binding.editTextAmount.setText(debt.amount.toString())
        binding.editTextPersonName.setText(debt.personName)
        when (debt.debtType) {
            DebtType.I_OWE -> binding.radioGroupDebtType.check(R.id.radioButtonIOwe)
            DebtType.THEY_OWE_ME -> binding.radioGroupDebtType.check(R.id.radioButtonTheyOweMe)
        }
        // Manejar isPaid nulable, tratando null como false para el CheckBox
        binding.checkBoxIsPaid.isChecked = debt.isPaid ?: false
    }

    private fun saveDebt() {
        val description = binding.editTextDescription.text.toString().trim()
        val amountString = binding.editTextAmount.text.toString().trim()
        val personName = binding.editTextPersonName.text.toString().trim()
        val isPaid = binding.checkBoxIsPaid.isChecked // Esto es Boolean, y se puede asignar a Boolean?

        if (description.isEmpty()) {
            binding.textFieldDescription.error = "La descripción es requerida"
            return
        } else {
            binding.textFieldDescription.error = null
        }

        if (amountString.isEmpty()) {
            binding.textFieldAmount.error = "El monto es requerido"
            return
        }
        val amount = amountString.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.textFieldAmount.error = "Ingrese un monto válido"
            return
        } else {
            binding.textFieldAmount.error = null
        }

        if (personName.isEmpty()) {
            binding.textFieldPersonName.error = "El nombre de la persona es requerido"
            return
        } else {
            binding.textFieldPersonName.error = null
        }

        val selectedDebtTypeId = binding.radioGroupDebtType.checkedRadioButtonId
        val debtType = if (selectedDebtTypeId == R.id.radioButtonIOwe) {
            DebtType.I_OWE
        } else {
            DebtType.THEY_OWE_ME
        }

        if (debtToEdit != null) {
            // Modo Edición: Actualizar deuda existente
            val updatedDebt = debtToEdit!!.copy(
                description = description,
                amount = amount,
                personName = personName,
                debtType = debtType,
                isPaid = isPaid
            )
            debtsViewModel.updateDebt(updatedDebt)
            Toast.makeText(context, "Deuda actualizada", Toast.LENGTH_SHORT).show()
        } else {
            // Modo Creación: Añadir nueva deuda
            val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                Toast.makeText(context, "Error: No hay usuario autenticado.", Toast.LENGTH_LONG).show()
                return
            }

            val newDebt = Debt(
                id = UUID.randomUUID().toString(),
                description = description,
                amount = amount,
                personName = personName,
                debtType = debtType,
                isPaid = isPaid,
                userId = currentUserId
            )
            debtsViewModel.addDebt(newDebt)
            Toast.makeText(context, "Deuda guardada", Toast.LENGTH_SHORT).show()
        }
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        debtToEdit = null
    }
}
