package com.floatdeskpet.app.data

import android.content.Context
import android.content.SharedPreferences

class PetSettings private constructor(context: Context) {
    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var posX: Int
        get() = prefs.getInt(KEY_X, UNSET)
        set(value) = prefs.edit().putInt(KEY_X, value).apply()

    var posY: Int
        get() = prefs.getInt(KEY_Y, UNSET)
        set(value) = prefs.edit().putInt(KEY_Y, value).apply()

    var sizeDp: Int
        get() = prefs.getInt(KEY_SIZE, 160)
        set(value) = prefs.edit().putInt(KEY_SIZE, value.coerceIn(100, 220)).apply()

    var opacity: Int
        get() = prefs.getInt(KEY_OPACITY, 100)
        set(value) = prefs.edit().putInt(KEY_OPACITY, value.coerceIn(30, 100)).apply()

    var passThrough: Boolean
        get() = prefs.getBoolean(KEY_GHOST, false)
        set(value) = prefs.edit().putBoolean(KEY_GHOST, value).apply()

    var alwaysShow: Boolean
        get() = prefs.getBoolean(KEY_ALWAYS, true)
        set(value) = prefs.edit().putBoolean(KEY_ALWAYS, value).apply()

    var running: Boolean
        get() = prefs.getBoolean(KEY_RUNNING, false)
        set(value) = prefs.edit().putBoolean(KEY_RUNNING, value).apply()

    var visible: Boolean
        get() = prefs.getBoolean(KEY_VISIBLE, true)
        set(value) = prefs.edit().putBoolean(KEY_VISIBLE, value).apply()

    var muted: Boolean
        get() = prefs.getBoolean(KEY_MUTE, false)
        set(value) = prefs.edit().putBoolean(KEY_MUTE, value).apply()

    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS, value).apply()

    var outfit: String
        get() = prefs.getString(KEY_OUTFIT, OUTFIT_CASUAL) ?: OUTFIT_CASUAL
        set(value) = prefs.edit().putString(KEY_OUTFIT, value).apply()

    var gender: String
        get() = prefs.getString(KEY_GENDER, GENDER_MALE) ?: GENDER_MALE
        set(value) = prefs.edit().putString(
            KEY_GENDER,
            if (value == GENDER_MALE) GENDER_MALE else GENDER_FEMALE,
        ).apply()

    var petName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value.trim().take(12)).apply()

    var configured: Boolean
        get() = prefs.getBoolean(KEY_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_CONFIGURED, value).apply()

    var style: String
        get() = prefs.getString(KEY_STYLE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_STYLE, value).apply()

    var namePickIndex: Int
        get() = prefs.getInt(KEY_NAME_PICK, 0)
        set(value) = prefs.edit().putInt(KEY_NAME_PICK, value).apply()

    var mood: Int
        get() = prefs.getInt(KEY_MOOD, 72)
        set(value) = prefs.edit().putInt(KEY_MOOD, value.coerceIn(0, 100)).apply()

    var affection: Int
        get() = prefs.getInt(KEY_AFFECTION, 48)
        set(value) = prefs.edit().putInt(KEY_AFFECTION, value.coerceIn(0, 100)).apply()

    var feedCount: Int
        get() = prefs.getInt(KEY_FEED, FEED_MAX)
        set(value) = prefs.edit().putInt(KEY_FEED, value.coerceIn(0, FEED_MAX)).apply()

    var lastDecayAt: Long
        get() = prefs.getLong(KEY_DECAY, 0L)
        set(value) = prefs.edit().putLong(KEY_DECAY, value).apply()

    var lastInteractAt: Long
        get() = prefs.getLong(KEY_INTERACT, 0L)
        set(value) = prefs.edit().putLong(KEY_INTERACT, value).apply()

    var lastFeedRegenAt: Long
        get() = prefs.getLong(KEY_FEED_REGEN, 0L)
        set(value) = prefs.edit().putLong(KEY_FEED_REGEN, value).apply()

    var lastVisitDay: Int
        get() = prefs.getInt(KEY_VISIT_DAY, 0)
        set(value) = prefs.edit().putInt(KEY_VISIT_DAY, value).apply()

    var streakDays: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_STREAK, value.coerceAtLeast(0)).apply()

    var lastStreakCelebrate: Int
        get() = prefs.getInt(KEY_STREAK_CELE, 0)
        set(value) = prefs.edit().putInt(KEY_STREAK_CELE, value).apply()

    var goalDay: Int
        get() = prefs.getInt(KEY_GOAL_DAY, 0)
        set(value) = prefs.edit().putInt(KEY_GOAL_DAY, value).apply()

    var goalPet: Boolean
        get() = prefs.getBoolean(KEY_GOAL_PET, false)
        set(value) = prefs.edit().putBoolean(KEY_GOAL_PET, value).apply()

    var goalFeed: Boolean
        get() = prefs.getBoolean(KEY_GOAL_FEED, false)
        set(value) = prefs.edit().putBoolean(KEY_GOAL_FEED, value).apply()

    var goalCardio: Boolean
        get() = prefs.getBoolean(KEY_GOAL_CARDIO, false)
        set(value) = prefs.edit().putBoolean(KEY_GOAL_CARDIO, value).apply()

    var goalBonus: Boolean
        get() = prefs.getBoolean(KEY_GOAL_BONUS, false)
        set(value) = prefs.edit().putBoolean(KEY_GOAL_BONUS, value).apply()

    var lastHappyRegenDay: Int
        get() = prefs.getInt(KEY_HAPPY_DAY, 0)
        set(value) = prefs.edit().putInt(KEY_HAPPY_DAY, value).apply()

    var lastHappyRegenAmount: Int
        get() = prefs.getInt(KEY_HAPPY_GAIN, 0)
        set(value) = prefs.edit().putInt(KEY_HAPPY_GAIN, value).apply()

    var missYou: Boolean
        get() = prefs.getBoolean(KEY_MISS_YOU, false)
        set(value) = prefs.edit().putBoolean(KEY_MISS_YOU, value).apply()

    var missYouDay: Int
        get() = prefs.getInt(KEY_MISS_DAY, 0)
        set(value) = prefs.edit().putInt(KEY_MISS_DAY, value).apply()

    var missYouCount: Int
        get() = prefs.getInt(KEY_MISS_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_MISS_COUNT, value).apply()

    var lastBondStage: Int
        get() = prefs.getInt(KEY_BOND_SEEN, -1)
        set(value) = prefs.edit().putInt(KEY_BOND_SEEN, value).apply()

    val isMale: Boolean get() = gender == GENDER_MALE

    fun resolvedStyle(): CharacterStyle = CharacterStyle.fromId(style, isMale)

    fun displayName(): String {
        val n = petName.trim()
        return n.ifEmpty { defaultName(isMale) }
    }

    fun needsSetup(): Boolean {
        if (configured) return false
        if (prefs.contains(KEY_RUNNING) || prefs.contains(KEY_X) || prefs.contains(KEY_OUTFIT)) {
            if (petName.isBlank()) petName = DEFAULT_FEMALE
            gender = GENDER_FEMALE
            configured = true
            return false
        }
        return true
    }

    fun applySetup(male: Boolean, name: String) {
        gender = if (male) GENDER_MALE else GENDER_FEMALE
        val trimmed = name.trim()
        petName = trimmed.ifEmpty { takeDefaultName(male) }
        val kept = CharacterStyle.fromId(style, male)
        if (style.isBlank() || kept.id != style) {
            applyStyle(CharacterStyle.defaultOf(male))
        } else if (male && outfit == OUTFIT_PAJAMA) {
            outfit = OUTFIT_CASUAL
        }
        configured = true
    }

    fun applyGender(male: Boolean) {
        val oldDefault = defaultName(!male)
        gender = if (male) GENDER_MALE else GENDER_FEMALE
        if (petName.isBlank() || petName == oldDefault || NameBank.pool(!male).contains(petName)) {
            petName = ""
        }
        applyStyle(CharacterStyle.defaultOf(male))
    }

    fun applyStyle(next: CharacterStyle) {
        style = next.id
        outfit = next.suggestedOutfit
        if (isMale && outfit == OUTFIT_PAJAMA) outfit = OUTFIT_CASUAL
    }

    fun takeDefaultName(male: Boolean): String {
        val picked = NameBank.pick(male, namePickIndex)
        namePickIndex = namePickIndex + 1
        return picked
    }

    companion object {
        const val PREFS = "pet"
        const val UNSET = Int.MIN_VALUE
        const val KEY_X = "pos_x"
        const val KEY_Y = "pos_y"
        const val KEY_SIZE = "size_dp"
        const val KEY_OPACITY = "opacity"
        const val KEY_GHOST = "pass_through"
        const val KEY_ALWAYS = "always_show"
        const val KEY_RUNNING = "running"
        const val KEY_VISIBLE = "visible"
        const val KEY_MUTE = "muted"
        const val KEY_TTS = "tts_enabled"
        const val KEY_OUTFIT = "outfit"
        const val KEY_GENDER = "gender"
        const val KEY_NAME = "pet_name"
        const val KEY_CONFIGURED = "configured"
        const val KEY_STYLE = "character_style"
        const val KEY_NAME_PICK = "name_pick_index"
        const val KEY_MOOD = "mood"
        const val KEY_AFFECTION = "affection"
        const val KEY_FEED = "feed_count"
        const val KEY_DECAY = "last_decay"
        const val KEY_INTERACT = "last_interact"
        const val KEY_FEED_REGEN = "last_feed_regen"
        const val KEY_VISIT_DAY = "last_visit_day"
        const val KEY_STREAK = "streak_days"
        const val KEY_STREAK_CELE = "streak_celebrate"
        const val KEY_GOAL_DAY = "goal_day"
        const val KEY_GOAL_PET = "goal_pet"
        const val KEY_GOAL_FEED = "goal_feed"
        const val KEY_GOAL_CARDIO = "goal_cardio"
        const val KEY_GOAL_BONUS = "goal_bonus"
        const val KEY_HAPPY_DAY = "happy_regen_day"
        const val KEY_HAPPY_GAIN = "happy_regen_gain"
        const val KEY_MISS_YOU = "miss_you"
        const val KEY_MISS_DAY = "miss_you_day"
        const val KEY_MISS_COUNT = "miss_you_count"
        const val KEY_BOND_SEEN = "bond_stage_seen"
        const val OUTFIT_CASUAL = "casual"
        const val OUTFIT_PAJAMA = "pajama"
        const val OUTFIT_HOODIE = "hoodie"
        const val GENDER_FEMALE = "female"
        const val GENDER_MALE = "male"
        const val DEFAULT_FEMALE = "晚晴"
        const val DEFAULT_MALE = "予安"
        const val FEED_MAX = 8
        const val FEED_REGEN_MS = 3L * 60L * 60L * 1000L

        fun defaultName(male: Boolean): String = if (male) DEFAULT_MALE else DEFAULT_FEMALE

        @Volatile
        private var instance: PetSettings? = null

        fun get(context: Context): PetSettings {
            return instance ?: synchronized(this) {
                instance ?: PetSettings(context).also { instance = it }
            }
        }
    }
}
