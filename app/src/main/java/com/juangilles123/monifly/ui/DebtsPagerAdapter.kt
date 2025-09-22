package com.juangilles123.monifly.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.juangilles123.monifly.data.model.Debt
import com.juangilles123.monifly.databinding.PageIOweBinding
import com.juangilles123.monifly.databinding.PageTheyOweMeBinding
import com.juangilles123.monifly.databinding.PagePaidDebtsBinding

class DebtsPagerAdapter(
    private val onEditClicked: (Debt) -> Unit,
    private val onDeleteClicked: (Debt) -> Unit,
    private val onMarkAsPaidClicked: (Debt) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val PAGE_I_OWE = 0
        private const val PAGE_THEY_OWE_ME = 1
        private const val PAGE_PAID_DEBTS = 2
    }

    private var iOweDebts: List<Debt> = emptyList()
    private var theyOweMeDebts: List<Debt> = emptyList()
    private var paidDebts: List<Debt> = emptyList()

    fun updateIOweDebts(debts: List<Debt>) {
        iOweDebts = debts
        notifyItemChanged(PAGE_I_OWE)
    }

    fun updateTheyOweMeDebts(debts: List<Debt>) {
        theyOweMeDebts = debts
        notifyItemChanged(PAGE_THEY_OWE_ME)
    }

    fun updatePaidDebts(debts: List<Debt>) {
        paidDebts = debts
        notifyItemChanged(PAGE_PAID_DEBTS)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            PAGE_I_OWE -> {
                val binding = PageIOweBinding.inflate(inflater, parent, false)
                IOweViewHolder(binding)
            }
            PAGE_THEY_OWE_ME -> {
                val binding = PageTheyOweMeBinding.inflate(inflater, parent, false)
                TheyOweMeViewHolder(binding)
            }
            PAGE_PAID_DEBTS -> {
                val binding = PagePaidDebtsBinding.inflate(inflater, parent, false)
                PaidDebtsViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is IOweViewHolder -> holder.bind(iOweDebts)
            is TheyOweMeViewHolder -> holder.bind(theyOweMeDebts)
            is PaidDebtsViewHolder -> holder.bind(paidDebts)
        }
    }

    inner class IOweViewHolder(private val binding: PageIOweBinding) : RecyclerView.ViewHolder(binding.root) {
        private val adapter = DebtAdapter(onEditClicked, onDeleteClicked, onMarkAsPaidClicked)

        init {
            binding.recyclerViewIOwe.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@IOweViewHolder.adapter
                // Agregar decoración de espaciado
                if (itemDecorationCount == 0) {
                    addItemDecoration(DebtItemDecoration(32))
                }
            }
        }

        fun bind(debts: List<Debt>) {
            adapter.submitList(debts)
        }
    }

    inner class TheyOweMeViewHolder(private val binding: PageTheyOweMeBinding) : RecyclerView.ViewHolder(binding.root) {
        private val adapter = DebtAdapter(onEditClicked, onDeleteClicked, onMarkAsPaidClicked)

        init {
            binding.recyclerViewTheyOwe.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@TheyOweMeViewHolder.adapter
                // Agregar decoración de espaciado
                if (itemDecorationCount == 0) {
                    addItemDecoration(DebtItemDecoration(32))
                }
            }
        }

        fun bind(debts: List<Debt>) {
            adapter.submitList(debts)
        }
    }

    inner class PaidDebtsViewHolder(private val binding: PagePaidDebtsBinding) : RecyclerView.ViewHolder(binding.root) {
        private val adapter = DebtAdapter(onEditClicked, onDeleteClicked, onMarkAsPaidClicked)

        init {
            binding.recyclerViewPaidDebts.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@PaidDebtsViewHolder.adapter
                // Agregar decoración de espaciado
                if (itemDecorationCount == 0) {
                    addItemDecoration(DebtItemDecoration(32))
                }
            }
        }

        fun bind(debts: List<Debt>) {
            adapter.submitList(debts)
            
            // Mostrar/ocultar estado vacío
            if (debts.isEmpty()) {
                binding.recyclerViewPaidDebts.visibility = android.view.View.GONE
                binding.layoutEmptyPaidDebts.visibility = android.view.View.VISIBLE
            } else {
                binding.recyclerViewPaidDebts.visibility = android.view.View.VISIBLE
                binding.layoutEmptyPaidDebts.visibility = android.view.View.GONE
            }
        }
    }
}