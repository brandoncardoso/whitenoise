package com.bcardoso.whitenoise.activities

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bcardoso.whitenoise.R
import com.bcardoso.whitenoise.interfaces.SoundControlInterface
import com.bcardoso.whitenoise.services.CustomIntentService
import com.bcardoso.whitenoise.services.CustomResultReceiver
import com.bcardoso.whitenoise.services.SoundService
import com.bcardoso.whitenoise.viewmodels.MainViewModel
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), SoundControlInterface, CustomResultReceiver.AppReceiver {
    private lateinit var soundService: SoundService
    private var isBound: Boolean = false

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SoundService.LocalBinder
            soundService = binder.getService()
            isBound = true

            viewModel.setActiveSounds(soundService.getActiveSounds())
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    private val viewModel: MainViewModel by viewModels()

    private val NOTIFICATION_CHANNEL_ID = "whitenoise"
    private val NOTIFICATION_ID = 0
    private lateinit var mNotificationManagerCompat: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private lateinit var topAppBar: androidx.appcompat.widget.Toolbar

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playbackStateBuilder: PlaybackStateCompat.Builder

    private val resultReceiver = CustomResultReceiver(Handler(), this)

    enum class ACTION(val id: String) {
        PLAY_PAUSE("com.bcardoso.whitenoise.action.PLAY_PAUSE")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val intent = Intent(this, SoundService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        viewModel.isPlaying.observe(this, ::onTogglePlayPause)
        viewModel.sleepTimerTimeRemaining.observe(this, ::onSleepTimerUpdate)
        viewModel.isSleepTimerFinished.observe(this, ::onSleepTimerFinished)

        topAppBar = findViewById(R.id.topAppBar)
        setSupportActionBar(findViewById(R.id.topAppBar))

        configureMediaSession()

        registerIntentService()

        mNotificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        createNotificationChannel()
        notificationBuilder = generateNotificationBuilder()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "whitenoise",
                NotificationManager.IMPORTANCE_LOW
            )
                .apply {
                    description = getString(R.string.channel_description)
                }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        notifyNotificationManager(NOTIFICATION_ID, notificationBuilder.build())
    }

    @SuppressLint("RestrictedApi")
    private fun updatePlayPauseAction(isPlaying: Boolean) {
        val playPauseIntent = Intent(applicationContext, CustomIntentService::class.java).apply {
            action = ACTION.PLAY_PAUSE.id
            putExtra("receiver", resultReceiver)
        }

        val playPausePendingIntent = PendingIntent.getService(
            applicationContext,
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
        mNotificationManagerCompat.notify(notificationId, notification)
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

    private fun onTogglePlayPause(isPlaying: Boolean) {
        if (isBound) {
            if (isPlaying) {
                soundService.play()
            } else {
                soundService.pause()
            }
            updatePlayPauseAction(isPlaying)
            updateNotification()
        }
    }

    override fun onCancelSleepTimer() {
        notificationBuilder.setContentText(null)
        updateNotification()
    }

    private fun onSleepTimerUpdate(remainingTimeMs: Long) {
        val timerString = String.format(
            "%02d:%02d:%02d", // HH:MM:SS
            TimeUnit.MILLISECONDS.toHours(remainingTimeMs), // hours
            TimeUnit.MILLISECONDS.toMinutes(remainingTimeMs) - // minutes
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(remainingTimeMs)),
            TimeUnit.MILLISECONDS.toSeconds(remainingTimeMs) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(remainingTimeMs)))

        notificationBuilder.setContentText(timerString)
        updateNotification()
    }

    private fun onSleepTimerFinished(isFinished: Boolean) {
        if (isBound && isFinished) {
            soundService.pause()
            notificationBuilder.setContentText("Timer finished.")
            updateNotification()
        }
    }

    override fun onDestroy() {
        mediaSession.release()
        mNotificationManagerCompat.cancelAll()
        unbindService(connection)
        super.onDestroy()
    }

    private fun registerIntentService() {
        val intent = Intent(applicationContext, CustomIntentService::class.java)
        intent.putExtra("receiver", resultReceiver)
        startService(intent)
    }

    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
        when (resultCode) {
            CustomIntentService.STATUS_FINISHED -> {
                resultData?.get("action")?.let { action ->
                    when (action) {
                        ACTION.PLAY_PAUSE.id -> {
                            viewModel.togglePlaying()
                        }
                    }
                }
            }
        }
    }

    private fun configureMediaSession() {
        mediaSession = MediaSessionCompat(this, "whitenoise")

        val initialState = if (viewModel.isPlaying.value == true)
            PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        playbackStateBuilder = PlaybackStateCompat.Builder()

        playbackStateBuilder
            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
            .setState(initialState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1F)

        mediaSession.setPlaybackState(playbackStateBuilder.build())

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPause() {
                viewModel.pause()
                super.onPause()
            }

            override fun onPlay() {
                viewModel.play()
                super.onPlay()
            }
        })

        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .build())

        mediaSession.isActive = true
    }
}