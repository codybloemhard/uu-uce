package com.uu_uce.allpins

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.R
import com.uu_uce.database.PinData
import kotlinx.android.synthetic.main.recyclerview_item.view.*

class PinListAdapter internal constructor(
    context: Context
) : RecyclerView.Adapter<PinListAdapter.PinViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var pins = emptyList<PinData>()

    inner class PinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val pinItemView: TextView = itemView.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinViewHolder {
        val itemView = inflater.inflate(R.layout.recyclerview_item, parent, false)
        return PinViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PinViewHolder, position: Int) {
        val current = pins[position]
        holder.pinItemView.text = current.title
    }

    internal fun setPins(pins: List<PinData>) {
        this.pins = pins
        Log.i("test", pins.toString())
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int {
        return pins.size
    }
}