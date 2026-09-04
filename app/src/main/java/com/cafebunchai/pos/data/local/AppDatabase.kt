package com.cafebunchai.pos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cafebunchai.pos.data.local.dao.CategoryDao
import com.cafebunchai.pos.data.local.dao.InventoryDao
import com.cafebunchai.pos.data.local.dao.MenuItemDao
import com.cafebunchai.pos.data.local.dao.OrderDao
import com.cafebunchai.pos.data.local.dao.OrderLineDao
import com.cafebunchai.pos.data.local.dao.RecipeDao
import com.cafebunchai.pos.data.local.entity.CategoryEntity
import com.cafebunchai.pos.data.local.entity.InventoryItemEntity
import com.cafebunchai.pos.data.local.entity.MenuItemEntity
import com.cafebunchai.pos.data.local.entity.OrderEntity
import com.cafebunchai.pos.data.local.entity.OrderLineEntity
import com.cafebunchai.pos.data.local.entity.RecipeLineEntity

@Database(
    entities = [
        CategoryEntity::class,
        MenuItemEntity::class,
        InventoryItemEntity::class,
        RecipeLineEntity::class,
        OrderEntity::class,
        OrderLineEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun menuItemDao(): MenuItemDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun recipeDao(): RecipeDao
    abstract fun orderDao(): OrderDao
    abstract fun orderLineDao(): OrderLineDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cafe_bun_chai.db",
                ).build().also { instance = it }
            }
        }
    }

    suspend fun seedIfEmpty() {
        if (categoryDao().getAll().isNotEmpty()) return
        val seed = SeedData.build()
        categoryDao().upsertAll(seed.categories)
        inventoryDao().upsertAll(seed.inventory)
        menuItemDao().upsertAll(seed.menuItems)
        recipeDao().upsertAll(seed.recipes)
    }
}
