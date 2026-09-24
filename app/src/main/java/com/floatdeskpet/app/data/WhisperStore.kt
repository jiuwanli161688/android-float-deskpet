package com.floatdeskpet.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Whisper(
    val id: Long,
    val text: String,
    val reply: String,
    val at: Long,
)

class WhisperStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(): List<Whisper> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Whisper(
                    id = o.optLong("id"),
                    text = o.optString("text"),
                    reply = o.optString("reply"),
                    at = o.optLong("at"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun add(raw: String): Whisper? {
        val text = raw.trim().take(MAX_LEN)
        if (text.isEmpty()) return null
        val now = System.currentTimeMillis()
        val item = Whisper(
            id = now,
            text = text,
            reply = REPLIES[text.hashCode().ushr(1) % REPLIES.size],
            at = now,
        )
        save(listOf(item) + all().take(MAX_KEEP - 1))
        return item
    }

    fun delete(id: Long) {
        save(all().filterNot { it.id == id })
    }

    private fun save(items: List<Whisper>) {
        val arr = JSONArray()
        items.forEach { w ->
            arr.put(
                JSONObject()
                    .put("id", w.id)
                    .put("text", w.text)
                    .put("reply", w.reply)
                    .put("at", w.at),
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        const val PREFS = "whispers"
        const val KEY = "items"
        const val MAX_LEN = 80
        const val MAX_KEEP = 80
        private val REPLIES = listOf(
            "我听到了，先收着。",
            "嗯，放在枕头边。",
            "好，不急回。",
            "这句话我记下了。",
            "今天也谢谢你愿意说。",
        )

        @Volatile
        private var instance: WhisperStore? = null

        fun get(context: Context): WhisperStore {
            return instance ?: synchronized(this) {
                instance ?: WhisperStore(context).also { instance = it }
            }
        }
    }
}
