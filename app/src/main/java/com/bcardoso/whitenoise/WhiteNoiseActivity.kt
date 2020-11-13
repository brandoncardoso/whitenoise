package com.bcardoso.whitenoise

import LoopMediaPlayer
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bcardoso.whitenoise.ui.main.SoundControlFragment
import java.util.concurrent.TimeUnit

data class Sound(var name: String, var id: Int, var initialVolume: Float = 0F) {
    var volume = initialVolume
}

class WhiteNoiseActivity : AppCompatActivity(), SoundControlInterface {
    private lateinit var volumePrefs: SharedPreferences

    private val NOTIFICATION_CHANNEL_ID = "whitenoise"
    private val NOTIFICATION_ID = 0
    private lateinit var mNotificationManagerCompat: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private var mIsPlaying = false
    private val mActiveSounds = mutableListOf<Pair<Sound, LoopMediaPlayer>>()
    private lateinit var mSounds: List<Sound>

    enum class ACTION(val id: String) {
        PLAY_TOGGLE("com.bcardoso.whitenoise.action.PLAY_TOGGLE")
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION.PLAY_TOGGLE.id -> {
                    togglePlayPause()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.`white_noise_activity`)

        volumePrefs = getSharedPreferences("volumes", Context.MODE_PRIVATE)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, SoundControlFragment.newInstance())
                .commitNow()
        }

        mSounds = listOf(
            Sound("Rain", R.raw.rain, volumePrefs.getFloat("Rain", 0F)),
            Sound("Thunder", R.raw.thunder, volumePrefs.getFloat("Thunder", 0F))
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

        mNotificationManagerCompat = NotificationManagerCompat.from(applicationContext);
        createNotificationChannel()
        notificationBuilder = generateNotificationBuilder()
        updatePlayToggleAction()
        updateNotification()
    }

    private fun startAllActiveSounds() {
        mActiveSounds.forEach { (_, mp) -> mp.start() }
    }

    private fun pauseAllActiveSounds() {
        mActiveSounds.forEach { (_, mp) -> mp.pause() }
    }

    private fun stopAllActiveSounds() {
        mActiveSounds.forEach { (_, mp) -> mp.stop() }
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
    private fun updatePlayToggleAction() {
        val playTogglePendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            Intent(ACTION.PLAY_TOGGLE.id),
            0
        )
        val playToggleAction = NotificationCompat.Action.Builder(
            if (mIsPlaying) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24,
            if (mIsPlaying) "Pause" else "Play",
            playTogglePendingIntent
        )
            .build()

        notificationBuilder.mActions = arrayListOf(playToggleAction)
    }

    private fun notifyNotificationManager(notificationId: Int, notification: Notification) {
        mNotificationManagerCompat.notify(notificationId, notification)
    }

    private fun generateNotificationBuilder(): NotificationCompat.Builder {
        val notifyIntent = Intent(this, WhiteNoiseActivity::class.java).apply {
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

    override fun isPlaying(): Boolean {
        return mIsPlaying
    }

    override fun getActiveSounds(): MutableList<Pair<Sound, LoopMediaPlayer>> {
        return mActiveSounds
    }

    override fun togglePlayPause(): Boolean {
        if (mIsPlaying) {
            pauseAllActiveSounds()
        } else {
            startAllActiveSounds()
        }
        mIsPlaying = !mIsPlaying
        updatePlayToggleAction()
        updateNotification()
        updateSoundControlFragment()
        return mIsPlaying
    }

    override fun updateSoundControlFragment() {
        val curFrag =
            supportFragmentManager.findFragmentById(R.id.container) as SoundControlFragment
        val newFrag = SoundControlFragment()
        newFrag.arguments = Bundle().apply {
            curFrag.getTimeRemaining()?.let { putLong(curFrag.TIME_REMAINING_KEY, it) }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, newFrag)
            .commitNow()
    }

    override fun pauseAllSounds() {
        pauseAllActiveSounds()
        mIsPlaying = false
        updateSoundControlFragment()
    }

    override fun onTimerUpdate(remainingTimeMs: Long) {
        notificationBuilder.setContentText(
            String.format(
                "%02d:%02d:%02d", // HH:MM:SS
                TimeUnit.MILLISECONDS.toHours(remainingTimeMs), // hours
                TimeUnit.MILLISECONDS.toMinutes(remainingTimeMs) - // minutes
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(remainingTimeMs)),
                TimeUnit.MILLISECONDS.toSeconds(remainingTimeMs) -
                        TimeUnit.MINUTES.toSeconds(
                            TimeUnit.MILLISECONDS.toMinutes(
                                remainingTimeMs
                            )
                        )
            )
        )
        updateNotification()
    }

    override fun onTimerCancel() {
        notificationBuilder.setContentText(null)
        updateNotification()
    }

    override fun onTimerFinish() {
        notificationBuilder.setContentText("Timer finished.")
        pauseAllSounds()
        updateNotification()
    }

    override fun onDestroy() {
        mNotificationManagerCompat.cancelAll()
        super.onDestroy()
    }
}