package com.bcardoso.whitenoise.adapters

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bcardoso.whitenoise.R
import com.bcardoso.whitenoise.utils.LoopMediaPlayer
import com.bcardoso.whitenoise.utils.Sound

class ActiveSoundViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.active_sound_list_item, parent, false)) {
    private var mNameView: TextView = itemView.findViewById(R.id.active_sound_list_item_name)
    private var mVolumeControl: SeekBar = itemView.findViewById(R.id.active_sound_list_item_volume)
    val volumePrefs = parent.context.getSharedPreferences("volumes", Context.MODE_PRIVATE)

    init {

        mVolumeControl.max = 100
    }

    fun bind(soundPair: Pair<Sound, LoopMediaPlayer>) {
        var (sound, mediaPlayer) = soundPair

        mNameView.text = sound.name
        mVolumeControl.progress = (sound.volume * 100).toInt()
        mVolumeControl.setOnSeekBarChangeListener(
            VolumeControlChangeListener(
                sound,
                mediaPlayer,
                volumePrefs
            )
        )
    }

    private class VolumeControlChangeListener(
        val sound: Sound,
        val mediaPlayer: LoopMediaPlayer,
        val volumePrefs: SharedPreferences
    ) : OnSeekBarChangeListener {
        override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
            val newVolume = progress / 100.0F
            sound.volume = newVolume
            mediaPlayer.setVolume(newVolume)
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(seekbar: SeekBar?) {
            with(volumePrefs.edit()) {
                if (seekbar != null) {
                    putFloat(sound.name, seekbar.progress / 100.0F)
                }
                apply()
            }
        }
    }
}