package com.floatdeskpet.app.data

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.widget.ImageView

enum class CharacterStyle(
    val id: String,
    val male: Boolean,
    val suggestedOutfit: String,
) {
    YUJIE("yujie", false, PetSettings.OUTFIT_HOODIE),
    LUOLI("luoli", false, PetSettings.OUTFIT_PAJAMA),
    QINGCHUN("qingchun", false, PetSettings.OUTFIT_CASUAL),
    WENROU("wenrou", true, PetSettings.OUTFIT_CASUAL),
    QINGSHUANG("qingshuang", true, PetSettings.OUTFIT_HOODIE),
    CHENWEN("chenwen", true, PetSettings.OUTFIT_CASUAL),
    DASHU("dashu", true, PetSettings.OUTFIT_HOODIE),
    CHENGGONG("chenggong", true, PetSettings.OUTFIT_CASUAL),
    ;

    fun titleRes(): Int = when (this) {
        YUJIE -> com.floatdeskpet.app.R.string.style_yujie
        LUOLI -> com.floatdeskpet.app.R.string.style_luoli
        QINGCHUN -> com.floatdeskpet.app.R.string.style_qingchun
        WENROU -> com.floatdeskpet.app.R.string.style_wenrou
        QINGSHUANG -> com.floatdeskpet.app.R.string.style_qingshuang
        CHENWEN -> com.floatdeskpet.app.R.string.style_chenwen
        DASHU -> com.floatdeskpet.app.R.string.style_dashu
        CHENGGONG -> com.floatdeskpet.app.R.string.style_chenggong
    }

    fun blurbRes(): Int = when (this) {
        YUJIE -> com.floatdeskpet.app.R.string.style_yujie_blurb
        LUOLI -> com.floatdeskpet.app.R.string.style_luoli_blurb
        QINGCHUN -> com.floatdeskpet.app.R.string.style_qingchun_blurb
        WENROU -> com.floatdeskpet.app.R.string.style_wenrou_blurb
        QINGSHUANG -> com.floatdeskpet.app.R.string.style_qingshuang_blurb
        CHENWEN -> com.floatdeskpet.app.R.string.style_chenwen_blurb
        DASHU -> com.floatdeskpet.app.R.string.style_dashu_blurb
        CHENGGONG -> com.floatdeskpet.app.R.string.style_chenggong_blurb
    }

    val hasDedicatedArt: Boolean get() = true

    fun colorFilter(): ColorMatrixColorFilter {
        val extra = when (this) {
            YUJIE -> floatArrayOf(
                1.10f, 0.04f, 0.00f, 0f, 10f,
                0.00f, 0.90f, 0.02f, 0f, -8f,
                0.00f, 0.00f, 0.86f, 0f, -12f,
                0f, 0f, 0f, 1f, 0f,
            )
            LUOLI -> floatArrayOf(
                1.08f, 0.06f, 0.06f, 0f, 18f,
                0.04f, 1.02f, 0.04f, 0f, 10f,
                0.06f, 0.04f, 1.10f, 0f, 16f,
                0f, 0f, 0f, 1f, 0f,
            )
            QINGCHUN -> floatArrayOf(
                1.02f, 0.02f, 0.04f, 0f, 12f,
                0.02f, 1.04f, 0.02f, 0f, 8f,
                0.02f, 0.04f, 1.08f, 0f, 14f,
                0f, 0f, 0f, 1f, 0f,
            )
            WENROU -> floatArrayOf(
                1.10f, 0.04f, 0.00f, 0f, 14f,
                0.03f, 1.00f, 0.02f, 0f, 4f,
                0.00f, 0.02f, 0.90f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f,
            )
            QINGSHUANG -> floatArrayOf(
                0.96f, 0.02f, 0.04f, 0f, 8f,
                0.02f, 1.06f, 0.04f, 0f, 12f,
                0.02f, 0.04f, 1.12f, 0f, 16f,
                0f, 0f, 0f, 1f, 0f,
            )
            CHENWEN -> floatArrayOf(
                0.92f, 0.04f, 0.02f, 0f, -6f,
                0.03f, 0.90f, 0.02f, 0f, -8f,
                0.02f, 0.02f, 0.84f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f,
            )
            DASHU -> floatArrayOf(
                1.16f, 0.08f, 0.00f, 0f, 8f,
                0.05f, 0.86f, 0.02f, 0f, -12f,
                0.00f, 0.02f, 0.70f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f,
            )
            CHENGGONG -> floatArrayOf(
                0.94f, 0.03f, 0.02f, 0f, -4f,
                0.02f, 0.92f, 0.02f, 0f, -6f,
                0.01f, 0.02f, 0.90f, 0f, -4f,
                0f, 0f, 0f, 1f, 0f,
            )
        }
        val matrix = ColorMatrix()
        val sat = when (this) {
            YUJIE -> 1.12f
            LUOLI -> 1.08f
            QINGCHUN -> 0.96f
            WENROU -> 1.06f
            QINGSHUANG -> 1.04f
            CHENWEN -> 0.78f
            DASHU -> 0.90f
            CHENGGONG -> 0.86f
        }
        matrix.setSaturation(sat)
        matrix.postConcat(ColorMatrix(extra))
        return ColorMatrixColorFilter(matrix)
    }

    companion object {
        fun all(male: Boolean): List<CharacterStyle> = entries.filter { it.male == male }

        fun defaultOf(male: Boolean): CharacterStyle = if (male) WENROU else QINGCHUN

        fun fromId(id: String?, male: Boolean): CharacterStyle {
            return entries.find { it.id == id && it.male == male } ?: defaultOf(male)
        }

        fun tint(view: ImageView, settings: PetSettings) {
            val style = fromId(settings.style, settings.isMale)
            if (!style.hasDedicatedArt) {
                view.colorFilter = style.colorFilter()
                return
            }
            when (CompanionBond.stage(settings)) {
                BondStage.TACIT -> view.setColorFilter(0x22C45C26)
                BondStage.BOND -> view.setColorFilter(0x33C45C26)
                else -> view.clearColorFilter()
            }
        }
    }
}
