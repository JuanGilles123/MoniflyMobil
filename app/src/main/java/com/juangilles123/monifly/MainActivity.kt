package com.juangilles123.monifly

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.juangilles123.monifly.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- CONFIGURACIÓN DE NAVEGACIÓN ---
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Conecta la Toolbar
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setupWithNavController(navController)

        // Conecta la BottomNavigationView
        binding.bottomNavView.setupWithNavController(navController)


        // --- ✅ LA LÓGICA CLAVE PARA MOSTRAR Y OCULTAR LA UI ---
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // Si el destino es el LoginFragment...
                R.id.loginFragment -> {
                    // Ocultamos la Toolbar y la barra de navegación inferior
                    binding.toolbar.visibility = View.GONE
                    binding.bottomNavView.visibility = View.GONE
                }
                // Para cualquier otro destino...
                else -> {
                    // Las volvemos a hacer visibles
                    binding.toolbar.visibility = View.VISIBLE
                    binding.bottomNavView.visibility = View.VISIBLE
                }
            }
        }
    }

    // Esta función es necesaria para que el botón de "atrás" en la toolbar funcione
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}