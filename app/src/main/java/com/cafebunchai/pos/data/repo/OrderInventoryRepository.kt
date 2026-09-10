package com.cafebunchai.pos.data.repo

import com.cafebunchai.pos.data.cloud.CafeCloudSnapshot
import com.cafebunchai.pos.data.cloud.CloudOrder
import com.cafebunchai.pos.data.local.entity.CategoryEntity
import com.cafebunchai.pos.data.local.entity.InventoryItemEntity
import com.cafebunchai.pos.data.local.entity.MenuItemEntity
import com.cafebunchai.pos.data.local.entity.RecipeLineEntity
import com.cafebunchai.pos.data.model.CartLine
import com.cafebunchai.pos.data.model.Category
import com.cafebunchai.pos.data.model.CompletedSale
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
    fun observeRecipes(): Flow<List<RecipeLine>>
    fun observeOrders(fromMillis: Long, toMillis: Long): Flow<List<Order>>

    suspend fun previewStockNeeds(cart: List<CartLine>): List<StockNeed>
    suspend fun completeOrder(
        cart: List<CartLine>,
        type: String,
        tableNote: String,
        payment: String,
        allowNegativeStock: Boolean,
        existingOrderId: String? = null,
    ): Result<CompletedSale>

    suspend fun cancelOrder(orderId: String, reason: String): Result<Unit>

    suspend fun adjustStock(inventoryId: String, delta: Double): Result<Unit>
    suspend fun upsertInventory(item: InventoryItem)

    suspend fun upsertMenuItem(item: MenuItem)
    suspend fun deleteMenuItem(id: String)
    suspend fun recipesFor(menuItemId: String): List<RecipeLine>
    suspend fun setRecipes(menuItemId: String, lines: List<RecipeLine>)

    suspend fun hydrateFromCloud()
    fun observeCloudTickets(): Flow<List<CloudOrder>>
    fun observeCloudInventory(): Flow<List<InventoryItemEntity>>
    fun observeCloudCatalog(): Flow<Triple<List<CategoryEntity>, List<MenuItemEntity>, List<RecipeLineEntity>>>
    suspend fun applyCloud(snapshot: CafeCloudSnapshot)
    suspend fun applyLiveTickets(tickets: List<CloudOrder>)
    suspend fun applyLiveInventory(items: List<InventoryItemEntity>)
    suspend fun applyLiveCatalog(
        categories: List<CategoryEntity>,
        menuItems: List<MenuItemEntity>,
        recipes: List<RecipeLineEntity>,
    )
    suspend fun pushLocalToCloud()
}
