package com.floatdeskpet.app.overlay

import com.floatdeskpet.app.R
import com.floatdeskpet.app.data.PetSettings

enum class PetPose {
    IDLE, BLINK, TILT, JUMP, WAVE, SLEEP, SHY, HAPPY, SAD
}

data class FrameSet(
    val idle: Int,
    val blink: Int,
    val tilt: Int,
    val tapJump: Int,
    val tapWave: Int,
    val sleep: Int,
    val shy: Int,
) {
    fun res(pose: PetPose): Int {
        return when (pose) {
            PetPose.IDLE -> idle
            PetPose.BLINK -> blink
            PetPose.TILT -> tilt
            PetPose.JUMP -> tapJump
            PetPose.WAVE, PetPose.HAPPY -> tapWave
            PetPose.SLEEP -> sleep
            PetPose.SHY -> shy
            PetPose.SAD -> tilt
        }
    }
}

object PetFrames {
    fun of(settings: PetSettings): FrameSet = of(settings.gender, settings.outfit)

    fun of(gender: String, outfit: String): FrameSet {
        if (gender == PetSettings.GENDER_MALE) {
            return when (outfit) {
                PetSettings.OUTFIT_HOODIE -> FrameSet(
                    idle = R.drawable.pet_m_hd_idle_0,
                    blink = R.drawable.pet_m_hd_idle_1,
                    tilt = R.drawable.pet_m_hd_idle_2,
                    tapJump = R.drawable.pet_m_hd_tap_0,
                    tapWave = R.drawable.pet_m_hd_tap_1,
                    sleep = R.drawable.pet_m_hd_sleep,
                    shy = R.drawable.pet_m_hd_shy,
                )
                else -> FrameSet(
                    idle = R.drawable.pet_m_idle_0,
                    blink = R.drawable.pet_m_idle_1,
                    tilt = R.drawable.pet_m_idle_2,
                    tapJump = R.drawable.pet_m_tap_0,
                    tapWave = R.drawable.pet_m_tap_1,
                    sleep = R.drawable.pet_m_sleep_0,
                    shy = R.drawable.pet_m_shy_0,
                )
            }
        }
        return when (outfit) {
            PetSettings.OUTFIT_PAJAMA -> FrameSet(
                idle = R.drawable.pet_pj_idle_0,
                blink = R.drawable.pet_pj_idle_1,
                tilt = R.drawable.pet_pj_idle_2,
                tapJump = R.drawable.pet_pj_tap_0,
                tapWave = R.drawable.pet_pj_tap_1,
                sleep = R.drawable.pet_pj_sleep,
                shy = R.drawable.pet_pj_idle_2,
            )
            PetSettings.OUTFIT_HOODIE -> FrameSet(
                idle = R.drawable.pet_hd_idle_0,
                blink = R.drawable.pet_hd_idle_1,
                tilt = R.drawable.pet_hd_idle_2,
                tapJump = R.drawable.pet_hd_tap_0,
                tapWave = R.drawable.pet_hd_tap_1,
                sleep = R.drawable.pet_hd_sleep,
                shy = R.drawable.pet_hd_idle_0,
            )
            else -> FrameSet(
                idle = R.drawable.pet_idle_0,
                blink = R.drawable.pet_idle_1,
                tilt = R.drawable.pet_idle_2,
                tapJump = R.drawable.pet_tap_0,
                tapWave = R.drawable.pet_tap_1,
                sleep = R.drawable.pet_sleep_0,
                shy = R.drawable.pet_shy_0,
            )
        }
    }
}
