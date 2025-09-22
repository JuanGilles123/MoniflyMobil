package com.juangilles123.monifly.utils

import android.util.Log
import com.juangilles123.monifly.data.model.Debt
import com.juangilles123.monifly.data.model.Transaction
import com.juangilles123.monifly.data.repository.DebtRepositoryInterface
import com.juangilles123.monifly.data.SupabaseManager
import io.github.jan.supabase.gotrue.auth

/**
 * NUEVA LÓGICA DE BALANCES SEGÚN ESPECIFICACIONES:
 * 
 * PANTALLA INICIO:
 * - Disponible: Dinero actual del usuario (todas las transacciones)
 * - Patrimonio Total: Transacciones + TODAS las deudas (pagadas o no)
 * 
 * PANTALLA DEUDAS:
 * - Me deben/Yo debo: Solo deudas ACTIVAS (para gestión diaria)
 * - Balance de Deudas: TODAS las deudas (para impacto histórico)
 * - Patrimonio Total: Mismo que pantalla inicio
 */
object BalanceCalculator {
    
    /**
     * DISPONIBLE: Dinero que realmente tiene el usuario ahora
     * Incluye TODAS las transacciones (reales + automáticas)
     */
    fun calculateAvailableMoney(transactions: List<Transaction>): Double {
        val totalIncomes = transactions.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.type == "expense" }.sumOf { it.amount }
        val available = totalIncomes - totalExpenses
        
        Log.d("BalanceCalculator", 
            "💰 DISPONIBLE: Ingresos=$totalIncomes - Gastos=$totalExpenses = $available"
        )
        
        return available
    }
    
    /**
     * BALANCE TRANSACCIONES: Solo transacciones reales (sin automáticas de deudas)
     * Para cálculo del patrimonio total
     */
    fun calculateTransactionsBalance(transactions: List<Transaction>): Double {
        // Filtrar transacciones reales (no automáticas, no de deudas)
        val realIncomes = transactions.filter { transaction ->
            transaction.type == "income" && 
            transaction.category != "Deudas" &&
            !isAutomaticTransaction(transaction)
        }
        
        val realExpenses = transactions.filter { transaction ->
            transaction.type == "expense" && 
            transaction.category != "Deudas" &&
            !isAutomaticTransaction(transaction)
        }
        
        val totalRealIncomes = realIncomes.sumOf { it.amount }
        val totalRealExpenses = realExpenses.sumOf { it.amount }
        val transactionsBalance = totalRealIncomes - totalRealExpenses
        
        Log.d("BalanceCalculator", 
            "📊 BALANCE TRANSACCIONES: Ingresos reales=$totalRealIncomes - Gastos reales=$totalRealExpenses = $transactionsBalance"
        )
        
        return transactionsBalance
    }
    
    /**
     * BALANCE DEUDAS ACTIVAS: Solo para mostrar "Me deben/Yo debo" en gestión diaria
     * Usa remainingAmount de deudas NO pagadas
     */
    suspend fun calculateActiveDebtsBalance(debtRepository: DebtRepositoryInterface): ActiveDebtsData {
        return try {
            val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                Log.w("BalanceCalculator", "Usuario no autenticado")
                return ActiveDebtsData(0.0, 0.0, 0.0)
            }

            val debtsResult = debtRepository.getDebtsByUserId(currentUserId)
            debtsResult.fold(
                onSuccess = { debts ->
                    // Solo deudas activas (no pagadas)
                    val activeDebts = debts.filter { debt -> 
                        debt.status?.lowercase() != "paid" 
                    }
                    
                    val totalOwedToMe = activeDebts.filter { debt -> 
                        debt.type == "debt_owed" || debt.type == "lent" 
                    }.sumOf { it.remainingAmount }
                    
                    val totalOwedByMe = activeDebts.filter { debt -> 
                        debt.type == "debt_owing" || debt.type == "owe" 
                    }.sumOf { it.remainingAmount }
                    
                    val netActiveDebts = totalOwedToMe - totalOwedByMe
                    
                    Log.d("BalanceCalculator", 
                        "⚖️ DEUDAS ACTIVAS: Me deben=$totalOwedToMe - Yo debo=$totalOwedByMe = $netActiveDebts (${activeDebts.size}/${debts.size} activas)"
                    )
                    
                    return ActiveDebtsData(totalOwedToMe, totalOwedByMe, netActiveDebts)
                },
                onFailure = { exception ->
                    Log.e("BalanceCalculator", "Error al obtener deudas activas: ${exception.message}")
                    return ActiveDebtsData(0.0, 0.0, 0.0)
                }
            )
        } catch (e: Exception) {
            Log.e("BalanceCalculator", "Error al calcular deudas activas", e)
            return ActiveDebtsData(0.0, 0.0, 0.0)
        }
    }
    
    /**
     * BALANCE TODAS LAS DEUDAS: Para patrimonio total y pantalla deudas
     * Incluye TODAS las deudas (pagadas + activas) usando originalAmount
     */
    suspend fun calculateAllDebtsBalance(debtRepository: DebtRepositoryInterface): Double {
        return try {
            val currentUserId = SupabaseManager.client.auth.currentUserOrNull()?.id
            if (currentUserId == null) {
                Log.w("BalanceCalculator", "Usuario no autenticado")
                return 0.0
            }

            val debtsResult = debtRepository.getDebtsByUserId(currentUserId)
            debtsResult.fold(
                onSuccess = { debts ->
                    // TODAS las deudas (pagadas + activas) usando originalAmount
                    val totalOwedToMe = debts.filter { debt -> 
                        debt.type == "debt_owed" || debt.type == "lent" 
                    }.sumOf { it.originalAmount }
                    
                    val totalOwedByMe = debts.filter { debt -> 
                        debt.type == "debt_owing" || debt.type == "owe" 
                    }.sumOf { it.originalAmount }
                    
                    val allDebtsBalance = totalOwedToMe - totalOwedByMe
                    
                    Log.d("BalanceCalculator", 
                        "📈 BALANCE TODAS LAS DEUDAS: Me deben=$totalOwedToMe - Yo debo=$totalOwedByMe = $allDebtsBalance (${debts.size} deudas totales)"
                    )
                    
                    // Log detallado por estado
                    val activeCount = debts.count { it.status?.lowercase() != "paid" }
                    val paidCount = debts.size - activeCount
                    Log.d("BalanceCalculator", "   Activas: $activeCount, Pagadas: $paidCount")
                    
                    return allDebtsBalance
                },
                onFailure = { exception ->
                    Log.e("BalanceCalculator", "Error al obtener todas las deudas: ${exception.message}")
                    return 0.0
                }
            )
        } catch (e: Exception) {
            Log.e("BalanceCalculator", "Error al calcular todas las deudas", e)
            return 0.0
        }
    }
    
    /**
     * PATRIMONIO TOTAL: Transacciones reales + TODAS las deudas
     * IGUAL PARA AMBAS PANTALLAS
     */
    suspend fun calculateTotalWealth(
        transactions: List<Transaction>,
        debtRepository: DebtRepositoryInterface
    ): WealthData {
        val availableMoney = calculateAvailableMoney(transactions)
        val transactionsBalance = calculateTransactionsBalance(transactions)
        val allDebtsBalance = calculateAllDebtsBalance(debtRepository)
        val activeDebtsData = calculateActiveDebtsBalance(debtRepository)
        val totalWealth = transactionsBalance + allDebtsBalance
        
        Log.d("BalanceCalculator", 
            "🏛️ PATRIMONIO TOTAL = Transacciones($transactionsBalance) + Todas las deudas($allDebtsBalance) = $totalWealth"
        )
        
        return WealthData(
            availableMoney = availableMoney,
            transactionsBalance = transactionsBalance,
            allDebtsBalance = allDebtsBalance,
            activeDebtsData = activeDebtsData,
            totalWealth = totalWealth
        )
    }
    
    /**
     * Detecta transacciones automáticas
     */
    private fun isAutomaticTransaction(transaction: Transaction): Boolean {
        val description = transaction.description ?: ""
        
        return description.contains("(Automático)") ||
               description.contains("[DEBT_ID:") ||
               description.startsWith("Pago de deuda:") ||
               description.startsWith("Cobro de deuda:") ||
               description.startsWith("Reversión por reactivación:")
    }
}

/**
 * Datos de deudas activas (para gestión diaria)
 */
data class ActiveDebtsData(
    val totalOwedToMe: Double,      // Me deben (solo activas)
    val totalOwedByMe: Double,      // Yo debo (solo activas) 
    val netBalance: Double          // Balance neto activo
)

/**
 * Datos completos de riqueza
 */
data class WealthData(
    val availableMoney: Double,         // Dinero disponible actual
    val transactionsBalance: Double,    // Balance de transacciones reales
    val allDebtsBalance: Double,        // Balance de TODAS las deudas
    val activeDebtsData: ActiveDebtsData, // Datos de deudas activas
    val totalWealth: Double            // Patrimonio total
)