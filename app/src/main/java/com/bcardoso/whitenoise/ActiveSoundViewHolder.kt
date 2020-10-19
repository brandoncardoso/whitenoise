package com.bcardoso.whitenoise

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActiveSoundViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.active_sound_list_item, parent, false)) {
    private var mNameView : TextView? = null

    init {
        mNameView = itemView.findViewById(R.id.active_sound_list_item_name)
    }

    fun bind(name: String) {
        mNameView?.text = name
    }
}