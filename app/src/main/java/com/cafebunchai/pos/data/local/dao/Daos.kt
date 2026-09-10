package com.cafebunchai.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cafebunchai.pos.data.local.entity.CategoryEntity
import com.cafebunchai.pos.data.local.entity.InventoryItemEntity
import com.cafebunchai.pos.data.local.entity.MenuItemEntity
import com.cafebunchai.pos.data.local.entity.OrderEntity
import com.cafebunchai.pos.data.local.entity.OrderLineEntity
import com.cafebunchai.pos.data.local.entity.RecipeLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder")
    suspend fun getAll(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface MenuItemDao {
    @Query("SELECT * FROM menu_items ORDER BY name")
    fun observeAll(): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items ORDER BY name")
    suspend fun getAll(): List<MenuItemEntity>

    @Query("SELECT * FROM menu_items WHERE id = :id")
    suspend fun getById(id: String): MenuItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MenuItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MenuItemEntity>)

    @Query("DELETE FROM menu_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM menu_items")
    suspend fun deleteAll()
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY name")
    fun observeAll(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items ORDER BY name")
    suspend fun getAll(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getById(id: String): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: InventoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InventoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<InventoryItemEntity>)

    @Update
    suspend fun update(item: InventoryItemEntity)

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAll()
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipe_lines")
    fun observeAll(): Flow<List<RecipeLineEntity>>

    @Query("SELECT * FROM recipe_lines")
    suspend fun getAll(): List<RecipeLineEntity>

    @Query("SELECT * FROM recipe_lines WHERE menuItemId = :menuItemId")
    suspend fun getForMenuItem(menuItemId: String): List<RecipeLineEntity>

    @Query("SELECT * FROM recipe_lines WHERE menuItemId = :menuItemId")
    fun observeForMenuItem(menuItemId: String): Flow<List<RecipeLineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(line: RecipeLineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lines: List<RecipeLineEntity>)

    @Query("DELETE FROM recipe_lines WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recipe_lines WHERE menuItemId = :menuItemId")
    suspend fun deleteForMenuItem(menuItemId: String)

    @Query("DELETE FROM recipe_lines")
    suspend fun deleteAll()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE createdAt BETWEEN :from AND :to ORDER BY createdAt DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAll(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(orders: List<OrderEntity>)

    @Query("DELETE FROM orders")
    suspend fun deleteAll()
}

@Dao
interface OrderLineDao {
    @Query("SELECT * FROM order_lines WHERE orderId = :orderId")
    suspend fun getForOrder(orderId: String): List<OrderLineEntity>

    @Query("SELECT * FROM order_lines")
    fun observeAll(): Flow<List<OrderLineEntity>>

    @Query("SELECT * FROM order_lines")
    suspend fun getAll(): List<OrderLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lines: List<OrderLineEntity>)

    @Query("DELETE FROM order_lines WHERE orderId = :orderId")
    suspend fun deleteForOrder(orderId: String)

    @Query("DELETE FROM order_lines")
    suspend fun deleteAll()
}
