package com.uu_uce.pins

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.util.JsonReader
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import java.io.StringReader


class PinContent(contentString: String) {
    val contentBlocks : List<ContentBlockInterface>
    init{
        contentBlocks = getContent(contentString)
    }

    private fun getContent(contentString: String) : List<ContentBlockInterface>{
            val reader = JsonReader(StringReader(contentString))

            return readContentBlocks(reader)
        }

    private fun readContentBlocks(reader: JsonReader) :  List<ContentBlockInterface>{
        val contentBlocks : MutableList<ContentBlockInterface> = mutableListOf()

        reader.beginArray()
        while (reader.hasNext()) {
            contentBlocks.add(readBlock(reader))
        }
        reader.endArray()
        return contentBlocks
    }

    private fun readBlock(reader: JsonReader): ContentBlockInterface {
        val dir             = "file:///data/data/com.uu_uce/files/pin_content/"

        var blockTag        = BlockTag.UNDEFINED
        var contentString   = ""
        var title           = ""
        var thumbnailURI    = Uri.EMPTY

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "tag" -> {
                    blockTag = blockTagFromString(reader.nextString())
                }
                "content" -> {
                    contentString = when(blockTag){
                        BlockTag.UNDEFINED  -> error("Undefined block tag")
                        BlockTag.TEXT       -> reader.nextString()
                        BlockTag.IMAGE      -> dir + "images/" + reader.nextString()
                        BlockTag.VIDEO      -> dir + "videos/" + reader.nextString()
                    }
                }
                "title" -> {
                    title = reader.nextString()
                }
                "thumbnail" -> {
                    thumbnailURI = Uri.parse(dir + "thumbnails/" + reader.nextString())
                }
                else -> {
                    error("Wrong content format")
                }
            }
        }
        reader.endObject()
        return when(blockTag){
            BlockTag.UNDEFINED  -> error("Undefined block tag")
            BlockTag.TEXT       -> TextContentBlock(contentString)
            BlockTag.IMAGE      -> ImageContentBlock(Uri.parse(contentString))
            BlockTag.VIDEO      -> VideoContentBlock(Uri.parse(contentString), thumbnailURI, title)
        }
    }
}

interface ContentBlockInterface{
    fun generateContent(layout : LinearLayout, activity : Activity)
}

class TextContentBlock(private val textContent : String) : ContentBlockInterface{
    override fun generateContent(layout : LinearLayout, activity : Activity){
        val content = TextView(activity)
        content.text = textContent
        content.setPadding(12,12,12,20)
        layout.addView(content)
    }
}

class ImageContentBlock(private val imageURI : Uri) : ContentBlockInterface{
    override fun generateContent(layout : LinearLayout, activity : Activity){
        val content = ImageView(activity)
        content.setImageURI(imageURI)
        layout.addView(content)
    }
}

class VideoContentBlock(private val videoURI : Uri, private val thumbnailURI : Uri, private val title : String) : ContentBlockInterface{
    override fun generateContent(layout : LinearLayout, activity : Activity){
        val relativeLayout = RelativeLayout(activity)
        val thumbnail = ImageView(activity)

        // Create thumbnail image
        if(thumbnailURI == Uri.EMPTY){
            val blackBox = CardView(activity)
            blackBox.setCardBackgroundColor(Color.BLACK) //TODO: maybe just set backgroundcolor of the layout to black?
            relativeLayout.addView(blackBox)

        }
        else{
            thumbnail.setImageURI(thumbnailURI)
            relativeLayout.addView(thumbnail)
        }
        relativeLayout.setBackgroundColor(Color.GREEN) //TODO: ADDED FOR DEBUGGING PURPOSES
        relativeLayout.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 600) //TODO: don't do magical numbers

        // Create play button
        val playButton = ImageView(activity)
        playButton.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.play, null) ?: error ("Image not found"))
        playButton.scaleType = ImageView.ScaleType.FIT_CENTER //TODO: find correct scaletype


        // Add thumbnail and button
        relativeLayout.addView(playButton)
        layout.addView(relativeLayout)
        relativeLayout.setOnClickListener{openVideoView(videoURI, title, activity)}
    }

    private fun openVideoView(videoURI: Uri, videoTitle : String, activity : Activity){
        val layoutInflater = activity.layoutInflater

        // build an custom view (to be inflated on top of our current view & build it's popup window)
        val customView = layoutInflater.inflate(R.layout.video_viewer, null)
        val popupWindow = PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val videoTitleText = customView.findViewById<TextView>(R.id.video_title_text)
        videoTitleText.text = videoTitle

        val videoPlayer = customView.findViewById<VideoView>(R.id.video_player)
        videoPlayer.setVideoURI(videoURI)
        val mediaController = MediaController(activity)
        videoPlayer.setMediaController(mediaController)
        mediaController.setAnchorView(customView)
        videoPlayer.start()

        val parentView = activity.findViewById<View>(R.id.geoMapLayout)
        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0)

        val btnClosePopupWindow = customView.findViewById<Button>(R.id.close_video_player)

        btnClosePopupWindow.setOnClickListener {
            popupWindow.dismiss()
        }
    }
}

enum class BlockTag{
    UNDEFINED,
    TEXT,
    IMAGE,
    VIDEO;
}

fun blockTagFromString(tagString : String) : BlockTag{
    return when (tagString) {
        "TEXT"  -> BlockTag.TEXT
        "IMAGE" -> BlockTag.IMAGE
        "VIDEO" -> BlockTag.VIDEO
        else    ->  BlockTag.UNDEFINED
    }
}



