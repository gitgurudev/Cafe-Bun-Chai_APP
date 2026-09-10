package com.cafebunchai.pos.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cafebunchai.pos.data.model.CartLine
import com.cafebunchai.pos.data.model.Category
import com.cafebunchai.pos.data.model.InventoryItem
import com.cafebunchai.pos.data.model.MenuItem
import com.cafebunchai.pos.data.model.Order
import com.cafebunchai.pos.data.model.OrderLine
import com.cafebunchai.pos.data.model.OrderStatus
import com.cafebunchai.pos.data.model.OrderType
import com.cafebunchai.pos.data.model.Payment
import com.cafebunchai.pos.data.model.RecipeLine
import com.cafebunchai.pos.data.repo.OrderInventoryRepository
import com.cafebunchai.pos.data.stock.StockMath
import com.cafebunchai.pos.ui.util.dayBounds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class OrderUiState(
    val categories: List<Category> = emptyList(),
    val items: List<MenuItem> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val recipes: List<RecipeLine> = emptyList(),
    val remaining: Map<String, Double> = emptyMap(),
    val unpaidOrders: List<Order> = emptyList(),
    val selectedCategoryId: String? = null,
    val cart: List<CartLine> = emptyList(),
    val type: String = OrderType.TAKEAWAY,
    val tableNote: String = "",
    val payment: String = Payment.UNPAID,
    val noteDraft: String = "",
    val noteItemId: String? = null,
    val message: String? = null,
    val busy: Boolean = false,
    val composing: Boolean = true,
    val editingOrderId: String? = null,
    val refillAlerts: List<InventoryItem> = emptyList(),
) {
    val sellableItems: List<MenuItem>
        get() = items.filter { it.isAvailable && canSell(it) }
    val visibleItems: List<MenuItem>
        get() {
            val cat = selectedCategoryId
            val sellable = sellableItems
            val effectiveCat =
                if (cat != null && sellable.none { it.categoryId == cat }) null else cat
            return sellable.filter { effectiveCat == null || it.categoryId == effectiveCat }
        }
    val visibleCategories: List<Category>
        get() {
            val ids = sellableItems.map { it.categoryId }.toSet()
            return categories.filter { it.id in ids }
        }
    val cartTotalPaise: Int get() = cart.sumOf { it.lineTotalPaise }
    val editingUnpaid: Boolean get() = editingOrderId != null
    fun canSell(item: MenuItem): Boolean =
        StockMath.canMake(item, 1, remaining, recipes, inventory)
}

class OrderViewModel(
    private val repo: OrderInventoryRepository,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow<String?>(null)
    private val cart = MutableStateFlow<List<CartLine>>(emptyList())
    private val type = MutableStateFlow(OrderType.TAKEAWAY)
    private val tableNote = MutableStateFlow("")
    private val payment = MutableStateFlow(Payment.UNPAID)
    private val noteDraft = MutableStateFlow("")
    private val noteItemId = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)
    private val composing = MutableStateFlow(true)
    private val editingOrderId = MutableStateFlow<String?>(null)
    private val refillAlerts = MutableStateFlow<List<InventoryItem>>(emptyList())
    private var autoListDone = false

    private data class Ticket(
        val selectedCategoryId: String?,
        val cart: List<CartLine>,
        val type: String,
        val tableNote: String,
        val payment: String,
    )

    private data class Session(
        val noteDraft: String,
        val noteItemId: String?,
        val message: String?,
        val busy: Boolean,
        val composing: Boolean,
        val editingOrderId: String?,
        val refillAlerts: List<InventoryItem>,
    )

    private data class Catalog(
        val categories: List<Category>,
        val items: List<MenuItem>,
        val inventory: List<InventoryItem>,
        val recipes: List<RecipeLine>,
    )

    val state: StateFlow<OrderUiState> = combine(
        combine(
            repo.observeCategories(),
            repo.observeMenuItems(),
            repo.observeInventory(),
            repo.observeRecipes(),
        ) { categories, items, inventory, recipes ->
            Catalog(categories, items, inventory, recipes)
        },
        run {
            val (from, to) = dayBounds(LocalDate.now(ZoneId.of("Asia/Kolkata")))
            repo.observeOrders(from, to)
        },
        combine(selectedCategory, cart, type, tableNote, payment) { cat, c, t, note, pay ->
            Ticket(cat, c, t, note, pay)
        },
        combine(
            combine(noteDraft, noteItemId, message) { draft, itemId, msg -> Triple(draft, itemId, msg) },
            combine(busy, composing, editingOrderId, refillAlerts) { b, c, e, refill ->
                Rest(b, c, e, refill)
            },
        ) { notes, rest ->
            Session(
                notes.first,
                notes.second,
                notes.third,
                rest.busy,
                rest.composing,
                rest.editingOrderId,
                rest.refillAlerts,
            )
        },
    ) { catalog, orders, ticket, session ->
        val unpaid = orders.filter {
            it.status == OrderStatus.COMPLETED &&
                it.payment == Payment.UNPAID &&
                it.lines.isNotEmpty()
        }
        if (!autoListDone) {
            autoListDone = true
            if (unpaid.isNotEmpty()) {
                composing.value = false
            }
        }
        val restore = unpaid.find { it.id == session.editingOrderId }?.lines.orEmpty()
        val remaining = StockMath.remainingAfterCart(
            catalog.inventory,
            catalog.recipes,
            ticket.cart,
            restore,
        )
        OrderUiState(
            categories = catalog.categories,
            items = catalog.items,
            inventory = catalog.inventory,
            recipes = catalog.recipes,
            remaining = remaining,
            unpaidOrders = unpaid,
            selectedCategoryId = ticket.selectedCategoryId,
            cart = ticket.cart,
            type = ticket.type,
            tableNote = ticket.tableNote,
            payment = ticket.payment,
            noteDraft = session.noteDraft,
            noteItemId = session.noteItemId,
            message = session.message,
            busy = session.busy,
            composing = session.composing,
            editingOrderId = session.editingOrderId,
            refillAlerts = session.refillAlerts,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, OrderUiState())

    fun showUnpaidList() {
        autoListDone = true
        val editing = editingOrderId.value
        if (editing != null && cart.value.isEmpty()) {
            withdrawTicket(editing)
            return
        }
        resetCompose()
        composing.value = false
    }

    fun newOrder() {
        autoListDone = true
        resetCompose()
        composing.value = true
    }

    fun openUnpaid(order: Order) {
        autoListDone = true
        editingOrderId.value = order.id
        type.value = order.type
        tableNote.value = order.tableNote
        payment.value = Payment.UNPAID
        val menuById = state.value.items.associateBy { it.id }
        cart.value = order.lines.map { line ->
            val menu = menuById[line.menuItemId] ?: MenuItem(
                id = line.menuItemId.ifBlank { line.id },
                name = line.nameSnapshot,
                categoryId = "",
                pricePaise = line.unitPricePaise,
                isAvailable = true,
                isCombo = false,
                noteHint = null,
            )
            CartLine(menu, line.qty, line.lineNote.orEmpty())
        }
        composing.value = true
    }

    fun selectCategory(id: String?) = selectedCategory.update { id }

    fun addItem(item: MenuItem) {
        if (!state.value.canSell(item)) {
            message.value = "${item.name} is out of stock"
            return
        }
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
        if (!state.value.canSell(item)) {
            message.value = "${item.name} is out of stock"
            noteItemId.value = null
            noteDraft.value = ""
            return
        }
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
        if (delta > 0) {
            val line = cart.value.find { it.menuItem.id == menuItemId && it.note == note } ?: return
            if (!state.value.canSell(line.menuItem)) return
        }
        cart.update { list ->
            list.mapNotNull { line ->
                if (line.menuItem.id != menuItemId || line.note != note) line
                else {
                    val q = line.qty + delta
                    if (q <= 0) null else line.copy(qty = q)
                }
            }
        }
        val editing = editingOrderId.value
        if (editing != null && cart.value.isEmpty()) {
            withdrawTicket(editing)
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

    fun clearRefillAlerts() {
        refillAlerts.value = emptyList()
    }

    fun tryComplete() {
        if (cart.value.isEmpty()) {
            val editing = editingOrderId.value
            if (editing != null) {
                withdrawTicket(editing)
                return
            }
            message.value = "Add items first"
            return
        }
        finish()
    }

    private fun withdrawTicket(orderId: String) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            val result = repo.cancelOrder(orderId, "Withdrawn")
            busy.value = false
            resetCompose()
            composing.value = true
            message.value = if (result.isSuccess) {
                "Ticket withdrawn"
            } else {
                result.exceptionOrNull()?.message ?: "Could not withdraw ticket"
            }
        }
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
                existingOrderId = s.editingOrderId,
            )
            busy.value = false
            result.onSuccess { sale ->
                val savedUnpaid = s.payment == Payment.UNPAID
                resetCompose()
                composing.value = !savedUnpaid
                refillAlerts.value = sale.refillAlerts
                message.value = if (savedUnpaid) "Unpaid ticket saved" else "Order paid"
            }.onFailure {
                message.value = it.message ?: "Could not save order"
            }
        }
    }

    private fun resetCompose() {
        cart.value = emptyList()
        tableNote.value = ""
        type.value = OrderType.TAKEAWAY
        payment.value = Payment.UNPAID
        editingOrderId.value = null
        noteItemId.value = null
        noteDraft.value = ""
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

private data class Rest(
    val busy: Boolean,
    val composing: Boolean,
    val editingOrderId: String?,
    val refillAlerts: List<InventoryItem>,
)
