package com.floatdeskpet.app.overlay

import com.floatdeskpet.app.data.PetSettings

enum class MoodTier { HAPPY, OK, LOW, SAD }

object PetStats {
    fun applyDecay(settings: PetSettings) {
        regenFeed(settings)
        val now = System.currentTimeMillis()
        val last = settings.lastDecayAt
        if (last <= 0L) {
            settings.lastDecayAt = now
            return
        }
        val minutes = ((now - last) / 60_000L).toInt()
        if (minutes <= 0) return
        settings.mood = settings.mood - (minutes * 0.5).toInt().coerceAtMost(28)
        settings.affection = settings.affection - (minutes * 0.18).toInt().coerceAtMost(16)
        settings.lastDecayAt = now
    }

    fun regenFeed(settings: PetSettings) {
        val now = System.currentTimeMillis()
        var last = settings.lastFeedRegenAt
        if (last <= 0L) {
            settings.lastFeedRegenAt = now
            return
        }
        var gained = 0
        while (last + PetSettings.FEED_REGEN_MS <= now && gained < PetSettings.FEED_MAX) {
            last += PetSettings.FEED_REGEN_MS
            gained += 1
        }
        if (gained > 0) {
            settings.feedCount = settings.feedCount + gained
            settings.lastFeedRegenAt = last
        }
    }

    fun touch(settings: PetSettings) {
        settings.lastInteractAt = System.currentTimeMillis()
        settings.lastDecayAt = System.currentTimeMillis()
    }

    fun onTap(settings: PetSettings) {
        settings.mood = settings.mood + 3
        settings.affection = settings.affection + 2
        touch(settings)
    }

    fun onPet(settings: PetSettings) {
        settings.mood = settings.mood + 10
        settings.affection = settings.affection + 8
        touch(settings)
    }

    fun feed(settings: PetSettings): Boolean {
        regenFeed(settings)
        if (settings.feedCount <= 0) return false
        settings.feedCount = settings.feedCount - 1
        settings.mood = settings.mood + 14
        settings.affection = settings.affection + 12
        touch(settings)
        return true
    }

    fun onSleep(settings: PetSettings) {
        settings.mood = settings.mood + 6
        touch(settings)
    }

    fun onPeek(settings: PetSettings) {
        settings.mood = settings.mood - 3
    }

    fun onCycle(settings: PetSettings) {
        settings.mood = settings.mood + 1
        settings.affection = settings.affection + 1
        touch(settings)
    }

    fun tier(mood: Int): MoodTier {
        return when {
            mood >= 72 -> MoodTier.HAPPY
            mood >= 42 -> MoodTier.OK
            mood >= 22 -> MoodTier.LOW
            else -> MoodTier.SAD
        }
    }
}
