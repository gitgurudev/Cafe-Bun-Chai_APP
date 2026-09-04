package com.cafebunchai.pos.data.cloud

import com.cafebunchai.pos.data.local.entity.CategoryEntity
import com.cafebunchai.pos.data.local.entity.InventoryItemEntity
import com.cafebunchai.pos.data.local.entity.MenuItemEntity
import com.cafebunchai.pos.data.local.entity.OrderEntity
import com.cafebunchai.pos.data.local.entity.OrderLineEntity
import com.cafebunchai.pos.data.local.entity.RecipeLineEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

const val CAFE_ID = "bun-chai"

data class CloudOrder(
    val order: OrderEntity,
    val lines: List<OrderLineEntity>,
)

data class CafeCloudSnapshot(
    val categories: List<CategoryEntity>,
    val menuItems: List<MenuItemEntity>,
    val inventory: List<InventoryItemEntity>,
    val recipes: List<RecipeLineEntity>,
    val tickets: List<CloudOrder>,
)

/**
 * Single-cafe Firestore layout (collections are created on first write):
 *
 * cafes/bun-chai
 * cafes/bun-chai/categories/{id}
 * cafes/bun-chai/menuItems/{id}
 * cafes/bun-chai/inventory/{id}
 * cafes/bun-chai/recipes/{id}
 * cafes/bun-chai/orders/{id}   // lines nested on the document
 */
class FirestoreCafeSync(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val cafeRef get() = db.collection("cafes").document(CAFE_ID)
    private val categoriesCol get() = cafeRef.collection("categories")
    private val menuCol get() = cafeRef.collection("menuItems")
    private val inventoryCol get() = cafeRef.collection("inventory")
    private val recipesCol get() = cafeRef.collection("recipes")
    private val ordersCol get() = cafeRef.collection("orders")

    suspend fun ensureCafe() {
        cafeRef.set(
            mapOf(
                "id" to CAFE_ID,
                "name" to "Cafe Bun Chai",
                "tagline" to "A Cup of Bliss",
                "schemaVersion" to 1,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    /** Copies leftover top-level `inventory` docs if the cafe path is still empty. */
    suspend fun migrateLegacyInventory() {
        val existing = inventoryCol.limit(1).get().await()
        if (!existing.isEmpty) return
        val legacy = db.collection("inventory").get().await()
        if (legacy.isEmpty) return
        val ops = legacy.documents.mapNotNull { doc ->
            val item = doc.toInventory() ?: return@mapNotNull null
            inventoryCol.document(item.id) to item.toMap()
        }
        commitSets(ops)
    }

    suspend fun pullSnapshot(): CafeCloudSnapshot {
        val categories = categoriesCol.get().await().documents.mapNotNull { it.toCategory() }
        val menuItems = menuCol.get().await().documents.mapNotNull { it.toMenu() }
        val inventory = inventoryCol.get().await().documents.mapNotNull { it.toInventory() }
        val recipes = recipesCol.get().await().documents.mapNotNull { it.toRecipe() }
        val tickets = ordersCol.get().await().documents.mapNotNull { it.toCloudOrder() }
        return CafeCloudSnapshot(categories, menuItems, inventory, recipes, tickets)
    }

    fun observe(): Flow<CafeCloudSnapshot> = combine(
        observeCol(categoriesCol) { it.toCategory() },
        observeCol(menuCol) { it.toMenu() },
        observeCol(inventoryCol) { it.toInventory() },
        observeCol(recipesCol) { it.toRecipe() },
        observeCol(ordersCol) { it.toCloudOrder() },
    ) { cats, menu, inv, rec, tickets ->
        CafeCloudSnapshot(cats, menu, inv, rec, tickets)
    }

    suspend fun pushEntireCafe(
        categories: List<CategoryEntity>,
        menuItems: List<MenuItemEntity>,
        inventory: List<InventoryItemEntity>,
        recipes: List<RecipeLineEntity>,
        tickets: List<CloudOrder>,
    ) {
        ensureCafe()
        val ops = buildList {
            addAll(categories.map { categoriesCol.document(it.id) to it.toMap() })
            addAll(menuItems.map { menuCol.document(it.id) to it.toMap() })
            addAll(inventory.map { inventoryCol.document(it.id) to it.toMap() })
            addAll(recipes.map { recipesCol.document(it.id) to it.toMap() })
            addAll(tickets.map { ordersCol.document(it.order.id) to it.toMap() })
        }
        commitSets(ops)
    }

    suspend fun pushInventory(item: InventoryItemEntity) {
        inventoryCol.document(item.id).set(item.toMap(), SetOptions.merge()).await()
    }

    suspend fun pushInventoryAll(items: List<InventoryItemEntity>) {
        commitSets(items.map { inventoryCol.document(it.id) to it.toMap() })
    }

    suspend fun pushMenu(item: MenuItemEntity) {
        menuCol.document(item.id).set(item.toMap(), SetOptions.merge()).await()
    }

    suspend fun deleteMenu(id: String) {
        val recipeDocs = recipesCol.whereEqualTo("menuItemId", id).get().await()
        recipeDocs.documents.forEach { it.reference.delete().await() }
        menuCol.document(id).delete().await()
    }

    suspend fun replaceRecipes(menuItemId: String, lines: List<RecipeLineEntity>) {
        val old = recipesCol.whereEqualTo("menuItemId", menuItemId).get().await()
        old.documents.forEach { it.reference.delete().await() }
        commitSets(lines.map { recipesCol.document(it.id) to it.toMap() })
    }

    suspend fun pushOrder(order: OrderEntity, lines: List<OrderLineEntity>) {
        ordersCol.document(order.id).set(CloudOrder(order, lines).toMap(), SetOptions.merge()).await()
    }

    private suspend fun commitSets(ops: List<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any?>>>) {
        if (ops.isEmpty()) return
        ops.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (ref, data) -> batch.set(ref, data, SetOptions.merge()) }
            batch.commit().await()
        }
    }

    private fun <T> observeCol(
        col: com.google.firebase.firestore.CollectionReference,
        map: (DocumentSnapshot) -> T?,
    ): Flow<List<T>> = callbackFlow {
        val reg = col.addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            trySend(snap.documents.mapNotNull(map))
        }
        awaitClose { reg.remove() }
    }
}

private fun CategoryEntity.toMap() = mapOf(
    "id" to id,
    "name" to name,
    "sortOrder" to sortOrder,
)

private fun MenuItemEntity.toMap() = mapOf(
    "id" to id,
    "name" to name,
    "categoryId" to categoryId,
    "pricePaise" to pricePaise,
    "isAvailable" to isAvailable,
    "isCombo" to isCombo,
    "noteHint" to noteHint,
)

private fun InventoryItemEntity.toMap() = mapOf(
    "id" to id,
    "name" to name,
    "unit" to unit,
    "qtyOnHand" to qtyOnHand,
    "lowStockThreshold" to lowStockThreshold,
)

private fun RecipeLineEntity.toMap() = mapOf(
    "id" to id,
    "menuItemId" to menuItemId,
    "inventoryItemId" to inventoryItemId,
    "qtyUsed" to qtyUsed,
)

private fun CloudOrder.toMap(): Map<String, Any?> = mapOf(
    "id" to order.id,
    "createdAt" to order.createdAt,
    "status" to order.status,
    "type" to order.type,
    "tableNote" to order.tableNote,
    "payment" to order.payment,
    "totalPaise" to order.totalPaise,
    "cancelReason" to order.cancelReason,
    "lines" to lines.map { line ->
        mapOf(
            "id" to line.id,
            "orderId" to line.orderId,
            "menuItemId" to line.menuItemId,
            "nameSnapshot" to line.nameSnapshot,
            "unitPricePaise" to line.unitPricePaise,
            "qty" to line.qty,
            "lineTotalPaise" to line.lineTotalPaise,
            "lineNote" to line.lineNote,
        )
    },
)

private fun DocumentSnapshot.intField(name: String): Int =
    (getLong(name) ?: getDouble(name)?.toLong() ?: 0L).toInt()

private fun DocumentSnapshot.longField(name: String): Long =
    getLong(name) ?: getDouble(name)?.toLong() ?: 0L

private fun DocumentSnapshot.toCategory(): CategoryEntity? {
    val id = getString("id") ?: id
    val name = getString("name") ?: return null
    return CategoryEntity(id, name, intField("sortOrder"))
}

private fun DocumentSnapshot.toMenu(): MenuItemEntity? {
    val id = getString("id") ?: id
    val name = getString("name") ?: return null
    val categoryId = getString("categoryId") ?: return null
    return MenuItemEntity(
        id = id,
        name = name,
        categoryId = categoryId,
        pricePaise = intField("pricePaise"),
        isAvailable = getBoolean("isAvailable") ?: true,
        isCombo = getBoolean("isCombo") ?: false,
        noteHint = getString("noteHint"),
    )
}

private fun DocumentSnapshot.toInventory(): InventoryItemEntity? {
    val id = getString("id") ?: id
    val name = getString("name") ?: return null
    return InventoryItemEntity(
        id = id,
        name = name,
        unit = getString("unit") ?: "pcs",
        qtyOnHand = getDouble("qtyOnHand") ?: 0.0,
        lowStockThreshold = getDouble("lowStockThreshold") ?: 0.0,
    )
}

private fun DocumentSnapshot.toRecipe(): RecipeLineEntity? {
    val id = getString("id") ?: id
    val menuItemId = getString("menuItemId") ?: return null
    val inventoryItemId = getString("inventoryItemId") ?: return null
    return RecipeLineEntity(
        id = id,
        menuItemId = menuItemId,
        inventoryItemId = inventoryItemId,
        qtyUsed = getDouble("qtyUsed") ?: 0.0,
    )
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toCloudOrder(): CloudOrder? {
    val orderId = getString("id") ?: id
    val order = OrderEntity(
        id = orderId,
        createdAt = longField("createdAt"),
        status = getString("status") ?: return null,
        type = getString("type") ?: "takeaway",
        tableNote = getString("tableNote").orEmpty(),
        payment = getString("payment") ?: "cash",
        totalPaise = intField("totalPaise"),
        cancelReason = getString("cancelReason"),
    )
    val rawLines = get("lines") as? List<Map<String, Any?>> ?: emptyList()
    val lines = rawLines.mapNotNull { m ->
        val lineId = m["id"] as? String ?: return@mapNotNull null
        OrderLineEntity(
            id = lineId,
            orderId = (m["orderId"] as? String) ?: orderId,
            menuItemId = (m["menuItemId"] as? String).orEmpty(),
            nameSnapshot = (m["nameSnapshot"] as? String).orEmpty(),
            unitPricePaise = (m["unitPricePaise"] as? Number)?.toInt() ?: 0,
            qty = (m["qty"] as? Number)?.toInt() ?: 1,
            lineTotalPaise = (m["lineTotalPaise"] as? Number)?.toInt() ?: 0,
            lineNote = m["lineNote"] as? String,
        )
    }
    return CloudOrder(order, lines)
}
