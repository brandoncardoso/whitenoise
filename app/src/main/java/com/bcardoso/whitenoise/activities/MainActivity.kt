package com.bcardoso.whitenoise.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bcardoso.whitenoise.R
import com.bcardoso.whitenoise.interfaces.SoundControlInterface
import com.bcardoso.whitenoise.interfaces.SoundServiceCallbacks
import com.bcardoso.whitenoise.services.SoundService
import com.bcardoso.whitenoise.viewmodels.MainViewModel
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), SoundControlInterface, SoundServiceCallbacks {
    private lateinit var soundService: SoundService
    private var isBound: Boolean = false

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as SoundService.LocalBinder
            soundService = binder.getService()
            isBound = true
            viewModel.setActiveSounds(soundService.getActiveSounds())
            soundService.setCallbacks(this@MainActivity)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    private val viewModel: MainViewModel by viewModels()

    private lateinit var topAppBar: androidx.appcompat.widget.Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val intent = Intent(this, SoundService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        viewModel.isPlaying.observe(this, ::onTogglePlayPauseFromFragment)
        viewModel.sleepTimerTimeRemaining.observe(this, ::onSleepTimerUpdate)
        viewModel.isSleepTimerFinished.observe(this, ::onSleepTimerFinished)

        topAppBar = findViewById(R.id.topAppBar)
        setSupportActionBar(findViewById(R.id.topAppBar))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

    private fun onTogglePlayPauseFromFragment(isPlaying: Boolean) {
        if (isBound) {
            if (isPlaying != soundService.isPlaying()) {
                soundService.togglePlayPause()
            }
        }
    }

    override fun onTogglePlayPause(isPlaying: Boolean){
        if (isPlaying != viewModel.isPlaying.value) {
            viewModel.setIsPlaying(isPlaying)
        }
    }

    override fun onCancelSleepTimer() {
        soundService.setNotificationContentText()
    }

    private fun onSleepTimerUpdate(remainingTimeMs: Long) {
        val timerString = String.format(
            "%02d:%02d:%02d", // HH:MM:SS
            TimeUnit.MILLISECONDS.toHours(remainingTimeMs), // hours
            TimeUnit.MILLISECONDS.toMinutes(remainingTimeMs) - // minutes
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(remainingTimeMs)),
            TimeUnit.MILLISECONDS.toSeconds(remainingTimeMs) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(remainingTimeMs)))

        soundService.setNotificationContentText(timerString)
    }

    private fun onSleepTimerFinished(isFinished: Boolean) {
        if (isBound && isFinished) {
            soundService.pause()
            soundService.setNotificationContentText("Timer finished.")
        }
    }

    override fun onDestroy() {
        unbindService(connection)
        super.onDestroy()
    }
}