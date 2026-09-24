package com.floatdeskpet.app.ui

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.floatdeskpet.app.R
import com.floatdeskpet.app.data.WhisperStore
import com.floatdeskpet.app.databinding.ActivityWhisperBinding
import java.util.Date

class WhisperActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWhisperBinding
    private lateinit var store: WhisperStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWhisperBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = WhisperStore.get(this)
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnSend.setOnClickListener { send() }
        binding.input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send()
                true
            } else {
                false
            }
        }
        render()
    }

    private fun send() {
        val saved = store.add(binding.input.text?.toString().orEmpty())
        if (saved == null) {
            binding.hint.text = getString(R.string.whisper_hint_empty)
            return
        }
        binding.input.setText("")
        binding.hint.text = getString(R.string.whisper_saved)
        render()
    }

    private fun render() {
        val list = binding.whisperList
        list.removeAllViews()
        val items = store.all()
        if (items.isEmpty()) {
            binding.empty.visibility = android.view.View.VISIBLE
            return
        }
        binding.empty.visibility = android.view.View.GONE
        val inflater = LayoutInflater.from(this)
        items.forEach { w ->
            val row = inflater.inflate(R.layout.item_whisper, list, false)
            row.findViewById<TextView>(R.id.whisperText).text = w.text
            row.findViewById<TextView>(R.id.whisperReply).text = w.reply
            row.findViewById<TextView>(R.id.whisperTime).text =
                DateFormat.format("M月d日 HH:mm", Date(w.at))
            row.findViewById<TextView>(R.id.btnDelete).setOnClickListener {
                store.delete(w.id)
                render()
            }
            list.addView(row)
        }
    }
}
