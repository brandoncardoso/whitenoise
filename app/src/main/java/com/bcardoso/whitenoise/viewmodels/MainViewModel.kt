package com.bcardoso.whitenoise.viewmodels

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bcardoso.whitenoise.utils.LoopMediaPlayer
import com.bcardoso.whitenoise.utils.Sound

class MainViewModel : ViewModel() {
    private var _activeSounds = MutableLiveData<MutableList<Pair<Sound, LoopMediaPlayer>>>()
    val activeSounds: LiveData<MutableList<Pair<Sound, LoopMediaPlayer>>>
        get() = _activeSounds

    private var _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean>
        get() = _isPlaying

    private var _sleepTimer = MutableLiveData<CountDownTimer>()
    private var _isSleepTimerFinished = MutableLiveData<Boolean>()
    val isSleepTimerFinished: LiveData<Boolean>
        get() = _isSleepTimerFinished
    private var _sleepTimerTimeRemaining = MutableLiveData<Long>()
    val sleepTimerTimeRemaining: LiveData<Long>
        get() = _sleepTimerTimeRemaining

    init {
        _isPlaying.value = false
    }

    fun setSleepTimer(millis: Long) {
        _isSleepTimerFinished.value = false

        _sleepTimer.value = object : CountDownTimer(millis, 1000) {
            override fun onTick(remainingTime: Long) {
                _sleepTimerTimeRemaining.value = remainingTime
            }

            override fun onFinish() {
                _isSleepTimerFinished.value = true
            }
        }
    }

    fun setActiveSounds(sounds: MutableList<Pair<Sound, LoopMediaPlayer>>) {
        _activeSounds.value = sounds
    }

    fun startSleepTimer() = _sleepTimer.value?.start()

    fun cancelSleepTimer() = _sleepTimer.value?.cancel()

    fun pause() {
        _isPlaying.value = false
    }

    fun play() {
        _isPlaying.value = true
    }

    fun togglePlaying() {
        _isPlaying.value = !_isPlaying.value!!
    }
}