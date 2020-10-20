package com.bcardoso.whitenoise

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.bcardoso.whitenoise.ui.main.Sound

class ActiveSoundAdapter(private var mActiveSounds: MutableList<Sound>) :
        RecyclerView.Adapter<ActiveSoundViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveSoundViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ActiveSoundViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: ActiveSoundViewHolder, position: Int) {
        holder.bind(mActiveSounds[position].name)

        holder.itemView.findViewById<Button>(R.id.active_sound_remove_button)
                .setOnClickListener { removeSound(holder) }
    }

    override fun getItemCount() = mActiveSounds.size

    public fun addSound(sound : Sound) {
        mActiveSounds.add(sound)
        notifyItemInserted(mActiveSounds.size)
    }

    private fun removeSound(holder : ActiveSoundViewHolder) {
        if (holder.adapterPosition >= 0) {
            mActiveSounds.removeAt(holder.adapterPosition)
            notifyItemRemoved(holder.adapterPosition)
            notifyItemRangeChanged(holder.adapterPosition, itemCount)
        }
    }
}