package com.bcardoso.whitenoise

import android.media.MediaPlayer
import com.bcardoso.whitenoise.ui.main.Sound

interface SoundControlInterface {
    fun isPlaying() : Boolean
    fun getActiveSounds() : MutableList<Pair<Sound, MediaPlayer>>
    fun togglePlayPause() : Boolean
    fun updateSoundControlFragment()
}