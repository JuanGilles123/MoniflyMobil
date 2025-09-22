package com.juangilles123.monifly.presentation.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.juangilles123.monifly.data.model.Goal
import com.juangilles123.monifly.databinding.DialogContributeGoalBinding
import com.juangilles123.monifly.presentation.viewmodel.GoalsViewModel
import java.text.NumberFormat
import java.util.*
class ContributeGoalDialog : DialogFragment() {

    private var _binding: DialogContributeGoalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalsViewModel by viewModels({ requireParentFragment() })
    private lateinit var goal: Goal

    companion object {
        private const val ARG_GOAL_ID = "goal_id"
        private const val ARG_GOAL_NAME = "goal_name"
        private const val ARG_GOAL_CURRENT = "goal_current"
        private const val ARG_GOAL_TARGET = "goal_target"
        private const val ARG_GOAL_PROGRESS = "goal_progress"

        fun newInstance(goalId: String): ContributeGoalDialog {
            return ContributeGoalDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_GOAL_ID, goalId)
                }
            }
        }

        fun newInstance(goal: Goal): ContributeGoalDialog {
            return ContributeGoalDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_GOAL_ID, goal.id)
                    putString(ARG_GOAL_NAME, goal.name)
                    putDouble(ARG_GOAL_CURRENT, goal.currentSaved)
                    putDouble(ARG_GOAL_TARGET, goal.targetAmount)
                    putInt(ARG_GOAL_PROGRESS, goal.progressPercentage)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            goal = Goal(
                id = it.getString(ARG_GOAL_ID) ?: "",
                userId = "",
                name = it.getString(ARG_GOAL_NAME) ?: "",
                description = null,
                targetAmount = it.getDouble(ARG_GOAL_TARGET),
                currentSaved = it.getDouble(ARG_GOAL_CURRENT),
                targetDate = null,
                status = "active",
                createdAt = null,
                updatedAt = null
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogContributeGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGoalInfo()
        setupClickListeners()
        setupDialog()
    }

    private fun setupDialog() {
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupGoalInfo() {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        
        binding.tvGoalName.text = goal.name
        binding.tvCurrentAmount.text = "Actual: ${numberFormat.format(goal.currentSaved)}"
        binding.tvTargetAmount.text = "Meta: ${numberFormat.format(goal.targetAmount)}"
        binding.tvRemainingAmount.text = "Falta: ${numberFormat.format(goal.remainingAmount)}"
        
        binding.progressBar.progress = goal.progressPercentage
        binding.tvProgressPercentage.text = "${goal.progressPercentage}%"
        
        // Configurar botones de montos sugeridos
        setupSuggestedAmounts()
    }

    private fun setupSuggestedAmounts() {
        val remaining = goal.remainingAmount
        val suggestions = listOf(
            remaining * 0.1, // 10% de lo que falta
            remaining * 0.25, // 25% de lo que falta
            remaining * 0.5, // 50% de lo que falta
            remaining // 100% de lo que falta (completar)
        )

        val numberFormat = NumberFormat.getNumberInstance(Locale("es", "CO"))
        
        binding.btnSuggestion1.text = numberFormat.format(suggestions[0].toInt())
        binding.btnSuggestion2.text = numberFormat.format(suggestions[1].toInt())
        binding.btnSuggestion3.text = numberFormat.format(suggestions[2].toInt())
        binding.btnSuggestion4.text = "Completar"
        
        binding.btnSuggestion1.setOnClickListener {
            binding.etContributionAmount.setText(suggestions[0].toInt().toString())
        }
        
        binding.btnSuggestion2.setOnClickListener {
            binding.etContributionAmount.setText(suggestions[1].toInt().toString())
        }
        
        binding.btnSuggestion3.setOnClickListener {
            binding.etContributionAmount.setText(suggestions[2].toInt().toString())
        }
        
        binding.btnSuggestion4.setOnClickListener {
            binding.etContributionAmount.setText(remaining.toInt().toString())
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnContribute.setOnClickListener {
            addContribution()
        }
    }

    private fun addContribution() {
        val amountText = binding.etContributionAmount.text.toString().trim()

        if (amountText.isEmpty()) {
            binding.etContributionAmount.error = "Ingresa un monto"
            return
        }

        val amount = try {
            amountText.toDouble()
        } catch (e: NumberFormatException) {
            binding.etContributionAmount.error = "Ingresa un monto válido"
            return
        }

        if (amount <= 0) {
            binding.etContributionAmount.error = "El monto debe ser mayor a 0"
            return
        }

        if (amount > goal.remainingAmount) {
            binding.etContributionAmount.error = "El monto supera lo que falta para completar la meta"
            return
        }

        // Calcular el nuevo monto total
        val newTotal = goal.currentSaved + amount
        
        // Actualizar la meta usando el ViewModel
        viewModel.addContribution(goal.id, newTotal)
        
        Toast.makeText(
            context, 
            "Contribución agregada exitosamente", 
            Toast.LENGTH_SHORT
        ).show()
        
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}