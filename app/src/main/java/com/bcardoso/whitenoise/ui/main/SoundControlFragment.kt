package com.bcardoso.whitenoise.ui.main

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcardoso.whitenoise.ActiveSoundAdapter
import com.bcardoso.whitenoise.R
import com.bcardoso.whitenoise.SoundControlInterface
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.TimeUnit

class SoundControlFragment : Fragment() {
    private lateinit var mContext: Context

    private lateinit var mPlayButton : FloatingActionButton
    private lateinit var mActiveSoundListView: RecyclerView
    private lateinit var mActiveSoundAdapter: ActiveSoundAdapter

    private lateinit var setTimerButton : ActionMenuItemView
    private lateinit var timeRemainingText : ActionMenuItemView
    private lateinit var cancelTimerButton : ActionMenuItemView
    private lateinit var countDownTimer : CountDownTimer
    private var countDownTimeRemaining : Long? = null
    val TIME_REMAINING_KEY = "TIME_REMAINING"

    private lateinit var mListener: SoundControlInterface

    companion object {
        fun newInstance() = SoundControlFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.sound_control_fragment, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        if (context is SoundControlInterface) mListener = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActiveSoundListView = view.findViewById(R.id.active_sound_list)
        mActiveSoundListView.layoutManager = LinearLayoutManager(mContext)
        mActiveSoundAdapter = ActiveSoundAdapter(mListener.getActiveSounds())
        mActiveSoundListView.adapter = mActiveSoundAdapter

        mPlayButton = view.findViewById(R.id.play_button)
        updatePlayButtonImage(mListener.isPlaying())
        mPlayButton.setOnClickListener{
            val isPlaying = mListener.togglePlayPause()
            updatePlayButtonImage(isPlaying)
        }

        setTimerButton = view.findViewById(R.id.mi_set_timer)
        setTimerButton.setOnClickListener(::openSetTimeDialog)
        timeRemainingText = view.findViewById(R.id.mi_time_remaining)
        cancelTimerButton = view.findViewById(R.id.mi_cancel_timer)
        cancelTimerButton.setOnClickListener{ cancelTimer() }
        cancelTimer()
        val timeRemaining =  arguments?.getLong(TIME_REMAINING_KEY)
        timeRemaining?.let { if (it > 0) setTimer(it) }
        //val addSoundButton = view.findViewById<ActionMenuItemView>(R.id.add_sound_button)
        //addSoundButton.setOnClickListener{ openAddSoundDialog(view.context) }
    }

    fun getTimeRemaining() : Long? {
        return countDownTimeRemaining
    }

    private fun cancelTimer() {
        if (this::countDownTimer.isInitialized) countDownTimer.cancel()
        timeRemainingText.visibility = View.GONE
        cancelTimerButton.visibility = View.GONE
    }

    private fun openSetTimeDialog(view: View) {
        val dialogBuilder = AlertDialog.Builder(view.context)
        val dialogView = requireActivity().layoutInflater.inflate(R.layout.timer_dialog, null)

        var curHours = 0
        var curMinutes = 0
        val hourMin = 0
        val hourMax = 23
        val hourIncrement = 1
        val minuteIncrement = 5

        val dialog = dialogBuilder
            .setView(dialogView)
            .setCancelable(true)
            .setPositiveButton("Set") { dialog, _ ->
                val totalMillis = ((curHours * 60 * 60000) + (curMinutes * 60000)).toLong()
                setTimer(totalMillis)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .setTitle("Stop in...")
            .create()

        val updatePositiveButton = {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = !(curMinutes == 0 && curHours == 0)
        }

        val hourText = dialogView.findViewById<TextView>(R.id.tv_hour)
        val minuteText = dialogView.findViewById<TextView>(R.id.tv_minute)

        dialogView.findViewById<ImageButton>(R.id.btn_hour_increase)?.setOnClickListener {
            curHours += hourIncrement
            if (curHours > hourMax) {
                curHours %= hourMax
            }
            hourText?.text = String.format("%02d", curHours)
            updatePositiveButton()
        }

        dialogView.findViewById<ImageButton>(R.id.btn_hour_decrease)?.setOnClickListener {
            curHours -= hourIncrement
            if (curHours < hourMin) {
                curHours += hourMax + 1
            }
            hourText?.text = String.format("%02d", curHours)
            updatePositiveButton()
        }

        dialogView.findViewById<ImageButton>(R.id.btn_minute_increase)?.setOnClickListener {
            curMinutes += minuteIncrement
            if (curMinutes >= 60) {
                curMinutes %= 60
            }
            minuteText?.text = String.format("%02d", curMinutes)
            updatePositiveButton()
        }

        dialogView.findViewById<ImageButton>(R.id.btn_minute_decrease)?.setOnClickListener {
            curMinutes -= minuteIncrement
            if (curMinutes < 0 ) {
                curMinutes += 60
            }
            minuteText?.text = String.format("%02d", curMinutes)
            updatePositiveButton()
        }

        dialog.show()
        updatePositiveButton()
    }

    private fun setTimer(timeInMillis: Long) {
        cancelTimer()
        timeRemainingText.visibility = View.VISIBLE
        cancelTimerButton.visibility = View.VISIBLE

        countDownTimer = object:CountDownTimer(timeInMillis, 1000) {
            override fun onTick(remainingTimeMs: Long) {
                countDownTimeRemaining = remainingTimeMs
                timeRemainingText.text = String.format(
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
                mListener.onTimerUpdate(remainingTimeMs)
            }

            override fun onFinish() {
                mListener.onTimerFinish()
            }
        }
        countDownTimer.start()
    }

    private fun updatePlayButtonImage(isPlaying: Boolean) {
        if (isPlaying) {
            mPlayButton.setImageResource(R.drawable.ic_baseline_pause_24)
        } else {
            mPlayButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelTimer()
    }

    /*
    private fun openAddSoundDialog(context : Context) {
        val multiItems = arrayOf("thunder", "waves", "forest")
        val checkedItems = booleanArrayOf(true, false, false)

        MaterialAlertDialogBuilder(context)
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(resources.getString(R.string.add)) { _, _ ->
                checkedItems.forEachIndexed { index, checked ->
                    if (checked) {
                        mActiveSoundAdapter.addSound(multiItems[index])
                    }
                }
            }
            .setMultiChoiceItems(multiItems, checkedItems) { _, _, _ -> }
            .show()
    }*/
}