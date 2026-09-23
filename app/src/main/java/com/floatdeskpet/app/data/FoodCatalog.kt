package com.floatdeskpet.app.data

enum class FoodKind(val title: String) {
    SNACK("小食"),
    MEAL("正餐"),
    SWEET("甜品"),
    DRINK("饮品"),
}

data class FoodItem(
    val id: String,
    val kind: FoodKind,
    val name: String,
    val icon: String,
    val cost: Int,
    val moodBoost: Int,
)

object FoodCatalog {
    val items: List<FoodItem> = listOf(
        FoodItem("cookie", FoodKind.SNACK, "小饼干", "🍪", 6, 8),
        FoodItem("bun", FoodKind.SNACK, "蜜豆包", "🍞", 8, 10),
        FoodItem("seaweed", FoodKind.SNACK, "海苔脆", "🍙", 10, 11),
        FoodItem("noodle", FoodKind.MEAL, "热汤面", "🍜", 16, 16),
        FoodItem("bento", FoodKind.MEAL, "便当", "🍱", 20, 18),
        FoodItem("rice", FoodKind.MEAL, "蛋炒饭", "🍚", 18, 17),
        FoodItem("cake", FoodKind.SWEET, "草莓蛋糕", "🍰", 14, 14),
        FoodItem("pudding", FoodKind.SWEET, "布丁", "🍮", 12, 13),
        FoodItem("ice", FoodKind.SWEET, "冰淇淋", "🍦", 15, 14),
        FoodItem("milk", FoodKind.DRINK, "温牛奶", "🥛", 8, 9),
        FoodItem("cocoa", FoodKind.DRINK, "热可可", "☕", 10, 11),
        FoodItem("tea", FoodKind.DRINK, "果茶", "🧃", 9, 10),
    )

    fun ofKind(kind: FoodKind): List<FoodItem> = items.filter { it.kind == kind }

    fun byId(id: String): FoodItem? = items.find { it.id == id }

    fun canAfford(balance: Int, cost: Int): Boolean = cost > 0 && balance >= cost

    fun afterSpend(balance: Int, cost: Int): Int = (balance - cost).coerceAtLeast(0)

    fun cardioKcal(rand: Int = kotlin.random.Random.nextInt(3, 29)): Int = rand.coerceIn(3, 28)
}
