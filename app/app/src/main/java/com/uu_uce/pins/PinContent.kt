package com.uu_uce.pins

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.JsonReader
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import com.uu_uce.VideoViewer
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
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

    private fun readBlock(reader: JsonReader): ContentBlockInterface {
        val dir             = "file:///data/data/com.uu_uce/files/pin_content/"

        var blockTag                                    = BlockTag.UNDEFINED
        var textString                                  = ""
        var fileName                                    = ""
        var title                                       = ""
        var thumbnailURI                                = Uri.EMPTY
        val mcCorrectOptions : MutableList<String>      = mutableListOf()
        val mcIncorrectOptions : MutableList<String>    = mutableListOf()

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
                        BlockTag.MCQUIZ     -> error("Multiple choice blocks can not be loaded from file")
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
                "mc_correct_option" -> {
                    mcCorrectOptions.add(reader.nextString())
                }
                "mc_incorrect_option" -> {
                    mcIncorrectOptions.add(reader.nextString())
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
            BlockTag.MCQUIZ     -> {
                if(mcIncorrectOptions.count() < 1 && mcCorrectOptions.count() < 1) {
                    error("Mutliple choice questions require at least one correct and one incorrect answer")
                }
                MCContentBlock( mcCorrectOptions, mcIncorrectOptions)
            }
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
        playButton.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_sprite_play, null) ?: error ("Image not found"))
        playButton.scaleType = ImageView.ScaleType.FIT_CENTER //TODO: find correct scaletype
        playButton.setOnClickListener{openVideoView(videoURI, title, activity)}

        // Add thumbnail and button
        relativeLayout.addView(playButton)
        layout.addView(relativeLayout)
    }

    private fun openVideoView(videoURI: Uri, videoTitle : String, activity : Activity){
        val intent = Intent(activity, VideoViewer::class.java)
        intent.putExtra("uri", videoURI)
        intent.putExtra("title", videoTitle)
        activity.startActivity(intent)
    }
}

class MCContentBlock(private val correctAnswers : List<String>, private val incorrectAnswers : List<String>) : ContentBlockInterface{
    override fun generateContent(layout: LinearLayout, activity: Activity) {
        val answers : MutableList<Pair<String, Boolean>> = mutableListOf()
        for(answer in correctAnswers) answers.add(Pair(answer, true))
        for(answer in incorrectAnswers) answers.add(Pair(answer, false))

        val shuffledAnswers = answers.shuffled()
        val table : TableLayout = TableLayout(activity)

        var currentRow = TableRow(activity)
        currentRow.gravity = Gravity.CENTER_HORIZONTAL
        for(i in 0 until shuffledAnswers.count()){
            val currentFrame = FrameLayout(activity)
            val frameParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            frameParams.setMargins(5, 5, 5, 5)
            if(shuffledAnswers[i].second){
                currentFrame.setOnClickListener {
                    Toast.makeText(activity, "Correct", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                currentFrame.setOnClickListener {
                    Toast.makeText(activity, "Incorrect", Toast.LENGTH_SHORT).show()
                }
            }

            currentFrame.layoutParams = frameParams

            val background = View(activity)
            background.setBackgroundColor(Color.RED)
            val backgroundParams = TableRow.LayoutParams(
                330,
                330
            )
            background.layoutParams = backgroundParams
            currentFrame.addView(background)

            val answer = TextView(activity)
            answer.text = shuffledAnswers[i].first
            answer.gravity = Gravity.CENTER
            val textParams = TableLayout.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            answer.layoutParams = textParams
            currentFrame.addView(answer)

            currentRow.addView(currentFrame)
            if(i % 2 == 1){
                table.addView(currentRow)
                currentRow = TableRow(activity)
                currentRow.gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        if(currentRow.childCount > 0){
            table.addView(currentRow)
        }
        layout.addView(table)
    }
}

enum class BlockTag{
    UNDEFINED,
    TEXT,
    IMAGE,
    MCQUIZ,
    VIDEO;
}

fun blockTagFromString(tagString : String) : BlockTag{
    return when (tagString) {
        "TEXT"      -> BlockTag.TEXT
        "IMAGE"     -> BlockTag.IMAGE
        "VIDEO"     -> BlockTag.VIDEO
        "MCQUIZ"    -> BlockTag.MCQUIZ
        else        -> BlockTag.UNDEFINED
    }
}



