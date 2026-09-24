package com.floatdeskpet.app.data

import com.floatdeskpet.app.R

data class GuidePage(
    val titleRes: Int,
    val bodyRes: Int,
    val artRes: Int,
    val artDescRes: Int,
)

object GuidePages {
    val all: List<GuidePage> = listOf(
        GuidePage(
            titleRes = R.string.guide_1_title,
            bodyRes = R.string.guide_1_body,
            artRes = R.drawable.pet_m_idle_0,
            artDescRes = R.string.guide_1_art,
        ),
        GuidePage(
            titleRes = R.string.guide_2_title,
            bodyRes = R.string.guide_2_body,
            artRes = R.drawable.pet_idle_0,
            artDescRes = R.string.guide_2_art,
        ),
        GuidePage(
            titleRes = R.string.guide_3_title,
            bodyRes = R.string.guide_3_body,
            artRes = R.drawable.pet_hd_idle_0,
            artDescRes = R.string.guide_3_art,
        ),
        GuidePage(
            titleRes = R.string.guide_4_title,
            bodyRes = R.string.guide_4_body,
            artRes = R.drawable.pet_m_hd_idle_0,
            artDescRes = R.string.guide_4_art,
        ),
    )
}
