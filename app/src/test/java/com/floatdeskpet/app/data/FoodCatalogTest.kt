package com.floatdeskpet.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodCatalogTest {
    @Test
    fun fourKindsAndPricedItems() {
        assertEquals(
            listOf("小食", "正餐", "甜品", "饮品"),
            FoodKind.entries.map { it.title },
        )
        assertTrue(FoodCatalog.items.size >= 8)
        assertTrue(FoodCatalog.items.all { it.cost > 0 && it.name.isNotBlank() && it.icon.isNotBlank() })
        FoodKind.entries.forEach { kind ->
            assertTrue(FoodCatalog.ofKind(kind).isNotEmpty())
        }
    }

    @Test
    fun spendRules() {
        assertTrue(FoodCatalog.canAfford(88, 16))
        assertFalse(FoodCatalog.canAfford(8, 16))
        assertFalse(FoodCatalog.canAfford(10, 0))
        assertEquals(72, FoodCatalog.afterSpend(88, 16))
        assertEquals(0, FoodCatalog.afterSpend(5, 16))
    }

    @Test
    fun cardioKcalRange() {
        repeat(20) {
            val n = FoodCatalog.cardioKcal()
            assertTrue(n in 3..28)
        }
        assertEquals(7, FoodCatalog.cardioKcal(7))
        assertEquals(3, FoodCatalog.cardioKcal(1))
        assertEquals(28, FoodCatalog.cardioKcal(99))
    }
}
