package com.juangilles123.monifly.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.juangilles123.monifly.R
import com.juangilles123.monifly.data.model.Goal
import com.juangilles123.monifly.databinding.FragmentGoalsBinding
import com.juangilles123.monifly.presentation.adapter.GoalAdapter
import com.juangilles123.monifly.presentation.viewmodel.GoalsViewModel
import com.juangilles123.monifly.data.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import com.juangilles123.monifly.presentation.dialog.CreateGoalDialog
import com.juangilles123.monifly.presentation.dialog.ContributeGoalDialog

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalsViewModel by viewModels()
    private lateinit var goalAdapter: GoalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        loadGoals()
    }

    private fun setupRecyclerView() {
        goalAdapter = GoalAdapter(
            onGoalClick = { goal ->
                // TODO: Abrir detalles de la meta
                showGoalDetails(goal)
            },
            onContributeClick = { goal ->
                // TODO: Abrir diálogo para agregar contribución
                showContributeDialog(goal)
            },
            onDeleteClick = { goal ->
                showDeleteConfirmation(goal)
            },
            onToggleComplete = { goal ->
                viewModel.toggleGoalCompletion(goal)
            }
        )

        binding.recyclerViewGoals.apply {
            adapter = goalAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.goals.collect { goals ->
                goalAdapter.submitList(goals)
                updateStatsCard(goals)
                binding.swipeRefreshGoals.isRefreshing = false
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (!isLoading) {
                    binding.swipeRefreshGoals.isRefreshing = false
                }
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    viewModel.clearErrorMessage()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddGoal.setOnClickListener {
            // TODO: Abrir diálogo para crear nueva meta
            showCreateGoalDialog()
        }

        binding.swipeRefreshGoals.setOnRefreshListener {
            loadGoals()
        }
    }

    private fun loadGoals() {
        val userId = SupabaseManager.client.auth.currentUserOrNull()?.id
        if (userId != null) {
            viewModel.loadGoals(userId)
        } else {
            Toast.makeText(context, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatsCard(goals: List<Goal>) {        
        val totalGoals = goals.size
        val completedGoals = goals.count { it.isCompleted }
        val totalSaved = goals.sumOf { it.currentSaved }
        val totalTarget = goals.sumOf { it.targetAmount }
        val overallProgress = if (totalTarget > 0) {
            ((totalSaved / totalTarget) * 100).toInt()
        } else 0

        binding.textViewTotalGoals.text = "$totalGoals"
        binding.textViewCompletedGoals.text = "$completedGoals"
        binding.textViewOverallProgress.text = "$overallProgress%"
        binding.progressBarOverall.progress = overallProgress

        // Formato de moneda
        val numberFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO"))
        binding.textViewTotalSaved.text = numberFormat.format(totalSaved)
        binding.textViewTotalTarget.text = numberFormat.format(totalTarget)
    }

    private fun showGoalDetails(goal: Goal) {
        val message = buildString {
            append("Meta: ${goal.name}\n")
            goal.description?.let { append("Descripción: $it\n") }
            append("Progreso: ${goal.progressPercentage}%\n")
            
            val numberFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO"))
            append("Ahorrado: ${numberFormat.format(goal.currentSaved)}\n")
            append("Meta: ${numberFormat.format(goal.targetAmount)}\n")
            append("Restante: ${numberFormat.format(goal.remainingAmount)}\n")
            
            goal.targetDate?.let { append("Fecha límite: $it\n") }
            append("Estado: ${goal.status ?: "Activa"}")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Detalles de la Meta")
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun showContributeDialog(goal: Goal) {
        val dialog = ContributeGoalDialog.newInstance(goal)
        dialog.show(childFragmentManager, "ContributeGoalDialog")
    }

    private fun showCreateGoalDialog() {
        val dialog = CreateGoalDialog.newInstance()
        dialog.getUserId = { SupabaseManager.client.auth.currentUserOrNull()?.id }
        dialog.show(childFragmentManager, "CreateGoalDialog")
    }

    private fun showDeleteConfirmation(goal: Goal) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Meta")
            .setMessage("¿Estás seguro de que quieres eliminar la meta '${goal.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteGoal(goal.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}