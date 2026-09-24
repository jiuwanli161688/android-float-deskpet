package com.floatdeskpet.app.overlay

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.floatdeskpet.app.data.PetSettings
import java.util.Locale

class PetVoice(context: Context) {
    private val app = context.applicationContext
    private val settings = PetSettings.get(app)
    private var tts: TextToSpeech? = null
    private var thread: HandlerThread? = null
    private var worker: Handler? = null
    @Volatile private var ready = false
    @Volatile private var started = false
    @Volatile private var voiceReady = false

    fun ensure() {
        if (started) return
        started = true
        val ht = HandlerThread("pet-tts").also { thread = it }
        ht.start()
        val h = Handler(ht.looper).also { worker = it }
        h.post {
            try {
                tts = TextToSpeech(app) { status ->
                    ready = status == TextToSpeech.SUCCESS && tts != null
                    if (ready) applyVoiceLocked()
                }
            } catch (_: Throwable) {
                ready = false
                tts = null
            }
        }
    }

    fun speak(text: String) {
        if (settings.muted || !settings.ttsEnabled) return
        val line = text.trim()
        if (line.isEmpty()) return
        ensure()
        worker?.post {
            val engine = tts ?: return@post
            if (!ready) return@post
            try {
                if (!voiceReady) applyVoiceLocked()
                engine.speak(line, TextToSpeech.QUEUE_FLUSH, null, "pet-line")
            } catch (_: Throwable) {
            }
        }
    }

    fun silence() {
        worker?.post {
            try {
                tts?.stop()
            } catch (_: Throwable) {
            }
        } ?: run {
            try {
                tts?.stop()
            } catch (_: Throwable) {
            }
        }
    }

    fun applyVoice() {
        voiceReady = false
        worker?.post { applyVoiceLocked() }
    }

    fun release() {
        ready = false
        voiceReady = false
        started = false
        val engine = tts
        tts = null
        worker?.post {
            try {
                engine?.stop()
                engine?.shutdown()
            } catch (_: Throwable) {
            }
        }
        worker = null
        try {
            thread?.quitSafely()
        } catch (_: Throwable) {
        }
        thread = null
    }

    private fun applyVoiceLocked() {
        val engine = tts ?: return
        if (!ready) return
        try {
            val locale = preferredLocale(engine)
            engine.language = locale
            if (!voiceReady) {
                val voice = pickVoice(engine, settings.isMale, locale)
                if (voice != null) {
                    try {
                        engine.voice = voice
                    } catch (_: Throwable) {
                    }
                }
                if (settings.isMale) {
                    val matched = voice != null && looksMale(voice)
                    engine.setPitch(if (matched) 0.96f else 0.78f)
                    engine.setSpeechRate(0.94f)
                } else {
                    val matched = voice != null && looksFemale(voice)
                    engine.setPitch(if (matched) 1.04f else 1.18f)
                    engine.setSpeechRate(1.0f)
                }
                voiceReady = true
            }
        } catch (_: Throwable) {
        }
    }

    private fun preferredLocale(engine: TextToSpeech): Locale {
        val candidates = listOf(Locale.SIMPLIFIED_CHINESE, Locale.CHINESE, Locale.TRADITIONAL_CHINESE)
        for (locale in candidates) {
            val avail = try {
                engine.isLanguageAvailable(locale)
            } catch (_: Throwable) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }
            if (avail >= TextToSpeech.LANG_AVAILABLE) return locale
        }
        return Locale.getDefault()
    }

    private fun pickVoice(engine: TextToSpeech, male: Boolean, locale: Locale): Voice? {
        val all = try {
            engine.voices
        } catch (_: Throwable) {
            null
        } ?: return null
        val local = all.filter { !it.isNetworkConnectionRequired }
        val lang = local.filter { it.locale.language == locale.language }
        val gendered = lang.filter { if (male) looksMale(it) else looksFemale(it) }
        val pool = when {
            gendered.isNotEmpty() -> gendered
            lang.isNotEmpty() -> lang
            else -> local
        }
        return pool.minWithOrNull(compareBy<Voice> { it.latency }.thenByDescending { it.quality })
            ?: pool.firstOrNull()
    }

    private fun looksMale(v: Voice): Boolean {
        val n = (v.name + " " + v.locale.toString()).lowercase()
        if (n.contains("female") || n.contains("woman") || n.contains("女")) return false
        return n.contains("male") || n.contains("man") || n.contains("男") ||
            n.contains("-m-") || n.contains("#male")
    }

    private fun looksFemale(v: Voice): Boolean {
        val n = (v.name + " " + v.locale.toString()).lowercase()
        return n.contains("female") || n.contains("woman") || n.contains("女") ||
            n.contains("-f-") || n.contains("#female")
    }
}
