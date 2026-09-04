package com.cafebunchai.pos.data.backup

import androidx.room.withTransaction
import com.cafebunchai.pos.data.local.AppDatabase
import com.cafebunchai.pos.data.local.entity.CategoryEntity
import com.cafebunchai.pos.data.local.entity.InventoryItemEntity
import com.cafebunchai.pos.data.local.entity.MenuItemEntity
import com.cafebunchai.pos.data.local.entity.OrderEntity
import com.cafebunchai.pos.data.local.entity.OrderLineEntity
import com.cafebunchai.pos.data.local.entity.RecipeLineEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val categories: List<CategoryEntityDto>,
    val menuItems: List<MenuItemEntityDto>,
    val inventory: List<InventoryItemEntityDto>,
    val recipes: List<RecipeLineEntityDto>,
    val orders: List<OrderEntityDto>,
    val orderLines: List<OrderLineEntityDto>,
)

@Serializable
data class CategoryEntityDto(val id: String, val name: String, val sortOrder: Int)

@Serializable
data class MenuItemEntityDto(
    val id: String,
    val name: String,
    val categoryId: String,
    val pricePaise: Int,
    val isAvailable: Boolean,
    val isCombo: Boolean,
    val noteHint: String? = null,
)

@Serializable
data class InventoryItemEntityDto(
    val id: String,
    val name: String,
    val unit: String,
    val qtyOnHand: Double,
    val lowStockThreshold: Double,
)

@Serializable
data class RecipeLineEntityDto(
    val id: String,
    val menuItemId: String,
    val inventoryItemId: String,
    val qtyUsed: Double,
)

@Serializable
data class OrderEntityDto(
    val id: String,
    val createdAt: Long,
    val status: String,
    val type: String,
    val tableNote: String,
    val payment: String,
    val totalPaise: Int,
    val cancelReason: String? = null,
)

@Serializable
data class OrderLineEntityDto(
    val id: String,
    val orderId: String,
    val menuItemId: String,
    val nameSnapshot: String,
    val unitPricePaise: Int,
    val qty: Int,
    val lineTotalPaise: Int,
    val lineNote: String? = null,
)

class BackupManager(private val db: AppDatabase) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportJson(): String {
        val payload = BackupPayload(
            exportedAt = System.currentTimeMillis(),
            categories = db.categoryDao().getAll().map { CategoryEntityDto(it.id, it.name, it.sortOrder) },
            menuItems = db.menuItemDao().getAll().map {
                MenuItemEntityDto(it.id, it.name, it.categoryId, it.pricePaise, it.isAvailable, it.isCombo, it.noteHint)
            },
            inventory = db.inventoryDao().getAll().map {
                InventoryItemEntityDto(it.id, it.name, it.unit, it.qtyOnHand, it.lowStockThreshold)
            },
            recipes = db.recipeDao().getAll().map {
                RecipeLineEntityDto(it.id, it.menuItemId, it.inventoryItemId, it.qtyUsed)
            },
            orders = db.orderDao().getAll().map {
                OrderEntityDto(
                    it.id, it.createdAt, it.status, it.type, it.tableNote, it.payment, it.totalPaise, it.cancelReason,
                )
            },
            orderLines = db.orderLineDao().getAll().map {
                OrderLineEntityDto(
                    it.id, it.orderId, it.menuItemId, it.nameSnapshot, it.unitPricePaise, it.qty, it.lineTotalPaise, it.lineNote,
                )
            },
        )
        return json.encodeToString(payload)
    }

    suspend fun importJson(text: String) {
        val payload = json.decodeFromString<BackupPayload>(text)
        db.withTransaction {
            db.orderLineDao().deleteAll()
            db.orderDao().deleteAll()
            db.recipeDao().deleteAll()
            db.menuItemDao().deleteAll()
            db.inventoryDao().deleteAll()
            db.categoryDao().deleteAll()
            db.categoryDao().upsertAll(
                payload.categories.map { CategoryEntity(it.id, it.name, it.sortOrder) },
            )
            db.inventoryDao().upsertAll(
                payload.inventory.map {
                    InventoryItemEntity(it.id, it.name, it.unit, it.qtyOnHand, it.lowStockThreshold)
                },
            )
            db.menuItemDao().upsertAll(
                payload.menuItems.map {
                    MenuItemEntity(it.id, it.name, it.categoryId, it.pricePaise, it.isAvailable, it.isCombo, it.noteHint)
                },
            )
            db.recipeDao().upsertAll(
                payload.recipes.map {
                    RecipeLineEntity(it.id, it.menuItemId, it.inventoryItemId, it.qtyUsed)
                },
            )
            db.orderDao().upsertAll(
                payload.orders.map {
                    OrderEntity(
                        it.id, it.createdAt, it.status, it.type, it.tableNote, it.payment, it.totalPaise, it.cancelReason,
                    )
                },
            )
            db.orderLineDao().upsertAll(
                payload.orderLines.map {
                    OrderLineEntity(
                        it.id, it.orderId, it.menuItemId, it.nameSnapshot, it.unitPricePaise, it.qty, it.lineTotalPaise, it.lineNote,
                    )
                },
            )
        }
    }
}
