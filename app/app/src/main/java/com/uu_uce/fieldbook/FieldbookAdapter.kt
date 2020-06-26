package com.uu_uce.fieldbook

import android.app.Activity
import android.content.DialogInterface
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toFile
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.R
import com.uu_uce.openFieldbookPopup
import com.uu_uce.pins.*

/**
 * The adapter manages the RecyclerView
 *
 * @property[activity] the associated activity
 * @property[viewModel] the viewModel of the Fieldbook
 * @property[rootView] the (root of the) currently opened view
 */
class FieldbookAdapter(
    private val activity    : Activity,
    private val viewModel   : FieldbookViewModel,
    private val rootView    : View
)
    : RecyclerView.Adapter<FieldbookAdapter.FieldbookViewHolder>()
{
    private var fieldbook: List<FieldbookEntry> = emptyList()
    private lateinit var parentView : ViewGroup

    var view: View? = null

    /**
     * Represents one item of the RecyclerView
     *
     * @property[parentView] the surrounding/entire View of one RecyclerView item
     * @constructor makes an PinViewHolder object
     */
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
            titleFb.text    = entry.title
            numberFb.text   = addLeadingZeros(index)
            locationFb.text = entry.location
            datetimeFb.text = entry.dateTime
        }

        val content = PinContent(entry.content, activity, true).contentBlocks

        setPreview(content, holder)

        holder.parentView.setOnClickListener {
            openFieldbookPopup(
                activity,
                view ?: rootView,
                entry,
                content
            )
        }

        holder.parentView.setOnLongClickListener {
            deleteFromFieldbook(entry, content)
        }
    }

    /**
     * Sets the preview of one Recycler Item. Uses the first available TextBlock,
     * else the thumbnail of the first Video- or ImageBlock
     *
     * @param[content] all ContentBlocks of this FieldbookEntry
     * @param[holder] holds the view of one RecyclerView item
     */
    private fun setPreview(content: List<ContentBlock>, holder: FieldbookViewHolder) {
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        var selectedThumbnail = false
        var thumbnailUri = Uri.EMPTY

        // Try to find the first text block in the list and show it as preview
        loop@ for (cB in content) {
            when (cB) {
                is TextContentBlock -> {
                    TextView(activity).apply {
                        textSize = 12f
                        setTextColor(ResourcesCompat.getColor(activity.resources, R.color.TextGrey, null))
                        text = cB.getTextContent()
                    }.also {
                        holder.frameFb.addView(it, params)
                    }
                    break@loop
                }
                else -> {
                    if (!selectedThumbnail) {
                        if (cB is ImageContentBlock)
                            thumbnailUri = cB.getThumbnailURI()
                        else if (cB is VideoContentBlock)
                            thumbnailUri = cB.getThumbnailURI()
                        selectedThumbnail = true
                    }
                }
            }
        }

        // When no text block was found, show the first image or video as preview
        if (holder.frameFb.childCount == 0)
            ImageView(activity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(thumbnailUri)
            }.also {
                holder.frameFb.addView(it, params)
            }
    }

    /**
     * Asks if you want to delete this FieldbookEntry. When you choose yes it:
     * Deletes the ContentBlock
     * Deletes the thumbnail for an Image- or a VideoBlock
     *
     * @param[entry] the FieldbookEntry that we long pressed
     * @param[content] all ContentBlocks of this FieldbookEntry
     */
    private fun deleteFromFieldbook(entry: FieldbookEntry, content: List<ContentBlock>): Boolean {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.delete_popup_title))
            .setMessage(activity.getString(R.string.fieldbook_pindeletion_popup_text))
            .setPositiveButton(activity.getString(R.string.positive_button_text)) { _: DialogInterface, _: Int ->
                viewModel.delete(entry)
                for (cB in content) {
                    when (cB) {
                        is ImageContentBlock ->
                            cB.getThumbnailURI().apply {
                                if (this != Uri.EMPTY)
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

    /**
     * Adds zeros in front of the id, for a notation with a uniform amount of digits
     *
     * @param[id] the index of the FieldbookEntry
     */
    private fun addLeadingZeros(id: Int) : String {
        val s = id.toString()
        val zero : String = "0".repeat(3-s.length)
        return "#$zero$id"
    }

    /**
     * Sets the property fieldbook
     *
     * @param[fieldbook] a list of all FieldbookEntries
     */
    fun setFieldbook(fieldbook: List<FieldbookEntry>) {
        this.fieldbook = fieldbook
        notifyDataSetChanged()
    }
}


