package com.bcardoso.whitenoise

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bcardoso.whitenoise.ui.main.SoundControlFragment

data class Sound(var name:String, var id: Int, var initialVolume:Float = 0F) {
    var volume = initialVolume
}

class WhiteNoiseActivity : AppCompatActivity(), SoundControlInterface {
    private val NOTIFICATION_CHANNEL_ID = "whitenoise"
    private lateinit var mNotificationManagerCompat : NotificationManagerCompat

    private var mIsPlaying = false
    private val mActiveSounds = mutableListOf<Pair<Sound, MediaPlayer>>()
    private val mSounds = listOf(
        Sound("Rain", R.raw.rain, .78F),
        Sound("Thunder", R.raw.thunder, 0.1F)
    )
    enum class ACTION(val id:String) {
        PLAY_TOGGLE("com.bcardoso.whitenoise.action.PLAY_TOGGLE")
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                ACTION.PLAY_TOGGLE.id -> {
                    togglePlayPause()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.`white_noise_activity`)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, SoundControlFragment.newInstance())
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

        registerReceiver(receiver, IntentFilter(ACTION.PLAY_TOGGLE.id))
        generateNotification()
    }

    private fun startAllActiveSounds() { mActiveSounds.forEach { (_, mp) -> mp.start() } }
    private fun pauseAllActiveSounds() { mActiveSounds.forEach { (_, mp) -> mp.pause() } }
    private fun stopAllActiveSounds() { mActiveSounds.forEach { (_, mp) -> mp.stop() }}

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

    private fun generateNotification() {
        val notifyIntent = Intent(this, WhiteNoiseActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        val playTogglePendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            Intent(ACTION.PLAY_TOGGLE.id),
            0)
        val playToggleAction = NotificationCompat.Action.Builder(
            if (mIsPlaying) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24,
            if (mIsPlaying) "Pause" else "Play",
            playTogglePendingIntent)
            .build()

        var notification
                = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentText("Whitenoise")
            .setSmallIcon(R.drawable.ic_baseline_play_arrow_24) // TODO app icon
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(notifyPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(playToggleAction) // #0
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0))
            .setOngoing(true)
            .setWhen(0)
            .build()

        notifyNotificationManager(0, notification)
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
        generateNotification()
        updateSoundControlFragment()
        return mIsPlaying
    }

    override fun updateSoundControlFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, SoundControlFragment())
            .commitNow()
    }

    override fun pauseAllSounds() {
        pauseAllActiveSounds()
        mIsPlaying = false
        generateNotification()
        updateSoundControlFragment()
    }
}