package com.uu_uce.pins

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.JsonReader
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.ImageViewer
import com.uu_uce.R
import com.uu_uce.VideoViewer
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.updateFiles
import java.io.StringReader

class PinContent(private val contentString: String) {
    val contentBlocks : List<ContentBlockInterface>
    var canCompletePin = false

    lateinit var parent : Pin
    init{
        contentBlocks = getContent()
    }


    private fun getContent() : List<ContentBlockInterface>{
            val reader = JsonReader(StringReader(contentString))

            return readContentBlocks(reader)
        }

    private fun readContentBlocks(reader: JsonReader) :  List<ContentBlockInterface>{
        val contentBlocks : MutableList<ContentBlockInterface> = mutableListOf()

        reader.beginArray()
        while (reader.hasNext()) {
            val curBlock = readBlock(reader)
            if(curBlock != null) {
                if(curBlock.canCompleteBlock) canCompletePin = true
                contentBlocks.add(curBlock)
            }
        }
        reader.endArray()
        return contentBlocks
    }

    // Generate ContentBlock from JSON string
    private fun readBlock(reader: JsonReader): ContentBlockInterface? {
        var blockTag                                    = BlockTag.UNDEFINED
        var textString                                  = ""
        var filePath                                    = ""
        var title                                       = ""
        var thumbnailURI                                = Uri.EMPTY
        val mcCorrectOptions : MutableList<String>      = mutableListOf()
        val mcIncorrectOptions : MutableList<String>    = mutableListOf()
        var reward                                      = 0

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "tag" -> {
                    blockTag = blockTagFromString(reader.nextString())
                }
                "text" -> {
                    textString = reader.nextString()
                }
                "file_path" -> {
                    filePath = when(blockTag) {
                        BlockTag.UNDEFINED  -> {
                            Logger.error("PinContent", "Tag needs to be specified before file_path")
                            ""
                        }
                        BlockTag.TEXT       -> {
                            //TODO: Add reading text from file?
                            Logger.log(LogType.NotImplemented, "PinContent", "file reading not implemented")
                            reader.nextString()
                            ""
                        }
                        BlockTag.IMAGE      -> reader.nextString()
                        BlockTag.VIDEO      -> reader.nextString()
                        BlockTag.MCQUIZ     -> {
                            Logger.error("PinContent", "multiple choice quiz can not be read from file")
                            reader.nextString()
                            ""
                        }
                    }
                }

                "title" -> {
                    title = reader.nextString()
                }
                "thumbnail" -> {
                    thumbnailURI = Uri.parse(reader.nextString())
                }
                "mc_correct_option" -> {
                    mcCorrectOptions.add(reader.nextString())
                }
                "mc_incorrect_option" -> {
                    mcIncorrectOptions.add(reader.nextString())
                }
                "reward" ->{
                    reward = reader.nextInt()
                }
                else -> {
                    Logger.error("PinContent", "Wrong content format")
                    reader.nextString()
                    reader.nextString()
                }
            }
        }
        reader.endObject()
        return when(blockTag){
            BlockTag.UNDEFINED  -> {
                Logger.error("PinContent", "No BlockTag specified")
                return null
            }
            BlockTag.TEXT       -> TextContentBlock(textString)
            BlockTag.IMAGE      -> ImageContentBlock(Uri.parse(filePath), thumbnailURI, title)
            BlockTag.VIDEO      -> VideoContentBlock(Uri.parse(filePath), thumbnailURI, title)
            BlockTag.MCQUIZ     -> {
                if(mcIncorrectOptions.count() < 1 && mcCorrectOptions.count() < 1) {
                    Logger.error("PinContent", "Mutliple choice questions require at least one correct and one incorrect answer")
                    return null
                }
                MCContentBlock( mcCorrectOptions, mcIncorrectOptions, reward)
            }
        }
    }
}

interface ContentBlockInterface{
    val canCompleteBlock : Boolean
    fun generateContent(blockId : Int, layout : LinearLayout, activity : Activity, view : View, parent : Pin?)
    fun getFilePaths() : List<String>
    override fun toString() : String
}

class TextContentBlock(private val textContent : String) : ContentBlockInterface{
    private val tag = BlockTag.TEXT
    override val canCompleteBlock = false
    override fun generateContent(blockId : Int, layout : LinearLayout, activity : Activity, view : View, parent : Pin?){
        val content = TextView(activity)
        content.text = textContent
        content.setPadding(12,12,12,20)
        content.gravity = Gravity.CENTER_HORIZONTAL
        layout.addView(content)
    }

    override fun getFilePaths() : List<String>{
        return listOf()
    }

    override fun toString() : String {
        return "{${tagToJsonString(tag)}," +
                "${textToJsonString(textContent)}}"
    }

    fun getTextContent() : String{
        return textContent
    }
}

class ImageContentBlock(private val imageURI : Uri, private val thumbnailURI: Uri, private val title : String? = null) : ContentBlockInterface{
    private val tag = BlockTag.IMAGE
    override val canCompleteBlock = false

    override fun generateContent(blockId : Int, layout : LinearLayout, activity : Activity, view : View, parent : Pin?){
        val content = ImageView(activity)
        try {
            content.setImageURI(imageURI)
            content.scaleType = ImageView.ScaleType.FIT_CENTER
            content.adjustViewBounds = true
        } catch (e: Exception) {
            Logger.error("PinContent","Couldn't load $imageURI, so loaded the thumbnail $thumbnailURI instead")
            content.setImageURI(thumbnailURI)
        }
        val imageLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        content.layoutParams = imageLayoutParams
        content.id = R.id.image_block

        content.setOnClickListener{
            openImageView(imageURI, title, activity)
        }

        layout.addView(content)
    }

    override fun getFilePaths() : List<String>{
        return listOf(imageURI.toString())
    }

    override fun toString() : String {
        return "{${tagToJsonString(tag)}," +
                "${fileToJsonString(imageURI)}," +
                "${thumbnailToJsonString(thumbnailURI)}}"
    }

    private fun openImageView(imageURI: Uri, imageTitle : String?, activity : Activity){
        val intent = Intent(activity, ImageViewer::class.java)

        intent.putExtra("uri", imageURI)
        if(imageTitle != null)
            intent.putExtra("title", imageTitle)
        activity.startActivity(intent)
    }

    fun getThumbnailURI() : Uri{
        return thumbnailURI
    }
}

class VideoContentBlock(private val videoURI : Uri, private val thumbnailURI : Uri, private val title : String? = null) : ContentBlockInterface{
    private val tag = BlockTag.VIDEO
    override val canCompleteBlock = false

    override fun generateContent(blockId : Int, layout : LinearLayout, activity : Activity, view : View, parent : Pin?){
        val frameLayout = FrameLayout(activity)

        // Create thumbnail image
        if(thumbnailURI == Uri.EMPTY){
            frameLayout.setBackgroundColor(Color.BLACK)
        }
        else{
            val thumbnail = ImageView(activity)
            thumbnail.setImageURI(thumbnailURI)
            thumbnail.scaleType = ImageView.ScaleType.FIT_CENTER
            thumbnail.adjustViewBounds = true
            frameLayout.addView(thumbnail)
        }

        frameLayout.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)

        // Create play button
        val playButton = ImageView(activity)
        playButton.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_sprite_play, null) ?: error ("Image not found"))
        playButton.scaleType = ImageView.ScaleType.CENTER_INSIDE
        val buttonLayout = FrameLayout.LayoutParams(view.width / 3, view.width / 3)
        buttonLayout.gravity = Gravity.CENTER
        playButton.layoutParams = buttonLayout

        // Add thumbnail and button
        frameLayout.addView(playButton)
        frameLayout.setOnClickListener{
            updateFiles(
                listOf(videoURI.toString()),
                activity,
                { openVideoView(videoURI, title, activity) },
                {}
            )
        }
        frameLayout.id = R.id.start_video_button
        layout.addView(frameLayout)
    }

    override fun getFilePaths() : List<String>{
        if(thumbnailURI == Uri.EMPTY) return listOf(videoURI.toString())
        return listOf(thumbnailURI.toString(), videoURI.toString())
    }

    override fun toString() : String {
        return "{${tagToJsonString(tag)}," +
                "${fileToJsonString(videoURI)}," +
                "${thumbnailToJsonString(thumbnailURI)}}"
    }

    private fun openVideoView(videoURI: Uri, videoTitle : String?, activity : Activity){
        val intent = Intent(activity, VideoViewer::class.java)

        intent.putExtra("uri", videoURI)
        if(videoTitle != null)
            intent.putExtra("title", videoTitle)
        activity.startActivity(intent)
    }

    fun getThumbnailURI() : Uri{
        return thumbnailURI
    }
}

class MCContentBlock(private val correctAnswers : List<String>, private val incorrectAnswers : List<String>, private val reward : Int) : ContentBlockInterface{
    private val tag = BlockTag.MCQUIZ
    override val canCompleteBlock = true
    private var selectedAnswer : Int = -1
    private lateinit var selectedBackground : CardView

    override fun generateContent(blockId : Int, layout: LinearLayout, activity: Activity, view : View, parent : Pin?) {
        if(parent == null) {
            Logger.error("PinContent","Mutliple choice quizzes can't be generated without a parent pin")
            return
        }

        selectedBackground = CardView(activity)
        parent.addQuestion(blockId, reward)

        val answers : MutableList<Pair<String, Boolean>> = mutableListOf()
        for(answer in correctAnswers) answers.add(Pair(answer, true))
        for(answer in incorrectAnswers) answers.add(Pair(answer, false))

        val shuffledAnswers = answers.shuffled()

        // Create tableLayout with first row
        val table = TableLayout(activity)
        table.id = R.id.multiple_choice_table
        var currentRow = TableRow(activity)
        currentRow.gravity = Gravity.CENTER_HORIZONTAL

        // Insert answers into rows
        for(i in 0 until shuffledAnswers.count()){
            val currentFrame = FrameLayout(activity)
            val frameParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            frameParams.setMargins(view.width / 36, view.width / 50, view.width / 36, view.width / 50)

            currentFrame.layoutParams = frameParams

            val background = CardView(activity)
            val backgroundParams = TableRow.LayoutParams(
                view.width * 8 / 20,
                view.width * 8 / 20
            )
            background.layoutParams = backgroundParams
            background.radius = 15f
            currentFrame.addView(background)

            val answer = TextView(activity)
            answer.text = shuffledAnswers[i].first
            answer.gravity = Gravity.CENTER
            answer.setTextColor(ContextCompat.getColor(activity, R.color.BestWhite))
            val textParams = TableLayout.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            answer.layoutParams = textParams
            background.addView(answer)

            currentRow.addView(currentFrame)

            if(parent.getStatus() < 2){
                background.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.Boyzone))
                currentFrame.setOnClickListener {
                    selectedBackground.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.Boyzone))
                    selectedAnswer = i
                    selectedBackground = background
                    background.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.OrangeHibiscus))
                    if(shuffledAnswers[i].second){
                        parent.answerQuestion(blockId, reward)
                    }
                    else{
                        parent.answerQuestion(blockId, 0)
                    }
                }
            }
            else{
                if(shuffledAnswers[i].second){
                    background.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.ReptileGreen))
                }
                else{
                    background.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.FusionRed))
                }
            }

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

    override fun getFilePaths(): List<String> {
        return listOf()
    }

    override fun toString(): String {
        return ""
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

fun tagToJsonString(tag: BlockTag) : String {
    return "\"tag\":\"$tag\""
}

fun textToJsonString(text: String) : String {
    return "\"text\":\"$text\""
}

fun fileToJsonString(filePath: Uri) : String {
    return "\"file_path\":\"$filePath\""
}

fun thumbnailToJsonString(thumbnail: Uri) : String {
    return "\"thumbnail\":\"$thumbnail\""
}



