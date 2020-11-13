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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import com.bcardoso.whitenoise.R
import com.bcardoso.whitenoise.databinding.TimerDialogBinding
import com.bcardoso.whitenoise.fragments.SoundControlFragment
import com.bcardoso.whitenoise.interfaces.SoundControlInterface
import com.bcardoso.whitenoise.utils.LoopMediaPlayer
import com.bcardoso.whitenoise.utils.TimerDialogTime
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.TimeUnit

data class Sound(var name: String, var id: Int, var initialVolume: Float = 0F) {
    var volume = initialVolume
}

class MainActivity : AppCompatActivity(), SoundControlInterface {
    private lateinit var volumePrefs: SharedPreferences

    private val NOTIFICATION_CHANNEL_ID = "whitenoise"
    private val NOTIFICATION_ID = 0
    private lateinit var mNotificationManagerCompat: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private lateinit var mPlayButton: FloatingActionButton
    private lateinit var timeRemainingText: MenuItem
    private lateinit var cancelTimerButton: MenuItem
    private lateinit var countDownTimer: CountDownTimer

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
        setContentView(R.layout.white_noise_activity)
        setSupportActionBar(findViewById(R.id.bottomAppBar))

        volumePrefs = getSharedPreferences("volumes", Context.MODE_PRIVATE)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, SoundControlFragment.newInstance())
                .commitNow()
        }

        mPlayButton = findViewById(R.id.play_button)
        updatePlayButtonImage()
        mPlayButton.setOnClickListener {
            togglePlayPause()
            updatePlayButtonImage()
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

        mNotificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        createNotificationChannel()
        notificationBuilder = generateNotificationBuilder()
        updatePlayToggleAction()
        updateNotification()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.bottom_app_bar, menu)
        cancelTimerButton = menu.findItem(R.id.mi_cancel_timer)
        timeRemainingText = menu.findItem(R.id.mi_time_remaining)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_set_timer -> openSetTimerDialog()
            R.id.mi_cancel_timer -> cancelTimer()
            R.id.mi_time_remaining -> return true
            else                   -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun startAllActiveSounds() {
        mActiveSounds.forEach { (_, mp) -> mp.start() }
        mIsPlaying = true
    }

    private fun pauseAllActiveSounds() {
        mActiveSounds.forEach { (_, mp) -> mp.pause() }
        mIsPlaying = false
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

    private fun updatePlayButtonImage() {
        if (mIsPlaying) {
            mPlayButton.setImageResource(R.drawable.ic_baseline_pause_24)
        } else {
            mPlayButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        }
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

    fun togglePlayPause() {
        if (mIsPlaying) {
            pauseAllActiveSounds()
        } else {
            startAllActiveSounds()
        }
        updatePlayToggleAction()
        updateNotification()
    }

    private fun cancelTimer() {
        if (this::countDownTimer.isInitialized) countDownTimer.cancel()
        timeRemainingText.isVisible = false
        cancelTimerButton.isVisible = false
        notificationBuilder.setContentText(null)
        updateNotification()
    }

    private fun openSetTimerDialog() {
        val binding = DataBindingUtil.inflate<TimerDialogBinding>(
            LayoutInflater.from(this),
            R.layout.timer_dialog,
            null,
            false
        )
        binding.lifecycleOwner = this
        binding.time = TimerDialogTime(0, 0)

        AlertDialog.Builder(this)
            .setView(binding.root)
            .setCancelable(true)
            .setPositiveButton("Start") { dialog, _ ->
                binding.time?.let { setTimer(it.getMillis()) }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .setTitle("Stop in...")
            .create()
            .show()
    }

    private fun setTimer(timeInMillis: Long) {
        cancelTimer()
        timeRemainingText.isVisible = true
        cancelTimerButton.isVisible = true

        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(remainingTimeMs: Long) {
                timeRemainingText.title = String.format(
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
                onTimerUpdate(remainingTimeMs)
            }

            override fun onFinish() = onTimerFinish()
        }
        countDownTimer.start()
    }

    fun onTimerUpdate(remainingTimeMs: Long) {
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

    fun onTimerFinish() {
        timeRemainingText.isVisible = false
        cancelTimerButton.isVisible = false
        pauseAllActiveSounds()
        notificationBuilder.setContentText("Timer finished.")
        updateNotification()
    }

    override fun onDestroy() {
        mNotificationManagerCompat.cancelAll()
        super.onDestroy()
    }
}