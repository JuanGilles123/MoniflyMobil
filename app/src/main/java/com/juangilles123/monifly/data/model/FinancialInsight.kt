package com.juangilles123.monifly.data.model

data class FinancialInsight(
    val title: String,
    val description: String,
    val type: InsightType,
    val importance: InsightImportance = InsightImportance.MEDIUM
)

enum class InsightType {
    SAVINGS,
    SPENDING,
    CATEGORIES,
    GOALS,
    WARNING,
    RECOMMENDATION
}

enum class InsightImportance {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class CategoryExpenseData(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val color: Int
)

data class MonthlyData(
    val month: String,
    val income: Double,
    val expenses: Double
)