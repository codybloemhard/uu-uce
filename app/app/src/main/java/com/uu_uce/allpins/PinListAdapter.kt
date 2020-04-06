package com.uu_uce.allpins

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.R
import com.uu_uce.database.PinConversion
import com.uu_uce.database.PinData
import com.uu_uce.database.PinViewModel

class PinListAdapter internal constructor(
    private val activity: Activity
) : RecyclerView.Adapter<PinListAdapter.PinViewHolder>() {

    private val resource = activity.resources
    private val inflater: LayoutInflater = LayoutInflater.from(activity)
    private var pins = emptyList<PinData>()
    private val pinViewModel: PinViewModel = ViewModelProvider(activity as ViewModelStoreOwner).get(PinViewModel::class.java)
    var activePopup: PopupWindow? = null

    inner class PinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val parentView : View = itemView
        val pinTitle: TextView = itemView.findViewById(R.id.textView)
        val pinCoord: TextView = itemView.findViewById(R.id.textView2)
        val pinType: ImageView = itemView.findViewById(R.id.type_image)
        val pinDiff: TextView = itemView.findViewById(R.id.textView4)
        val pinDiffC: View = itemView.findViewById(R.id.diff)
        val pinStatus: CheckBox = itemView.findViewById(R.id.checkBox)
        val pinButton: Button = itemView.findViewById(R.id.open_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinViewHolder {
        val itemView = inflater.inflate(R.layout.recyclerview_item, parent, false)
        return PinViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PinViewHolder, position: Int) {
        val current = pins[position]
        holder.pinTitle.text = current.title
        holder.pinCoord.text = current.location
        holder.pinStatus.isChecked = (current.status == 2)
        holder.pinButton.setOnClickListener{
            val pinConverter = PinConversion(activity)
            val pin = pinConverter.pinDataToPin(current, pinViewModel)
            pin.getContent().parent = pin
            pin.openPinPopupWindow(holder.parentView, activity) {activePopup = null}
            activePopup = pin.popupWindow
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

        when(current.type){
            "TEXT" -> holder.pinType.setImageDrawable(
                ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_text_black, null) ?: error ("Image not found"))
            "IMAGE" -> holder.pinType.setImageDrawable(
            ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_image_black, null) ?: error ("Image not found"))
            "VIDEO" -> holder.pinType.setImageDrawable(
            ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_video_black, null) ?: error ("Image not found"))
            "MCQUIZ" -> holder.pinType.setImageDrawable(
                ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quest_black, null) ?: error ("Image not found"))
        }
    }

    internal fun setPins(newPinData: List<PinData>, viewModel: PinViewModel) {
        val tempPins : MutableList<PinData> = mutableListOf()
        // Update pins from new data
        for(newPin in newPinData) {
            if(newPin.status > 0){
                // Pin is not unlocked yet
                tempPins.add(newPin)
            }
            else if (newPin.status == -1) {
                // Pin needs recalculation
                val predecessorIds = newPin.predecessorIds.split(',').map{id -> id.toInt()}
                if (predecessorIds[0] != -1) {
                    viewModel.tryUnlock(newPin.pinId, predecessorIds) {}
                }
            }
        }
        pins = tempPins
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return pins.size
    }
}