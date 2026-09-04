package com.cafebunchai.pos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "menu_items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId")],
)
data class MenuItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String,
    val pricePaise: Int,
    val isAvailable: Boolean,
    val isCombo: Boolean,
    val noteHint: String?,
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val unit: String,
    val qtyOnHand: Double,
    val lowStockThreshold: Double,
)

@Entity(
    tableName = "recipe_lines",
    foreignKeys = [
        ForeignKey(
            entity = MenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = InventoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventoryItemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("menuItemId"), Index("inventoryItemId")],
)
data class RecipeLineEntity(
    @PrimaryKey val id: String,
    val menuItemId: String,
    val inventoryItemId: String,
    val qtyUsed: Double,
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val status: String,
    val type: String,
    val tableNote: String,
    val payment: String,
    val totalPaise: Int,
    val cancelReason: String?,
)

@Entity(
    tableName = "order_lines",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("orderId")],
)
data class OrderLineEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val menuItemId: String,
    val nameSnapshot: String,
    val unitPricePaise: Int,
    val qty: Int,
    val lineTotalPaise: Int,
    val lineNote: String?,
)
