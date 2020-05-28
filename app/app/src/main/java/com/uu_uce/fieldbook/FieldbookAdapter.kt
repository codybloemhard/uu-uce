package com.uu_uce.fieldbook

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color.rgb
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toFile
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.R
import com.uu_uce.openFieldbookPopup
import com.uu_uce.pins.ImageContentBlock
import com.uu_uce.pins.PinContent
import com.uu_uce.pins.TextContentBlock
import com.uu_uce.pins.VideoContentBlock

class FieldbookAdapter(
    private val activity    : Activity,
    private val viewModel   : FieldbookViewModel,
    private val rootView    : View
)
    : RecyclerView.Adapter<FieldbookAdapter.FieldbookViewHolder>()
{
    private var fieldbook: List<FieldbookEntry> = emptyList()
    private lateinit var parentView : ViewGroup

    class FieldbookViewHolder(val parentView: View) : RecyclerView.ViewHolder(parentView) {
        val titleFb     : TextView      = itemView.findViewById(R.id.title)
        val numberFb    : TextView      = itemView.findViewById(R.id.number)
        val locationFb  : TextView      = itemView.findViewById(R.id.pin_coordinates)
        val datetimeFb  : TextView      = itemView.findViewById(R.id.datetime)
        val frameFb     : FrameLayout   = itemView.findViewById(R.id.frame_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldbookViewHolder {
        val itemView = activity.layoutInflater.inflate(R.layout.fieldbook_recyclerview_item, parent, false)
        parentView = parent
        return FieldbookViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return fieldbook.size
    }

    override fun onBindViewHolder(holder: FieldbookViewHolder, position: Int) {
        holder.frameFb.removeAllViews()
        val entry : FieldbookEntry = fieldbook[position]
        val index = entry.id
        holder.apply {
            titleFb.text = entry.title
            numberFb.text = addLeadingZeros(index)
            locationFb.text = entry.location
            datetimeFb.text = entry.dateTime
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        val content = PinContent(entry.content, activity).contentBlocks

        var isThumbnail = false
        var thumbnailUri = Uri.EMPTY

        loop@ for (cB in content) {
            when (cB) {
                is TextContentBlock -> {
                    TextView(activity).apply {
                        textSize = 12f
                        setTextColor(rgb(100, 100, 100))
                        text = cB.getTextContent()
                    }.also {
                        holder.frameFb.addView(it, params)
                    }
                    break@loop
                }
                else -> {
                    if (!isThumbnail) {
                        if (cB is ImageContentBlock)
                            thumbnailUri = cB.getThumbnailURI()
                        else if (cB is VideoContentBlock)
                            thumbnailUri = cB.getThumbnailURI()
                        isThumbnail = true
                    }
                }
            }
        }

        if (holder.frameFb.childCount == 0)
            ImageView(activity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(thumbnailUri)
            }.also {
                holder.frameFb.addView(it, params)
            }

        holder.parentView.setOnClickListener {
            openFieldbookPopup(
                activity,
                rootView,
                entry,
                content
            )
        }

        holder.parentView.setOnLongClickListener(
            View.OnLongClickListener(
                fun (_): Boolean {
                    AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.delete_popup_title))
                        .setMessage(activity.getString(R.string.fieldbook_pindeletion_popup_text))
                        .setPositiveButton(activity.getString(R.string.positive_button_text)) { _: DialogInterface, _: Int ->
                            viewModel.delete(entry)
                            for (cB in content) {
                                when (cB) {
                                    is ImageContentBlock ->
                                        cB.getThumbnailURI().apply {
                                            if (this!=Uri.EMPTY)
                                                toFile().delete()
                                        }
                                    is VideoContentBlock ->
                                        cB.getThumbnailURI().apply {
                                            if (this!=Uri.EMPTY)
                                                toFile().delete()
                                        }
                                }
                            }
                        }
                        .setNegativeButton(activity.getString(R.string.negative_button_text)) { _: DialogInterface, _: Int -> }
                        .show()
                    return true
                }
            )
        )
    }

    fun setFieldbook(fieldbook: List<FieldbookEntry>) {
        this.fieldbook = fieldbook
        notifyDataSetChanged()
    }

    private fun addLeadingZeros(id: Int) : String {
        val s = id.toString()
        val zero : String = "0".repeat(3-s.length)
        return "#$zero$id"
    }
}