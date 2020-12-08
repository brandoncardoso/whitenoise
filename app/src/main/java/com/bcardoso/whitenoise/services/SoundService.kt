package com.bcardoso.whitenoise.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.os.Binder
import android.os.IBinder
import com.bcardoso.whitenoise.R
import com.bcardoso.whitenoise.utils.LoopMediaPlayer
import com.bcardoso.whitenoise.utils.Sound

class SoundService : Service() {
    private val binder = LocalBinder()

    private var isPlaying = false

    private lateinit var volumePrefs: SharedPreferences
    private lateinit var sounds: List<Sound>
    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()
    private lateinit var activeSounds: MutableList<Pair<Sound, LoopMediaPlayer>>

    override fun onCreate() {
        super.onCreate()

        volumePrefs = getSharedPreferences("volumes", Context.MODE_PRIVATE)

        sounds = listOf(
            Sound("Rain", R.raw.rain, volumePrefs.getFloat("Rain", 0F)),
            Sound("Thunder", R.raw.thunder, volumePrefs.getFloat("Thunder", 0F)),
            Sound("Forest", R.raw.forest, volumePrefs.getFloat("Forest", 0F)),
            Sound("Waves", R.raw.forest, volumePrefs.getFloat("Waves", 0F))
        )

        activeSounds = MutableList(sounds.size) { i ->
            Pair(sounds[i], LoopMediaPlayer.create(this, sounds[i].id, audioAttributes, sounds[i].volume))
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY
    }

    fun play() {
        if (!isPlaying) {
            isPlaying = true
            activeSounds.forEach { (_, mp) -> mp.start() }
        }
    }

    fun pause() {
        if (isPlaying) {
            isPlaying = false
            activeSounds.forEach { (_, mp) -> mp.pause() }
        }
    }

    fun getActiveSounds(): MutableList<Pair<Sound, LoopMediaPlayer>> {
        return activeSounds
    }

    inner class LocalBinder : Binder() {
        fun getService(): SoundService {
            return this@SoundService
        }
    }
}