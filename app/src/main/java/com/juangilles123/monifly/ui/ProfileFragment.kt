package com.juangilles123.monifly.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.juangilles123.monifly.R
import com.juangilles123.monifly.databinding.FragmentProfileBinding
import com.juangilles123.monifly.data.repository.ProfileRepository
import com.juangilles123.monifly.data.repository.TransactionRepositoryImpl
import com.juangilles123.monifly.data.model.Profile
import com.juangilles123.monifly.data.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import android.widget.Toast
import java.text.NumberFormat
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var profileRepository: ProfileRepository
    private lateinit var transactionRepository: TransactionRepositoryImpl
    private var currentProfile: Profile? = null
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        profileRepository = ProfileRepository()
        transactionRepository = TransactionRepositoryImpl()
        
        setupSwipeRefresh()
        setupClickListeners()
        loadUserData()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUserData()
        }
    }

    private fun loadUserData() {
        binding.swipeRefreshLayout.isRefreshing = true
        
        lifecycleScope.launch {
            try {
                val currentUser = SupabaseManager.client.auth.currentUserOrNull()
                
                if (currentUser != null) {
                    // Cargar datos del perfil
                    val profileResult = profileRepository.getProfile(currentUser.id)
                    
                    if (profileResult.isSuccess) {
                        currentProfile = profileResult.getOrNull()
                    } else {
                        // Crear perfil por defecto si no existe
                        createDefaultProfile(currentUser.id, currentUser.email ?: "")
                    }
                    
                    // Cargar estadísticas de transacciones
                    loadTransactionStats(currentUser.id)
                    
                    updateProfileUI()
                } else {
                    showErrorMessage("Usuario no autenticado")
                    // Redirigir al login
                    // navigateToLogin()
                }
                
            } catch (e: Exception) {
                showErrorMessage("Error al cargar datos: ${e.message}")
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private suspend fun createDefaultProfile(userId: String, email: String) {
        try {
            val defaultProfile = Profile(
                id = userId,
                fullName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                countryCode = "CO",
                currentStreak = 0,
                maxStreak = 0,
                hasSeenWelcome = false
            )
            
            val result = profileRepository.createProfile(defaultProfile)
            if (result.isSuccess) {
                currentProfile = result.getOrNull()
            }
        } catch (e: Exception) {
            showErrorMessage("Error al crear perfil: ${e.message}")
        }
    }
    
    private suspend fun loadTransactionStats(userId: String) {
        try {
            val transactionsResult = transactionRepository.getTransactionsByUserId(userId)
            if (transactionsResult.isSuccess) {
                val transactions = transactionsResult.getOrNull() ?: emptyList()
                
                // Calcular balance total
                val incomes = transactions.filter { it.type == "income" }.sumOf { it.amount }
                val expenses = transactions.filter { it.type == "expense" }.sumOf { it.amount }
                val totalBalance = incomes - expenses
                
                // Actualizar UI con datos reales
                activity?.runOnUiThread {
                    binding.textViewTotalTransactions.text = transactions.size.toString()
                    binding.textViewCurrentBalance.text = currencyFormat.format(totalBalance)
                    
                    // Cambiar color según balance
                    val balanceColor = if (totalBalance >= 0) {
                        resources.getColor(android.R.color.holo_green_dark, null)
                    } else {
                        resources.getColor(android.R.color.holo_red_dark, null)
                    }
                    binding.textViewCurrentBalance.setTextColor(balanceColor)
                }
            }
        } catch (e: Exception) {
            showErrorMessage("Error al cargar estadísticas: ${e.message}")
        }
    }
    
    private fun updateProfileUI() {
        currentProfile?.let { profile ->
            val currentUser = SupabaseManager.client.auth.currentUserOrNull()
            
            // Actualizar información básica del usuario
            binding.textViewUserName.text = profile.fullName ?: "Usuario"
            binding.textViewUserEmail.text = currentUser?.email ?: "email@ejemplo.com"
            
            // Calcular días en Monifly (simulado por ahora)
            val accountDays = calculateAccountDays()
            binding.textViewAccountDays.text = "$accountDays días"
            
            // Actualizar iniciales del avatar
            val initials = getInitials(profile.fullName ?: currentUser?.email ?: "U")
            binding.textViewUserInitials.text = initials
        }
    }
    
    private fun calculateAccountDays(): Long {
        // Por ahora calculamos desde una fecha fija, en el futuro podríamos guardar la fecha de registro
        val startDate = LocalDate.of(2024, 1, 1) // Fecha simulada de inicio
        val currentDate = LocalDate.now()
        return ChronoUnit.DAYS.between(startDate, currentDate)
    }
    
    private fun getInitials(name: String): String {
        return if (name.contains("@")) {
            // Si es email, tomar primera letra
            name.first().uppercaseChar().toString()
        } else {
            // Si es nombre, tomar primeras letras de nombre y apellido
            name.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .ifEmpty { "U" }
        }
    }
    
    private fun showErrorMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun setupClickListeners() {
        // Configuración
        binding.cardAccountSettings.setOnClickListener {
            navigateToSettings()
        }
        
        // Cerrar sesión
        binding.buttonLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }
    
    private fun navigateToSettings() {
        findNavController().navigate(
            ProfileFragmentDirections.actionProfileFragmentToSettingsFragment()
        )
    }
    
    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Cerrar Sesión") { dialog, _ ->
                performLogout()
                dialog.dismiss()
            }
            .show()
    }
    
    private fun performLogout() {
        lifecycleScope.launch {
            try {
                binding.swipeRefreshLayout.isRefreshing = true
                
                // Cerrar sesión en Supabase
                SupabaseManager.client.auth.signOut()
                
                // Mostrar mensaje de éxito
                Toast.makeText(context, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show()
                
                // Navegar al login y limpiar el stack de navegación
                findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
                
            } catch (e: Exception) {
                showErrorMessage("Error al cerrar sesión: ${e.message}")
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}