package com.bcardoso.whitenoise.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcardoso.whitenoise.R
import com.bcardoso.whitenoise.adapters.ActiveSoundAdapter
import com.bcardoso.whitenoise.databinding.TimerDialogBinding
import com.bcardoso.whitenoise.interfaces.SoundControlInterface
import com.bcardoso.whitenoise.utils.TimerDialogTime
import com.bcardoso.whitenoise.viewmodels.MainViewModel
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.TimeUnit

class SoundControlFragment : Fragment() {
    private lateinit var mContext: Context

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var mActiveSoundListView: RecyclerView
    private lateinit var mActiveSoundAdapter: ActiveSoundAdapter

    private lateinit var mListener: SoundControlInterface

    private lateinit var bottomAppBar: BottomAppBar
    private lateinit var cancelTimerButton: MenuItem
    private lateinit var timeRemainingText: MenuItem
    private lateinit var mPlayButton: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sound_control_fragment, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        if (context is SoundControlInterface) mListener = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.isPlaying.observe(viewLifecycleOwner, ::updatePlayButtonImage)
        viewModel.sleepTimerTimeRemaining.observe(viewLifecycleOwner, ::updateTimerText)
        viewModel.isSleepTimerFinished.observe(viewLifecycleOwner, ::onSleepTimerFinished)

        mActiveSoundListView = view.findViewById(R.id.active_sound_list)
        mActiveSoundListView.layoutManager = LinearLayoutManager(mContext)
        mActiveSoundAdapter = ActiveSoundAdapter(mListener.getActiveSounds())
        mActiveSoundListView.adapter = mActiveSoundAdapter

        bottomAppBar = view.findViewById(R.id.bottomAppBar)
        cancelTimerButton = bottomAppBar.menu.findItem(R.id.mi_cancel_timer)
        timeRemainingText = bottomAppBar.menu.findItem(R.id.mi_time_remaining)

        mPlayButton = view.findViewById(R.id.play_button)
        mPlayButton.setOnClickListener {
            viewModel.togglePlaying()
        }

        bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.mi_set_timer -> {
                    openSetTimerDialog()
                    true
                }
                R.id.mi_cancel_timer -> {
                    viewModel.cancelSleepTimer()
                    mListener.onCancelSleepTimer()
                    timeRemainingText.isVisible = false
                    cancelTimerButton.isVisible = false
                    true
                }
                R.id.mi_time_remaining -> true
                else                   -> false
            }
        }
        //val addSoundButton = view.findViewById<ActionMenuItemView>(R.id.add_sound_button)
        //addSoundButton.setOnClickListener{ openAddSoundDialog(view.context) }
    }

    private fun updateTimerText(remainingTimeMillis: Long?) {
        remainingTimeMillis?.let {
            val timerString = String.format(
                "%02d:%02d:%02d", // HH:MM:SS
                TimeUnit.MILLISECONDS.toHours(it), // hours
                TimeUnit.MILLISECONDS.toMinutes(it) - // minutes
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(it)),
                TimeUnit.MILLISECONDS.toSeconds(it) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(it)))

            timeRemainingText.title = timerString
        }
    }

    private fun onSleepTimerFinished(isFinished: Boolean) {
        if (isFinished) {
            timeRemainingText.isVisible = false
            cancelTimerButton.isVisible = false
        }
    }

    private fun openSetTimerDialog() {
        val binding = DataBindingUtil.inflate<TimerDialogBinding>(
            LayoutInflater.from(context),
            R.layout.timer_dialog,
            null,
            false
        )
        binding.lifecycleOwner = this
        binding.time = TimerDialogTime(0, 0)

        context?.let { context ->
            AlertDialog.Builder(context)
                .setView(binding.root)
                .setCancelable(true)
                .setPositiveButton("Start") { dialog, _ ->
                    binding.time?.let { time ->
                        viewModel.setSleepTimer(time.getMillis())
                        viewModel.startSleepTimer()
                        timeRemainingText.isVisible = true
                        cancelTimerButton.isVisible = true
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                .setTitle("Stop in...")
                .create()
                .show()
        }
    }

    private fun updatePlayButtonImage(isPlaying: Boolean) {
        if (isPlaying) {
            mPlayButton.setImageResource(R.drawable.ic_baseline_pause_24)
        } else {
            mPlayButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        }
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