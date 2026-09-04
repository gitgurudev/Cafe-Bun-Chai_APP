package com.cafebunchai.pos.data.repo

import androidx.room.withTransaction
import com.cafebunchai.pos.data.cloud.CafeCloudSnapshot
import com.cafebunchai.pos.data.cloud.CloudOrder
import com.cafebunchai.pos.data.cloud.FirestoreCafeSync
import com.cafebunchai.pos.data.local.AppDatabase
import com.cafebunchai.pos.data.local.entity.CategoryEntity
import com.cafebunchai.pos.data.local.entity.InventoryItemEntity
import com.cafebunchai.pos.data.local.entity.MenuItemEntity
import com.cafebunchai.pos.data.local.entity.OrderEntity
import com.cafebunchai.pos.data.local.entity.OrderLineEntity
import com.cafebunchai.pos.data.local.entity.RecipeLineEntity
import com.cafebunchai.pos.data.model.CartLine
import com.cafebunchai.pos.data.model.Category
import com.cafebunchai.pos.data.model.InventoryItem
import com.cafebunchai.pos.data.model.MenuItem
import com.cafebunchai.pos.data.model.Order
import com.cafebunchai.pos.data.model.OrderLine
import com.cafebunchai.pos.data.model.OrderStatus
import com.cafebunchai.pos.data.model.RecipeLine
import com.cafebunchai.pos.data.model.StockNeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomOrderInventoryRepository(
    private val db: AppDatabase,
    private val cloud: FirestoreCafeSync,
) : OrderInventoryRepository {

    private val categories = db.categoryDao()
    private val menu = db.menuItemDao()
    private val inventory = db.inventoryDao()
    private val recipes = db.recipeDao()
    private val orders = db.orderDao()
    private val lines = db.orderLineDao()

    override fun observeCategories(): Flow<List<Category>> =
        categories.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeMenuItems(): Flow<List<MenuItem>> =
        menu.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeInventory(): Flow<List<InventoryItem>> =
        inventory.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun hydrateFromCloud() {
        runCatching {
            cloud.ensureCafe()
            cloud.migrateLegacyInventory()
            val remote = cloud.pullSnapshot()
            val cloudEmpty = remote.categories.isEmpty() && remote.menuItems.isEmpty()
            if (cloudEmpty) {
                pushLocalToCloud()
            } else {
                applyCloud(remote)
                val remoteIds = remote.tickets.map { it.order.id }.toSet()
                orders.getAll().forEach { order ->
                    if (order.id !in remoteIds) {
                        runCatching { cloud.pushOrder(order, lines.getForOrder(order.id)) }
                    }
                }
            }
        }
    }

    override fun observeCloud() = cloud.observe()

    override suspend fun applyCloud(snapshot: CafeCloudSnapshot) {
        db.withTransaction {
            if (snapshot.categories.isNotEmpty()) {
                recipes.deleteAll()
                menu.deleteAll()
                inventory.deleteAll()
                categories.deleteAll()
                categories.upsertAll(snapshot.categories)
                inventory.upsertAll(snapshot.inventory)
                menu.upsertAll(snapshot.menuItems)
                recipes.upsertAll(snapshot.recipes)
            } else if (snapshot.inventory.isNotEmpty()) {
                inventory.upsertAll(snapshot.inventory)
            }
            snapshot.tickets.forEach { ticket ->
                orders.upsert(ticket.order)
                lines.deleteForOrder(ticket.order.id)
                if (ticket.lines.isNotEmpty()) lines.upsertAll(ticket.lines)
            }
        }
    }

    override suspend fun pushLocalToCloud() {
        val tickets = orders.getAll().map { CloudOrder(it, lines.getForOrder(it.id)) }
        cloud.pushEntireCafe(
            categories.getAll(),
            menu.getAll(),
            inventory.getAll(),
            recipes.getAll(),
            tickets,
        )
    }

    private suspend fun pushStock(id: String) {
        val item = inventory.getById(id) ?: return
        runCatching { cloud.pushInventory(item) }
    }

    private suspend fun pushInventoryAfterSale() {
        runCatching { cloud.pushInventoryAll(inventory.getAll()) }
    }

    override fun observeOrders(fromMillis: Long, toMillis: Long): Flow<List<Order>> {
        return combine(
            orders.observeBetween(fromMillis, toMillis),
            lines.observeAll(),
        ) { orderRows, allLines ->
            val byOrder = allLines.groupBy { it.orderId }
            orderRows.map { o ->
                o.toModel(byOrder[o.id].orEmpty().map { it.toModel() })
            }
        }
    }

    override suspend fun previewStockNeeds(cart: List<CartLine>): List<StockNeed> {
        val totals = mutableMapOf<String, Double>()
        for (line in cart) {
            for (r in recipes.getForMenuItem(line.menuItem.id)) {
                totals[r.inventoryItemId] = (totals[r.inventoryItemId] ?: 0.0) + r.qtyUsed * line.qty
            }
        }
        return totals.mapNotNull { (invId, needed) ->
            val item = inventory.getById(invId)?.toModel() ?: return@mapNotNull null
            StockNeed(item, needed, item.qtyOnHand)
        }
    }

    override suspend fun completeOrder(
        cart: List<CartLine>,
        type: String,
        tableNote: String,
        payment: String,
        allowNegativeStock: Boolean,
    ): Result<Order> {
        if (cart.isEmpty()) return Result.failure(IllegalStateException("Cart is empty"))
        return runCatching {
            db.withTransaction {
                val orderId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val orderLines = cart.map { c ->
                    OrderLineEntity(
                        id = UUID.randomUUID().toString(),
                        orderId = orderId,
                        menuItemId = c.menuItem.id,
                        nameSnapshot = c.menuItem.name,
                        unitPricePaise = c.menuItem.pricePaise,
                        qty = c.qty,
                        lineTotalPaise = c.lineTotalPaise,
                        lineNote = c.note.ifBlank { null },
                    )
                }
                val total = orderLines.sumOf { it.lineTotalPaise }
                val order = OrderEntity(
                    id = orderId,
                    createdAt = now,
                    status = OrderStatus.COMPLETED,
                    type = type,
                    tableNote = tableNote,
                    payment = payment,
                    totalPaise = total,
                    cancelReason = null,
                )
                orders.upsert(order)
                lines.upsertAll(orderLines)
                order.toModel(orderLines.map { it.toModel() })
            }
        }.also { result ->
            if (result.isSuccess) {
                val order = result.getOrNull()
                if (order != null) {
                    runCatching {
                        val entity = orders.getById(order.id) ?: return@runCatching
                        cloud.pushOrder(entity, lines.getForOrder(order.id))
                    }
                }
            }
        }
    }

    override suspend fun cancelOrder(orderId: String, reason: String): Result<Unit> {
        return runCatching {
            db.withTransaction {
                val existing = orders.getById(orderId) ?: error("Order not found")
                if (existing.status == OrderStatus.CANCELLED) return@withTransaction
                orders.upsert(
                    existing.copy(
                        status = OrderStatus.CANCELLED,
                        cancelReason = reason,
                    ),
                )
            }
        }.also {
            if (it.isSuccess) {
                runCatching {
                    val entity = orders.getById(orderId) ?: return@runCatching
                    cloud.pushOrder(entity, lines.getForOrder(orderId))
                }
            }
        }
    }

    override suspend fun adjustStock(inventoryId: String, delta: Double): Result<Unit> {
        return runCatching {
            val item = inventory.getById(inventoryId) ?: error("Stock item not found")
            inventory.update(item.copy(qtyOnHand = item.qtyOnHand + delta))
            pushStock(inventoryId)
        }
    }

    override suspend fun upsertInventory(item: InventoryItem) {
        val entity = item.toEntity()
        if (inventory.getById(entity.id) == null) {
            inventory.insert(entity)
        } else {
            inventory.update(entity)
        }
        runCatching { cloud.pushInventory(entity) }
    }

    override suspend fun upsertMenuItem(item: MenuItem) {
        menu.upsert(item.toEntity())
        runCatching { cloud.pushMenu(item.toEntity()) }
    }

    override suspend fun deleteMenuItem(id: String) {
        recipes.deleteForMenuItem(id)
        menu.deleteById(id)
        runCatching { cloud.deleteMenu(id) }
    }

    override suspend fun recipesFor(menuItemId: String): List<RecipeLine> =
        recipes.getForMenuItem(menuItemId).map { it.toModel() }

    override suspend fun setRecipes(menuItemId: String, newLines: List<RecipeLine>) {
        recipes.deleteForMenuItem(menuItemId)
        recipes.upsertAll(newLines.map { it.toEntity() })
        runCatching { cloud.replaceRecipes(menuItemId, newLines.map { it.toEntity() }) }
    }

    private suspend fun applyStockDelta(cart: List<CartLine>, multiplier: Double) {
        val totals = mutableMapOf<String, Double>()
        for (line in cart) {
            for (r in recipes.getForMenuItem(line.menuItem.id)) {
                totals[r.inventoryItemId] =
                    (totals[r.inventoryItemId] ?: 0.0) + r.qtyUsed * line.qty * multiplier
            }
        }
        for ((invId, delta) in totals) {
            val item = inventory.getById(invId) ?: continue
            inventory.update(item.copy(qtyOnHand = item.qtyOnHand + delta))
        }
    }
}

private fun fmt(n: Double): String =
    if (n % 1.0 == 0.0) n.toInt().toString() else "%.2f".format(n)

fun CategoryEntity.toModel() = Category(id, name, sortOrder)

fun MenuItemEntity.toModel() = MenuItem(id, name, categoryId, pricePaise, isAvailable, isCombo, noteHint)

fun MenuItem.toEntity() = MenuItemEntity(id, name, categoryId, pricePaise, isAvailable, isCombo, noteHint)

fun InventoryItemEntity.toModel() = InventoryItem(id, name, unit, qtyOnHand, lowStockThreshold)

fun InventoryItem.toEntity() = InventoryItemEntity(id, name, unit, qtyOnHand, lowStockThreshold)

fun RecipeLineEntity.toModel() = RecipeLine(id, menuItemId, inventoryItemId, qtyUsed)

fun RecipeLine.toEntity() = RecipeLineEntity(id, menuItemId, inventoryItemId, qtyUsed)

fun OrderLineEntity.toModel() = OrderLine(
    id, orderId, menuItemId, nameSnapshot, unitPricePaise, qty, lineTotalPaise, lineNote,
)

fun OrderEntity.toModel(orderLines: List<OrderLine>) = Order(
    id, createdAt, status, type, tableNote, payment, totalPaise, cancelReason, orderLines,
)
