package com.uu_uce.allpins

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import com.mikhaellopez.circleview.CircleView
import com.uu_uce.R
import com.uu_uce.pins.PinContent

class PinListAdapter internal constructor(
    private val activity: Activity
) : RecyclerView.Adapter<PinListAdapter.PinViewHolder>() {

    private val resource = activity.resources
    private val inflater: LayoutInflater = LayoutInflater.from(activity)
    private var pinDataList = emptyList<PinData>()
    private var pinCanComplete = emptyList<Boolean>()
    private val pinViewModel: PinViewModel = ViewModelProvider(activity as ViewModelStoreOwner).get(PinViewModel::class.java)
    var activePopup: PopupWindow? = null

    inner class PinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val parentView : View = itemView
        val fullView : LinearLayout = itemView.findViewById(R.id.recyclerview_item)
        val pinTitle: TextView = itemView.findViewById(R.id.allpins_recyclerview_item_title)
        val pinCoord: TextView = itemView.findViewById(R.id.pin_coordinates)
        val pinType: ImageView = itemView.findViewById(R.id.type_image)
        val pinDiffC: CircleView = itemView.findViewById(R.id.diff)
        val pinStatus: ImageView = itemView.findViewById(R.id.checkBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinViewHolder {
        val itemView = inflater.inflate(R.layout.allpins_recyclerview_item, parent, false)
        return PinViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PinViewHolder, position: Int) {
        val current = pinDataList[position]
        holder.pinTitle.text = current.title
        holder.pinCoord.text = current.location

        // Set completed marker
        if(pinCanComplete[position] && current.status == 2){
            holder.pinStatus.visibility = View.VISIBLE
        }
        else{
            holder.pinStatus.visibility = View.GONE
        }

        holder.fullView.setOnClickListener{
            val pinConverter = PinConversion(activity)
            val pin = pinConverter.pinDataToPin(current, pinViewModel)
            pin.getContent().parent = pin
            pin.openContent(holder.parentView, activity) {activePopup = null}
            activePopup = pin.popupWindow
        }

        when(current.difficulty){
            1       -> holder.pinDiffC.circleColor = Color.parseColor("#00B222")
            2       -> holder.pinDiffC.circleColor = Color.parseColor("#FF862F")
            3       -> holder.pinDiffC.circleColor = Color.parseColor("#EC1A3D")
            else    -> holder.pinDiffC.circleColor = Color.parseColor("#686868")
        }

        when(current.type){
            "TEXT" -> holder.pinType.setImageDrawable(
                ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_text, null) ?: error ("Image not found"))
            "IMAGE" -> holder.pinType.setImageDrawable(
            ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_image, null) ?: error ("Image not found"))
            "VIDEO" -> holder.pinType.setImageDrawable(
            ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_video, null) ?: error ("Image not found"))
            "MCQUIZ" -> holder.pinType.setImageDrawable(
                ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quiz, null) ?: error ("Image not found"))
        }
    }

    internal fun setPins(newPinData: List<PinData>, viewModel: PinViewModel) {
        val tempPins : MutableList<PinData> = mutableListOf()
        val tempCanComplete : MutableList<Boolean> = mutableListOf()
        // Update pins from new data
        for(newPin in newPinData) {
            if(newPin.status > 0){
                // Pin is not unlocked yet
                tempPins.add(newPin)
                tempCanComplete.add(PinContent(newPin.content,activity).canCompletePin)
            }
            else if (newPin.status == -1) {
                // Pin needs recalculation
                val predecessorIds = newPin.predecessorIds.split(',').map{id -> id.toInt()}
                if (predecessorIds[0] != -1) {
                    viewModel.tryUnlock(newPin.pinId, predecessorIds) {}
                }
            }
        }
        pinDataList = tempPins
        pinCanComplete = tempCanComplete
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return pinDataList.size
    }
}