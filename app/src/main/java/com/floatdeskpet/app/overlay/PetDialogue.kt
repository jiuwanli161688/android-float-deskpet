package com.floatdeskpet.app.overlay

import java.util.Calendar

object PetDialogue {
    private val morning = listOf("早上好呀～", "新的一天，一起加油哦", "早饭记得吃～")
    private val noon = listOf("午饭吃了吗？", "午安～稍微歇一会儿吧", "太阳好暖呀")
    private val evening = listOf("晚上好", "今天辛苦啦", "要不要喝杯热的？")
    private val night = listOf("好晚了呢", "要早点睡哦", "我陪你再待一会儿")

    private val tapHappy = listOf("欸嘿～", "被发现啦", "嘿嘿，又点我")
    private val tapOk = listOf("嗯？", "我在呢", "怎么啦～")
    private val tapLow = listOf("有点没精神…", "陪陪我好不好", "今天好安静呀")

    private val petLines = listOf("嘿嘿，好舒服～", "再摸摸嘛", "心都要化掉了")
    private val feedLines = listOf("谢谢你！好好吃～", "零食！最喜欢了", "啊呜，幸福。")
    private val noSnack = listOf("零食吃完啦，过一会儿再来～", "口袋空空的…")
    private val sleepLines = listOf("那我眯一会儿…", "呼…不要吵我哦", "晚安，做个好梦")
    private val peekLines = listOf("那我躲边上歇会儿～", "先藏一下下", "有事再叫我呀")
    private val wakeLines = listOf("我回来啦～", "想我了吗？", "等好久哦")
    private val appearLines = listOf("我来啦～", "杏杏报到！", "今天也请多指教")
    private val batteryLow = listOf("电量有点低了哦，注意休息", "手机也要充电呀")
    private val charging = listOf("在充电呀，安心～", "慢慢充，我不乱跑")
    private val happyCycle = listOf("今天心情超好！", "比心～", "要一直这样下去")
    private val shyCycle = listOf("才、才没有害羞…", "不要一直看啦", "脸好烫")
    private val sleepCycle = listOf("好困呀…", "再让我躺一会", "Zzz")
    private val sadCycle = listOf("有点想你了…", "会一直等你的", "抱一下就好")
    private val ambientHappy = listOf("发呆也很好玩", "要不要歇一会儿？", "我在这儿陪你")
    private val ambientOk = listOf("工作还顺利吗", "喝口水呀", "我轻轻待着就好")
    private val ambientLow = listOf("有点寂寞呢", "记得看看我哦", "今天好长啊")

    fun greeting(): String = pick(slotPool()) + " " + pick(appearLines)

    fun timeOfDay(): String = pick(slotPool())

    fun tap(mood: Int): String {
        return pick(
            when (PetStats.tier(mood)) {
                MoodTier.HAPPY -> tapHappy
                MoodTier.OK -> tapOk
                MoodTier.LOW, MoodTier.SAD -> tapLow
            },
        )
    }

    fun pet(): String = pick(petLines)
    fun feed(): String = pick(feedLines)
    fun noSnack(): String = pick(noSnack)
    fun sleep(): String = pick(sleepLines)
    fun peek(): String = pick(peekLines)
    fun wake(): String = pick(wakeLines)
    fun batteryLow(): String = pick(batteryLow)
    fun charging(): String = pick(charging)

    fun pose(pose: PetPose): String {
        return pick(
            when (pose) {
                PetPose.HAPPY, PetPose.WAVE, PetPose.JUMP -> happyCycle
                PetPose.SHY -> shyCycle
                PetPose.SLEEP -> sleepCycle
                PetPose.SAD, PetPose.TILT -> sadCycle
                else -> tapOk
            },
        )
    }

    fun ambient(mood: Int): String {
        return pick(
            when (PetStats.tier(mood)) {
                MoodTier.HAPPY -> ambientHappy
                MoodTier.OK -> ambientOk
                MoodTier.LOW, MoodTier.SAD -> ambientLow
            },
        )
    }

    private fun slotPool(): List<String> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..10 -> morning
            in 11..13 -> noon
            in 17..20 -> evening
            else -> if (hour in 14..16) noon else night
        }
    }

    private fun pick(pool: List<String>): String = pool.random()
}
