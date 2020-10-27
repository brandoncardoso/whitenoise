package com.bcardoso.whitenoise

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bcardoso.whitenoise.ui.main.SoundControlFragment
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

    enum class ACTION(val id:String) {
        PLAY_TOGGLE("com.bcardoso.whitenoise.action.PLAY_TOGGLE")
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

        generateNotification()
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

        val playToggleIntent = Intent(this, WhiteNoiseActivity::class.java).apply {
            action = ACTION.PLAY_TOGGLE.id
        }
        val playTogglePendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            playToggleIntent,
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        when(intent?.action) {
            ACTION.PLAY_TOGGLE.id -> {
                togglePlayPause()
            }
        }
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
        return mIsPlaying
    }
}