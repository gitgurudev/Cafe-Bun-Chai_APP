package com.cafebunchai.pos.data.local

import com.cafebunchai.pos.data.local.entity.CategoryEntity
import com.cafebunchai.pos.data.local.entity.InventoryItemEntity
import com.cafebunchai.pos.data.local.entity.MenuItemEntity
import com.cafebunchai.pos.data.local.entity.RecipeLineEntity

data class SeedBundle(
    val categories: List<CategoryEntity>,
    val inventory: List<InventoryItemEntity>,
    val menuItems: List<MenuItemEntity>,
    val recipes: List<RecipeLineEntity>,
)

/**
 * Starter menu from Cafe Bun Chai Instagram. Prices are placeholders — edit in Menu.
 */
object SeedData {
    // Categories
    const val CAT_CHAI = "cat-chai"
    const val CAT_BUN = "cat-bun-maska"
    const val CAT_FRIES = "cat-fries"
    const val CAT_DRINKS = "cat-drinks"
    const val CAT_BURGERS = "cat-burgers"
    const val CAT_SANDWICHES = "cat-sandwiches"
    const val CAT_COMBOS = "cat-combos"
    const val CAT_ICY = "cat-icy-pops"
    const val CAT_SNACKS = "cat-snacks"

    // Inventory
    const val INV_BUNS = "inv-buns"
    const val INV_BUTTER = "inv-butter"
    const val INV_MILK = "inv-milk"
    const val INV_TEA = "inv-tea"
    const val INV_JAGGERY = "inv-jaggery"
    const val INV_COFFEE = "inv-coffee"
    const val INV_MAGGI = "inv-maggi"
    const val INV_FRIES = "inv-fries"
    const val INV_PATTY = "inv-patty"
    const val INV_BREAD = "inv-bread"
    const val INV_NACHOS = "inv-nachos"
    const val INV_ICY = "inv-icy-pops"

    // Menu
    const val MENU_JAGGERY_TEA = "menu-jaggery-tea"
    const val MENU_CUTTING_CHAI = "menu-cutting-chai"
    const val MENU_COFFEE = "menu-coffee"
    const val MENU_BUN_MASKA = "menu-grilled-bun-maska"
    const val MENU_CHAI_BUN = "menu-chai-bun-combo"
    const val MENU_MAGGI = "menu-maggi"
    const val MENU_MAGGI_FRIES = "menu-maggi-fries"
    const val MENU_NACHOS = "menu-nachos"
    const val MENU_BURGER = "menu-veg-burger"
    const val MENU_SANDWICH = "menu-veg-sandwich"
    const val MENU_FRIES = "menu-fries"
    const val MENU_ICY = "menu-icy-pops"

    fun build(): SeedBundle {
        val categories = listOf(
            CategoryEntity(CAT_CHAI, "Chai", 0),
            CategoryEntity(CAT_BUN, "Bun Maska", 1),
            CategoryEntity(CAT_FRIES, "Fries", 2),
            CategoryEntity(CAT_DRINKS, "Drinks", 3),
            CategoryEntity(CAT_BURGERS, "Burgers", 4),
            CategoryEntity(CAT_SANDWICHES, "Sandwiches", 5),
            CategoryEntity(CAT_COMBOS, "Combos", 6),
            CategoryEntity(CAT_ICY, "Icy Pops", 7),
            CategoryEntity(CAT_SNACKS, "Snacks", 8),
        )

        val inventory = listOf(
            InventoryItemEntity(INV_BUNS, "Buns", "pcs", 40.0, 10.0),
            InventoryItemEntity(INV_BUTTER, "Butter / Maska", "kg", 2.0, 0.3),
            InventoryItemEntity(INV_MILK, "Milk", "litre", 10.0, 2.0),
            InventoryItemEntity(INV_TEA, "Tea leaves", "kg", 1.0, 0.2),
            InventoryItemEntity(INV_JAGGERY, "Jaggery", "kg", 2.0, 0.3),
            InventoryItemEntity(INV_COFFEE, "Coffee", "kg", 0.5, 0.1),
            InventoryItemEntity(INV_MAGGI, "Maggi packs", "pcs", 24.0, 6.0),
            InventoryItemEntity(INV_FRIES, "Fries portions", "pcs", 30.0, 8.0),
            InventoryItemEntity(INV_PATTY, "Burger patties", "pcs", 20.0, 5.0),
            InventoryItemEntity(INV_BREAD, "Bread slices", "pcs", 40.0, 10.0),
            InventoryItemEntity(INV_NACHOS, "Nachos portions", "pcs", 15.0, 4.0),
            InventoryItemEntity(INV_ICY, "Icy Pops", "pcs", 33.0, 11.0),
        )

        val menu = listOf(
            MenuItemEntity(MENU_JAGGERY_TEA, "Jaggery Tea", CAT_CHAI, 2500, true, false, null),
            MenuItemEntity(MENU_CUTTING_CHAI, "Cutting Chai", CAT_CHAI, 2000, true, false, null),
            MenuItemEntity(MENU_COFFEE, "Coffee", CAT_DRINKS, 4000, true, false, null),
            MenuItemEntity(MENU_BUN_MASKA, "Grilled Bun Maska", CAT_BUN, 4000, true, false, null),
            MenuItemEntity(MENU_CHAI_BUN, "Chai + Bun Maska combo", CAT_COMBOS, 6000, true, true, null),
            MenuItemEntity(MENU_MAGGI, "Maggi", CAT_SNACKS, 5000, true, false, null),
            MenuItemEntity(MENU_MAGGI_FRIES, "Maggi & Fries", CAT_COMBOS, 9000, true, true, null),
            MenuItemEntity(MENU_NACHOS, "Nachos Cheese Corn Chaat", CAT_SNACKS, 8000, true, false, null),
            MenuItemEntity(MENU_BURGER, "Veg Burger", CAT_BURGERS, 8000, true, false, null),
            MenuItemEntity(MENU_SANDWICH, "Veg Sandwich", CAT_SANDWICHES, 7000, true, false, null),
            MenuItemEntity(MENU_FRIES, "Fries", CAT_FRIES, 5000, true, false, null),
            MenuItemEntity(
                MENU_ICY,
                "Icy Pops",
                CAT_ICY,
                2000,
                true,
                false,
                "Note flavour (11 flavours)",
            ),
        )

        fun r(id: String, menuId: String, invId: String, qty: Double) =
            RecipeLineEntity(id, menuId, invId, qty)

        val recipes = listOf(
            r("r-jt-milk", MENU_JAGGERY_TEA, INV_MILK, 0.15),
            r("r-jt-tea", MENU_JAGGERY_TEA, INV_TEA, 0.005),
            r("r-jt-jag", MENU_JAGGERY_TEA, INV_JAGGERY, 0.02),
            r("r-cc-milk", MENU_CUTTING_CHAI, INV_MILK, 0.12),
            r("r-cc-tea", MENU_CUTTING_CHAI, INV_TEA, 0.004),
            r("r-cf-milk", MENU_COFFEE, INV_MILK, 0.15),
            r("r-cf-cof", MENU_COFFEE, INV_COFFEE, 0.008),
            r("r-bm-bun", MENU_BUN_MASKA, INV_BUNS, 1.0),
            r("r-bm-but", MENU_BUN_MASKA, INV_BUTTER, 0.02),
            r("r-cb-milk", MENU_CHAI_BUN, INV_MILK, 0.15),
            r("r-cb-tea", MENU_CHAI_BUN, INV_TEA, 0.005),
            r("r-cb-jag", MENU_CHAI_BUN, INV_JAGGERY, 0.02),
            r("r-cb-bun", MENU_CHAI_BUN, INV_BUNS, 1.0),
            r("r-cb-but", MENU_CHAI_BUN, INV_BUTTER, 0.02),
            r("r-mg-pack", MENU_MAGGI, INV_MAGGI, 1.0),
            r("r-mf-pack", MENU_MAGGI_FRIES, INV_MAGGI, 1.0),
            r("r-mf-fry", MENU_MAGGI_FRIES, INV_FRIES, 1.0),
            r("r-na-n", MENU_NACHOS, INV_NACHOS, 1.0),
            r("r-bg-bun", MENU_BURGER, INV_BUNS, 1.0),
            r("r-bg-pat", MENU_BURGER, INV_PATTY, 1.0),
            r("r-sw-br", MENU_SANDWICH, INV_BREAD, 2.0),
            r("r-fr", MENU_FRIES, INV_FRIES, 1.0),
            r("r-icy", MENU_ICY, INV_ICY, 1.0),
        )

        return SeedBundle(categories, inventory, menu, recipes)
    }
}
