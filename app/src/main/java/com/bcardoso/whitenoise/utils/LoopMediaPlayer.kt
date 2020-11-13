package com.bcardoso.whitenoise.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener

class LoopMediaPlayer private constructor(
    private val context: Context,
    private val resId: Int = 0,
    private val audioAttributes: AudioAttributes,
    private var volume: Float
) {
    private var currentPlayer: MediaPlayer
    private lateinit var nextPlayer: MediaPlayer

    companion object {
        fun create(context: Context, resId: Int, attr: AudioAttributes, volume: Float): LoopMediaPlayer {
            return LoopMediaPlayer(context, resId, attr, volume)
        }
    }

    init {
        currentPlayer = MediaPlayer.create(context, resId, audioAttributes, 1)
        currentPlayer.setVolume(volume, volume)
        createNextMediaPlayer()
    }

    private fun createNextMediaPlayer() {
        nextPlayer = MediaPlayer.create(context, resId, audioAttributes, 1)
        nextPlayer.setVolume(volume, volume)
        currentPlayer.setNextMediaPlayer(nextPlayer)
        currentPlayer.setOnCompletionListener(onCompletionListener)
    }

    private val onCompletionListener = OnCompletionListener { mediaPlayer ->
        mediaPlayer.reset()
        mediaPlayer.release()
        currentPlayer = nextPlayer
        createNextMediaPlayer()
    }

    fun start() {
        currentPlayer.start()
        currentPlayer.setOnCompletionListener(onCompletionListener)
    }

    fun stop() {
        currentPlayer.stop()
    }

    fun pause() {
        currentPlayer.pause()
    }

    fun setVolume(newVolume: Float) {
        volume = newVolume
        currentPlayer.setVolume(volume, volume)
    }
}