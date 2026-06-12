package com.example.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SyncPosApplication
import com.example.core.database.entity.ExpenseEntity
import com.example.core.database.entity.OrderEntity
import com.example.core.database.entity.ProductEntity
import com.example.core.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class DashboardUiState(
    val totalRevenue: Double = 0.0,
    val totalOrdersCount: Int = 0,
    val totalExpenses: Double = 0.0,
    val lowStockCount: Int = 0,
    val paymentCashAmount: Double = 0.0,
    val paymentCardAmount: Double = 0.0,
    val paymentUpiAmount: Double = 0.0,
    val lowStockWarningProducts: List<ProductEntity> = emptyList(),
    val topSellers: List<TopSellerItem> = emptyList(),
    val last7DaysSales: List<Double> = List(7) { 0.0 },
    val last30DaysSales: List<Double> = List(30) { 0.0 },
    val monthlySales: List<Double> = List(12) { 0.0 }
)

data class TopSellerItem(
    val name: String,
    val category: String,
    val price: Double,
    val quantitySold: Int
)

class DashboardViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    // Combine flows to compute live statistics reactively
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllOrders(),
        repository.getAllProducts(),
        repository.getAllExpenses(),
        repository.getAllOrderItems()
    ) { orders, products, expenses, orderItems ->
        val cutoffTime = getCutoffTimeInMillis()

        // Filter orders and expenses based on selected duration
        val filteredOrders = orders.filter { it.createdAt >= cutoffTime }
        val filteredExpenses = expenses.filter { it.createdAt >= cutoffTime }

        val totalRevenue = filteredOrders.sumOf { it.totalAmount }
        val totalExpenses = filteredExpenses.sumOf { it.amount }

        // Low stock calculations
        val lowStockProducts = products.filter { it.stock <= it.minStock }
        val lowStockCount = lowStockProducts.size

        // Payment split calculations
        var cashAmount = 0.0
        var cardAmount = 0.0
        var upiAmount = 0.0
        for (order in filteredOrders) {
            when (order.paymentMethod.lowercase()) {
                "cash" -> cashAmount += order.totalAmount
                "card" -> cardAmount += order.totalAmount
                "upi" -> upiAmount += order.totalAmount
            }
        }

        // Compute top sellers using all time data
        val productSalesCount = orderItems.groupBy { it.productId }.mapValues { entry -> entry.value.sumOf { it.quantity.toInt() } }
        val sortedTopSellers = products.mapNotNull { prod ->
            val quantitySold = productSalesCount[prod.id] ?: 0
            if (quantitySold > 0) {
                TopSellerItem(
                    name = prod.name,
                    category = prod.category,
                    price = prod.price,
                    quantitySold = quantitySold
                )
            } else null
        }.sortedByDescending { it.quantitySold }.take(10)
        
        // 7 Days Sales Trend
        val last7DaysSales = MutableList(7) { 0.0 }
        // 30 Days Sales Trend
        val last30DaysSales = MutableList(30) { 0.0 }
        // 12 Months Sales Trend
        val monthlySales = MutableList(12) { 0.0 }
        
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        
        for (order in orders) {
            cal.timeInMillis = order.createdAt
            
            // Monthly logic
            if (cal.get(Calendar.YEAR) == currentYear) {
                monthlySales[cal.get(Calendar.MONTH)] += order.totalAmount
            }
            
            // Days ago logic
            val orderDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            val orderYear = cal.get(Calendar.YEAR)
            val daysAgo = if (currentYear == orderYear) {
                currentDayOfYear - orderDayOfYear
            } else {
               // rough approx
               val maxDays = if (orderYear % 4 == 0) 366 else 365
               (currentDayOfYear + maxDays) - orderDayOfYear
            }
            
            if (daysAgo in 0..6) {
                last7DaysSales[6 - daysAgo] += order.totalAmount // 6 is today, 0 is 6 days ago
            }
            if (daysAgo in 0..29) {
                 last30DaysSales[29 - daysAgo] += order.totalAmount
            }
        }

        DashboardUiState(
            totalRevenue = totalRevenue,
            totalOrdersCount = filteredOrders.size,
            totalExpenses = totalExpenses,
            lowStockCount = lowStockCount,
            paymentCashAmount = cashAmount,
            paymentCardAmount = cardAmount,
            paymentUpiAmount = upiAmount,
            lowStockWarningProducts = lowStockProducts,
            topSellers = sortedTopSellers,
            last7DaysSales = last7DaysSales.toList(),
            last30DaysSales = last30DaysSales.toList(),
            monthlySales = monthlySales.toList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    private fun getCutoffTimeInMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal.timeInMillis
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = (application as SyncPosApplication).repository
            return DashboardViewModel(application, repository) as T
        }
    }
}
