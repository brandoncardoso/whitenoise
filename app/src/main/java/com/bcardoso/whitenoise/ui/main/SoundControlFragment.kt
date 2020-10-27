package com.bcardoso.whitenoise.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcardoso.whitenoise.ActiveSoundAdapter
import com.bcardoso.whitenoise.R
import com.bcardoso.whitenoise.SoundControlInterface
import com.google.android.material.floatingactionbutton.FloatingActionButton

data class Sound(var name:String, var id: Int, var volume:Float = 0F)

class SoundControlFragment : Fragment() {
    private lateinit var mContext: Context

    private lateinit var mPlayButton : FloatingActionButton
    private lateinit var mActiveSoundListView: RecyclerView
    private lateinit var mActiveSoundAdapter: ActiveSoundAdapter

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
        //val addSoundButton = view.findViewById<ActionMenuItemView>(R.id.add_sound_button)
        //addSoundButton.setOnClickListener{ openAddSoundDialog(view.context) }
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