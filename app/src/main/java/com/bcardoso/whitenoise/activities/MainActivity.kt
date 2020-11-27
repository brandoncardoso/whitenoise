package com.bcardoso.whitenoise.activities

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bcardoso.whitenoise.R
import com.bcardoso.whitenoise.interfaces.SoundControlInterface
import com.bcardoso.whitenoise.utils.LoopMediaPlayer
import com.bcardoso.whitenoise.viewmodels.MainViewModel
import java.util.concurrent.TimeUnit

data class Sound(var name: String, var id: Int, var initialVolume: Float = 0F) {
    var volume = initialVolume
}

class MainActivity : AppCompatActivity(), SoundControlInterface {
    private lateinit var volumePrefs: SharedPreferences
    private val viewModel: MainViewModel by viewModels()

    private val NOTIFICATION_CHANNEL_ID = "whitenoise"
    private val NOTIFICATION_ID = 0
    private lateinit var mNotificationManagerCompat: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private lateinit var topAppBar: androidx.appcompat.widget.Toolbar
    private lateinit var countDownTimer: CountDownTimer

    private val mActiveSounds = mutableListOf<Pair<Sound, LoopMediaPlayer>>()
    private lateinit var mSounds: List<Sound>

    enum class ACTION(val id: String) {
        PLAY_TOGGLE("com.bcardoso.whitenoise.action.PLAY_TOGGLE")
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION.PLAY_TOGGLE.id -> {
                    viewModel.togglePlaying()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        viewModel.isPlaying.observe(this, ::togglePlayPause)

        topAppBar = findViewById(R.id.topAppBar)
        setSupportActionBar(findViewById(R.id.topAppBar))

        volumePrefs = getSharedPreferences("volumes", Context.MODE_PRIVATE)

        mSounds = listOf(
            Sound("Rain", R.raw.rain, volumePrefs.getFloat("Rain", 0F)),
            Sound("Thunder", R.raw.thunder, volumePrefs.getFloat("Thunder", 0F)),
            Sound("Forest", R.raw.forest, volumePrefs.getFloat("Forest", 0F)),
            Sound("Waves", R.raw.forest, volumePrefs.getFloat("Waves", 0F))
        )

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        mSounds.forEach { sound ->
            mActiveSounds.add(
                Pair(
                    sound,
                    LoopMediaPlayer.create(this, sound.id, audioAttributes, sound.volume)
                )
            )
        }

        registerReceiver(receiver, IntentFilter(ACTION.PLAY_TOGGLE.id))

        mNotificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        createNotificationChannel()
        notificationBuilder = generateNotificationBuilder()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

    private fun startAllActiveSounds() {
        mActiveSounds.forEach { (_, mp) -> mp.start() }
    }

    private fun pauseAllActiveSounds() {
        mActiveSounds.forEach { (_, mp) -> mp.pause() }
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
    private fun updatePlayToggleAction(isPlaying: Boolean) {
        val playTogglePendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            Intent(ACTION.PLAY_TOGGLE.id),
            0
        )
        val playToggleAction = NotificationCompat.Action.Builder(
            if (isPlaying) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24,
            if (isPlaying) "Pause" else "Play",
            playTogglePendingIntent
        )
            .build()

        notificationBuilder.mActions = arrayListOf(playToggleAction)
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
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentText("Whitenoise")
            .setSmallIcon(R.drawable.ic_baseline_play_arrow_24) // TODO app icon
            .setContentIntent(notifyPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0)
            )
            .setWhen(0)
    }

    override fun getActiveSounds(): MutableList<Pair<Sound, LoopMediaPlayer>> {
        return mActiveSounds
    }

    private fun togglePlayPause(isPlaying: Boolean) {
        if (isPlaying) {
            startAllActiveSounds()
        } else {
            pauseAllActiveSounds()
        }
        updatePlayToggleAction(isPlaying)
        updateNotification()
    }

    override fun cancelTimer() {
        if (this::countDownTimer.isInitialized) countDownTimer.cancel()
        notificationBuilder.setContentText(null)
        updateNotification()
    }

    override fun setTimer(millis: Long, timerText: MenuItem) {
        cancelTimer()

        countDownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(remainingTimeMs: Long) = onTimerUpdate(remainingTimeMs, timerText)
            override fun onFinish() = onTimerFinish(timerText)
        }
        countDownTimer.start()
    }

    fun onTimerUpdate(remainingTimeMs: Long, timerText: MenuItem) {
        val timerString = String.format(
            "%02d:%02d:%02d", // HH:MM:SS
            TimeUnit.MILLISECONDS.toHours(remainingTimeMs), // hours
            TimeUnit.MILLISECONDS.toMinutes(remainingTimeMs) - // minutes
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(remainingTimeMs)),
            TimeUnit.MILLISECONDS.toSeconds(remainingTimeMs) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(remainingTimeMs)))

        timerText.title = timerString
        notificationBuilder.setContentText(timerString)
        updateNotification()
    }

    fun onTimerFinish(timerText: MenuItem) {
        pauseAllActiveSounds()
        timerText.title = null
        notificationBuilder.setContentText("Timer finished.")
        updateNotification()
    }

    override fun onDestroy() {
        mNotificationManagerCompat.cancelAll()
        super.onDestroy()
    }
}