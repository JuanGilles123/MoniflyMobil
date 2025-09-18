package com.juangilles123.monifly.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.juangilles123.monifly.R
import com.juangilles123.monifly.databinding.ItemDashboardHeaderBinding
import com.juangilles123.monifly.databinding.ItemTransactionBinding
import java.util.Date

// Define los tipos de ítems que el RecyclerView puede mostrar
sealed class DashboardListItem {
    data class Header(
        val periodTitle: String = "Balance del Mes",
        val periodBalance: String = "$0.00",
        val totalBalance: String = "$0.00",
        val activePeriod: TimePeriod = TimePeriod.MONTH,
        val streakCount: Int = 0 // Nuevo campo para la racha
    ) : DashboardListItem()
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
    private val onTimePeriodSelected: (TimePeriod) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TRANSACTION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DashboardListItem.Header -> VIEW_TYPE_HEADER
            is DashboardListItem.Transaction -> VIEW_TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemDashboardHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding, onAddIncomeClicked, onAddExpenseClicked, onTimePeriodSelected)
            }
            VIEW_TYPE_TRANSACTION -> {
                val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                TransactionItemViewHolder(binding, onDeleteClicked)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val currentItem = items[position]) {
            is DashboardListItem.Header -> (holder as HeaderViewHolder).bind(currentItem)
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
        private val onAddExpenseClickedCallback: () -> Unit,
        private val onTimePeriodSelectedCallback: (TimePeriod) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var isProgrammaticChange: Boolean = false

        init {
            binding.periodToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked && !isProgrammaticChange) {
                    val selectedPeriod = when (checkedId) {
                        R.id.buttonDay -> TimePeriod.DAY
                        R.id.buttonWeek -> TimePeriod.WEEK
                        R.id.buttonMonth -> TimePeriod.MONTH
                        else -> return@addOnButtonCheckedListener
                    }
                    Log.d("HeaderViewHolder", "Botón seleccionado por usuario: $checkedId, Período: $selectedPeriod")
                    onTimePeriodSelectedCallback(selectedPeriod)
                }
            }
        }

        fun bind(headerItem: DashboardListItem.Header) {
            Log.d("HeaderViewHolder", "Binding Header. Periodo: ${headerItem.activePeriod}, Titulo: ${headerItem.periodTitle}")
            binding.buttonAddIncome.setOnClickListener { onAddIncomeClickedCallback() }
            binding.buttonAddExpense.setOnClickListener { onAddExpenseClickedCallback() }
            
            binding.periodBalanceTitle.text = headerItem.periodTitle
            binding.periodBalanceAmount.text = headerItem.periodBalance
            binding.totalBalanceTitle.text = itemView.context.getString(R.string.dashboard_total_balance_title)
            binding.totalBalanceAmount.text = headerItem.totalBalance

            // Actualizar el texto de la racha
            binding.textStreak.text = itemView.context.getString(R.string.dashboard_streak_days_placeholder, headerItem.streakCount)


            isProgrammaticChange = true 
            when (headerItem.activePeriod) {
                TimePeriod.DAY -> binding.periodToggleGroup.check(R.id.buttonDay)
                TimePeriod.WEEK -> binding.periodToggleGroup.check(R.id.buttonWeek)
                TimePeriod.MONTH -> binding.periodToggleGroup.check(R.id.buttonMonth)
            }
            isProgrammaticChange = false 
        }
    }

    inner class TransactionItemViewHolder(
        private val binding: ItemTransactionBinding,
        private val onDeleteClickedCallback: (transactionId: String, isExpense: Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransactionViewData) {
            binding.textDescription.text = item.description
            binding.textCategory.text = item.categoryOrType
            binding.textAmount.text = item.amountFormatted
            // Asignar la fecha formateada al TextView correspondiente (que añadiremos al XML)
            binding.textTransactionDate.text = item.dateFormatted // Suponiendo que el ID es textTransactionDate

            binding.buttonDeleteTransaction.setOnClickListener { 
                onDeleteClickedCallback(item.id, item.isExpense)
            }
            val context = binding.root.context
            if (item.isExpense) {
                binding.iconType.setImageResource(R.drawable.ic_arrow_downward)
                val redColor = ContextCompat.getColor(context, android.R.color.holo_red_dark)
                binding.iconType.setColorFilter(redColor)
                binding.textAmount.setTextColor(redColor)
            } else { 
                binding.iconType.setImageResource(R.drawable.ic_arrow_upward)
                val greenColor = ContextCompat.getColor(context, android.R.color.holo_green_dark)
                binding.iconType.setColorFilter(greenColor)
                binding.textAmount.setTextColor(greenColor)
            }
        }
    }
}
