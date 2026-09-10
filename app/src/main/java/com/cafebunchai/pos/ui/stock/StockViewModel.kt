package com.cafebunchai.pos.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cafebunchai.pos.data.model.InventoryItem
import com.cafebunchai.pos.data.repo.OrderInventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class StockEditor(
    val id: String,
    val name: String,
    val unit: String,
    val qty: String,
    val low: String,
)

data class StockUiState(
    val items: List<InventoryItem> = emptyList(),
    val editor: StockEditor? = null,
)

class StockViewModel(
    private val repo: OrderInventoryRepository,
) : ViewModel() {

    private val editor = MutableStateFlow<StockEditor?>(null)

    val state: StateFlow<StockUiState> = combine(
        repo.observeInventory(),
        editor,
    ) { items, ed ->
        StockUiState(items, ed)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StockUiState())

    fun openEdit(item: InventoryItem) {
        editor.value = StockEditor(
            id = item.id,
            name = item.name,
            unit = item.unit,
            qty = formatQty(item.qtyOnHand),
            low = formatQty(item.lowStockThreshold),
        )
    }

    fun startNew() {
        editor.value = StockEditor(
            id = UUID.randomUUID().toString(),
            name = "",
            unit = "pcs",
            qty = "0",
            low = "5",
        )
    }

    fun close() {
        editor.value = null
    }

    fun update(transform: (StockEditor) -> StockEditor) {
        val cur = editor.value ?: return
        editor.value = transform(cur)
    }

    fun save() {
        val ed = editor.value ?: return
        if (ed.name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repo.upsertInventory(
                    InventoryItem(
                        id = ed.id,
                        name = ed.name.trim(),
                        unit = ed.unit,
                        qtyOnHand = ed.qty.toDoubleOrNull() ?: 0.0,
                        lowStockThreshold = ed.low.toDoubleOrNull() ?: 0.0,
                    ),
                )
            }
            editor.value = null
        }
    }

    companion object {
        fun factory(repo: OrderInventoryRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = StockViewModel(repo) as T
        }
    }
}

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)
