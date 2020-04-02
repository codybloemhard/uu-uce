package com.uu_uce.pins

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.JsonReader
import android.view.Gravity
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import com.uu_uce.VideoViewer
import java.io.StringReader

class PinContent(contentString: String) {
    val contentBlocks : List<ContentBlockInterface>
    lateinit var parent : Pin
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

    // Generate ContentBlock from JSON string
    private fun readBlock(reader: JsonReader): ContentBlockInterface {
        var blockTag        = BlockTag.UNDEFINED
        var textString      = ""
        var filePath        = ""
        var title           = ""
        var thumbnailURI    = Uri.EMPTY

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "tag" -> {
                    blockTag = blockTagFromString(reader.nextString())
                    if(blockTag == BlockTag.UNDEFINED) error("Undefined block tag")
                }
                "text" -> {
                    textString = reader.nextString()
                }
                "file_path" -> {
                    filePath = when(blockTag) {
                        BlockTag.UNDEFINED  -> error("Undefined block tag")
                        BlockTag.TEXT       -> error("Undefined function") //TODO: Add reading text from file
                        BlockTag.IMAGE      -> reader.nextString()
                        BlockTag.VIDEO      -> reader.nextString()
                    }
                }

                "title" -> {
                    title = reader.nextString()
                }
                "thumbnail" -> {
                    thumbnailURI = Uri.parse(reader.nextString())
                    //if(blockTag != BlockTag.VIDEO) //TODO: alert user that only VideoContentBlock uses thumbnail
                }
                else -> {
                    error("Wrong content format")
                }
            }
        }
        reader.endObject()
        return when(blockTag){
            BlockTag.UNDEFINED  -> error("Undefined block tag")
            BlockTag.TEXT       -> TextContentBlock(textString)
            BlockTag.IMAGE      -> ImageContentBlock(Uri.parse(filePath))
            BlockTag.VIDEO      -> VideoContentBlock(Uri.parse(filePath), thumbnailURI, title)
        }
    }
}

interface ContentBlockInterface{
    fun generateContent(layout : LinearLayout, activity : Activity)
    fun getFilePath() : List<String>
}

class TextContentBlock(val textContent : String) : ContentBlockInterface {
    override fun generateContent(layout : LinearLayout, activity : Activity) {
        val content = TextView(activity)
        content.text = textContent
        content.setPadding(12,12,12,20)
        layout.addView(content)
    }
    override fun getFilePath() : List<String>{
        return listOf()
    }
}

class ImageContentBlock(val imageURI : Uri) : ContentBlockInterface{
    override fun generateContent(layout : LinearLayout, activity : Activity){
        val content = ImageView(activity)
        content.setImageURI(imageURI)

        layout.addView(content)
    }

    override fun getFilePath() : List<String>{
        return listOf(imageURI.toString())
    }
}

class VideoContentBlock(private val videoURI : Uri, val thumbnailURI : Uri, private val title : String) : ContentBlockInterface{
    override fun generateContent(layout : LinearLayout, activity : Activity){

        val frameLayout = FrameLayout(activity)

        // Create thumbnail image
        if(thumbnailURI == Uri.EMPTY){
            frameLayout.setBackgroundColor(Color.BLACK)
        }
        else{
            val thumbnail = ImageView(activity)
            thumbnail.setImageURI(thumbnailURI)
            thumbnail.scaleType = ImageView.ScaleType.CENTER
            frameLayout.addView(thumbnail)
        }

        frameLayout.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)

        // Create play button
        val playButton = ImageView(activity)
        playButton.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_sprite_play, null) ?: error ("Image not found"))
        playButton.scaleType = ImageView.ScaleType.CENTER_INSIDE
        val buttonLayout = FrameLayout.LayoutParams(500, 500) // TODO: convert dp to pixels
        buttonLayout.gravity = Gravity.CENTER
        playButton.layoutParams = buttonLayout
        playButton.setOnClickListener{openVideoView(videoURI, title, activity)}

        // Add thumbnail and button
        frameLayout.addView(playButton)
        layout.addView(frameLayout)
    }

    override fun getFilePath() : List<String>{
        if(thumbnailURI == Uri.EMPTY) return listOf()
        return listOf(thumbnailURI.toString())
    }

    private fun openVideoView(videoURI: Uri, videoTitle : String, activity : Activity){
        val intent = Intent(activity, VideoViewer::class.java)

        intent.putExtra("uri", videoURI)
        intent.putExtra("title", videoTitle)
        activity.startActivity(intent)
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



