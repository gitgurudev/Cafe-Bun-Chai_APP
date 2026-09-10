package com.cafebunchai.pos.data.stock

import com.cafebunchai.pos.data.model.CartLine
import com.cafebunchai.pos.data.model.InventoryItem
import com.cafebunchai.pos.data.model.MenuItem
import com.cafebunchai.pos.data.model.OrderLine
import com.cafebunchai.pos.data.model.RecipeLine

object StockMath {
    fun usage(
        menuItemId: String,
        menuName: String,
        qty: Int,
        recipes: List<RecipeLine>,
        stock: List<InventoryItem>,
    ): Map<String, Double> {
        val recipeLines = recipes.filter { it.menuItemId == menuItemId }
        if (recipeLines.isNotEmpty()) {
            val totals = mutableMapOf<String, Double>()
            for (line in recipeLines) {
                totals[line.inventoryItemId] =
                    (totals[line.inventoryItemId] ?: 0.0) + line.qtyUsed * qty
            }
            return totals
        }
        val match = matchStock(menuName, stock) ?: return emptyMap()
        return mapOf(match.id to qty.toDouble())
    }

    fun remainingAfterCart(
        stock: List<InventoryItem>,
        recipes: List<RecipeLine>,
        cart: List<CartLine>,
        restore: List<OrderLine>,
    ): Map<String, Double> {
        val qty = stock.associate { it.id to it.qtyOnHand }.toMutableMap()
        for (line in restore) {
            for ((id, amount) in usage(line.menuItemId, line.nameSnapshot, line.qty, recipes, stock)) {
                qty[id] = (qty[id] ?: 0.0) + amount
            }
        }
        for (line in cart) {
            for ((id, amount) in usage(line.menuItem.id, line.menuItem.name, line.qty, recipes, stock)) {
                qty[id] = (qty[id] ?: 0.0) - amount
            }
        }
        return qty
    }

    fun canMake(
        item: MenuItem,
        qty: Int,
        remaining: Map<String, Double>,
        recipes: List<RecipeLine>,
        stock: List<InventoryItem>,
    ): Boolean {
        val need = usage(item.id, item.name, qty, recipes, stock)
        if (need.isEmpty()) return true
        return need.all { (id, amount) -> (remaining[id] ?: 0.0) + 0.0001 >= amount }
    }

    private fun matchStock(menuName: String, stock: List<InventoryItem>): InventoryItem? {
        val n = normalizeName(menuName)
        if (n.isBlank()) return null
        stock.find { normalizeName(it.name) == n }?.let { return it }
        val contained = stock.filter {
            val sn = normalizeName(it.name)
            sn.contains(n) || n.contains(sn)
        }
        if (contained.size == 1) return contained.first()
        val tokens = n.split(" ").filter { it.length >= 4 }
        for (token in tokens) {
            val hits = stock.filter { normalizeName(it.name).contains(token) }
            if (hits.size == 1) return hits.first()
        }
        return null
    }

    private fun normalizeName(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
}
