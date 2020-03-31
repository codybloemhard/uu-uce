package com.uu_uce.fieldbook

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.R
import com.uu_uce.database.PinData
import com.uu_uce.pins.*

class FieldbookAdapter(val activity: Activity) : RecyclerView.Adapter<FieldbookAdapter.FieldbookViewHolder>() {

    private lateinit var fieldbook: MutableList<FieldbookEntry>

    class FieldbookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val parentView = itemView
        val numberFb = parentView.findViewById<TextView>(R.id.no)
        val locationFb = parentView.findViewById<TextView>(R.id.pin_coordinates)
        val datetimeFb = parentView.findViewById<TextView>(R.id.datetime)
        val textFb = parentView.findViewById<TextView>(R.id.text_preview)
        val imageFb = parentView.findViewById<ImageView>(R.id.image_preview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldbookViewHolder {
        val itemView = activity.layoutInflater.inflate(R.layout.fieldbook_recyclerview_item, parent, false)
        return FieldbookViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return fieldbook.size
    }

    override fun onBindViewHolder(holder: FieldbookViewHolder, position: Int) {
        val entry : FieldbookEntry = fieldbook[position]
        holder.numberFb.text = addLeadingZeros(entry.id)
        holder.locationFb.text = entry.location
        holder.datetimeFb.text = entry.dateTime

        val content = PinContent(entry.content)

        for (cB in content.contentBlocks)
        {
            if (cB is TextContentBlock) {
                holder.textFb.text = cB.textContent
                break
            }
        }
        for (cB in content.contentBlocks)
        {
            if (cB is ImageContentBlock) {
                holder.imageFb.setImageURI(cB.imageURI)
                break
            }
            if (cB is VideoContentBlock) {
                holder.imageFb.setImageURI(cB.thumbnailURI)
                break
            }
        }

        holder.parentView.setOnClickListener {
            //TODO: this isn't the correct parentView
            openPinPopupWindow(entry.location,content,holder.parentView,activity)
        }
    }

    fun setFieldbook(fieldbook: MutableList<FieldbookEntry>) {
        this.fieldbook = fieldbook
        notifyDataSetChanged()
    }
}

fun addLeadingZeros(id: Int) : String {
    val s = id.toString()
    val zero : String = "0".repeat(3-s.length)
    return "#$zero$id"
}