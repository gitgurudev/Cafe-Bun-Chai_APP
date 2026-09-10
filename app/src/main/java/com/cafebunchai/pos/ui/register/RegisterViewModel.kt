package com.cafebunchai.pos.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cafebunchai.pos.data.model.Order
import com.cafebunchai.pos.data.model.OrderStatus
import com.cafebunchai.pos.data.model.Payment
import com.cafebunchai.pos.data.repo.OrderInventoryRepository
import com.cafebunchai.pos.ui.util.rangeBounds
import com.cafebunchai.pos.ui.util.thisMonth
import com.cafebunchai.pos.ui.util.thisWeek
import com.cafebunchai.pos.ui.util.thisYear
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class RegisterPreset { TODAY, WEEK, MONTH, YEAR, CUSTOM }

enum class RegisterPayFilter { ALL, PAID, UNPAID }

data class RegisterUiState(
    val from: LocalDate = LocalDate.now(),
    val to: LocalDate = LocalDate.now(),
    val preset: RegisterPreset = RegisterPreset.TODAY,
    val payFilter: RegisterPayFilter = RegisterPayFilter.ALL,
    val orders: List<Order> = emptyList(),
    val message: String? = null,
    val ready: Boolean = false,
) {
    val completed: List<Order> get() = orders.filter { it.status == OrderStatus.COMPLETED }
    val dayTotalPaise: Int get() = completed.sumOf { it.totalPaise }
    val cashPaise: Int get() = completed.filter { it.payment == Payment.CASH }.sumOf { it.totalPaise }
    val upiPaise: Int get() = completed.filter { it.payment == Payment.UPI }.sumOf { it.totalPaise }
    val unpaidPaise: Int get() = completed.filter { it.payment == Payment.UNPAID }.sumOf { it.totalPaise }
    val unpaidCount: Int get() = completed.count { it.payment == Payment.UNPAID }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModel(
    private val repo: OrderInventoryRepository,
) : ViewModel() {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val fromDate = MutableStateFlow(LocalDate.now(zone))
    private val toDate = MutableStateFlow(LocalDate.now(zone))
    private val preset = MutableStateFlow(RegisterPreset.TODAY)
    private val payFilter = MutableStateFlow(RegisterPayFilter.ALL)
    private val filterTouched = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<RegisterUiState> = combine(
        combine(fromDate, toDate, preset, payFilter, filterTouched) { f, t, p, pay, touched ->
            RegisterQuery(f, t, p, pay, touched)
        }.flatMapLatest { q ->
            val (start, end) = rangeBounds(q.from, q.to)
            repo.observeOrders(start, end).map { orders ->
                val unpaid = orders.filter {
                    it.status == OrderStatus.COMPLETED &&
                        it.payment == Payment.UNPAID &&
                        it.lines.isNotEmpty()
                }
                val filter = when {
                    q.filterTouched -> q.payFilter
                    unpaid.isNotEmpty() -> RegisterPayFilter.UNPAID
                    else -> RegisterPayFilter.ALL
                }
                val visible = when (filter) {
                    RegisterPayFilter.ALL -> orders
                    RegisterPayFilter.UNPAID -> unpaid
                    RegisterPayFilter.PAID -> orders.filter {
                        it.status == OrderStatus.COMPLETED && it.payment != Payment.UNPAID
                    }
                }
                RegisterUiState(
                    from = q.from,
                    to = q.to,
                    preset = q.preset,
                    payFilter = filter,
                    orders = visible,
                    ready = true,
                )
            }
        },
        message,
    ) { ui, msg ->
        ui.copy(message = msg)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RegisterUiState())

    fun presetToday() = setRange(LocalDate.now(zone), LocalDate.now(zone), RegisterPreset.TODAY)

    fun presetWeek() {
        val (f, t) = thisWeek()
        setRange(f, t, RegisterPreset.WEEK)
    }

    fun presetMonth() {
        val (f, t) = thisMonth()
        setRange(f, t, RegisterPreset.MONTH)
    }

    fun presetYear() {
        val (f, t) = thisYear()
        setRange(f, t, RegisterPreset.YEAR)
    }

    fun setFrom(date: LocalDate) {
        val to = toDate.value
        if (date.isAfter(to)) setRange(to, date, RegisterPreset.CUSTOM)
        else setRange(date, to, RegisterPreset.CUSTOM)
    }

    fun setTo(date: LocalDate) {
        val from = fromDate.value
        if (date.isBefore(from)) setRange(date, from, RegisterPreset.CUSTOM)
        else setRange(from, date, RegisterPreset.CUSTOM)
    }

    fun prevDay() {
        if (fromDate.value == toDate.value) {
            val d = fromDate.value.minusDays(1)
            setRange(d, d, RegisterPreset.CUSTOM)
        }
    }

    fun nextDay() {
        if (fromDate.value == toDate.value) {
            val d = fromDate.value.plusDays(1)
            setRange(d, d, RegisterPreset.CUSTOM)
        }
    }

    fun setPayFilter(value: RegisterPayFilter) {
        filterTouched.value = true
        payFilter.value = value
    }

    fun cancel(orderId: String, reason: String) {
        viewModelScope.launch {
            repo.cancelOrder(orderId, reason).onFailure {
                message.value = it.message
            }
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun setRange(from: LocalDate, to: LocalDate, kind: RegisterPreset) {
        fromDate.value = from
        toDate.value = to
        preset.value = kind
    }

    companion object {
        fun factory(repo: OrderInventoryRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RegisterViewModel(repo) as T
        }
    }
}

private data class RegisterQuery(
    val from: LocalDate,
    val to: LocalDate,
    val preset: RegisterPreset,
    val payFilter: RegisterPayFilter,
    val filterTouched: Boolean,
)
