package com.bcardoso.whitenoise.ui.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.core.view.size
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcardoso.whitenoise.ActiveSoundAdapter
import com.bcardoso.whitenoise.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainFragment : Fragment() {
    private val mActiveSounds = arrayListOf<String>("rain", "beach", "fire", "wind")
    private lateinit var mActiveSoundList : RecyclerView
    private lateinit var mActiveSoundAdapter: ActiveSoundAdapter

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.main_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActiveSoundList = view.findViewById<RecyclerView>(R.id.active_sound_list)
        mActiveSoundList.layoutManager = LinearLayoutManager(context)
        mActiveSoundAdapter = ActiveSoundAdapter(mActiveSounds)
        mActiveSoundList.adapter = mActiveSoundAdapter

        val addSoundButton =
            view.findViewById<ActionMenuItemView>(R.id.add_sound_button)
        addSoundButton.setOnClickListener{ openAddSoundDialog(view.context) }
    }

    private fun openAddSoundDialog(context : Context) {
        val multiItems = arrayOf("thunder", "waves", "forest")
        val checkedItems = booleanArrayOf(true, false, false)

        MaterialAlertDialogBuilder(context)
            .setNeutralButton(resources.getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(resources.getString(R.string.add)) { dialog, _ ->
                checkedItems.forEachIndexed { index, checked ->
                    if (checked) {
                        mActiveSoundAdapter.addSound(multiItems[index])
                    }
                }
            }
            .setMultiChoiceItems(multiItems, checkedItems) { _, _, _ -> }
            .show()
    }
}