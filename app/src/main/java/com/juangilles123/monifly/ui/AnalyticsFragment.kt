package com.juangilles123.monifly.ui

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.juangilles123.monifly.R
import com.juangilles123.monifly.data.SupabaseManager
import com.juangilles123.monifly.data.repository.TransactionRepositoryImpl
import com.juangilles123.monifly.data.repository.GoalRepository
import com.juangilles123.monifly.data.repository.DebtRepositoryImpl
import com.juangilles123.monifly.ui.adapter.InsightsAdapter
import com.juangilles123.monifly.data.model.*
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

class AnalyticsFragment : Fragment() {
    
    // Repository
    private lateinit var transactionRepository: TransactionRepositoryImpl
    private lateinit var goalRepository: GoalRepository
    private lateinit var debtRepository: DebtRepositoryImpl
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    
    // Views
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvIncomeAmount: TextView
    private lateinit var tvExpenseAmount: TextView
    private lateinit var tvBalanceAmount: TextView
    private lateinit var pieChartCategories: PieChart
    private lateinit var barChartMonthly: BarChart
    private lateinit var recyclerViewInsights: RecyclerView
    private lateinit var tvCategoriesEmpty: TextView
    private lateinit var tvMonthlyEmpty: TextView
    private lateinit var tvInsightsEmpty: TextView
    private lateinit var toggleGroupPeriod: MaterialButtonToggleGroup
    private lateinit var btnMonth: MaterialButton
    private lateinit var btnYear: MaterialButton
    private lateinit var btnAll: MaterialButton
    
    // Adapters
    private lateinit var insightsAdapter: InsightsAdapter
    
    // Current period
    private var currentPeriod = AnalyticsPeriod.MONTH

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        transactionRepository = TransactionRepositoryImpl()
        goalRepository = GoalRepository()
        debtRepository = DebtRepositoryImpl()
        
        initViews(view)
        setupRecyclerView()
        setupCharts()
        setupPeriodSelector()
        
        loadData()
    }

    private fun initViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefreshAnalytics)
        tvIncomeAmount = view.findViewById(R.id.tvIncomeAmount)
        tvExpenseAmount = view.findViewById(R.id.tvExpenseAmount)
        tvBalanceAmount = view.findViewById(R.id.tvBalanceAmount)
        pieChartCategories = view.findViewById(R.id.pieChartCategories)
        barChartMonthly = view.findViewById(R.id.barChartMonthly)
        recyclerViewInsights = view.findViewById(R.id.recyclerViewInsights)
        tvCategoriesEmpty = view.findViewById(R.id.tvCategoriesEmpty)
        tvMonthlyEmpty = view.findViewById(R.id.tvMonthlyEmpty)
        tvInsightsEmpty = view.findViewById(R.id.tvInsightsEmpty)
        toggleGroupPeriod = view.findViewById(R.id.toggleGroupPeriod)
        btnMonth = view.findViewById(R.id.btnMonth)
        btnYear = view.findViewById(R.id.btnYear)
        btnAll = view.findViewById(R.id.btnAll)
        
        swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    private fun setupRecyclerView() {
        insightsAdapter = InsightsAdapter()
        recyclerViewInsights.apply {
            adapter = insightsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupCharts() {
        setupPieChart()
        setupBarChart()
    }
    
    private fun isDarkMode(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun setupPieChart() {
        val textColor = if (isDarkMode()) {
            Color.WHITE
        } else {
            ContextCompat.getColor(requireContext(), R.color.text_primary)
        }
        
        val holeColor = if (isDarkMode()) {
            ContextCompat.getColor(requireContext(), R.color.background)
        } else {
            Color.WHITE
        }
        
        pieChartCategories.apply {
            // Basic configuration
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            
            // Touch interaction
            dragDecelerationFrictionCoef = 0.95f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            
            // Center text
            centerText = "Categorías"
            setCenterTextSize(16f)
            setCenterTextColor(textColor)
            
            // Legend
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                xEntrySpace = 7f
                yEntrySpace = 0f
                yOffset = 0f
                this.textColor = textColor
            }
            
            // Entry label styling
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(11f)
            
            // Hole
            isDrawHoleEnabled = true
            setHoleColor(holeColor)
            holeRadius = 40f
            transparentCircleRadius = 45f
            setDrawCenterText(true)
        }
    }

    private fun setupBarChart() {
        val textColor = if (isDarkMode()) {
            Color.WHITE
        } else {
            ContextCompat.getColor(requireContext(), R.color.text_secondary)
        }
        
        barChartMonthly.apply {
            // Basic configuration
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(false)
            
            // Legend
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                this.textColor = textColor
            }
            
            // X Axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = 6
                this.textColor = textColor
            }
            
            // Y Axis Left
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                this.textColor = textColor
            }
            
            // Y Axis Right
            axisRight.isEnabled = false
            
            // Animation
            animateY(1400)
        }
    }

    private fun setupPeriodSelector() {
        toggleGroupPeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentPeriod = when (checkedId) {
                    R.id.btnMonth -> AnalyticsPeriod.MONTH
                    R.id.btnYear -> AnalyticsPeriod.YEAR
                    R.id.btnAll -> AnalyticsPeriod.ALL
                    else -> AnalyticsPeriod.MONTH
                }
                loadData()
            }
        }
    }

    private fun loadData() {
        val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
        if (currentUserId == null) {
            // Show default data or error message
            tvIncomeAmount.text = currencyFormatter.format(0.0)
            tvExpenseAmount.text = currencyFormatter.format(0.0)
            tvBalanceAmount.text = currencyFormatter.format(0.0)
            Toast.makeText(context, "Inicia sesión para ver tus analytics", Toast.LENGTH_SHORT).show()
            swipeRefresh.isRefreshing = false
            return
        }

        lifecycleScope.launch {
            try {
                Log.d("AnalyticsFragment", "Loading analytics data for user: $currentUserId")
                
                // Load data from all sources in parallel
                val transactionsResult = transactionRepository.getTransactionsByUserId(currentUserId)
                val goalsResult = goalRepository.getGoalsByUserId(currentUserId)
                val debtsResult = debtRepository.getDebtsByUserId(currentUserId)
                
                transactionsResult.fold(
                    onSuccess = { transactions ->
                        Log.d("AnalyticsFragment", "Transactions loaded: ${transactions.size}")
                        
                        // Process transactions based on period
                        val filteredTransactions = filterTransactionsByPeriod(transactions)
                        
                        // Combine with goals and debts data
                        val goals = goalsResult.getOrNull() ?: emptyList()
                        val debts = debtsResult.getOrNull() ?: emptyList()
                        
                        val analyticsData = processRealDataToAnalytics(filteredTransactions, goals, debts)
                        
                        // Update UI
                        updateSummaryCards(analyticsData)
                        updateCharts(analyticsData)
                        updateInsights(analyticsData)
                        
                        swipeRefresh.isRefreshing = false
                        
                        Log.d("AnalyticsFragment", "Analytics updated - Income: ${analyticsData.totalIncome}, Expenses: ${analyticsData.totalExpenses}")
                    },
                    onFailure = { exception ->
                        Log.e("AnalyticsFragment", "Error loading transactions", exception)
                        
                        // Show fallback data
                        val emptyData = AnalyticsData(0.0, 0.0, emptyList(), emptyList())
                        updateSummaryCards(emptyData)
                        updateCharts(emptyData)
                        updateInsights(emptyData)
                        
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(context, "Error al cargar analytics: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Log.e("AnalyticsFragment", "Exception loading analytics", e)
                swipeRefresh.isRefreshing = false
                Toast.makeText(context, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun filterTransactionsByPeriod(transactions: List<Transaction>): List<Transaction> {
        val currentDate = Calendar.getInstance()
        
        return when (currentPeriod) {
            AnalyticsPeriod.MONTH -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                transactions.filter { transaction ->
                    try {
                        val transactionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .parse(transaction.createdAt.substringBefore('.'))
                        transactionDate?.after(startOfMonth.time) == true
                    } catch (e: Exception) {
                        Log.w("AnalyticsFragment", "Error parsing date: ${transaction.createdAt}", e)
                        false
                    }
                }
            }
            AnalyticsPeriod.YEAR -> {
                val startOfYear = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                transactions.filter { transaction ->
                    try {
                        val transactionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .parse(transaction.createdAt.substringBefore('.'))
                        transactionDate?.after(startOfYear.time) == true
                    } catch (e: Exception) {
                        Log.w("AnalyticsFragment", "Error parsing date: ${transaction.createdAt}", e)
                        false
                    }
                }
            }
            AnalyticsPeriod.ALL -> transactions
        }
    }

    private fun processRealDataToAnalytics(transactions: List<Transaction>, goals: List<Goal>, debts: List<Debt>): AnalyticsData {
        // Calculate totals
        val incomeTransactions = transactions.filter { it.type == Transaction.TYPE_INCOME }
        val expenseTransactions = transactions.filter { it.type == Transaction.TYPE_EXPENSE }
        
        val totalIncome = incomeTransactions.sumOf { it.amount }
        val totalExpenses = expenseTransactions.sumOf { it.amount }
        
        // Generate category expenses data
        val categoryExpenses = generateCategoryExpensesFromTransactions(expenseTransactions)
        
        // Generate monthly data
        val monthlyData = generateMonthlyDataFromTransactions(transactions)
        
        return AnalyticsData(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            categoryExpenses = categoryExpenses,
            monthlyData = monthlyData
        )
    }

    private fun generateCategoryExpensesFromTransactions(expenseTransactions: List<Transaction>): List<CategoryExpenseData> {
        if (expenseTransactions.isEmpty()) return emptyList()
        
        val totalExpenses = expenseTransactions.sumOf { it.amount }
        if (totalExpenses == 0.0) return emptyList()
        
        // Group by category
        val categoryTotals = expenseTransactions.groupBy { transaction ->
            transaction.category?.takeIf { it.isNotBlank() } ?: "Sin categoría"
        }.mapValues { (_, transactions) ->
            transactions.sumOf { it.amount }
        }
        
        // Convert to CategoryExpenseData with colors
        val categoryColors = mapOf(
            "Alimentación" to R.color.expense_red,
            "Transporte" to R.color.monifly_secondary,
            "Entretenimiento" to R.color.monifly_accent,
            "Servicios" to R.color.monifly_primary,
            "Salud" to R.color.income_green,
            "Educación" to R.color.debt_orange,
            "Ropa" to R.color.balance_blue,
            "Sin categoría" to R.color.text_secondary
        )
        
        return categoryTotals.map { (category, amount) ->
            val percentage = ((amount / totalExpenses) * 100).toFloat()
            val colorRes = categoryColors[category] ?: R.color.text_secondary
            val color = ContextCompat.getColor(requireContext(), colorRes)
            
            CategoryExpenseData(
                category = category,
                amount = amount,
                percentage = percentage,
                color = color
            )
        }.sortedByDescending { it.amount }.take(5) // Show top 5 categories
    }

    private fun generateMonthlyDataFromTransactions(transactions: List<Transaction>): List<MonthlyData> {
        if (transactions.isEmpty()) return emptyList()
        
        val monthNames = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", 
                               "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        
        // Group transactions by month
        val transactionsByMonth = transactions.groupBy { transaction ->
            try {
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .parse(transaction.createdAt.substringBefore('.'))
                val calendar = Calendar.getInstance().apply { time = date!! }
                calendar.get(Calendar.MONTH)
            } catch (e: Exception) {
                Log.w("AnalyticsFragment", "Error parsing date for monthly grouping: ${transaction.createdAt}", e)
                -1 // Invalid month
            }
        }.filterKeys { it != -1 } // Remove invalid months
        
        // Calculate monthly totals
        val monthlyTotals = mutableMapOf<Int, Pair<Double, Double>>() // month to (income, expenses)
        
        transactionsByMonth.forEach { (month, monthTransactions) ->
            val income = monthTransactions.filter { it.type == Transaction.TYPE_INCOME }.sumOf { it.amount }
            val expenses = monthTransactions.filter { it.type == Transaction.TYPE_EXPENSE }.sumOf { it.amount }
            monthlyTotals[month] = Pair(income, expenses)
        }
        
        // Convert to MonthlyData list, showing last 6 months with data
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val monthsToShow = (0..5).map { offset ->
            val monthIndex = (currentMonth - offset + 12) % 12
            monthIndex
        }.reversed()
        
        return monthsToShow.mapNotNull { monthIndex ->
            val (income, expenses) = monthlyTotals[monthIndex] ?: return@mapNotNull null
            MonthlyData(
                month = monthNames[monthIndex],
                income = income,
                expenses = expenses
            )
        }.ifEmpty {
            // If no data, show current month with zeros
            listOf(MonthlyData(monthNames[currentMonth], 0.0, 0.0))
        }
    }

    private fun updateSummaryCards(data: AnalyticsData) {
        tvIncomeAmount.text = currencyFormatter.format(data.totalIncome)
        tvExpenseAmount.text = currencyFormatter.format(data.totalExpenses)
        
        val balance = data.totalIncome - data.totalExpenses
        tvBalanceAmount.text = currencyFormatter.format(balance)
        
        // Update balance color based on positive/negative
        val balanceColor = if (balance >= 0) {
            ContextCompat.getColor(requireContext(), R.color.income_green)
        } else {
            ContextCompat.getColor(requireContext(), R.color.expense_red)
        }
        tvBalanceAmount.setTextColor(balanceColor)
    }

    private fun updateCharts(data: AnalyticsData) {
        updatePieChart(data.categoryExpenses)
        updateBarChart(data.monthlyData)
    }

    private fun updatePieChart(categoryData: List<CategoryExpenseData>) {
        if (categoryData.isEmpty()) {
            pieChartCategories.visibility = View.GONE
            tvCategoriesEmpty.visibility = View.VISIBLE
            return
        }
        
        pieChartCategories.visibility = View.VISIBLE
        tvCategoriesEmpty.visibility = View.GONE
        
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()
        
        categoryData.forEach { category ->
            entries.add(PieEntry(category.percentage, category.category))
            colors.add(category.color)
        }
        
        val dataSet = PieDataSet(entries, "Categorías").apply {
            setDrawIcons(false)
            sliceSpace = 3f
            selectionShift = 5f
            setColors(colors)
            valueTextColor = Color.WHITE
            valueTextSize = 11f
            valueFormatter = PercentFormatter()
        }
        
        val pieData = PieData(dataSet)
        pieChartCategories.data = pieData
        pieChartCategories.invalidate()
    }

    private fun updateBarChart(monthlyData: List<MonthlyData>) {
        if (monthlyData.isEmpty()) {
            barChartMonthly.visibility = View.GONE
            tvMonthlyEmpty.visibility = View.VISIBLE
            return
        }
        
        barChartMonthly.visibility = View.VISIBLE
        tvMonthlyEmpty.visibility = View.GONE
        
        val valueTextColor = if (isDarkMode()) {
            Color.WHITE
        } else {
            ContextCompat.getColor(requireContext(), R.color.text_primary)
        }
        
        val incomeEntries = mutableListOf<BarEntry>()
        val expenseEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        
        monthlyData.forEachIndexed { index, data ->
            incomeEntries.add(BarEntry(index.toFloat(), data.income.toFloat()))
            expenseEntries.add(BarEntry(index.toFloat(), data.expenses.toFloat()))
            labels.add(data.month)
        }
        
        val incomeDataSet = BarDataSet(incomeEntries, "Ingresos").apply {
            color = ContextCompat.getColor(requireContext(), R.color.income_green)
            this.valueTextColor = valueTextColor
            valueTextSize = 10f
        }
        
        val expenseDataSet = BarDataSet(expenseEntries, "Gastos").apply {
            color = ContextCompat.getColor(requireContext(), R.color.expense_red)
            this.valueTextColor = valueTextColor
            valueTextSize = 10f
        }
        
        val barData = BarData(incomeDataSet, expenseDataSet).apply {
            barWidth = 0.35f
        }
        
        barChartMonthly.apply {
            data = barData
            groupBars(0f, 0.3f, 0.05f)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size
            xAxis.granularity = 1f
            invalidate()
        }
    }

    private fun updateInsights(data: AnalyticsData) {
        val insights = generateInsights(data)
        
        if (insights.isEmpty()) {
            recyclerViewInsights.visibility = View.GONE
            tvInsightsEmpty.visibility = View.VISIBLE
        } else {
            recyclerViewInsights.visibility = View.VISIBLE
            tvInsightsEmpty.visibility = View.GONE
            insightsAdapter.submitList(insights)
        }
    }

    private fun generateInsights(data: AnalyticsData): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        
        // Balance insight
        val balance = data.totalIncome - data.totalExpenses
        if (balance > 0) {
            insights.add(
                FinancialInsight(
                    title = "Balance Positivo",
                    description = "¡Excelente! Has ahorrado ${currencyFormatter.format(balance)} este período.",
                    type = InsightType.SAVINGS,
                    importance = InsightImportance.HIGH
                )
            )
        } else if (balance < 0) {
            insights.add(
                FinancialInsight(
                    title = "Balance Negativo",
                    description = "Has gastado ${currencyFormatter.format(balance.absoluteValue)} más de lo que has ganado.",
                    type = InsightType.WARNING,
                    importance = InsightImportance.CRITICAL
                )
            )
        }
        
        // Expense ratio insight
        if (data.totalIncome > 0) {
            val expenseRatio = (data.totalExpenses / data.totalIncome) * 100
            when {
                expenseRatio > 90 -> {
                    insights.add(
                        FinancialInsight(
                            title = "Gastos Altos",
                            description = "Estás gastando ${expenseRatio.toInt()}% de tus ingresos. Considera reducir gastos.",
                            type = InsightType.WARNING,
                            importance = InsightImportance.HIGH
                        )
                    )
                }
                expenseRatio > 70 -> {
                    insights.add(
                        FinancialInsight(
                            title = "Gastos Moderados",
                            description = "Gastas ${expenseRatio.toInt()}% de tus ingresos. Intenta ahorrar más.",
                            type = InsightType.RECOMMENDATION,
                            importance = InsightImportance.MEDIUM
                        )
                    )
                }
                else -> {
                    insights.add(
                        FinancialInsight(
                            title = "Gastos Controlados",
                            description = "¡Buen trabajo! Solo gastas ${expenseRatio.toInt()}% de tus ingresos.",
                            type = InsightType.SAVINGS,
                            importance = InsightImportance.MEDIUM
                        )
                    )
                }
            }
        }
        
        // Top category insight
        val topCategory = data.categoryExpenses.maxByOrNull { it.amount }
        if (topCategory != null && topCategory.amount > 0) {
            insights.add(
                FinancialInsight(
                    title = "Categoría Principal",
                    description = "${topCategory.category} representa el ${topCategory.percentage.toInt()}% de tus gastos (${currencyFormatter.format(topCategory.amount)}).",
                    type = InsightType.CATEGORIES,
                    importance = InsightImportance.MEDIUM
                )
            )
        }
        
        // Monthly trend insight
        if (data.monthlyData.size >= 2) {
            val lastMonth = data.monthlyData.last()
            val previousMonth = data.monthlyData[data.monthlyData.size - 2]
            val expenseChange = lastMonth.expenses - previousMonth.expenses
            
            if (expenseChange > 0) {
                insights.add(
                    FinancialInsight(
                        title = "Incremento en Gastos",
                        description = "Tus gastos aumentaron ${currencyFormatter.format(expenseChange)} respecto al mes anterior.",
                        type = InsightType.WARNING,
                        importance = InsightImportance.MEDIUM
                    )
                )
            } else if (expenseChange < 0) {
                insights.add(
                    FinancialInsight(
                        title = "Reducción en Gastos",
                        description = "¡Genial! Redujiste tus gastos en ${currencyFormatter.format(expenseChange.absoluteValue)} respecto al mes anterior.",
                        type = InsightType.SAVINGS,
                        importance = InsightImportance.HIGH
                    )
                )
            }
        }
        
        // Goals insight
        insights.add(
            FinancialInsight(
                title = "Metas de Ahorro",
                description = "Revisa tus metas en la sección de Objetivos para mantener el rumbo hacia tus objetivos financieros.",
                type = InsightType.GOALS,
                importance = InsightImportance.LOW
            )
        )
        
        return insights
    }

    companion object {
        private const val TAG = "AnalyticsFragment"
    }
}

enum class AnalyticsPeriod {
    MONTH, YEAR, ALL
}

data class AnalyticsData(
    val totalIncome: Double,
    val totalExpenses: Double,
    val categoryExpenses: List<CategoryExpenseData>,
    val monthlyData: List<MonthlyData>
)