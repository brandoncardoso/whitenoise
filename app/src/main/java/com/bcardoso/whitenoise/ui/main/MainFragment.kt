package com.bcardoso.whitenoise.ui.main

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.size
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcardoso.whitenoise.ActiveSoundAdapter
import com.bcardoso.whitenoise.R

class MainFragment : Fragment() {
    private val mActiveSounds = arrayListOf<String>("rain", "beach", "fire", "wind")
    private lateinit var mActiveSoundList : RecyclerView

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.main_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActiveSoundList = view.findViewById<RecyclerView>(R.id.active_sound_list)
        mActiveSoundList.layoutManager = LinearLayoutManager(context)
        val mActiveSoundAdapter = ActiveSoundAdapter(mActiveSounds)
        mActiveSoundList.adapter = mActiveSoundAdapter

        val addSoundButton = view.findViewById<Button>(R.id.add_sound_button)
        addSoundButton.setOnClickListener{ mActiveSoundAdapter.addSound("thunder") }
    }

    companion object {
        fun newInstance() = MainFragment()
    }
}