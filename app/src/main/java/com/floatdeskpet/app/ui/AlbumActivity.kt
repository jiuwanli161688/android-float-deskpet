package com.floatdeskpet.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.floatdeskpet.app.R
import com.floatdeskpet.app.data.AlbumStore
import com.floatdeskpet.app.data.CompanionBond
import com.floatdeskpet.app.data.PetSettings
import com.floatdeskpet.app.databinding.ActivityAlbumBinding

class AlbumActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { finish() }
        val settings = PetSettings.get(this)
        val album = AlbumStore.get(this)
        album.sync(settings)
        val list = binding.albumList
        val inflater = LayoutInflater.from(this)
        var opened = 0
        album.cards().forEach { card ->
            val on = album.unlocked(card.id)
            if (on) opened += 1
            val row = inflater.inflate(R.layout.item_album_card, list, false)
            row.findViewById<TextView>(R.id.cardTitle).text = getString(card.title)
            row.findViewById<TextView>(R.id.cardBody).text = getString(if (on) card.body else card.locked)
            row.alpha = if (on) 1f else 0.62f
            list.addView(row)
        }
        if (opened == 0) {
            binding.albumHint.text = getString(R.string.album_empty)
        } else {
            binding.albumHint.text = getString(
                R.string.album_count,
                opened,
                album.cards().size,
                getString(
                    when (CompanionBond.stage(settings)) {
                        com.floatdeskpet.app.data.BondStage.FIRST -> R.string.bond_first
                        com.floatdeskpet.app.data.BondStage.WARM -> R.string.bond_warm
                        com.floatdeskpet.app.data.BondStage.TACIT -> R.string.bond_tacit
                        com.floatdeskpet.app.data.BondStage.BOND -> R.string.bond_bond
                    },
                ),
            )
        }
    }
}
