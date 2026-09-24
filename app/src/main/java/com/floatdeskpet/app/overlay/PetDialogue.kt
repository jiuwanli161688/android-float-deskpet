package com.floatdeskpet.app.overlay

import com.floatdeskpet.app.data.BondStage
import com.floatdeskpet.app.data.CompanionBond
import com.floatdeskpet.app.data.CompanionDay
import com.floatdeskpet.app.data.PetSettings
import java.util.Calendar
import kotlin.random.Random

object PetDialogue {
    private val morningF = listOf("早上好呀～", "新的一天，一起加油哦", "早饭记得吃～", "窗边的光好浅")
    private val noonF = listOf("午饭吃了吗？", "午安～稍微歇一会儿吧", "太阳好暖呀", "下午也慢慢来")
    private val eveningF = listOf("晚上好", "今天辛苦啦", "要不要喝杯热的？", "天色软下来了")
    private val nightF = listOf("好晚了呢", "要早点睡哦", "我陪你再待一会儿", "夜里也有人在")

    private val morningM = listOf("早啊", "新的一天，我陪你", "早饭别忘了", "光刚好")
    private val noonM = listOf("午饭吃了吗", "午安，歇一会儿", "太阳挺好", "不急")
    private val eveningM = listOf("晚上了", "今天辛苦了", "要不要喝点热的", "天暗下来了")
    private val nightM = listOf("有点晚了", "早点睡吧", "我再陪你一会儿", "夜也安静")

    private val stageWarmF = listOf("比刚认识时，更熟一点了", "我会记得你回来的样子")
    private val stageTacitF = listOf("不用多说，我懂", "这样待着就刚好")
    private val stageBondF = listOf("有你在，心就稳", "这段日子，我收着")
    private val stageWarmM = listOf("比刚见面，近一点了", "你回来，我知道")
    private val stageTacitM = listOf("不用多讲", "这样就好")
    private val stageBondM = listOf("有你在就够", "这些日子我记得")

    private val tapHappyF = listOf("欸嘿～", "被发现啦", "嘿嘿，又点我")
    private val tapOkF = listOf("嗯？", "我在呢", "怎么啦～")
    private val tapLowF = listOf("有点没精神…", "陪陪我好不好", "今天好安静呀")
    private val tapHappyM = listOf("嘿", "被抓到了", "又点我")
    private val tapOkM = listOf("嗯？", "我在", "怎么了")
    private val tapLowM = listOf("有点没劲…", "陪我待一会", "今天好安静")

    private val petF = listOf("嘿嘿，好舒服～", "再摸摸嘛", "心都要化掉了")
    private val petM = listOf("还不错", "再来一下", "心都要化了")
    private val feedF = listOf("谢谢你！好好吃～", "这个味道刚刚好", "啊呜，幸福。")
    private val feedM = listOf("谢了，真好吃", "来得正好", "啊，幸福。")
    private val noSnackF = listOf("幸福点还差一点，先摸摸我攒一点？", "口袋空空的…改天再带好吃的来。")
    private val noSnackM = listOf("幸福点还差一点，先摸摸我攒一点", "改天再带好吃的来")
    private val cardioF = listOf("哈…好像动掉了 %d 千卡，算个玩笑。", "跳了几下，说是 %d 千卡。别当真。")
    private val cardioM = listOf("出了点汗，据说 %d 千卡。别信。", "随便跳跳，说是 %d 千卡。")
    private val sleepF = listOf("那我眯一会儿…", "呼…不要吵我哦", "晚安，做个好梦")
    private val sleepM = listOf("那我眯一会儿", "别吵我", "晚安")
    private val peekF = listOf("那我躲边上歇会儿～", "先藏一下下", "有事再叫我呀")
    private val peekM = listOf("我去边上歇会儿", "先躲一下", "有事叫我")
    private val wakeF = listOf("我回来啦～", "想我了吗？", "等好久哦")
    private val wakeM = listOf("回来了", "想我了？", "等挺久了")
    private val appearF = listOf("我来啦～", "%s报到！", "今天也请多指教")
    private val appearM = listOf("我来了", "%s报到。", "今天也拜托你了")
    private val batteryLowF = listOf("电量有点低了哦，注意休息", "手机也要充电呀")
    private val batteryLowM = listOf("电量有点低了，注意休息", "手机也要充电")
    private val chargingF = listOf("在充电呀，安心～", "慢慢充，我不乱跑")
    private val chargingM = listOf("在充电，安心", "慢慢充，我不乱跑")
    private val happyF = listOf("今天心情超好！", "比心～", "要一直这样下去")
    private val happyM = listOf("今天心情很好", "继续保持", "挺开心的")
    private val shyF = listOf("才、才没有害羞…", "不要一直看啦", "脸好烫")
    private val shyM = listOf("才没有害羞", "别一直看", "脸有点热")
    private val sleepCycleF = listOf("好困呀…", "再让我躺一会", "Zzz")
    private val sleepCycleM = listOf("困了…", "再躺一会", "Zzz")
    private val sadF = listOf("有点想你了…", "会一直等你的", "抱一下就好")
    private val sadM = listOf("有点想你", "会等你的", "抱一下就好")
    private val ambientHappyF = listOf("发呆也很好玩", "要不要歇一会儿？", "我在这儿陪你")
    private val ambientOkF = listOf("工作还顺利吗", "喝口水呀", "我轻轻待着就好")
    private val ambientLowF = listOf("有点寂寞呢", "记得看看我哦", "今天好长啊")
    private val ambientHappyM = listOf("发呆也不错", "要不要歇一会儿", "我在这儿")
    private val ambientOkM = listOf("工作还顺利吗", "喝口水", "我安静待着")
    private val ambientLowM = listOf("有点寂寞", "记得看看我", "今天好长")

    fun greeting(s: PetSettings): String = pick(slotPool(s)) + " " + named(s, if (s.isMale) appearM else appearF)

    fun timeOfDay(s: PetSettings): String = pick(rhythmPool(s))

    fun todayLine(s: PetSettings): String = pickDaily(s, rhythmPool(s))

    fun nextTodayLine(s: PetSettings, current: String): String {
        val pool = rhythmPool(s)
        if (pool.isEmpty()) return current
        val rest = pool.filter { it != current }
        return if (rest.isEmpty()) pick(pool) else pick(rest)
    }

    fun tap(s: PetSettings): String {
        val base = pick(
            when (PetStats.tier(s.mood)) {
                MoodTier.HAPPY -> if (s.isMale) tapHappyM else tapHappyF
                MoodTier.OK -> if (s.isMale) tapOkM else tapOkF
                MoodTier.LOW, MoodTier.SAD -> if (s.isMale) tapLowM else tapLowF
            },
        )
        return if (CompanionBond.richer(s) && Random.nextFloat() < 0.4f) {
            pick(slotPool(s)) + "，" + base
        } else {
            base
        }
    }

    fun pet(s: PetSettings): String = pick(if (s.isMale) petM else petF)
    fun feed(s: PetSettings, food: String = ""): String {
        val line = pick(if (s.isMale) feedM else feedF)
        return if (food.isEmpty()) line else "$food，$line"
    }
    fun noSnack(s: PetSettings): String = pick(if (s.isMale) noSnackM else noSnackF)
    fun cardio(s: PetSettings, kcal: Int): String = pick(if (s.isMale) cardioM else cardioF).format(kcal)
    fun sleep(s: PetSettings): String = pick(if (s.isMale) sleepM else sleepF)
    fun peek(s: PetSettings): String = pick(if (s.isMale) peekM else peekF)
    fun wake(s: PetSettings): String = pick(if (s.isMale) wakeM else wakeF)
    fun batteryLow(s: PetSettings): String = pick(if (s.isMale) batteryLowM else batteryLowF)
    fun charging(s: PetSettings): String = pick(if (s.isMale) chargingM else chargingF)

    fun pose(s: PetSettings, pose: PetPose): String {
        return pick(
            when (pose) {
                PetPose.HAPPY, PetPose.WAVE, PetPose.JUMP -> if (s.isMale) happyM else happyF
                PetPose.SHY -> if (s.isMale) shyM else shyF
                PetPose.SLEEP -> if (s.isMale) sleepCycleM else sleepCycleF
                PetPose.SAD, PetPose.TILT -> if (s.isMale) sadM else sadF
                else -> if (s.isMale) tapOkM else tapOkF
            },
        )
    }

    fun ambient(s: PetSettings): String {
        if (Random.nextFloat() < 0.7f) return timeOfDay(s)
        return pick(
            when (PetStats.tier(s.mood)) {
                MoodTier.HAPPY -> if (s.isMale) ambientHappyM else ambientHappyF
                MoodTier.OK -> if (s.isMale) ambientOkM else ambientOkF
                MoodTier.LOW, MoodTier.SAD -> if (s.isMale) ambientLowM else ambientLowF
            },
        )
    }

    fun streak(s: PetSettings, days: Int): String {
        return if (s.isMale) {
            "连续 %d 天了，我都在。".format(days)
        } else {
            "已经一起过了 %d 天呢。".format(days)
        }
    }

    private fun rhythmPool(s: PetSettings): List<String> {
        return slotPool(s) + stageExtra(s) + styleExtra(s)
    }

    private fun slotPool(s: PetSettings): List<String> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val male = s.isMale
        return when (hour) {
            in 6..10 -> if (male) morningM else morningF
            in 11..16 -> if (male) noonM else noonF
            in 17..21 -> if (male) eveningM else eveningF
            else -> if (male) nightM else nightF
        }
    }

    private fun stageExtra(s: PetSettings): List<String> {
        val male = s.isMale
        return when (CompanionBond.stage(s)) {
            BondStage.FIRST -> emptyList()
            BondStage.WARM -> if (male) stageWarmM else stageWarmF
            BondStage.TACIT -> if (male) stageTacitM else stageTacitF
            BondStage.BOND -> if (male) stageBondM else stageBondF
        }
    }

    private fun styleExtra(s: PetSettings): List<String> {
        if (!CompanionBond.richer(s)) return emptyList()
        return when (s.resolvedStyle().id) {
            "yujie" -> listOf(if (s.isMale) "茶还温着" else "茶先给你")
            "luoli" -> listOf(if (s.isMale) "软软待着" else "轻轻挨着你")
            "qingchun" -> listOf("窗边刚刚好")
            "wenrou" -> listOf(if (s.isMale) "不急，我在" else "不急，我在呢")
            "qingshuang" -> listOf("风也干净")
            "chenwen" -> listOf("伞还在原处")
            "dashu" -> listOf("外套给你")
            "chenggong" -> listOf("开完会，也坐一会儿")
            else -> emptyList()
        }
    }

    private fun named(s: PetSettings, pool: List<String>): String {
        return pick(pool).replace("%s", s.displayName())
    }

    private fun pick(pool: List<String>): String = pool.random()

    private fun pickDaily(s: PetSettings, pool: List<String>): String {
        if (pool.isEmpty()) return ""
        val seed = CompanionDay.todayKey() * 31 + s.displayName().hashCode()
        val i = (seed and 0x7fffffff) % pool.size
        return pool[i]
    }
}
