package com.uu_uce.pins

import android.app.Activity
import android.net.Uri
import android.util.JsonReader
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
        var blockTag : BlockTag = BlockTag.UNDEFINED
        var returnBlock : ContentBlockInterface
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "tag" -> {
                    blockTag = blockTagFromString(reader.nextString())
                }
                "content" -> {
                    returnBlock = when(blockTag){
                        BlockTag.UNDEFINED  -> error("Undefined block tag")
                        BlockTag.TEXT       -> TextContentBlock(reader.nextString())
                        BlockTag.IMAGE      -> ImageContentBlock(Uri.parse("file:///data/data/com.uu_uce/files/pin_content/images/" + reader.nextString()))
                        BlockTag.VIDEO      -> VideoContentBlock(Uri.parse("file:///data/data/com.uu_uce/files/pin_content/videos/" + reader.nextString()))
                    }

                    reader.endObject()
                    return returnBlock
                }
                else -> {
                    error("Wrong content format")
                }
            }
        }
        error("Wrong content format")
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

class VideoContentBlock(private val videoURI : Uri) : ContentBlockInterface{
    override fun generateContent(layout : LinearLayout, activity : Activity){
        val relativeLayout = RelativeLayout(activity)

        // Create thumbnail image
        /*val thumbnail = ImageView(context)
        thumbnail.setImageURI(thumbnailURI)
        relativeLayout.addView(thumbnail)*/

        // Create play button
        val btnTag = Button(activity)
        btnTag.background = ResourcesCompat.getDrawable(activity.resources, R.drawable.play, null) ?: error ("Image not found")
        btnTag.setOnClickListener{openVideoView(videoURI, activity)}
        btnTag.height = btnTag.width
        layout.addView(btnTag)

        // Add thumbnail and button
        layout.addView(relativeLayout)
    }

    private fun openVideoView(videoURI: Uri, activity : Activity){
        val layoutInflater = activity.layoutInflater

        // build an custom view (to be inflated on top of our current view & build it's popup window)
        val customView = layoutInflater.inflate(R.layout.video_viewer, null)
        val popupWindow = PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val videoPlayer = customView.findViewById<VideoView>(R.id.video_player)
        videoPlayer.setVideoURI(videoURI)
        val mediaController = MediaController(activity)
        videoPlayer.setMediaController(mediaController)
        mediaController.setAnchorView(customView)

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



