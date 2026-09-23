package com.floatdeskpet.app.data

import android.content.Context
import com.floatdeskpet.app.R

data class AlbumCard(
    val id: String,
    val title: Int,
    val body: Int,
    val locked: Int,
    val minStage: BondStage = BondStage.FIRST,
)

class AlbumStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun unlocked(id: String): Boolean = prefs.getBoolean(id, false)

    fun unlock(id: String): Boolean {
        if (unlocked(id)) return false
        prefs.edit().putBoolean(id, true).apply()
        return true
    }

    fun sync(settings: PetSettings): List<String> {
        val fresh = mutableListOf<String>()
        fun hit(id: String, cond: Boolean) {
            if (cond && unlock(id)) fresh += id
        }
        hit(MEET, settings.streakDays >= 1 || settings.lastVisitDay > 0)
        hit(PET, settings.goalPet || settings.affection >= 50)
        hit(FEED, settings.goalFeed)
        hit(CARDIO, settings.goalCardio)
        hit(S3, settings.streakDays >= 3)
        hit(S7, settings.streakDays >= 7)
        hit(S14, settings.streakDays >= 14)
        hit(S30, settings.streakDays >= 30)
        val stage = CompanionBond.stage(settings)
        hit(WARM, stage.ordinal >= BondStage.WARM.ordinal)
        hit(TACIT, stage.ordinal >= BondStage.TACIT.ordinal)
        hit(BOND, stage.ordinal >= BondStage.BOND.ordinal)
        hit(WISH, CompanionBond.richer(settings))
        hit(KEEPSAKE, stage == BondStage.BOND)
        return fresh
    }

    fun cards(): List<AlbumCard> = CATALOG

    companion object {
        const val PREFS = "album"
        const val MEET = "meet"
        const val PET = "pet"
        const val FEED = "feed"
        const val CARDIO = "cardio"
        const val S3 = "streak3"
        const val S7 = "streak7"
        const val S14 = "streak14"
        const val S30 = "streak30"
        const val WARM = "stage_warm"
        const val TACIT = "stage_tacit"
        const val BOND = "stage_bond"
        const val WISH = "wish"
        const val KEEPSAKE = "keepsake"

        val CATALOG = listOf(
            AlbumCard(MEET, R.string.album_meet_t, R.string.album_meet_b, R.string.album_meet_l),
            AlbumCard(PET, R.string.album_pet_t, R.string.album_pet_b, R.string.album_pet_l),
            AlbumCard(FEED, R.string.album_feed_t, R.string.album_feed_b, R.string.album_feed_l),
            AlbumCard(CARDIO, R.string.album_cardio_t, R.string.album_cardio_b, R.string.album_cardio_l),
            AlbumCard(S3, R.string.album_s3_t, R.string.album_s3_b, R.string.album_s3_l),
            AlbumCard(S7, R.string.album_s7_t, R.string.album_s7_b, R.string.album_s7_l),
            AlbumCard(S14, R.string.album_s14_t, R.string.album_s14_b, R.string.album_s14_l),
            AlbumCard(S30, R.string.album_s30_t, R.string.album_s30_b, R.string.album_s30_l),
            AlbumCard(WARM, R.string.album_warm_t, R.string.album_warm_b, R.string.album_warm_l, BondStage.WARM),
            AlbumCard(TACIT, R.string.album_tacit_t, R.string.album_tacit_b, R.string.album_tacit_l, BondStage.TACIT),
            AlbumCard(BOND, R.string.album_bond_t, R.string.album_bond_b, R.string.album_bond_l, BondStage.BOND),
            AlbumCard(WISH, R.string.album_wish_t, R.string.album_wish_b, R.string.album_wish_l, BondStage.WARM),
            AlbumCard(KEEPSAKE, R.string.album_keep_t, R.string.album_keep_b, R.string.album_keep_l, BondStage.BOND),
        )

        @Volatile
        private var instance: AlbumStore? = null

        fun get(context: Context): AlbumStore {
            return instance ?: synchronized(this) {
                instance ?: AlbumStore(context).also { instance = it }
            }
        }
    }
}
