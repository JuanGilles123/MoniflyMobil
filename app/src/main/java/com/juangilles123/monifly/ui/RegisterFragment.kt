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
import com.juangilles123.monifly.databinding.FragmentRegisterBinding
import com.juangilles123.monifly.data.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "RegisterFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.buttonRegister.setOnClickListener {
            performRegistration()
        }

        binding.textViewLogin.setOnClickListener {
            // Navegar de vuelta al login
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun performRegistration() {
        val fullName = binding.editTextFullName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

        // Validaciones
        if (fullName.isEmpty()) {
            binding.editTextFullName.error = "El nombre es requerido"
            return
        }

        if (fullName.length < 2) {
            binding.editTextFullName.error = "El nombre debe tener al menos 2 caracteres"
            return
        }

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

        if (confirmPassword.isEmpty()) {
            binding.editTextConfirmPassword.error = "Confirma tu contraseña"
            return
        }

        if (password != confirmPassword) {
            binding.editTextConfirmPassword.error = "Las contraseñas no coinciden"
            return
        }

        // Limpiar errores
        clearErrors()

        // Mostrar indicador de carga
        setLoadingState(true)

        // Realizar registro
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Intentando registro con email: $email")
                
                SupabaseManager.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                Log.d(TAG, "Registro exitoso")
                
                // Registro exitoso
                setLoadingState(false)
                
                Toast.makeText(
                    requireContext(), 
                    "¡Cuenta creada exitosamente! Revisa tu email para confirmar tu cuenta.", 
                    Toast.LENGTH_LONG
                ).show()
                
                // Navegar al dashboard (Supabase ya maneja la sesión automáticamente)
                navigateToDashboard()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error en registro", e)
                setLoadingState(false)
                
                val errorMessage = when {
                    e.message?.contains("User already registered") == true || 
                    e.message?.contains("already exists") == true -> 
                        "Este email ya está registrado. Intenta iniciar sesión."
                    e.message?.contains("Invalid email") == true -> 
                        "Email inválido. Verifica el formato."
                    e.message?.contains("Password should be") == true -> 
                        "La contraseña debe cumplir con los requisitos mínimos."
                    e.message?.contains("Signup is disabled") == true -> 
                        "El registro está temporalmente deshabilitado."
                    else -> "Error al crear la cuenta: ${e.message}"
                }
                
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun clearErrors() {
        binding.editTextFullName.error = null
        binding.editTextEmail.error = null
        binding.editTextPassword.error = null
        binding.editTextConfirmPassword.error = null
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.buttonRegister.isEnabled = !isLoading
        binding.editTextFullName.isEnabled = !isLoading
        binding.editTextEmail.isEnabled = !isLoading
        binding.editTextPassword.isEnabled = !isLoading
        binding.editTextConfirmPassword.isEnabled = !isLoading
        binding.textViewLogin.isEnabled = !isLoading
        
        if (isLoading) {
            binding.buttonRegister.text = "Creando cuenta..."
        } else {
            binding.buttonRegister.text = "Crear Cuenta"
        }
    }

    private fun navigateToDashboard() {
        try {
            findNavController().navigate(R.id.action_registerFragment_to_dashboardFragment)
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