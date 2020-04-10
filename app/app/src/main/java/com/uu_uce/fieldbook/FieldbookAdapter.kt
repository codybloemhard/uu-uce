package com.uu_uce.fieldbook

import android.app.Activity
import android.content.DialogInterface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.R
import com.uu_uce.pins.*

class FieldbookAdapter(val activity: Activity, private val viewModel: FieldbookViewModel) : RecyclerView.Adapter<FieldbookAdapter.FieldbookViewHolder>() {

    private var fieldbook: MutableList<FieldbookEntry> = mutableListOf()

    class FieldbookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val parentView = itemView
        val numberFb: TextView? = parentView.findViewById(R.id.no)
        val locationFb: TextView? = parentView.findViewById(R.id.pin_coordinates)
        val datetimeFb: TextView? = parentView.findViewById(R.id.datetime)
        val textFb: TextView? = parentView.findViewById(R.id.text_preview)
        val imageFb: ImageView? = parentView.findViewById(R.id.image_preview)
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
        holder.numberFb?.text = addLeadingZeros(entry.id)
        holder.locationFb?.text = entry.location
        holder.datetimeFb?.text = entry.dateTime

        val content = PinContent(entry.content)

        var uri: Uri = Uri.EMPTY

        var displayingText = false
        var displayingImage = false

        for (cB in content.contentBlocks)
        {
            if (displayingText && displayingImage)
                break

            if (cB is TextContentBlock && !displayingText) {
                displayingText = true
                holder.textFb?.text = cB.getTextContent()
            } else if (!displayingImage) {
                displayingImage = true

                if (cB is ImageContentBlock) {
                    uri = cB.getImageURI()
                    holder.imageFb?.setImageURI(uri)
                }
                if (cB is VideoContentBlock) {
                    uri = cB.getThumbnailURI()
                    holder.imageFb?.setImageURI(uri)
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

                    customView.findViewById<CheckBox>(R.id.complete_box).isEnabled = false
                    customView.findViewById<CheckBox>(R.id.complete_box).isVisible = false
                    customView.findViewById<CheckBox>(R.id.complete_box).isClickable = false

                    // Add the title for the popup window
                    val windowTitle = customView.findViewById<TextView>(R.id.popup_window_title)
                    windowTitle.text = entry.location

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
                    AlertDialog.Builder(activity)
                        .setTitle("Delete")
                        .setMessage("Are you sure you want to delete this entry?")
                        .setPositiveButton("YES") { _: DialogInterface, _: Int ->
                            viewModel.delete(entry)
                            if(uri != Uri.EMPTY) uri.toFile().delete()
                        }
                        .setNegativeButton("NO") { _: DialogInterface, _: Int ->

                        }
                        .show()
                    return true
                }
            )
        )
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