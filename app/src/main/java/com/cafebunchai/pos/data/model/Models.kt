package com.cafebunchai.pos.data.model

data class Category(
    val id: String,
    val name: String,
    val sortOrder: Int,
)

data class MenuItem(
    val id: String,
    val name: String,
    val categoryId: String,
    val pricePaise: Int,
    val isAvailable: Boolean,
    val isCombo: Boolean,
    val noteHint: String?,
)

data class InventoryItem(
    val id: String,
    val name: String,
    val unit: String,
    val qtyOnHand: Double,
    val lowStockThreshold: Double,
) {
    val isLow: Boolean get() = qtyOnHand <= lowStockThreshold
}

data class RecipeLine(
    val id: String,
    val menuItemId: String,
    val inventoryItemId: String,
    val qtyUsed: Double,
)

data class Order(
    val id: String,
    val createdAt: Long,
    val status: String,
    val type: String,
    val tableNote: String,
    val payment: String,
    val totalPaise: Int,
    val cancelReason: String?,
    val lines: List<OrderLine> = emptyList(),
)

data class OrderLine(
    val id: String,
    val orderId: String,
    val menuItemId: String,
    val nameSnapshot: String,
    val unitPricePaise: Int,
    val qty: Int,
    val lineTotalPaise: Int,
    val lineNote: String?,
)

data class CartLine(
    val menuItem: MenuItem,
    val qty: Int,
    val note: String = "",
) {
    val lineTotalPaise: Int get() = menuItem.pricePaise * qty
}

data class StockNeed(
    val inventory: InventoryItem,
    val needed: Double,
    val available: Double,
) {
    val shortfall: Boolean get() = needed > available + 0.0001
}

data class CompletedSale(
    val order: Order,
    val refillAlerts: List<InventoryItem> = emptyList(),
)

object OrderStatus {
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"
}

object OrderType {
    const val DINE_IN = "dine_in"
    const val TAKEAWAY = "takeaway"
}

object Payment {
    const val CASH = "cash"
    const val UPI = "upi"
    const val UNPAID = "unpaid"
}
