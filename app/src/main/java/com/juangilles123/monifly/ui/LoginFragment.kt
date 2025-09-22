package com.juangilles123.monifly.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.juangilles123.monifly.R
import com.juangilles123.monifly.databinding.FragmentLoginBinding
import com.juangilles123.monifly.data.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "LoginFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        checkIfUserIsLoggedIn()
    }

    private fun setupClickListeners() {
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }

        binding.textViewCreateAccount.setOnClickListener {
            // Navegar a la pantalla de registro
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.textViewForgotPassword.setOnClickListener {
            // TODO: Implementar recuperación de contraseña
            Toast.makeText(requireContext(), "Funcionalidad próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkIfUserIsLoggedIn() {
        // Si ya hay un usuario logueado, navegar al dashboard
        val currentUser = SupabaseManager.client.auth.currentUserOrNull()
        if (currentUser != null) {
            Log.d(TAG, "Usuario ya autenticado: ${currentUser.email}")
            navigateToDashboard()
        }
    }

    private fun performLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        // Validaciones
        if (email.isEmpty()) {
            binding.editTextEmail.error = "El email es requerido"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.error = "Email inválido"
            return
        }

        if (password.isEmpty()) {
            binding.editTextPassword.error = "La contraseña es requerida"
            return
        }

        if (password.length < 6) {
            binding.editTextPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        // Limpiar errores
        binding.editTextEmail.error = null
        binding.editTextPassword.error = null

        // Mostrar indicador de carga
        setLoadingState(true)

        // Realizar login
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Intentando login con email: $email")
                
                SupabaseManager.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                Log.d(TAG, "Login exitoso")
                
                // Login exitoso, navegar al dashboard
                setLoadingState(false)
                navigateToDashboard()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error en login", e)
                setLoadingState(false)
                
                val errorMessage = when {
                    e.message?.contains("Invalid login credentials") == true -> 
                        "Credenciales inválidas. Verifica tu email y contraseña."
                    e.message?.contains("Email not confirmed") == true -> 
                        "Confirma tu email antes de iniciar sesión."
                    e.message?.contains("Too many requests") == true -> 
                        "Demasiados intentos. Intenta más tarde."
                    else -> "Error al iniciar sesión: ${e.message}"
                }
                
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.buttonLogin.isEnabled = !isLoading
        binding.editTextEmail.isEnabled = !isLoading
        binding.editTextPassword.isEnabled = !isLoading
        binding.textViewCreateAccount.isEnabled = !isLoading
        binding.textViewForgotPassword.isEnabled = !isLoading
        
        if (isLoading) {
            binding.buttonLogin.text = "Iniciando sesión..."
        } else {
            binding.buttonLogin.text = "Iniciar Sesión"
        }
    }

    private fun navigateToDashboard() {
        try {
            findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
        } catch (e: Exception) {
            Log.e(TAG, "Error navegando al dashboard", e)
            // Si hay error en la navegación, recargar la actividad principal
            requireActivity().recreate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}