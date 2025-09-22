package com.juangilles123.monifly.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.juangilles123.monifly.R
import com.juangilles123.monifly.data.model.Goal
import com.juangilles123.monifly.databinding.ItemGoalBinding
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class GoalAdapter(
    private val onGoalClick: (Goal) -> Unit,
    private val onContributeClick: (Goal) -> Unit,
    private val onDeleteClick: (Goal) -> Unit,
    private val onToggleComplete: (Goal) -> Unit
) : ListAdapter<Goal, GoalAdapter.GoalViewHolder>(GoalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val binding = ItemGoalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GoalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GoalViewHolder(
        private val binding: ItemGoalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(goal: Goal) {
            val context = binding.root.context
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

            // Información básica
            binding.textViewGoalTitle.text = goal.name
            binding.textViewGoalDescription.text = goal.description ?: ""
            binding.textViewGoalCategory.text = goal.status?.uppercase() ?: "ACTIVA"
            
            // Montos
            binding.textViewCurrentAmount.text = numberFormat.format(goal.currentSaved)
            binding.textViewTargetAmount.text = numberFormat.format(goal.targetAmount)
            binding.textViewRemainingAmount.text = numberFormat.format(goal.remainingAmount)
            
            // Progreso
            binding.progressBarGoal.progress = goal.progressPercentage
            binding.textViewProgress.text = "${goal.progressPercentage}%"
            
            // Estado del chip
            when {
                goal.isCompleted -> {
                    binding.chipGoalStatus.text = "Completada"
                    binding.chipGoalStatus.setChipBackgroundColorResource(R.color.income_green)
                }
                goal.progressPercentage >= 75 -> {
                    binding.chipGoalStatus.text = "Casi lista"
                    binding.chipGoalStatus.setChipBackgroundColorResource(R.color.monifly_primary)
                }
                goal.progressPercentage >= 50 -> {
                    binding.chipGoalStatus.text = "En progreso"
                    binding.chipGoalStatus.setChipBackgroundColorResource(R.color.debt_orange)
                }
                else -> {
                    binding.chipGoalStatus.text = "Iniciando"
                    binding.chipGoalStatus.setChipBackgroundColorResource(R.color.text_tertiary)
                }
            }
            
            // Fecha límite
            goal.targetDate?.let { dateString ->
                try {
                    val targetDate = LocalDate.parse(dateString)
                    val today = LocalDate.now()
                    val daysRemaining = ChronoUnit.DAYS.between(today, targetDate)
                    
                    val formattedDate = targetDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    
                    when {
                        daysRemaining > 0 -> {
                            binding.textViewDeadline.text = "Fecha límite: $formattedDate"
                            binding.textViewDeadline.setTextColor(
                                context.getColor(
                                    if (daysRemaining <= 30) R.color.expense_red 
                                    else R.color.text_tertiary
                                )
                            )
                        }
                        daysRemaining == 0L -> {
                            binding.textViewDeadline.text = "¡Vence hoy!"
                            binding.textViewDeadline.setTextColor(context.getColor(R.color.expense_red))
                        }
                        else -> {
                            binding.textViewDeadline.text = "Vencida: $formattedDate"
                            binding.textViewDeadline.setTextColor(context.getColor(R.color.expense_red))
                        }
                    }
                } catch (e: Exception) {
                    binding.textViewDeadline.text = "Fecha: ${goal.targetDate}"
                }
            } ?: run {
                binding.textViewDeadline.text = "Sin fecha límite"
            }
            
            // Botón de completar/reactivar
            if (goal.isCompleted) {
                binding.buttonToggleComplete.text = "Reactivar"
                binding.buttonToggleComplete.setTextColor(context.getColor(R.color.debt_orange))
            } else {
                binding.buttonToggleComplete.text = "Completar"
                binding.buttonToggleComplete.setTextColor(context.getColor(R.color.monifly_primary))
            }
            
            // Listeners
            binding.root.setOnClickListener { onGoalClick(goal) }
            binding.buttonToggleComplete.setOnClickListener { onToggleComplete(goal) }
            binding.buttonDeleteGoal.setOnClickListener { onDeleteClick(goal) }
            
            // Agregar click listener para contribución en toda la card
            binding.root.setOnLongClickListener { 
                onContributeClick(goal)
                true
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
}