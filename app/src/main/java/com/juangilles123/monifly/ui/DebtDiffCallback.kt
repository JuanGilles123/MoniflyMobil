package com.juangilles123.monifly.ui

import androidx.recyclerview.widget.DiffUtil
import com.juangilles123.monifly.data.model.Debt

class DebtDiffCallback : DiffUtil.ItemCallback<Debt>() {
    override fun areItemsTheSame(oldItem: Debt, newItem: Debt): Boolean {
        // Los ítems son los mismos si sus IDs son iguales
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Debt, newItem: Debt): Boolean {
        // El contenido es el mismo si los objetos son iguales (Debt es una data class)
        return oldItem == newItem
    }
}