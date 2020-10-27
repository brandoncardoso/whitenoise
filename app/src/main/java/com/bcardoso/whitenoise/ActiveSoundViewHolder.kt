package com.bcardoso.whitenoise

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActiveSoundViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.active_sound_list_item, parent, false)) {
    private var mNameView : TextView = itemView.findViewById(R.id.active_sound_list_item_name)
    private var mVolumeControl : SeekBar = itemView.findViewById(R.id.active_sound_list_item_volume)

    init {
        mVolumeControl.max = 100
    }

    fun bind(soundPair : Pair<Sound, MediaPlayer>) {
        var (sound, mediaPlayer) = soundPair

        mNameView.text = sound.name
        mVolumeControl.progress = (sound.volume * 100).toInt()
        mVolumeControl.setOnSeekBarChangeListener(VolumeControlChangeListener(mediaPlayer))
    }

    private class VolumeControlChangeListener(val mediaPlayer: MediaPlayer) : OnSeekBarChangeListener {
        override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
            val newVolume = progress / 100.0F
            mediaPlayer.setVolume(newVolume, newVolume)
        }

        override fun onStartTrackingTouch(p0: SeekBar?) { }
        override fun onStopTrackingTouch(p0: SeekBar?) { }
    }
}