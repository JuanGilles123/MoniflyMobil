package com.juangilles123.monifly.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.juangilles123.monifly.R
import com.juangilles123.monifly.databinding.ItemDashboardHeaderBinding
import com.juangilles123.monifly.databinding.ItemTransactionBinding
import com.juangilles123.monifly.databinding.LayoutTotalBalanceBinding
import com.juangilles123.monifly.databinding.ItemHistoryTitleBinding
import java.util.Date

// Define los tipos de ítems que el RecyclerView puede mostrar
sealed class DashboardListItem {
    data class Header(
        val availableMoney: String = "$0.00", // Dinero Disponible (solo transacciones)
        val debtBalance: String = "$0.00",    // Balance en Deudas
        val totalWealth: String = "$0.00",    // Patrimonio Total
        val streakCount: Int = 0              // Racha de días con transacciones
    ) : DashboardListItem()
    object HistoryTitle : DashboardListItem()
    data class Transaction(val data: TransactionViewData) : DashboardListItem()
}

// Datos para cada transacción
data class TransactionViewData(
    val id: String, 
    val description: String,
    val categoryOrType: String,
    val amountFormatted: String,
    val isExpense: Boolean,
    val rawAmount: Double,
    val originalDate: Date,
    val dateFormatted: String // Nuevo campo para la fecha formateada
)

class TransactionAdapter(
    private var items: List<DashboardListItem>,
    private val onAddIncomeClicked: () -> Unit, 
    private val onAddExpenseClicked: () -> Unit,
    private val onDeleteClicked: (transactionId: String, isExpense: Boolean) -> Unit,
    private val onEditClicked: (transactionId: String, isExpense: Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_HISTORY_TITLE = 1
        private const val VIEW_TYPE_TRANSACTION = 2
        // VIEW_TYPE_TOTAL_BALANCE removido ya que ahora está integrado en el header
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DashboardListItem.Header -> VIEW_TYPE_HEADER
            is DashboardListItem.HistoryTitle -> VIEW_TYPE_HISTORY_TITLE
            is DashboardListItem.Transaction -> VIEW_TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemDashboardHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding, onAddIncomeClicked, onAddExpenseClicked)
            }
            VIEW_TYPE_HISTORY_TITLE -> {
                val binding = ItemHistoryTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HistoryTitleViewHolder(binding)
            }
            VIEW_TYPE_TRANSACTION -> {
                val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                TransactionItemViewHolder(binding, onDeleteClicked, onEditClicked)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val currentItem = items[position]) {
            is DashboardListItem.Header -> (holder as HeaderViewHolder).bind(currentItem)
            is DashboardListItem.HistoryTitle -> (holder as HistoryTitleViewHolder).bind()
            is DashboardListItem.Transaction -> (holder as TransactionItemViewHolder).bind(currentItem.data)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<DashboardListItem>) {
        items = newItems
        notifyDataSetChanged() 
    }

    inner class HeaderViewHolder(
        private val binding: ItemDashboardHeaderBinding,
        private val onAddIncomeClickedCallback: () -> Unit,
        private val onAddExpenseClickedCallback: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(headerItem: DashboardListItem.Header) {
            binding.cardAddIncome.setOnClickListener { 
                onAddIncomeClickedCallback()
            }
            
            binding.cardAddExpense.setOnClickListener { 
                onAddExpenseClickedCallback()
            }
            
            // Actualizar los nuevos campos de balance
            binding.availableMoneyAmount.text = headerItem.availableMoney
            binding.debtBalanceAmount.text = headerItem.debtBalance
            binding.totalWealthAmount.text = headerItem.totalWealth

            // Actualizar el texto de la racha
            binding.textStreak.text = itemView.context.getString(R.string.dashboard_streak_days_placeholder, headerItem.streakCount)
        }
    }

    inner class TransactionItemViewHolder(
        private val binding: ItemTransactionBinding,
        private val onDeleteClickedCallback: (transactionId: String, isExpense: Boolean) -> Unit,
        private val onEditClickedCallback: (transactionId: String, isExpense: Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionViewData) {
            binding.textDescription.text = item.description
            binding.textCategory.text = item.categoryOrType
            binding.textAmount.text = item.amountFormatted
            binding.textTransactionDate.text = item.dateFormatted

            // Configurar ícono según el tipo de transacción
            if (item.isExpense) {
                binding.iconType.setImageResource(R.drawable.ic_arrow_downward)
            } else {
                binding.iconType.setImageResource(R.drawable.ic_arrow_upward)
            }

            binding.buttonDeleteTransaction.setOnClickListener { 
                onDeleteClickedCallback(item.id, item.isExpense)
            }
            
            binding.buttonEditTransaction.setOnClickListener { 
                onEditClickedCallback(item.id, item.isExpense)
            }

            // Aplicar colores según el tipo de transacción (similar a las deudas)
            val context = binding.root.context
            val isNightMode = (context.resources.configuration.uiMode and 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES

            if (item.isExpense) {
                // Rojo para gastos
                binding.transactionCard.strokeColor = ContextCompat.getColor(context, R.color.transaction_expense_card_stroke)
                binding.transactionCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.transaction_expense_card_background))
                // Texto contrastante según el modo
                val textColor = if (isNightMode) android.R.color.white else android.R.color.black
                val secondaryTextColor = if (isNightMode) android.R.color.white else R.color.text_secondary
                binding.textDescription.setTextColor(ContextCompat.getColor(context, textColor))
                binding.textAmount.setTextColor(ContextCompat.getColor(context, textColor))
                binding.textCategory.setTextColor(ContextCompat.getColor(context, secondaryTextColor))
                binding.textTransactionDate.setTextColor(ContextCompat.getColor(context, secondaryTextColor))
                
                // Color del ícono para gastos
                binding.iconType.setColorFilter(ContextCompat.getColor(context, if (isNightMode) android.R.color.white else android.R.color.black))
            } else { 
                // Verde para ingresos
                binding.transactionCard.strokeColor = ContextCompat.getColor(context, R.color.transaction_income_card_stroke)
                binding.transactionCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.transaction_income_card_background))
                // Texto contrastante según el modo
                val textColor = if (isNightMode) android.R.color.white else android.R.color.black
                val secondaryTextColor = if (isNightMode) android.R.color.white else R.color.text_secondary
                binding.textDescription.setTextColor(ContextCompat.getColor(context, textColor))
                binding.textAmount.setTextColor(ContextCompat.getColor(context, textColor))
                binding.textCategory.setTextColor(ContextCompat.getColor(context, secondaryTextColor))
                binding.textTransactionDate.setTextColor(ContextCompat.getColor(context, secondaryTextColor))
                
                // Color del ícono para ingresos
                binding.iconType.setColorFilter(ContextCompat.getColor(context, if (isNightMode) android.R.color.white else android.R.color.black))
            }
        }
    }

    inner class HistoryTitleViewHolder(
        private val binding: ItemHistoryTitleBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            // El título ya está definido en el layout XML, no necesita configuración adicional
        }
    }
}
