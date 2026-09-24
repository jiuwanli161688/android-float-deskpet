package com.floatdeskpet.app.data

import com.floatdeskpet.app.overlay.PetDialogue
import java.util.Calendar

object CompanionSchedule {
    fun line(settings: PetSettings): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val slot = when (hour) {
            in 6..10 -> morning(settings)
            in 11..16 -> noon(settings)
            in 17..21 -> evening(settings)
            else -> night(settings)
        }
        val extra = stageExtra(settings)
        val pool = if (extra.isEmpty()) slot else slot + extra
        val seed = CompanionDay.todayKey() * 17 + settings.resolvedStyle().id.hashCode()
        return pool[(seed and 0x7fffffff) % pool.size]
    }

    fun next(settings: PetSettings, current: String): String {
        val now = line(settings)
        if (now != current) return now
        return PetDialogue.nextTodayLine(settings, current)
    }

    private fun morning(s: PetSettings): List<String> {
        val base = if (s.isMale) {
            listOf("晒一会儿太阳", "慢慢吃早饭", "把窗开一条缝")
        } else {
            listOf("把头发拢一拢", "喝一口温的", "在窗边站一会儿")
        }
        return base + styleWish(s, "早")
    }

    private fun noon(s: PetSettings): List<String> {
        val base = if (s.isMale) {
            listOf("歇口气再走", "找个阴凉处", "把水喝完")
        } else {
            listOf("午安眯一小下", "把袖子挽起来", "吃点软的")
        }
        return base + styleWish(s, "午")
    }

    private fun evening(s: PetSettings): List<String> {
        val base = if (s.isMale) {
            listOf("把灯调暗一点", "坐下来不说话", "换双舒服的鞋")
        } else {
            listOf("泡一杯热的", "把外套披上", "听一会儿夜")
        }
        return base + styleWish(s, "晚")
    }

    private fun night(s: PetSettings): List<String> {
        val base = if (s.isMale) {
            listOf("早点躺下", "把明天的事先放下", "我守着就好")
        } else {
            listOf("把灯留一盏小的", "把心事放旁边", "我陪你再待一会儿")
        }
        return base + styleWish(s, "夜")
    }

    private fun styleWish(s: PetSettings, slot: String): List<String> {
        return when (s.resolvedStyle().id) {
            "yujie" -> listOf("把茶续上")
            "luoli" -> listOf("软软靠着你")
            "qingchun" -> listOf("看一会儿浅光")
            "wenrou" -> listOf("不急，坐着就好")
            "qingshuang" -> listOf("吹一口干净的风")
            "chenwen" -> listOf("把伞放回原处")
            "dashu" -> listOf("把外套给你")
            "chenggong" -> listOf("开完会也坐一会儿")
            else -> emptyList()
        }.map { if (slot == "夜") it else it }
    }

    private fun stageExtra(s: PetSettings): List<String> {
        return when (CompanionBond.stage(s)) {
            BondStage.FIRST -> emptyList()
            BondStage.WARM -> listOf(if (s.isMale) "再靠近一点坐" else "想被轻轻看看")
            BondStage.TACIT -> listOf(if (s.isMale) "不用多讲" else "这样就刚好")
            BondStage.BOND -> listOf(if (s.isMale) "把今天收进心里" else "把今天轻轻收好")
        }
    }
}
