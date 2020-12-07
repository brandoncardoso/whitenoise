package com.bcardoso.whitenoise.utils

data class Sound(var name: String, var id: Int, var initialVolume: Float = 0F) {
    var volume = initialVolume
}