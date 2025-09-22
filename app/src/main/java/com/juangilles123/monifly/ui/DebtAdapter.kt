package com.juangilles123.monifly.ui

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.juangilles123.monifly.R
import com.juangilles123.monifly.data.model.Debt
import com.juangilles123.monifly.data.model.DebtType
import com.juangilles123.monifly.databinding.ItemDebtBinding
import java.text.NumberFormat
import java.util.Locale

class DebtAdapter(
    private val onEditClicked: (Debt) -> Unit,
    private val onDeleteClicked: (Debt) -> Unit,
    private val onMarkAsPaidClicked: (Debt) -> Unit
) : ListAdapter<Debt, DebtAdapter.DebtViewHolder>(DebtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val binding = ItemDebtBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DebtViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        val debt = getItem(position)
        holder.bind(debt, onEditClicked, onDeleteClicked, onMarkAsPaidClicked)
    }

    inner class DebtViewHolder(private val binding: ItemDebtBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(debt: Debt, onEditClicked: (Debt) -> Unit, onDeleteClicked: (Debt) -> Unit, onMarkAsPaidClicked: (Debt) -> Unit) {
            try {
                // Mostrar nombre completo de la deuda o persona
                val displayName = if (!debt.description.isNullOrBlank()) {
                    debt.description
                } else if (!debt.personName.isNullOrBlank()) {
                    debt.personName
                } else {
                    debt.title ?: "Sin nombre"
                }
                binding.textViewDebtName.text = displayName
                
                // Formatear y mostrar el monto
                try {
                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                    // Para deudas pagadas, mostrar el monto original; para activas, mostrar el monto restante
                    val isPaid = debt.isPaid == true || debt.status?.lowercase() == "paid"
                    val displayAmount = if (isPaid) debt.originalAmount else debt.remainingAmount
                    binding.textViewDebtAmount.text = currencyFormat.format(displayAmount)
                } catch (e: Exception) {
                    val displayAmount = if (debt.isPaid == true || debt.status?.lowercase() == "paid") {
                        debt.originalAmount
                    } else {
                        debt.remainingAmount
                    }
                    binding.textViewDebtAmount.text = "$ $displayAmount"
                }

                // Configurar el color de la tarjeta según el tipo de deuda
                val context = binding.root.context
                val isNightMode = (context.resources.configuration.uiMode and 
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                
                when (debt.debtType) {
                    DebtType.I_OWE -> {
                        // Rojo para "Yo debo"
                        binding.debtCard.strokeColor = ContextCompat.getColor(context, R.color.debt_owe_card_stroke)
                        binding.debtCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.debt_owe_card_background))
                        // Texto contrastante según el modo
                        val textColor = if (isNightMode) android.R.color.white else android.R.color.black
                        binding.textViewDebtName.setTextColor(ContextCompat.getColor(context, textColor))
                        binding.textViewDebtAmount.setTextColor(ContextCompat.getColor(context, textColor))
                    }
                    DebtType.THEY_OWE_ME -> {
                        // Verde para "Me deben"
                        binding.debtCard.strokeColor = ContextCompat.getColor(context, R.color.debt_owed_card_stroke)
                        binding.debtCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.debt_owed_card_background))
                        // Texto contrastante según el modo
                        val textColor = if (isNightMode) android.R.color.white else android.R.color.black
                        binding.textViewDebtName.setTextColor(ContextCompat.getColor(context, textColor))
                        binding.textViewDebtAmount.setTextColor(ContextCompat.getColor(context, textColor))
                    }
                    else -> {
                        // Color neutro por defecto
                        binding.debtCard.strokeColor = ContextCompat.getColor(context, R.color.card_stroke)
                        binding.debtCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
                        binding.textViewDebtName.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        binding.textViewDebtAmount.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    }
                }

                // Configurar el estado de la deuda
                val isPaid = debt.isPaid == true || debt.status?.lowercase() == "paid"
                
                when {
                    isPaid -> {
                        binding.chipDebtStatus.text = "Pagado"
                        binding.chipDebtStatus.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(binding.root.context, R.color.income_green)
                        )
                        binding.chipDebtStatus.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                        
                        // Aplicar opacidad reducida para deudas pagadas
                        binding.root.alpha = 0.7f
                    }
                    debt.status?.lowercase() == "overdue" -> {
                        binding.chipDebtStatus.text = "Vencido"
                        binding.chipDebtStatus.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(binding.root.context, R.color.expense_red)
                        )
                        binding.chipDebtStatus.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                        binding.root.alpha = 1.0f
                    }
                    debt.status?.lowercase() == "cancelled" -> {
                        binding.chipDebtStatus.text = "Cancelado"
                        binding.chipDebtStatus.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(binding.root.context, R.color.text_secondary)
                        )
                        binding.chipDebtStatus.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                        binding.root.alpha = 0.7f
                    }
                    else -> {
                        binding.chipDebtStatus.text = "Pendiente"
                        binding.chipDebtStatus.chipBackgroundColor = ColorStateList.valueOf(
                            ContextCompat.getColor(binding.root.context, R.color.monifly_primary)
                        )
                        binding.chipDebtStatus.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.white))
                        binding.root.alpha = 1.0f
                    }
                }

                // Configurar listeners de los botones
                // Mostrar/ocultar botón de marcar como pagado
                if (isPaid) {
                    binding.markAsPaidButton.visibility = android.view.View.GONE
                } else {
                    binding.markAsPaidButton.visibility = android.view.View.VISIBLE
                    binding.markAsPaidButton.setOnClickListener {
                        onMarkAsPaidClicked(debt)
                    }
                }

                binding.editButton.setOnClickListener {
                    onEditClicked(debt)
                }

                binding.deleteButton.setOnClickListener {
                    onDeleteClicked(debt)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Configuración mínima para evitar crashes
                binding.textViewDebtName.text = "Error al cargar deuda"
                binding.textViewDebtAmount.text = "$0"
                binding.chipDebtStatus.text = "Error"
            }
        }
    }
}