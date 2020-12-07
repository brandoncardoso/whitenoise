package com.bcardoso.whitenoise.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bcardoso.whitenoise.utils.LoopMediaPlayer
import com.bcardoso.whitenoise.utils.Sound

class ActiveSoundAdapter() : RecyclerView.Adapter<ActiveSoundViewHolder>() {
    private var mActiveSounds: MutableList<Pair<Sound, LoopMediaPlayer>> = mutableListOf()

    constructor(activeSoundList: MutableList<Pair<Sound, LoopMediaPlayer>>) : this() {
        mActiveSounds = activeSoundList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveSoundViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ActiveSoundViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: ActiveSoundViewHolder, position: Int) {
        holder.bind(mActiveSounds[position])

        //holder.itemView.findViewById<Button>(R.id.active_sound_remove_button).setOnClickListener { removeSound(holder) }
    }

    override fun getItemCount() = mActiveSounds.size

    fun updateList(data: MutableList<Pair<Sound, LoopMediaPlayer>>) {
        mActiveSounds = data
        notifyDataSetChanged()
    }
    /*
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
    */
}