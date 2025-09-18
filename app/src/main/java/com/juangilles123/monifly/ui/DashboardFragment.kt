package com.juangilles123.monifly.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
// import androidx.core.content.ContextCompat // No longer strictly needed if observePeriodTotals is removed
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
// import androidx.lifecycle.Lifecycle // No longer strictly needed if observePeriodTotals is removed
import androidx.lifecycle.ViewModelProvider
// import androidx.lifecycle.lifecycleScope // No longer strictly needed if observePeriodTotals is removed
// import androidx.lifecycle.repeatOnLifecycle // No longer strictly needed if observePeriodTotals is removed
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.juangilles123.monifly.R
import com.juangilles123.monifly.databinding.FragmentDashboardBinding
import com.juangilles123.monifly.util.TransactionEventBus
// import kotlinx.coroutines.launch // No longer strictly needed if observePeriodTotals is removed
// import java.text.NumberFormat // No longer strictly needed if observePeriodTotals is removed
// import java.util.Locale // No longer strictly needed if observePeriodTotals is removed

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var dashboardViewModel: DashboardViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val factory = DashboardViewModelFactory()
        dashboardViewModel = ViewModelProvider(this, factory).get(DashboardViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeDashboardViewModel()
        // Llamada a observePeriodTotals() eliminada
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            items = emptyList(),
            onAddIncomeClicked = {
                findNavController().navigate(R.id.action_dashboardFragment_to_addIncomeFragment)
            },
            onAddExpenseClicked = {
                findNavController().navigate(R.id.action_dashboardFragment_to_addExpenseFragment)
            },
            onDeleteClicked = { transactionId, isExpense ->
                showDeleteConfirmationDialog(transactionId, isExpense)
            },
            onTimePeriodSelected = { selectedPeriod ->
                Log.d("DashboardFragment", "Período seleccionado en UI: $selectedPeriod. Llamando a ViewModel.")
                dashboardViewModel.setTimePeriod(selectedPeriod)
            }
        )
        
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }
    }

    private fun showDeleteConfirmationDialog(transactionId: String, isExpense: Boolean) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar esta transacción?")
            .setPositiveButton("Eliminar") { _, _ ->
                dashboardViewModel.deleteTransaction(transactionId, isExpense)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeDashboardViewModel() {
        dashboardViewModel.dashboardItems.observe(viewLifecycleOwner) { dashboardItems ->
            Log.d("DashboardFragment", "Actualizando lista de ítems del dashboard: ${dashboardItems.size}")
            transactionAdapter.submitList(dashboardItems)
        }

        dashboardViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("DashboardFragment", "Estado de carga: $isLoading")
            val isEmptyOrJustHeader = transactionAdapter.itemCount <= 1
            binding.progressBar.isVisible = isLoading && isEmptyOrJustHeader
        }

        dashboardViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Log.e("DashboardFragment", "Error recibido: $it")
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                binding.progressBar.isVisible = false
            }
        }

        dashboardViewModel.transactionDeletedEvent.observe(viewLifecycleOwner) { event ->
            val (success, errorMessage) = event
            if (success) {
                Toast.makeText(context, "Transacción eliminada exitosamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error al eliminar: ${errorMessage ?: "Desconocido"}", Toast.LENGTH_LONG).show()
            }
        }

        TransactionEventBus.refreshRequest.observe(viewLifecycleOwner) { refresh ->
            if (refresh == true) {
                Log.d("DashboardFragment", "Refresh request recibido. Llamando a viewModel.loadDashboardData()")
                dashboardViewModel.loadDashboardData()
            }
        }
    }

    // Función observePeriodTotals() eliminada completamente

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
