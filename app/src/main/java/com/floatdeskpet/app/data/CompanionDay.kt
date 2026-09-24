package com.floatdeskpet.app.data

import android.content.Context
import com.floatdeskpet.app.auth.AuthStore
import java.util.Calendar

data class DayTick(
    val recovered: Int,
    val celebrate: Int,
)

object CompanionDay {
    val MILESTONES = intArrayOf(3, 7, 14, 30)

    fun todayKey(): Int = keyOf(Calendar.getInstance())

    fun tick(context: Context): DayTick {
        val settings = PetSettings.get(context)
        val recovered = recoverHappiness(AuthStore.get(context), settings)
        val celebrate = markVisit(settings)
        return DayTick(recovered, celebrate)
    }

    fun markVisit(settings: PetSettings): Int {
        val today = todayKey()
        val last = settings.lastVisitDay
        if (last == today) return 0
        if (last == shift(today, -1)) {
            settings.streakDays = settings.streakDays + 1
        } else {
            settings.streakDays = 1
            settings.lastStreakCelebrate = 0
        }
        settings.lastVisitDay = today
        val streak = settings.streakDays
        val hit = MILESTONES.lastOrNull { it <= streak && it > settings.lastStreakCelebrate } ?: 0
        if (hit > 0) settings.lastStreakCelebrate = hit
        return hit
    }

    fun recoverHappiness(auth: AuthStore, settings: PetSettings): Int {
        val today = todayKey()
        if (settings.lastHappyRegenDay == today) return settings.lastHappyRegenAmount
        val gained = auth.recoverHappiness(DAILY_HAPPINESS, HAPPINESS_SOFT_CAP)
        settings.lastHappyRegenDay = today
        settings.lastHappyRegenAmount = gained
        return gained
    }

    fun notePet(settings: PetSettings, auth: AuthStore): Boolean {
        rolloverGoals(settings)
        if (settings.goalPet) return false
        settings.goalPet = true
        rewardStep(settings, auth)
        return true
    }

    fun noteFeed(settings: PetSettings, auth: AuthStore): Boolean {
        rolloverGoals(settings)
        if (settings.goalFeed) return false
        settings.goalFeed = true
        rewardStep(settings, auth)
        return true
    }

    fun noteCardio(settings: PetSettings, auth: AuthStore): Boolean {
        rolloverGoals(settings)
        if (settings.goalCardio) return false
        settings.goalCardio = true
        rewardStep(settings, auth)
        return true
    }

    fun doneCount(settings: PetSettings): Int {
        rolloverGoals(settings)
        var n = 0
        if (settings.goalPet) n += 1
        if (settings.goalFeed) n += 1
        if (settings.goalCardio) n += 1
        return n
    }

    fun rolloverGoals(settings: PetSettings) {
        val today = todayKey()
        if (settings.goalDay == today) return
        settings.goalDay = today
        settings.goalPet = false
        settings.goalFeed = false
        settings.goalCardio = false
        settings.goalBonus = false
    }

    private fun rewardStep(settings: PetSettings, auth: AuthStore) {
        auth.addHappiness(2)
        settings.affection = settings.affection + 1
        if (settings.goalPet && settings.goalFeed && settings.goalCardio && !settings.goalBonus) {
            settings.goalBonus = true
            auth.addHappiness(4)
            settings.affection = settings.affection + 2
        }
    }

    private fun keyOf(c: Calendar): Int {
        return c.get(Calendar.YEAR) * 10000 +
            (c.get(Calendar.MONTH) + 1) * 100 +
            c.get(Calendar.DAY_OF_MONTH)
    }

    private fun shift(day: Int, delta: Int): Int {
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, day / 10000)
        c.set(Calendar.MONTH, (day / 100 % 100) - 1)
        c.set(Calendar.DAY_OF_MONTH, day % 100)
        c.set(Calendar.HOUR_OF_DAY, 12)
        c.add(Calendar.DAY_OF_MONTH, delta)
        return keyOf(c)
    }

    const val DAILY_HAPPINESS = 8
    const val HAPPINESS_SOFT_CAP = 120
}
