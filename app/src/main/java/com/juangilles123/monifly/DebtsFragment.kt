package com.juangilles123.monifly

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.juangilles123.monifly.data.model.Debt
import com.juangilles123.monifly.databinding.FragmentDebtsBinding
import com.juangilles123.monifly.databinding.DialogSelectDebtTypeBinding // Importar el binding del dialog
import com.juangilles123.monifly.ui.DebtAdapter
import com.juangilles123.monifly.ui.DebtsPagerAdapter
import com.juangilles123.monifly.util.TransactionEventBus
import com.juangilles123.monifly.data.model.DebtType
import com.juangilles123.monifly.ui.DebtsViewModel
import com.juangilles123.monifly.ui.DebtsViewModelFactory
import com.juangilles123.monifly.data.repository.TransactionRepositoryImpl
import com.juangilles123.monifly.utils.BalanceCalculator
import com.juangilles123.monifly.data.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DebtsFragment : Fragment() {

    private var _binding: FragmentDebtsBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagerAdapter: DebtsPagerAdapter
    private val transactionRepository = TransactionRepositoryImpl()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

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

        setupSwipeRefresh()
        setupViewPager()
        observeViewModel()
        observeDebtTotals()
        updateTotalBalance() // Cargar balance total inicialmente

        binding.fabAddDebt.setOnClickListener {
            showAddDebtTypeDialog()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshDebts.apply {
            // Configurar colores del SwipeRefresh para que coincidan con el tema
            setColorSchemeResources(
                R.color.swipe_refresh_primary,
                R.color.swipe_refresh_secondary,
                R.color.swipe_refresh_accent
            )
            
            // Configurar el listener de refresh
            setOnRefreshListener {
                refreshData()
            }
            
            // Inicialmente no mostrar el loading
            isRefreshing = false
        }
        
        // Observar el estado de carga del ViewModel
        observeLoadingState()
    }

    private fun observeLoadingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                debtsViewModel.isLoading.collect { isLoading ->
                    binding.swipeRefreshDebts.isRefreshing = isLoading
                }
            }
        }
        
        // Timeout de seguridad para detener el refresh después de 10 segundos
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(10000) // 10 segundos
            if (binding.swipeRefreshDebts.isRefreshing) {
                binding.swipeRefreshDebts.isRefreshing = false
            }
        }
    }

    private fun refreshData() {
        // Llamar al método de refresh del ViewModel
        debtsViewModel.refreshDebts()
    }

    private fun showAddDebtTypeDialog() {
        val dialogBinding = DialogSelectDebtTypeBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar Tipo de Deuda")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar") { d, _ ->
                d.dismiss()
            }
            .create()

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

    private fun setupViewPager() {
        pagerAdapter = DebtsPagerAdapter(
            onEditClicked = { debt ->
                navigateToEditDebtFragment(debt.debtType, debt.id)
            },
            onDeleteClicked = { debt ->
                showDeleteConfirmationDialog(debt)
            },
            onMarkAsPaidClicked = { debt ->
                showMarkAsPaidConfirmationDialog(debt)
            }
        )

        binding.viewPagerDebts.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayoutDebts, binding.viewPagerDebts) { tab, position ->
            tab.text = when (position) {
                0 -> "Yo Debo"
                1 -> "Me Deben"
                2 -> "Pagadas"
                else -> ""
            }
        }.attach()
    }

    private fun navigateToEditDebtFragment(debtType: DebtType, debtId: String) {
        val action = DebtsFragmentDirections.actionDebtsFragmentToAddDebtFragment(
            debtTypeArg = debtType,
            debtId = debtId
        )
        findNavController().navigate(action)
    }

    private fun showMarkAsPaidConfirmationDialog(debt: Debt) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        val amountFormatted = currencyFormat.format(debt.originalAmount)
        
        val messageText = when (debt.type.lowercase()) {
            "debt_owing", "owe" -> {
                "¿Confirmas que pagaste la deuda de $amountFormatted a ${debt.personName}?\n\n" +
                "Se restará $amountFormatted de tu dinero disponible."
            }
            "debt_owed", "lent" -> {
                "¿Confirmas que ${debt.personName} te pagó la deuda de $amountFormatted?\n\n" +
                "Se sumará $amountFormatted a tu dinero disponible."
            }
            else -> {
                "¿Confirmas que quieres marcar esta deuda como pagada?"
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar pago de deuda")
            .setMessage(messageText)
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Confirmar") { dialog, _ ->
                debtsViewModel.markDebtAsPaid(debt.id)
                dialog.dismiss()
            }
            .show()
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
            pagerAdapter.updateIOweDebts(debts)
            // Detener el refresh cuando se actualicen los datos
            binding.swipeRefreshDebts.isRefreshing = false
        }

        debtsViewModel.theyOweMeDebts.observe(viewLifecycleOwner) { debts ->
            pagerAdapter.updateTheyOweMeDebts(debts)
            // Detener el refresh cuando se actualicen los datos
            binding.swipeRefreshDebts.isRefreshing = false
        }

        debtsViewModel.paidDebts.observe(viewLifecycleOwner) { debts ->
            pagerAdapter.updatePaidDebts(debts)
            // Detener el refresh cuando se actualicen los datos
            binding.swipeRefreshDebts.isRefreshing = false
        }

        // Observar errores del ViewModel usando StateFlow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                debtsViewModel.error.collect { errorMessage ->
                    errorMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        binding.swipeRefreshDebts.isRefreshing = false
                    }
                }
            }
        }
        
        // Observar cambios en transacciones para actualizar balance disponible
        TransactionEventBus.refreshRequest.observe(viewLifecycleOwner) { timestamp ->
            android.util.Log.d("DebtsFragment", "TransactionEventBus recibido: $timestamp")
            if (timestamp != null) {
                // Actualizar balance cuando se creen/reviertan transacciones automáticas
                updateTotalBalance()
            }
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
                        // Este es el balance neto de deudas activas - solo para logs
                        Log.d("DebtsFragment", "Balance neto activas: $balance")
                        // Actualizar balance total cuando cambien las deudas
                        updateTotalBalance()
                    }
                }
                // MOSTRAR: Balance de TODAS las deudas en la UI
                launch {
                    debtsViewModel.allDebtsBalance.collect { allDebtsBalance ->
                        binding.textViewNetBalanceDebts.text = currencyFormat.format(allDebtsBalance)
                        Log.d("DebtsFragment", "Balance TODAS las deudas mostrado: $allDebtsBalance")
                    }
                }
            }
        }
    }

    private fun updateTotalBalance() {
        android.util.Log.d("DebtsFragment", "updateTotalBalance() llamado")
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Pequeña espera para asegurar que la transacción se haya guardado en la BD
                kotlinx.coroutines.delay(100)
                // Obtener balance de transacciones
                val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
                if (currentUserId == null) {
                    binding.textViewTransactionBalance.text = currencyFormat.format(0.0)
                    binding.textViewGrandTotal.text = currencyFormat.format(0.0)
                    return@launch
                }

                val transactionsResult = transactionRepository.getTransactionsByUserId(currentUserId)
                if (transactionsResult.isSuccess) {
                    val transactions = transactionsResult.getOrNull() ?: emptyList()
                    
                    // USAR NUEVA LÓGICA SEGÚN ESPECIFICACIONES
                    val wealthData = BalanceCalculator.calculateTotalWealth(transactions, debtsViewModel.repository)
                    
                    // Obtener balance de TODAS las deudas para mostrar en pantalla
                    val allDebtsBalance = debtsViewModel.allDebtsBalance.value

                    // PANTALLA DEUDAS: Disponible + Patrimonio Total
                    binding.textViewTransactionBalance.text = currencyFormat.format(wealthData.availableMoney)
                    binding.textViewGrandTotal.text = currencyFormat.format(wealthData.totalWealth)
                    
                    android.util.Log.d("DebtsFragment_NewLogic", 
                        "🏪 PANTALLA DEUDAS:" +
                        " | Disponible: ${wealthData.availableMoney}" +
                        " | Balance Todas Deudas: ${allDebtsBalance}" +
                        " | Patrimonio Total: ${wealthData.totalWealth}" +
                        " | (Me deben activas: ${wealthData.activeDebtsData.totalOwedToMe})" +
                        " | (Yo debo activas: ${wealthData.activeDebtsData.totalOwedByMe})"
                    )
                    
                } else {
                    binding.textViewTransactionBalance.text = currencyFormat.format(0.0)
                    binding.textViewGrandTotal.text = currencyFormat.format(0.0)
                }

            } catch (e: Exception) {
                android.util.Log.e("DebtsFragment", "Error al calcular balance total: ${e.message}", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // Verificación de seguridad: detener cualquier refresh que esté activo
        _binding?.swipeRefreshDebts?.isRefreshing = false
    }
}
