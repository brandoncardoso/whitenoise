package com.bcardoso.whitenoise.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class TimerDialogTime(hoursInitial: Int, minutesInitial: Int) {
    private var _hours: MutableLiveData<Int> = MutableLiveData(hoursInitial)
    private var _minutes: MutableLiveData<Int> = MutableLiveData(minutesInitial)
    val hours: LiveData<Int> = _hours
    val minutes: LiveData<Int> = _minutes

    private val hoursIncrement = 1
    private val minutesIncrement = 5

    private fun increment(x: Int, increment: Int, max: Int): Int {
        var retVal = x + increment
        if (retVal >= max) {
            retVal %= max
        }
        return retVal
    }

    private fun decrement(x: Int, decrement: Int, max: Int, min: Int): Int {
        var retVal = x - decrement
        if (retVal < min) {
            retVal += max
        }
        return retVal
    }

    fun incrementHours() {
        _hours.value = increment(_hours.value!!, hoursIncrement, 24)
    }

    fun decrementHours() {
        _hours.value = decrement(_hours.value!!, hoursIncrement, 24, 0)
    }

    fun incrementMinutes() {
        _minutes.value = increment(_minutes.value!!, minutesIncrement, 60)
    }

    fun decrementMinutes() {
        _minutes.value = decrement(_minutes.value!!, minutesIncrement, 60, 0)
    }

    fun getMillis(): Long {
        return (((_hours.value!! * 60) + _minutes.value!!) * 60 * 1000).toLong()
    }
}