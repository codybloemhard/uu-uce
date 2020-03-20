package com.uu_uce.allpins

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.R
import com.uu_uce.database.PinData

class PinListAdapter internal constructor(
    context: Context
) : RecyclerView.Adapter<PinListAdapter.PinViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var pins = emptyList<PinData>()

    inner class PinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val pinTitle: TextView = itemView.findViewById(R.id.textView)
        val pinCoord: TextView = itemView.findViewById(R.id.textView2)
        val pinType: TextView = itemView.findViewById(R.id.textView3)
        val pinDiff: TextView = itemView.findViewById(R.id.textView4)
        val pinDiffC: View = itemView.findViewById(R.id.diff)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinViewHolder {
        val itemView = inflater.inflate(R.layout.recyclerview_item, parent, false)
        return PinViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PinViewHolder, position: Int) {
        val current = pins[position]
        holder.pinTitle.text = current.title
        holder.pinCoord.text = current.location
        holder.pinType.text = current.type
        when(current.difficulty){
            1 -> {
                holder.pinDiff.text = "Easy"
                holder.pinDiffC.setBackgroundColor(Color.parseColor("#00B222"))
            }
            2 -> {
                holder.pinDiff.text = "Medium"
                holder.pinDiffC.setBackgroundColor(Color.parseColor("#FF862F"))
            }
            3 -> {
                holder.pinDiff.text = "Hard"
                holder.pinDiffC.setBackgroundColor(Color.parseColor("#EC1A3D"))
            }
            else -> {
                holder.pinDiff.text = "Unknown"
                holder.pinDiffC.setBackgroundColor(Color.parseColor("#686868"))
            }
        }

    }

    internal fun setPins(pins: List<PinData>) {
        this.pins = pins
        notifyDataSetChanged()
    }



    override fun getItemCount(): Int {
        return pins.size
    }
}