package com.bcardoso.whitenoise.interfaces

import com.bcardoso.whitenoise.activities.Sound
import com.bcardoso.whitenoise.utils.LoopMediaPlayer

interface SoundControlInterface {
    fun getActiveSounds(): MutableList<Pair<Sound, LoopMediaPlayer>>
    fun onCancelSleepTimer()
}