package com.juangilles123.monifly.ui

import android.content.Intent
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
import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.data.model.Goal
import com.juangilles123.monifly.databinding.FragmentGoalsBinding
import com.juangilles123.monifly.presentation.viewmodel.GoalsViewModel
import com.juangilles123.monifly.ui.adapters.GoalsAdapter
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!

    private val goalsViewModel: GoalsViewModel by viewModels()
    private lateinit var goalsAdapter: GoalsAdapter

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
        goalsAdapter = GoalsAdapter(
            onItemClicked = { goal ->
                // Card click no longer triggers edit
            },
            onEditClicked = { goal ->
                showEditGoalDialog(goal)
            },
            onDeleteClicked = { goal ->
                showDeleteConfirmationDialog(goal)
            },
            onToggleClicked = { goal ->
                goalsViewModel.toggleGoalCompletion(goal)
            }
        )

        binding.recyclerViewGoals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = goalsAdapter
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            goalsViewModel.goals.collect { goals ->
                goalsAdapter.submitList(goals)
                updateStats()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            goalsViewModel.isLoading.collect { isLoading ->
                binding.swipeRefreshGoals.isRefreshing = isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            goalsViewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    goalsViewModel.clearErrorMessage()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddGoal.setOnClickListener {
            showCreateGoalDialog()
        }

        binding.swipeRefreshGoals.setOnRefreshListener {
            loadGoals()
        }
    }

    private fun loadGoals() {
        try {
            val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
            if (currentUserId != null) {
                goalsViewModel.loadGoals(currentUserId)
            } else {
                Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al cargar metas: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCreateGoalDialog() {
        try {
            val intent = Intent(requireContext(), AddEditGoalActivity::class.java)
            intent.putExtra(AddEditGoalActivity.EXTRA_IS_EDIT_MODE, false)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al abrir pantalla de creación: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(goal: Goal) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar Meta")
            .setMessage("¿Estás seguro de que quieres eliminar la meta \"${goal.name}\"? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                goalsViewModel.deleteGoal(goal.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditGoalDialog(goal: Goal) {
        try {
            val intent = Intent(requireContext(), AddEditGoalActivity::class.java)
            intent.putExtra(AddEditGoalActivity.EXTRA_IS_EDIT_MODE, true)
            intent.putExtra(AddEditGoalActivity.EXTRA_GOAL_ID, goal.id)
            intent.putExtra(AddEditGoalActivity.EXTRA_GOAL_NAME, goal.name)
            intent.putExtra(AddEditGoalActivity.EXTRA_GOAL_DESCRIPTION, goal.description ?: "")
            intent.putExtra(AddEditGoalActivity.EXTRA_GOAL_TARGET_AMOUNT, goal.targetAmount)
            intent.putExtra(AddEditGoalActivity.EXTRA_GOAL_CURRENT_AMOUNT, goal.currentSaved)
            intent.putExtra(AddEditGoalActivity.EXTRA_GOAL_TARGET_DATE, goal.targetDate)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al abrir pantalla de edición: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStats() {
        try {
            val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            
            binding.textViewTotalGoals.text = goalsViewModel.getTotalGoals().toString()
            binding.textViewCompletedGoals.text = goalsViewModel.getCompletedGoals().toString()
            
            binding.textViewTotalTarget.text = formatter.format(goalsViewModel.getTotalTarget())
            binding.textViewTotalSaved.text = formatter.format(goalsViewModel.getTotalSaved())
            
            val progress = goalsViewModel.getOverallProgress()
            binding.textViewOverallProgress.text = "${progress}%"
            binding.progressBarOverall.progress = progress
        } catch (e: Exception) {
            // Silent error handling for stats update
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh goals when returning from AddEditGoalActivity
        loadGoals()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
