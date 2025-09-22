package com.juangilles123.monifly.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.juangilles123.monifly.databinding.FragmentConfigurationBinding

class ConfigurationFragment : Fragment() {

    private var _binding: FragmentConfigurationBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "monifly_preferences"
        private const val KEY_DARK_MODE = "dark_mode_enabled"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        setupDarkModeSwitch()
    }

    private fun setupDarkModeSwitch() {
        // Cargar el estado actual del modo oscuro
        val isDarkModeEnabled = sharedPreferences.getBoolean(KEY_DARK_MODE, false)
        binding.switchDarkMode.isChecked = isDarkModeEnabled

        // Configurar el listener para cambios en el switch
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // Guardar la preferencia
            sharedPreferences.edit().putBoolean(KEY_DARK_MODE, isChecked).apply()
            
            // Aplicar el cambio de tema inmediatamente
            applyTheme(isChecked)
        }
    }

    private fun applyTheme(isDarkMode: Boolean) {
        val nightMode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        
        // Aplicar el nuevo tema
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}