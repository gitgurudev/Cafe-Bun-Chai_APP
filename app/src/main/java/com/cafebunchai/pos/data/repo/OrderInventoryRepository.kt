package com.cafebunchai.pos.data.repo

import com.cafebunchai.pos.data.cloud.CafeCloudSnapshot
import com.cafebunchai.pos.data.model.CartLine
import com.cafebunchai.pos.data.model.Category
import com.cafebunchai.pos.data.model.InventoryItem
import com.cafebunchai.pos.data.model.MenuItem
import com.cafebunchai.pos.data.model.Order
import com.cafebunchai.pos.data.model.RecipeLine
import com.cafebunchai.pos.data.model.StockNeed
import kotlinx.coroutines.flow.Flow

interface OrderInventoryRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeMenuItems(): Flow<List<MenuItem>>
    fun observeInventory(): Flow<List<InventoryItem>>
    fun observeOrders(fromMillis: Long, toMillis: Long): Flow<List<Order>>

    suspend fun previewStockNeeds(cart: List<CartLine>): List<StockNeed>
    suspend fun completeOrder(
        cart: List<CartLine>,
        type: String,
        tableNote: String,
        payment: String,
        allowNegativeStock: Boolean,
    ): Result<Order>

    suspend fun cancelOrder(orderId: String, reason: String): Result<Unit>

    suspend fun adjustStock(inventoryId: String, delta: Double): Result<Unit>
    suspend fun upsertInventory(item: InventoryItem)

    suspend fun upsertMenuItem(item: MenuItem)
    suspend fun deleteMenuItem(id: String)
    suspend fun recipesFor(menuItemId: String): List<RecipeLine>
    suspend fun setRecipes(menuItemId: String, lines: List<RecipeLine>)

    suspend fun hydrateFromCloud()
    fun observeCloud(): Flow<CafeCloudSnapshot>
    suspend fun applyCloud(snapshot: CafeCloudSnapshot)
    suspend fun pushLocalToCloud()
}
