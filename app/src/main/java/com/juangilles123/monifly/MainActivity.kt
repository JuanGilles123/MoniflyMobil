package com.juangilles123.monifly

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.juangilles123.monifly.databinding.ActivityMainBinding
import com.juangilles123.monifly.data.SupabaseManager
import io.github.jan.supabase.gotrue.auth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val PREFS_NAME = "monifly_settings"
        private const val THEME_KEY = "app_theme"
        private const val THEME_LIGHT = "light"
        private const val THEME_DARK = "dark"
        private const val THEME_SYSTEM = "system"
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar el tema guardado antes de super.onCreate()
        applyStoredTheme()
        
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // AppBarLayout con fitsSystemWindows="true" debería manejar los insets superiores.
        // Por lo tanto, el listener para la Toolbar ya no es necesario aquí.

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = insets.bottom, left = insets.left, right = insets.right)
            windowInsets
        }
        
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        setSupportActionBar(binding.toolbar) // binding.toolbar sigue siendo válido
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        binding.bottomNavView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    // Ocultar AppBar y BottomNav en pantallas de autenticación
                    binding.appBarLayout.visibility = View.GONE
                    binding.bottomNavView.visibility = View.GONE
                }
                else -> {
                    // Mostrar AppBar y BottomNav en pantallas principales
                    binding.appBarLayout.visibility = View.VISIBLE
                    binding.bottomNavView.visibility = View.VISIBLE
                }
            }
        }

        // Verificar autenticación y navegar si es necesario
        checkAuthenticationAndNavigate(navController)
    }

    private fun checkAuthenticationAndNavigate(navController: androidx.navigation.NavController) {
        val currentUser = SupabaseManager.client.auth.currentUserOrNull()
        
        if (currentUser != null) {
            Log.d(TAG, "Usuario autenticado encontrado: ${currentUser.email}")
            // Si hay usuario logueado y estamos en login, navegar al dashboard
            if (navController.currentDestination?.id == R.id.loginFragment) {
                navController.navigate(R.id.action_loginFragment_to_dashboardFragment)
            }
        } else {
            Log.d(TAG, "No hay usuario autenticado")
            // Si no hay usuario y no estamos en pantallas de auth, navegar al login
            val currentDestId = navController.currentDestination?.id
            if (currentDestId != R.id.loginFragment && currentDestId != R.id.registerFragment) {
                navController.navigate(R.id.loginFragment)
            }
        }
    }

    private fun applyStoredTheme() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTheme = sharedPreferences.getString(THEME_KEY, THEME_SYSTEM) ?: THEME_SYSTEM
        
        val nightMode = when (currentTheme) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}