package com.juangilles123.monifly.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.juangilles123.monifly.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    // Variable para acceder a todos los elementos del XML de forma segura
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // "Infla" o dibuja el diseño en la pantalla
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Aquí pondremos la lógica del Dashboard, como cargar las transacciones
    }


    // Esto es importante para limpiar la memoria cuando el fragmento se destruye
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}