package com.cafebunchai.pos.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cafebunchai.pos.data.model.Order
import com.cafebunchai.pos.data.model.OrderStatus
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

data class RegisterUiState(
    val from: LocalDate = LocalDate.now(),
    val to: LocalDate = LocalDate.now(),
    val preset: RegisterPreset = RegisterPreset.TODAY,
    val orders: List<Order> = emptyList(),
    val message: String? = null,
) {
    val completed: List<Order> get() = orders.filter { it.status == OrderStatus.COMPLETED }
    val dayTotalPaise: Int get() = completed.sumOf { it.totalPaise }
    val cashPaise: Int get() = completed.filter { it.payment == "cash" }.sumOf { it.totalPaise }
    val upiPaise: Int get() = completed.filter { it.payment == "upi" }.sumOf { it.totalPaise }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModel(
    private val repo: OrderInventoryRepository,
) : ViewModel() {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val fromDate = MutableStateFlow(LocalDate.now(zone))
    private val toDate = MutableStateFlow(LocalDate.now(zone))
    private val preset = MutableStateFlow(RegisterPreset.TODAY)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<RegisterUiState> = combine(
        combine(fromDate, toDate, preset) { f, t, p -> Triple(f, t, p) }
            .flatMapLatest { (f, t, p) ->
                val (start, end) = rangeBounds(f, t)
                repo.observeOrders(start, end).map { orders ->
                    RegisterUiState(from = f, to = t, preset = p, orders = orders)
                }
            },
        message,
    ) { ui, msg ->
        ui.copy(message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegisterUiState())

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
