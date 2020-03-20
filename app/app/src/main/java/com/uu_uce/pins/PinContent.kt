package com.uu_uce.pins

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.util.JsonReader
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
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
        var textString   = ""
        var fileName        = ""
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
                    //if(blockTag != BlockTag.TEXT) //TODO: alert user that only TextContentBlock uses text
                }
                "file_name" -> {
                    fileName = when(blockTag) {
                        BlockTag.UNDEFINED  -> error("Undefined block tag")
                        BlockTag.TEXT       -> error("Undefined function") //TODO: Add reading text from file
                        BlockTag.IMAGE      -> dir + "images/" + reader.nextString()
                        BlockTag.VIDEO      -> dir + "videos/" + reader.nextString()
                    }
                }

                "title" -> {
                    title = reader.nextString()
                    //if(blockTag != BlockTag.VIDEO) //TODO: alert user that only VideoContentBlock uses title
                }
                "thumbnail" -> {
                    thumbnailURI = Uri.parse(dir + "videos/thumbnails/" + reader.nextString())
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
            BlockTag.IMAGE      -> ImageContentBlock(Uri.parse(fileName))
            BlockTag.VIDEO      -> VideoContentBlock(Uri.parse(fileName), thumbnailURI, title)
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

        val relativeLayout = RelativeLayout(activity) //TODO: maybe make this an constraintlayout?

        // Create thumbnail image
        if(thumbnailURI == Uri.EMPTY){
            relativeLayout.setBackgroundColor(Color.BLACK)
        }
        else{
            val thumbnail = ImageView(activity)
            thumbnail.setImageURI(thumbnailURI)
            thumbnail.scaleType = ImageView.ScaleType.FIT_CENTER
            relativeLayout.addView(thumbnail)
        }

        //relativeLayout.setBackgroundColor(Color.GREEN) //TODO: ADDED FOR DEBUGGING PURPOSES
        relativeLayout.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 600) //TODO: don't do magical numbers

        // Create play button
        val playButton = ImageView(activity)
        playButton.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.play, null) ?: error ("Image not found"))
        playButton.scaleType = ImageView.ScaleType.FIT_CENTER //TODO: find correct scaletype
        playButton.setOnClickListener{openVideoView(videoURI, title, activity)}

        // Add thumbnail and button
        relativeLayout.addView(playButton)
        layout.addView(relativeLayout)
    }

    private fun openVideoView(videoURI: Uri, videoTitle : String, activity : Activity){
        val layoutInflater = activity.layoutInflater

        // build an custom view (to be inflated on top of our current view & build it's popup window)
        val customView = layoutInflater.inflate(R.layout.video_viewer, null)
        val popupWindow = PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val videoTitleText = customView.findViewById<TextView>(R.id.video_title_text)
        videoTitleText.text = videoTitle

        initializeVideoPlayer(videoURI, customView, activity)

        val parentView = activity.findViewById<View>(R.id.geoMapLayout)
        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0)

        val btnClosePopupWindow = customView.findViewById<Button>(R.id.close_video_player)

        btnClosePopupWindow.setOnClickListener {
            popupWindow.dismiss()
        }
    }

    private fun initializeVideoPlayer(videoURI: Uri, view: View, activity: Activity) {
        val videoPlayer: VLCVideoLayout = view.findViewById<VLCVideoLayout>(R.id.video_player)

        /* PLEASE KEEP COMMENTED: NEED THIS FOR FURTHER DEVELOPMENT
        val playerSurface: SurfaceView = activity.findViewById(R.id.player_surface)
        val surfaceHolder = playerSurface.holder
        val surface = surfaceHolder.surface
        val surfaceFrame: FrameLayout = activity.findViewById(R.id.player_surface_frame)
         */

        val libVLC: LibVLC = LibVLC(activity)
        val mediaPlayer: MediaPlayer = MediaPlayer(libVLC)

        mediaPlayer.attachViews(videoPlayer, null, false, false)
        val media: Media = Media(libVLC, videoURI)
        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
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



