package com.bcardoso.whitenoise

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.bcardoso.whitenoise.ui.main.MainFragment
import com.bcardoso.whitenoise.ui.main.Sound

class WhiteNoiseActivity : AppCompatActivity(), SoundControlInterface {
    val NOTIFICATION_CHANNEL_ID = "whitenoise"
    private lateinit var mNotificationManagerCompat : NotificationManagerCompat

    private var mIsPlaying = false
    private val mActiveSounds = mutableListOf<Pair<Sound, MediaPlayer>>()
    private val mSounds = listOf(
        Sound("Rain", R.raw.rain, .78F),
        Sound("Thunder", R.raw.thunder, 0.1F)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow()
        }

        mNotificationManagerCompat = NotificationManagerCompat.from(applicationContext);
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        mSounds.forEach { sound ->
            val mediaPlayer = MediaPlayer.create(this, sound.id, audioAttributes, 1)
            mediaPlayer.setAudioAttributes(audioAttributes)
            mediaPlayer.setVolume(sound.volume, sound.volume)
            mediaPlayer.isLooping = true
            mActiveSounds.add(Pair(sound, mediaPlayer))
        }
    }

    private fun startAllActiveSounds() { mActiveSounds.forEach { (_, mp) -> mp.start() } }
    private fun pauseAllActiveSounds() { mActiveSounds.forEach { (_, mp) -> mp.pause() } }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun notifyNotificationManager(notificationId: Int, notification: Notification) {
        mNotificationManagerCompat.notify(notificationId, notification)
    }

    override fun isPlaying(): Boolean {
        return mIsPlaying
    }

    override fun getActiveSounds(): MutableList<Pair<Sound, MediaPlayer>> {
        return mActiveSounds
    }

    override fun togglePlayPause(): Boolean {
        if (mIsPlaying) {
            pauseAllActiveSounds()
        } else {
            startAllActiveSounds()
        }
        mIsPlaying = !mIsPlaying
        return mIsPlaying
    }
}