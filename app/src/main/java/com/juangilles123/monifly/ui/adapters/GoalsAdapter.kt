package com.juangilles123.monifly.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.juangilles123.monifly.R
import com.juangilles123.monifly.data.model.Goal
import com.juangilles123.monifly.databinding.ItemGoalBinding
import java.text.NumberFormat
import java.util.*

class GoalsAdapter(
    private val onItemClicked: (Goal) -> Unit,
    private val onEditClicked: (Goal) -> Unit,
    private val onDeleteClicked: (Goal) -> Unit,
    private val onToggleClicked: (Goal) -> Unit
) : ListAdapter<Goal, GoalsAdapter.GoalViewHolder>(GoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = getItem(position)
        holder.bind(goal, onItemClicked, onEditClicked, onDeleteClicked, onToggleClicked)
    }

    inner class GoalViewHolder(private val binding: ItemGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            goal: Goal,
            onItemClicked: (Goal) -> Unit,
            onEditClicked: (Goal) -> Unit,
            onDeleteClicked: (Goal) -> Unit,
            onToggleClicked: (Goal) -> Unit
        ) {
            try {
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                
                // Información básica
                binding.textViewGoalTitle.text = goal.title
                binding.textViewGoalDescription.text = goal.description ?: "Sin descripción"
                binding.textViewGoalCategory.text = goal.category ?: "General"
                
                // Amounts
                binding.textViewCurrentAmount.text = currencyFormat.format(goal.currentAmount)
                binding.textViewTargetAmount.text = currencyFormat.format(goal.targetAmount)
                binding.textViewRemainingAmount.text = currencyFormat.format(goal.remainingAmount)
                
                // Progress
                binding.progressBarGoal.progress = goal.progressPercentage
                binding.textViewProgress.text = "${goal.progressPercentage}%"
                
                // Deadline
                binding.textViewDeadline.text = goal.deadline?.let { "Fecha límite: $it" } ?: "Sin fecha límite"
                
                // Estado de completado
                if (goal.isCompleted) {
                    binding.chipGoalStatus.text = "Completada"
                    binding.chipGoalStatus.setChipBackgroundColorResource(R.color.income_green_light)
                    binding.chipGoalStatus.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.income_green)
                    )
                    binding.progressBarGoal.progress = 100
                    binding.textViewProgress.text = "100%"
                } else {
                    binding.chipGoalStatus.text = "En progreso"
                    binding.chipGoalStatus.setChipBackgroundColorResource(R.color.debt_status_background)
                    binding.chipGoalStatus.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.debt_status_text)
                    )
                }

                // Listeners
                // Remove click listener from card so only edit button triggers edit mode
                binding.root.setOnClickListener(null)

                binding.buttonEditGoal.setOnClickListener {
                    onEditClicked(goal)
                }

                binding.buttonToggleComplete.setOnClickListener {
                    onToggleClicked(goal)
                }

                binding.buttonDeleteGoal.setOnClickListener {
                    onDeleteClicked(goal)
                }

                // Update toggle button text
                binding.buttonToggleComplete.text = if (goal.isCompleted) "Reactivar" else "Completar"

            } catch (e: Exception) {
                e.printStackTrace()
                // Configuración mínima en caso de error
                binding.textViewGoalTitle.text = goal.title
                binding.textViewGoalDescription.text = "Error al cargar"
                binding.progressBarGoal.progress = 0
            }
        }
    }
}

class GoalDiffCallback : DiffUtil.ItemCallback<Goal>() {
    override fun areItemsTheSame(oldItem: Goal, newItem: Goal): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Goal, newItem: Goal): Boolean {
        return oldItem == newItem
    }
}