package com.uu_uce.fieldbook

import android.app.Activity
import android.content.ContentResolver
import android.content.DialogInterface
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.R
import com.uu_uce.pins.*
import java.io.File

class FieldbookAdapter(val activity: Activity, private val viewModel: FieldbookViewModel) : RecyclerView.Adapter<FieldbookAdapter.FieldbookViewHolder>() {

    private var fieldbook: MutableList<FieldbookEntry> = mutableListOf()

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

        var uri: Uri = Uri.EMPTY

        var displayingText = false
        var displayingImage = false

        for (cB in content.contentBlocks)
        {
            if (displayingText && displayingImage)
                break

            if (cB is TextContentBlock && !displayingText) {
                displayingText = true
                holder.textFb.text = cB.textContent
            } else if (!displayingImage) {
                displayingImage = true

                if (cB is ImageContentBlock) {
                    uri = cB.imageURI
                    holder.imageFb.setImageURI(uri)
                }
                if (cB is VideoContentBlock) {
                    uri = cB.thumbnailURI
                    holder.imageFb.setImageURI(uri)
                }
            }
        }

        holder.parentView.setOnClickListener {
            //TODO: this isn't the correct parentView
            openPinPopupWindow(entry.location,content,holder.parentView,activity)
        }

        holder.parentView.setOnLongClickListener(
            View.OnLongClickListener(
                fun (_): Boolean {
                    AlertDialog.Builder(activity)
                        .setTitle("Delete")
                        .setMessage("Are you sure you want to delete this entry?")
                        .setPositiveButton("YES") { _: DialogInterface, _: Int ->
                            viewModel.delete(entry)
                            uri.toFile().delete()
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