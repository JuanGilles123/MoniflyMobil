package com.juangilles123.monifly.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.juangilles123.monifly.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private companion object {
        const val PREFS_NAME = "monifly_settings"
        const val THEME_KEY = "app_theme"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupThemeSettings()
        updateCurrentThemeDisplay()
    }

    override fun onResume() {
        super.onResume()
        // Actualizar el display del tema cuando se regrese al fragment
        updateCurrentThemeDisplay()
    }

    private fun setupThemeSettings() {
        binding.buttonChangeTheme.setOnClickListener {
            showThemeSelectionDialog()
        }
    }
    
    private fun showSimpleThemeDialog() {
        val themes = arrayOf("Claro", "Oscuro", "Sistema")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tema")
            .setItems(themes) { _, which ->
                val newTheme = when (which) {
                    0 -> THEME_LIGHT
                    1 -> THEME_DARK
                    else -> THEME_SYSTEM
                }
                saveThemePreference(newTheme)
                Toast.makeText(context, "Aplicando ${themes[which]}...", Toast.LENGTH_SHORT).show()
                applyTheme(newTheme)
            }
            .show()
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf("🌞 Modo Claro", "🌙 Modo Oscuro", "🔄 Seguir Sistema")
        val currentTheme = getCurrentTheme()
        val selectedIndex = when (currentTheme) {
            THEME_LIGHT -> 0
            THEME_DARK -> 1
            THEME_SYSTEM -> 2
            else -> 2
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🎨 Seleccionar Tema")
            .setSingleChoiceItems(themes, selectedIndex) { dialog, which ->
                val newTheme = when (which) {
                    0 -> THEME_LIGHT
                    1 -> THEME_DARK
                    2 -> THEME_SYSTEM
                    else -> THEME_SYSTEM
                }
                
                // Guardar la preferencia
                saveThemePreference(newTheme)
                
                // Actualizar display
                updateCurrentThemeDisplay()
                
                // Mostrar confirmación
                Toast.makeText(
                    context,
                    "✅ ${themes[which]} aplicado",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Cerrar diálogo
                dialog.dismiss()
                
                // Aplicar tema después de un delay
                view?.postDelayed({
                    applyTheme(newTheme)
                }, 300)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun getCurrentTheme(): String {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(THEME_KEY, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    private fun saveThemePreference(theme: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(THEME_KEY, theme).apply()
    }

    private fun applyTheme(theme: String) {
        val mode = when (theme) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        // Aplicar el tema inmediatamente
        AppCompatDelegate.setDefaultNightMode(mode)
        
        // También recrear la actividad para que el cambio sea inmediato y completo
        requireActivity().recreate()
    }

    private fun updateCurrentThemeDisplay() {
        val currentTheme = getCurrentTheme()
        val displayText = when (currentTheme) {
            THEME_LIGHT -> "Modo Claro"
            THEME_DARK -> "Modo Oscuro"
            THEME_SYSTEM -> "Seguir Sistema"
            else -> "Seguir Sistema"
        }
        
        binding.textViewCurrentTheme.text = displayText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}