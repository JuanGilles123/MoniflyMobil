package com.juangilles123.monifly

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.juangilles123.monifly.data.SupabaseClient
import com.juangilles123.monifly.databinding.ActivityMainBinding
import io.github.jan.supabase.gotrue.auth // Importación añadida
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
    }

    private fun setupListeners() {
        binding.buttonLogin.setOnClickListener {
            val emailValue = binding.editTextEmail.text.toString() // Renombrado para evitar conflicto
            val passwordValue = binding.editTextPassword.text.toString() // Renombrado para evitar conflicto

            if (emailValue.isBlank() || passwordValue.isBlank()) {
                Toast.makeText(this, "Por favor, ingresa correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                binding.buttonLogin.isEnabled = false
                try {
                    SupabaseClient.client.auth.signInWith(Email) {
                        email = emailValue
                        password = passwordValue
                    }
                    Toast.makeText(this@MainActivity, "¡Login exitoso!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.buttonLogin.isEnabled = true
                }
            }
        }
    }
}
