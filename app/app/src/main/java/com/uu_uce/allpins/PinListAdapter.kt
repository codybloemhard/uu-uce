package com.uu_uce.allpins

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.R
import com.uu_uce.database.PinData
import com.uu_uce.pins.PinContent
import com.uu_uce.pins.openPinPopupWindow

class PinListAdapter internal constructor(
    private val activity: Activity
) : RecyclerView.Adapter<PinListAdapter.PinViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(activity)
    private var pins = emptyList<PinData>()

    inner class PinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val parentView : View = itemView
        val pinTitle: TextView = itemView.findViewById(R.id.textView)
        val pinCoord: TextView = itemView.findViewById(R.id.textView2)
        val pinType: TextView = itemView.findViewById(R.id.textView3)
        val pinDiff: TextView = itemView.findViewById(R.id.textView4)
        val pinDiffC: View = itemView.findViewById(R.id.diff)
        val pinButton: Button = itemView.findViewById(R.id.open_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinViewHolder {
        val itemView = inflater.inflate(R.layout.allpins_recyclerview_item, parent, false)
        return PinViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PinViewHolder, position: Int) {
        val current = pins[position]
        holder.pinTitle.text = current.title
        holder.pinCoord.text = current.location
        holder.pinType.text = current.type
        holder.pinButton.setOnClickListener{
            openPinPopupWindow(current.title, PinContent(current.content), holder.parentView, activity)
        }

        when(current.difficulty){
            1 -> {
                holder.pinDiff.text = activity.getString(R.string.easy)
                holder.pinDiffC.setBackgroundColor(Color.parseColor("#00B222"))
            }
            2 -> {
                holder.pinDiff.text = activity.getString(R.string.medium)
                holder.pinDiffC.setBackgroundColor(Color.parseColor("#FF862F"))
            }
            3 -> {
                holder.pinDiff.text = activity.getString(R.string.hard)
                holder.pinDiffC.setBackgroundColor(Color.parseColor("#EC1A3D"))
            }
            else -> {
                holder.pinDiff.text = activity.getString(R.string.unknown)
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