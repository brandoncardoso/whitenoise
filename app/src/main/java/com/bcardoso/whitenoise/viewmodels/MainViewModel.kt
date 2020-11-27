package com.bcardoso.whitenoise.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    private var _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean>
        get() = _isPlaying

    init {
        _isPlaying.value = false
    }

    fun play() {
        _isPlaying.value = true
    }

    fun stop() {
        _isPlaying.value = false
    }

    fun togglePlaying() {
        _isPlaying.value = !_isPlaying.value!!
    }
}