package com.juangilles123.monifly

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog // Importante para usarlo con setView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.juangilles123.monifly.data.model.Debt
import com.juangilles123.monifly.databinding.FragmentDebtsBinding
import com.juangilles123.monifly.databinding.DialogSelectDebtTypeBinding // Importar el binding del dialog
import com.juangilles123.monifly.ui.DebtAdapter
import com.juangilles123.monifly.data.model.DebtType
import com.juangilles123.monifly.ui.DebtsViewModel
import com.juangilles123.monifly.ui.DebtsViewModelFactory
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DebtsFragment : Fragment() {

    private var _binding: FragmentDebtsBinding? = null
    private val binding get() = _binding!!

    private lateinit var iOweAdapter: DebtAdapter
    private lateinit var theyOweMeAdapter: DebtAdapter

    private val debtsViewModel: DebtsViewModel by navGraphViewModels(R.id.nav_graph) {
        DebtsViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDebtsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModel()
        observeDebtTotals()

        binding.fabAddDebt.setOnClickListener {
            showAddDebtTypeDialog()
        }
    }

    private fun showAddDebtTypeDialog() {
        val dialogBinding = DialogSelectDebtTypeBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar Tipo de Deuda")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar") { d, _ -> // Opcional: mantener botón de cancelar
                d.dismiss()
            }
            .create() // Crear el diálogo para poder interactuar con su vista

        dialogBinding.cardOptionIOwe.setOnClickListener {
            navigateToAddDebtFragment(DebtType.I_OWE)
            dialog.dismiss()
        }

        dialogBinding.cardOptionTheyOweMe.setOnClickListener {
            navigateToAddDebtFragment(DebtType.THEY_OWE_ME)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun navigateToAddDebtFragment(debtType: DebtType) {
        val action = DebtsFragmentDirections.actionDebtsFragmentToAddDebtFragment(
            debtTypeArg = debtType
        )
        findNavController().navigate(action)
    }

    private fun setupRecyclerViews() {
        iOweAdapter = DebtAdapter(
            onItemClicked = { debt ->
                navigateToAddDebtFragment(debt.debtType, debt.id)
            },
            onDeleteClicked = { debt ->
                showDeleteConfirmationDialog(debt)
            }
        )
        theyOweMeAdapter = DebtAdapter(
            onItemClicked = { debt ->
                navigateToAddDebtFragment(debt.debtType, debt.id)
            },
            onDeleteClicked = { debt ->
                showDeleteConfirmationDialog(debt)
            }
        )

        binding.recyclerViewIOwe.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = iOweAdapter
        }

        binding.recyclerViewTheyOwe.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = theyOweMeAdapter
        }
    }

    private fun navigateToAddDebtFragment(debtType: DebtType, debtId: String) {
        val action = DebtsFragmentDirections.actionDebtsFragmentToAddDebtFragment(
            debtTypeArg = debtType,
            debtId = debtId
        )
        findNavController().navigate(action)
    }

    private fun showDeleteConfirmationDialog(debt: Debt) {
        context?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que quieres eliminar la deuda \"${debt.description}\"? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Eliminar") { dialog, _ ->
                    debtsViewModel.deleteDebt(debt)
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun observeViewModel() {
        debtsViewModel.iOweDebts.observe(viewLifecycleOwner) { debts ->
            iOweAdapter.submitList(debts)
        }

        debtsViewModel.theyOweMeDebts.observe(viewLifecycleOwner) { debts ->
            theyOweMeAdapter.submitList(debts)
        }
    }

    private fun observeDebtTotals() {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    debtsViewModel.totalOwedToMe.collect { total ->
                        binding.textViewTotalOwedToMeDebts.text = currencyFormat.format(total)
                    }
                }
                launch {
                    debtsViewModel.totalOwedByMe.collect { total ->
                        binding.textViewTotalOwedByMeDebts.text = currencyFormat.format(total)
                    }
                }
                launch {
                    debtsViewModel.netBalance.collect { balance ->
                        binding.textViewNetBalanceDebts.text = currencyFormat.format(balance)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
