package com.bcardoso.whitenoise

import LoopMediaPlayer

interface SoundControlInterface {
    fun isPlaying(): Boolean
    fun getActiveSounds(): MutableList<Pair<Sound, LoopMediaPlayer>>
    fun togglePlayPause(): Boolean
    fun updateSoundControlFragment()
    fun pauseAllSounds()
    fun onTimerUpdate(remainingTimeMs: Long)
    fun onTimerFinish()
    fun onTimerCancel()
}