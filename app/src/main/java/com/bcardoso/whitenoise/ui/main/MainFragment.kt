package com.bcardoso.whitenoise.ui.main

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcardoso.whitenoise.ActiveSoundAdapter
import com.bcardoso.whitenoise.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

data class Sound(var name:String, var id: Int, var volume:Float = 0F)

class MainFragment : Fragment() {
    private lateinit var mActiveSoundListView: RecyclerView
    private lateinit var mActiveSoundAdapter: ActiveSoundAdapter

    private val mSounds = listOf<Sound>(
            Sound("Rain", R.raw.rain, .78F),
            Sound("Thunder", R.raw.thunder, 0.1F)
    )
    private val mActiveSounds = mutableListOf<Pair<Sound, MediaPlayer>>()

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.main_fragment, container, false)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        mSounds.forEach { sound ->
            var mediaPlayer = MediaPlayer.create(context, sound.id, audioAttributes, 1)
            mediaPlayer.setAudioAttributes(audioAttributes)
            mediaPlayer.setVolume(sound.volume, sound.volume)
            mediaPlayer.isLooping = true
            mActiveSounds.add(Pair(sound, mediaPlayer))
        }

        mActiveSoundListView = view.findViewById<RecyclerView>(R.id.active_sound_list)
        mActiveSoundListView.layoutManager = LinearLayoutManager(context)
        mActiveSoundAdapter = ActiveSoundAdapter(mActiveSounds)
        mActiveSoundListView.adapter = mActiveSoundAdapter

        var isPlaying = false
        val playButton = view.findViewById<FloatingActionButton>(R.id.play_button)
        playButton.setOnClickListener {
            if (isPlaying) {
                playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                mActiveSounds.forEach { activeSound -> activeSound.second.pause() }
            } else {
                playButton.setImageResource(R.drawable.ic_baseline_pause_24)
                mActiveSounds.forEach { activeSound -> activeSound.second.start() }
            }
            isPlaying = !isPlaying
        }
        //val addSoundButton = view.findViewById<ActionMenuItemView>(R.id.add_sound_button)
        //addSoundButton.setOnClickListener{ openAddSoundDialog(view.context) }
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