package com.cafebunchai.pos.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cafebunchai.pos.data.model.CartLine
import com.cafebunchai.pos.data.model.Category
import com.cafebunchai.pos.data.model.MenuItem
import com.cafebunchai.pos.data.model.OrderType
import com.cafebunchai.pos.data.model.Payment
import com.cafebunchai.pos.data.repo.OrderInventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderUiState(
    val categories: List<Category> = emptyList(),
    val items: List<MenuItem> = emptyList(),
    val selectedCategoryId: String? = null,
    val cart: List<CartLine> = emptyList(),
    val type: String = OrderType.TAKEAWAY,
    val tableNote: String = "",
    val payment: String = Payment.CASH,
    val noteDraft: String = "",
    val noteItemId: String? = null,
    val message: String? = null,
    val busy: Boolean = false,
) {
    val visibleItems: List<MenuItem>
        get() = items.filter { it.isAvailable && (selectedCategoryId == null || it.categoryId == selectedCategoryId) }
    val cartTotalPaise: Int get() = cart.sumOf { it.lineTotalPaise }
}

class OrderViewModel(
    private val repo: OrderInventoryRepository,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<String?>(null)
    private val cart = MutableStateFlow<List<CartLine>>(emptyList())
    private val type = MutableStateFlow(OrderType.TAKEAWAY)
    private val tableNote = MutableStateFlow("")
    private val payment = MutableStateFlow(Payment.CASH)
    private val noteDraft = MutableStateFlow("")
    private val noteItemId = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)

    private data class Ticket(
        val selectedCategoryId: String?,
        val cart: List<CartLine>,
        val type: String,
        val tableNote: String,
        val payment: String,
    )

    private data class Notes(
        val noteDraft: String,
        val noteItemId: String?,
        val message: String?,
        val busy: Boolean,
    )

    val state: StateFlow<OrderUiState> = combine(
        repo.observeCategories(),
        repo.observeMenuItems(),
        combine(selectedCategory, cart, type, tableNote, payment) { cat, c, t, note, pay ->
            Ticket(cat, c, t, note, pay)
        },
        combine(noteDraft, noteItemId, message, busy) { draft, itemId, msg, b ->
            Notes(draft, itemId, msg, b)
        },
    ) { categories, items, ticket, notes ->
        OrderUiState(
            categories = categories,
            items = items,
            selectedCategoryId = ticket.selectedCategoryId,
            cart = ticket.cart,
            type = ticket.type,
            tableNote = ticket.tableNote,
            payment = ticket.payment,
            noteDraft = notes.noteDraft,
            noteItemId = notes.noteItemId,
            message = notes.message,
            busy = notes.busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderUiState())

    fun selectCategory(id: String?) = selectedCategory.update { id }

    fun addItem(item: MenuItem) {
        if (item.noteHint != null) {
            noteItemId.value = item.id
            noteDraft.value = ""
            return
        }
        bump(item, 1, "")
    }

    fun confirmNote() {
        val id = noteItemId.value ?: return
        val item = state.value.items.find { it.id == id } ?: return
        bump(item, 1, noteDraft.value)
        noteItemId.value = null
        noteDraft.value = ""
    }

    fun cancelNote() {
        noteItemId.value = null
        noteDraft.value = ""
    }

    fun setNoteDraft(text: String) {
        noteDraft.value = text
    }

    fun changeQty(menuItemId: String, note: String, delta: Int) {
        cart.update { list ->
            list.mapNotNull { line ->
                if (line.menuItem.id != menuItemId || line.note != note) line
                else {
                    val q = line.qty + delta
                    if (q <= 0) null else line.copy(qty = q)
                }
            }
        }
    }

    fun setType(value: String) {
        type.value = value
    }

    fun setTableNote(value: String) {
        tableNote.value = value
    }

    fun setPayment(value: String) {
        payment.value = value
    }

    fun clearMessage() {
        message.value = null
    }

    fun tryComplete() {
        if (cart.value.isEmpty()) {
            message.value = "Add items first"
            return
        }
        finish()
    }

    private fun finish() {
        viewModelScope.launch {
            busy.value = true
            val s = state.value
            val result = repo.completeOrder(
                cart = s.cart,
                type = s.type,
                tableNote = s.tableNote,
                payment = s.payment,
                allowNegativeStock = true,
            )
            busy.value = false
            result.onSuccess {
                cart.value = emptyList()
                tableNote.value = ""
                message.value = "Order saved"
            }.onFailure {
                message.value = it.message ?: "Could not save order"
            }
        }
    }

    private fun bump(item: MenuItem, delta: Int, note: String) {
        cart.update { list ->
            val existing = list.find { it.menuItem.id == item.id && it.note == note }
            if (existing == null) {
                list + CartLine(item, delta.coerceAtLeast(1), note)
            } else {
                list.map {
                    if (it === existing) it.copy(qty = it.qty + delta) else it
                }
            }
        }
    }

    companion object {
        fun factory(repo: OrderInventoryRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = OrderViewModel(repo) as T
        }
    }
}
