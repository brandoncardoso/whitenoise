package com.bcardoso.whitenoise.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bcardoso.whitenoise.R
import com.bcardoso.whitenoise.activities.MainActivity
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

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playbackStateBuilder: PlaybackStateCompat.Builder

    private val PLAY_PAUSE_ACTION =  "com.bcardoso.whitenoise.action.PLAY_PAUSE"

    private val NOTIFICATION_CHANNEL_ID = "whitenoise"
    private val NOTIFICATION_ID = 0
    private lateinit var notificationManagerCompat: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder

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

        configureMediaSession()

        notificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        createNotificationChannel()
        notificationBuilder = generateNotificationBuilder()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                PLAY_PAUSE_ACTION -> togglePlayPause()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaSession.release()
        notificationManagerCompat.cancelAll()
        super.onDestroy()
    }

    fun togglePlayPause() {
        if (isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (!isPlaying) {
            isPlaying = true
            activeSounds.forEach { (_, mp) -> mp.start() }
            updatePlayPauseAction()
            updateNotification()
        }
    }

    fun pause() {
        if (isPlaying) {
            isPlaying = false
            activeSounds.forEach { (_, mp) -> mp.pause() }
            updatePlayPauseAction()
            updateNotification()
        }
    }

    fun getActiveSounds(): MutableList<Pair<Sound, LoopMediaPlayer>> {
        return activeSounds
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, "whitenoise", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
            }

            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        notifyNotificationManager(NOTIFICATION_ID, notificationBuilder.build())
    }

    @SuppressLint("RestrictedApi")
    private fun updatePlayPauseAction() {
        val playPauseIntent = Intent(this, SoundService::class.java)
        playPauseIntent.action = PLAY_PAUSE_ACTION

        val playPausePendingIntent = PendingIntent.getService(
            this,
            System.currentTimeMillis().toInt(),
            playPauseIntent,
            0
        )

        val playPauseAction = NotificationCompat.Action.Builder(
            if (isPlaying) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24,
            if (isPlaying) "Pause" else "Play",
            playPausePendingIntent
        ).build()

        notificationBuilder.mActions = arrayListOf(playPauseAction)
    }

    private fun notifyNotificationManager(notificationId: Int, notification: Notification) {
        notificationManagerCompat.notify(notificationId, notification)
    }

    private fun generateNotificationBuilder(): NotificationCompat.Builder {
        val notifyIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_play_arrow_24) // TODO app icon
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(notifyPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .setWhen(0)
    }

    fun setNotificationContentText(text: String = "") {
        notificationBuilder.setContentText(text)
        updateNotification()
    }

    private fun configureMediaSession() {
        mediaSession = MediaSessionCompat(this, "whitenoise")

        val initialState = if (isPlaying)
            PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        playbackStateBuilder = PlaybackStateCompat.Builder()

        playbackStateBuilder
            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
            .setState(initialState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1F)

        mediaSession.setPlaybackState(playbackStateBuilder.build())

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPause() {
                pause()
                super.onPause()
            }

            override fun onPlay() {
                play()
                super.onPlay()
            }
        })

        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .build())

        mediaSession.isActive = true
    }

    inner class LocalBinder : Binder() {
        fun getService(): SoundService {
            return this@SoundService
        }
    }
}