package com.cafebunchai.pos.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cafebunchai.pos.data.model.InventoryItem
import com.cafebunchai.pos.data.model.Order
import com.cafebunchai.pos.data.model.OrderStatus
import com.cafebunchai.pos.data.model.Payment
import com.cafebunchai.pos.data.repo.OrderInventoryRepository
import com.cafebunchai.pos.ui.util.dayBounds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class ItemShare(
    val name: String,
    val qty: Int,
    val paise: Int,
    val share: Float,
)

data class DashboardUiState(
    val todayTotalPaise: Int = 0,
    val cashPaise: Int = 0,
    val upiPaise: Int = 0,
    val unpaidPaise: Int = 0,
    val orderCount: Int = 0,
    val cancelledCount: Int = 0,
    val avgOrderPaise: Int = 0,
    val topBySales: ItemShare? = null,
    val topByQty: ItemShare? = null,
    val items: List<ItemShare> = emptyList(),
    val lowStock: List<InventoryItem> = emptyList(),
    val recent: List<Order> = emptyList(),
    val ready: Boolean = false,
)

class DashboardViewModel(
    repo: OrderInventoryRepository,
) : ViewModel() {

    val state: StateFlow<DashboardUiState> = combine(
        run {
            val (from, to) = dayBounds(LocalDate.now(ZoneId.of("Asia/Kolkata")))
            repo.observeOrders(from, to)
        },
        repo.observeInventory(),
    ) { orders, stock ->
        val done = orders.filter { it.status == OrderStatus.COMPLETED }
        val total = done.sumOf { it.totalPaise }
        val grouped = mutableMapOf<String, Pair<Int, Int>>()
        for (order in done) {
            for (line in order.lines) {
                val cur = grouped[line.nameSnapshot] ?: (0 to 0)
                grouped[line.nameSnapshot] = (cur.first + line.qty) to (cur.second + line.lineTotalPaise)
            }
        }
        val items = grouped.entries
            .map { (name, v) ->
                ItemShare(
                    name = name,
                    qty = v.first,
                    paise = v.second,
                    share = if (total <= 0) 0f else v.second.toFloat() / total.toFloat(),
                )
            }
            .sortedByDescending { it.paise }
        DashboardUiState(
            todayTotalPaise = total,
            cashPaise = done.filter { it.payment == Payment.CASH }.sumOf { it.totalPaise },
            upiPaise = done.filter { it.payment == Payment.UPI }.sumOf { it.totalPaise },
            unpaidPaise = done.filter { it.payment == Payment.UNPAID }.sumOf { it.totalPaise },
            orderCount = done.size,
            cancelledCount = orders.count { it.status == OrderStatus.CANCELLED },
            avgOrderPaise = if (done.isEmpty()) 0 else total / done.size,
            topBySales = items.maxByOrNull { it.paise },
            topByQty = items.maxByOrNull { it.qty },
            items = items.take(6),
            lowStock = stock.filter { it.isLow }.sortedBy { it.qtyOnHand },
            recent = orders.take(8),
            ready = true,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DashboardUiState())

    companion object {
        fun factory(repo: OrderInventoryRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(repo) as T
        }
    }
}
