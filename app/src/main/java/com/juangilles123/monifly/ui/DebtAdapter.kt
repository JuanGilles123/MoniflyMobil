package com.juangilles123.monifly.ui

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
    private val onItemClicked: (Debt) -> Unit,
    private val onDeleteClicked: (Debt) -> Unit
) : ListAdapter<Debt, DebtAdapter.DebtViewHolder>(DebtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val binding = ItemDebtBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DebtViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        val debt = getItem(position)
        holder.bind(debt, onItemClicked, onDeleteClicked)
    }

    inner class DebtViewHolder(private val binding: ItemDebtBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(debt: Debt, onItemClicked: (Debt) -> Unit, onDeleteClicked: (Debt) -> Unit) {
            binding.textViewDebtDescription.text = debt.description
            
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            binding.textViewDebtAmount.text = currencyFormat.format(debt.amount)

            val personPrefix = if (debt.debtType == DebtType.I_OWE) "A: " else "De: "
            binding.textViewDebtPerson.text = personPrefix + debt.personName

            // Condición modificada para manejar Boolean?
            if (debt.isPaid == true) {
                binding.textViewDebtDescription.paintFlags = binding.textViewDebtDescription.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.textViewDebtAmount.paintFlags = binding.textViewDebtAmount.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                val disabledColor = ContextCompat.getColor(binding.root.context, R.color.text_disabled)
                binding.textViewDebtDescription.setTextColor(disabledColor)
                binding.textViewDebtAmount.setTextColor(disabledColor)
                binding.textViewDebtPerson.setTextColor(disabledColor)
            } else {
                binding.textViewDebtDescription.paintFlags = binding.textViewDebtDescription.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.textViewDebtAmount.paintFlags = binding.textViewDebtAmount.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                val primaryTextColor = ContextCompat.getColor(binding.root.context, android.R.color.black) // O tu color primario de texto
                binding.textViewDebtDescription.setTextColor(primaryTextColor)
                binding.textViewDebtAmount.setTextColor(primaryTextColor)
                binding.textViewDebtPerson.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)) // O tu color secundario
            }

            binding.root.setOnClickListener {
                onItemClicked(debt)
            }

            binding.imageViewDeleteDebt.setOnClickListener {
                onDeleteClicked(debt)
            }
        }
    }
}