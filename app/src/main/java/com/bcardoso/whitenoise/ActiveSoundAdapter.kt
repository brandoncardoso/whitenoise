package com.bcardoso.whitenoise

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView

class ActiveSoundAdapter(private var mSounds: ArrayList<String>) :
        RecyclerView.Adapter<ActiveSoundViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveSoundViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ActiveSoundViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: ActiveSoundViewHolder, position: Int) {
        holder.bind(mSounds[position])

        holder.itemView.findViewById<Button>(R.id.active_sound_remove_button)
                .setOnClickListener { removeSound(holder) }
    }

    override fun getItemCount() = mSounds.size

    public fun addSound(name : String) {
        mSounds.add(name)
        notifyItemInserted(mSounds.size)
    }

    private fun removeSound(holder : ActiveSoundViewHolder) {
        if (holder.adapterPosition >= 0) {
            mSounds.removeAt(holder.adapterPosition)
            notifyItemRemoved(holder.adapterPosition)
            notifyItemRangeChanged(holder.adapterPosition, mSounds.size)
        }
    }
}