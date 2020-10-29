package com.bcardoso.whitenoise

import android.media.MediaPlayer

interface SoundControlInterface {
    fun isPlaying() : Boolean
    fun getActiveSounds() : MutableList<Pair<Sound, MediaPlayer>>
    fun togglePlayPause() : Boolean
    fun updateSoundControlFragment()
    fun pauseAllSounds()
}