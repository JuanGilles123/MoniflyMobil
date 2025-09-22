package com.juangilles123.monifly.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.juangilles123.monifly.R
import com.juangilles123.monifly.data.model.FinancialInsight
import com.juangilles123.monifly.data.model.InsightImportance
import com.juangilles123.monifly.data.model.InsightType

class InsightsAdapter : ListAdapter<FinancialInsight, InsightsAdapter.InsightViewHolder>(InsightDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsightViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_insight, parent, false)
        return InsightViewHolder(view)
    }

    override fun onBindViewHolder(holder: InsightViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InsightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle = itemView.findViewById<TextView>(R.id.tvInsightTitle)
        private val tvDescription = itemView.findViewById<TextView>(R.id.tvInsightDescription)
        private val tvIcon = itemView.findViewById<TextView>(R.id.tvInsightIcon)
        private val cardInsight = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardInsight)

        fun bind(insight: FinancialInsight) {
            tvTitle.text = insight.title
            tvDescription.text = insight.description

            // Set icon based on type
            val iconText = when (insight.type) {
                InsightType.SAVINGS -> "💰"
                InsightType.SPENDING -> "💳"
                InsightType.CATEGORIES -> "📊"
                InsightType.GOALS -> "🎯"
                InsightType.WARNING -> "⚠️"
                InsightType.RECOMMENDATION -> "💡"
            }
            tvIcon.text = iconText

            // Set card background and stroke based on importance
            val context = itemView.context
            when (insight.importance) {
                InsightImportance.LOW -> {
                    cardInsight.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
                    cardInsight.strokeColor = ContextCompat.getColor(context, R.color.text_tertiary)
                    cardInsight.strokeWidth = 1
                }
                InsightImportance.MEDIUM -> {
                    cardInsight.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
                    cardInsight.strokeColor = ContextCompat.getColor(context, R.color.monifly_secondary)
                    cardInsight.strokeWidth = 2
                }
                InsightImportance.HIGH -> {
                    cardInsight.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
                    cardInsight.strokeColor = ContextCompat.getColor(context, R.color.monifly_accent)
                    cardInsight.strokeWidth = 2
                }
                InsightImportance.CRITICAL -> {
                    cardInsight.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
                    cardInsight.strokeColor = ContextCompat.getColor(context, R.color.expense_red)
                    cardInsight.strokeWidth = 3
                }
            }
            
            // Use standard text colors that adapt to theme
            tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            tvDescription.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }
    }

    private class InsightDiffCallback : DiffUtil.ItemCallback<FinancialInsight>() {
        override fun areItemsTheSame(oldItem: FinancialInsight, newItem: FinancialInsight): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: FinancialInsight, newItem: FinancialInsight): Boolean {
            return oldItem == newItem
        }
    }
}