package com.uu_uce.fieldbook

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color.rgb
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toFile
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.R
import com.uu_uce.pins.ImageContentBlock
import com.uu_uce.pins.PinContent
import com.uu_uce.pins.TextContentBlock
import com.uu_uce.pins.VideoContentBlock

class FieldbookAdapter(val activity: Activity, private val viewModel: FieldbookViewModel) : RecyclerView.Adapter<FieldbookAdapter.FieldbookViewHolder>() {

    private var fieldbook: List<FieldbookEntry> = emptyList()

    class FieldbookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val parentView = itemView
        val numberFb: TextView = parentView.findViewById(R.id.title)
        val locationFb: TextView = parentView.findViewById(R.id.pin_coordinates)
        val datetimeFb: TextView = parentView.findViewById(R.id.datetime)
        val frameFb: FrameLayout = parentView.findViewById(R.id.frame_layout)
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
        //holder.numberFb.text = addLeadingZeros(entry.id)
        holder.numberFb.text = entry.title
        holder.locationFb.text = entry.location
        holder.datetimeFb.text = entry.dateTime

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        val content = PinContent(entry.content)

        var uri: Uri = Uri.EMPTY

        var isThumbnail = false

        //val textFb: TextView = parentView.findViewById(R.id.text_preview)
        //val imageFb: ImageView = parentView.findViewById(R.id.image_preview)

        //1x alles doorlopen, niet meer checken op dingen die we al gehad hebben...
        //als er een textblock is, gebruiken we die...
        //anders gebruiken we een afbeelding (sws thumbnail)
        //anders gebruiken we een video thumbnail

        loop@ for (cB in content.contentBlocks) {
            when (cB) {
                is TextContentBlock -> {
                    TextView(activity).apply {
                        textSize = 12f
                        setTextColor(rgb(100, 100, 100))
                        text = cB.getTextContent()
                        //Text direction?
                        //What to do when text goes over the edge?
                    }.also {
                        holder.frameFb.addView(it, params)
                    }
                    break@loop
                }
                else -> {
                    if (!isThumbnail) {
                        if (cB is ImageContentBlock)
                            uri = cB.getThumbnailURI()
                        else if (cB is VideoContentBlock)
                            uri = cB.getThumbnailURI()
                        ImageView(activity).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            setImageURI(uri)
                        }.also {
                            holder.frameFb.addView(it, params)
                        }
                        isThumbnail = true
                    }
                }
            }
        }

        holder.parentView.setOnClickListener (
            View.OnClickListener(
                fun (v) {
                    val layoutInflater = activity.layoutInflater

                    // Build an custom view (to be inflated on top of our current view & build it's popup window)
                    val customView = layoutInflater.inflate(R.layout.pin_content_view, null, false)

                    val popupWindow = PopupWindow(
                        customView,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Add the title for the popup window
                    val windowTitle = customView.findViewById<TextView>(R.id.popup_window_title)
                    windowTitle.text = entry.title

                    // Add content to popup window
                    val layout: LinearLayout = customView.findViewById(R.id.scrollLayout)

                    // Fill layout of popup
                    for(i in 0 until content.contentBlocks.count()) {
                        content.contentBlocks[i].generateContent(i, layout, activity, customView, null)
                    }

                    // Open popup
                    popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0)

                    // Get elements
                    val btnClosePopupWindow =
                        customView.findViewById<Button>(R.id.popup_window_close_button)

                    // Set onClickListeners
                    btnClosePopupWindow.setOnClickListener {
                        popupWindow.dismiss()
                    }
                }
            )
        )

        holder.parentView.setOnLongClickListener(
            View.OnLongClickListener(
                fun (_): Boolean {
                    println(uri)
                    AlertDialog.Builder(activity)
                        .setTitle("Delete")
                        .setMessage("Are you sure you want to delete this entry?")
                        .setPositiveButton("YES") { _: DialogInterface, _: Int ->
                            viewModel.delete(entry)
                            if (uri != Uri.EMPTY) uri.toFile().delete()
                        }
                        .setNegativeButton("NO") { _: DialogInterface, _: Int ->

                        }
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
}