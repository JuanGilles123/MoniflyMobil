package com.juangilles123.monifly.ui

import android.app.AlertDialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.juangilles123.monifly.R
import com.juangilles123.monifly.databinding.FragmentDashboardBinding
import com.juangilles123.monifly.util.TransactionEventBus

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
        setupSwipeRefresh()
        setupEmptyStateButtons()
        observeDashboardViewModel()
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
            onEditClicked = { transactionId, isExpense ->
                editTransaction(transactionId, isExpense)
            }
        )
        
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.d("DashboardFragment", "SwipeRefresh triggered - reloading data")
            dashboardViewModel.loadDashboardData()
        }
        
        // Configurar colores personalizados del SwipeRefresh para MoniflyMobil
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.swipe_refresh_primary,      // Verde dinero
            R.color.swipe_refresh_secondary,    // Azul confianza
            R.color.swipe_refresh_accent        // Naranja energía
        )
    }

    private fun setupEmptyStateButtons() {
        // Botones del estado vacío
        val emptyStateView = binding.emptyStateLayout.root
        emptyStateView.findViewById<View>(R.id.buttonAddIncomeEmpty)?.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_addIncomeFragment)
        }
        
        emptyStateView.findViewById<View>(R.id.buttonAddExpenseEmpty)?.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_addExpenseFragment)
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

    private fun editTransaction(transactionId: String, isExpense: Boolean) {
        if (isExpense) {
            // Navegar al fragmento de editar gasto
            val bundle = Bundle().apply {
                putString("transactionId", transactionId)
            }
            findNavController().navigate(R.id.action_dashboardFragment_to_addExpenseFragment, bundle)
        } else {
            // Navegar al fragmento de editar ingreso
            val bundle = Bundle().apply {
                putString("transactionId", transactionId)
            }
            findNavController().navigate(R.id.action_dashboardFragment_to_addIncomeFragment, bundle)
        }
    }

    private fun observeDashboardViewModel() {
        dashboardViewModel.dashboardItems.observe(viewLifecycleOwner) { dashboardItems ->
            Log.d("DashboardFragment", "Actualizando lista de ítems del dashboard: ${dashboardItems.size}")
            transactionAdapter.submitList(dashboardItems)
            
            // Mostrar/ocultar estado vacío basado en si hay transacciones
            val hasTransactions = dashboardItems.size > 1 // >1 porque incluye el header
            showEmptyState(!hasTransactions)
        }

        dashboardViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("DashboardFragment", "Estado de carga: $isLoading")
            
            // Ocultar SwipeRefresh si está activo
            binding.swipeRefreshLayout.isRefreshing = false
            
            // Mostrar/ocultar loading inicial solo si no hay datos con animación
            val hasData = transactionAdapter.itemCount > 0
            if (isLoading && !hasData) {
                animateProgressBar(true)
            } else {
                animateProgressBar(false)
            }
        }

        dashboardViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Log.e("DashboardFragment", "Error recibido: $it")
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                
                // Ocultar SwipeRefresh en caso de error
                binding.swipeRefreshLayout.isRefreshing = false
                animateProgressBar(false)
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

        TransactionEventBus.refreshRequest.observe(viewLifecycleOwner) { timestamp ->
            if (timestamp != null) {
                Log.d("DashboardFragment", "Refresh request recibido. Llamando a viewModel.loadDashboardData()")
                dashboardViewModel.loadDashboardData()
            }
        }
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            // Mostrar estado vacío
            binding.swipeRefreshLayout.isVisible = false
            binding.emptyStateLayout.root.isVisible = true
        } else {
            // Ocultar estado vacío y mostrar contenido
            binding.emptyStateLayout.root.isVisible = false
            binding.swipeRefreshLayout.isVisible = true
        }
        
        Log.d("DashboardFragment", "Mostrando estado vacío: $show")
    }

    private fun animateProgressBar(show: Boolean) {
        if (show) {
            binding.progressBar.isVisible = true
        } else {
            binding.progressBar.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
