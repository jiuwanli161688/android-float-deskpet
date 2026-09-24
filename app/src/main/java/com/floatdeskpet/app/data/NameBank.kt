package com.floatdeskpet.app.data

object NameBank {
    val female = listOf(
        "晚晴",
        "疏影",
        "青柠",
        "栖迟",
        "南枝",
        "澄予",
        "未央",
        "听潮",
        "软风",
        "星予",
    )

    val male = listOf(
        "景行",
        "予安",
        "清和",
        "知夏",
        "远舟",
        "予白",
        "栖川",
        "望舒",
        "澄川",
        "临风",
    )

    fun pool(male: Boolean): List<String> = if (male) this.male else female

    fun pick(male: Boolean, index: Int): String {
        val names = pool(male)
        val i = index.mod(names.size)
        return names[i]
    }
}
