package com.floatdeskpet.app.data

enum class BondStage {
    FIRST,
    WARM,
    TACIT,
    BOND,
}

object CompanionBond {
    fun score(settings: PetSettings): Int {
        return ((settings.affection * 7) + (settings.mood * 3)) / 10
    }

    fun stage(settings: PetSettings): BondStage {
        val s = score(settings)
        return when {
            s >= 75 -> BondStage.BOND
            s >= 50 -> BondStage.TACIT
            s >= 25 -> BondStage.WARM
            else -> BondStage.FIRST
        }
    }

    fun richer(settings: PetSettings): Boolean = stage(settings) != BondStage.FIRST

    fun progress(settings: PetSettings): Int {
        val s = score(settings).coerceIn(0, 100)
        val (lo, hi) = when (stage(settings)) {
            BondStage.FIRST -> 0 to 25
            BondStage.WARM -> 25 to 50
            BondStage.TACIT -> 50 to 75
            BondStage.BOND -> 75 to 100
        }
        if (hi <= lo) return 100
        return (((s - lo) * 100) / (hi - lo)).coerceIn(0, 100)
    }

    fun futureCosmeticKeys(settings: PetSettings): List<String> {
        return when (stage(settings)) {
            BondStage.FIRST -> emptyList()
            BondStage.WARM -> listOf("tone")
            BondStage.TACIT -> listOf("tone", "idle_extra")
            BondStage.BOND -> listOf("tone", "idle_extra", "keepsake")
        }
    }
}
