package com.uu_uce.pins

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.InputType
import android.util.JsonReader
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.github.chrisbanes.photoview.PhotoView
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.uu_uce.R
import com.uu_uce.VideoViewer
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import java.io.StringReader

class PinContent(
    private val contentString: String,
    val context: Context
) {
    val contentBlocks : MutableList<ContentBlockInterface>
    var canCompletePin = false

    lateinit var parent : Pin
    init{
        contentBlocks = getContent()
    }


    private fun getContent() : MutableList<ContentBlockInterface>{
            val reader = JsonReader(StringReader(contentString))

            return readContentBlocks(reader)
        }

    private fun readContentBlocks(reader: JsonReader) :  MutableList<ContentBlockInterface>{
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
            BlockTag.TEXT       -> TextContentBlock(textString, context)
            BlockTag.IMAGE      -> ImageContentBlock(Uri.parse(filePath), thumbnailURI, context)
            BlockTag.VIDEO      -> VideoContentBlock(Uri.parse(filePath), thumbnailURI, context, title)
            BlockTag.MCQUIZ     -> {
                if(mcIncorrectOptions.count() < 1 && mcCorrectOptions.count() < 1) {
                    Logger.error("PinContent", "Mutliple choice questions require at least one correct and one incorrect answer")
                    return null
                }
                MCContentBlock( mcCorrectOptions, mcIncorrectOptions, reward, context)
            }
        }
    }
}

interface ContentBlockInterface {
    val content: View
    val tag : BlockTag
    val canCompleteBlock : Boolean
    fun generateContent(blockId : Int, layout : LinearLayout, view : View, parent : Pin?)
    fun getFilePath() : List<String>
    override fun toString() : String
}

class EditTextBlock(
    private val context: Context
)
    : ContentBlockInterface
{
    override var content = EditText(context)
    override val tag = BlockTag.TEXT
    override val canCompleteBlock = false
    override fun generateContent(blockId : Int, layout : LinearLayout, view : View, parent : Pin?) {
        content = EditText(context).apply {
            inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isSingleLine = false
            imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION
            id = R.id.text_field
        }.also{
            layout.addView(it)
        }
    }

    override fun getFilePath() : List<String> {
        return listOf()
    }

    override fun toString() : String {
        return "{${tagToJsonString(tag)}," +
                "${textToJsonString(content.text.toString())}}"
    }
}

class TextContentBlock(
    private val textContent : String,
    private val context: Context
)
    : ContentBlockInterface
{
    override var content = TextView(context)
    override val tag = BlockTag.TEXT
    override val canCompleteBlock = false
    override fun generateContent(blockId : Int, layout : LinearLayout, view : View, parent : Pin?){
        content = TextView(context).apply {
            text = textContent
            setPadding(12, 12, 12, 20)
            gravity = Gravity.CENTER_HORIZONTAL
        }.also{
            layout.addView(it)
        }
    }

    override fun getFilePath() : List<String>{
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

class ImageContentBlock(
    private val imageURI : Uri,
    private val thumbnailURI: Uri,
    private val context: Context
)
    : ContentBlockInterface
{
    override var content = PhotoView(context)
    override val tag = BlockTag.IMAGE
    override val canCompleteBlock = false
    override fun generateContent(blockId : Int, layout : LinearLayout, view : View, parent : Pin?){
        content = PhotoView(context)
        try {
            content.apply {
                setImageURI(imageURI)
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
            }
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
        PhotoViewAttacher(content)

        layout.addView(content)
    }

    override fun getFilePath() : List<String>{
        return listOf(imageURI.toString())
    }

    override fun toString() : String {
        return "{${tagToJsonString(tag)}," +
                "${fileToJsonString(imageURI)}," +
                "${thumbnailToJsonString(thumbnailURI)}}"
    }

    fun getThumbnailURI() : Uri{
        return thumbnailURI
    }
}

class VideoContentBlock(
    private val videoURI : Uri,
    private val thumbnailURI : Uri,
    private val context: Context,
    private val title : String? = null
)
    : ContentBlockInterface
{
    override var content = FrameLayout(context)
    override val tag = BlockTag.VIDEO
    override val canCompleteBlock = false
    override fun generateContent(blockId : Int, layout : LinearLayout, view : View, parent : Pin?){
        content = FrameLayout(context)

        // Create thumbnail image
        if(thumbnailURI == Uri.EMPTY){
            content.setBackgroundColor(Color.BLACK)
        }
        else{
            val thumbnail = ImageView(context)
            thumbnail.setImageURI(thumbnailURI)
            thumbnail.scaleType = ImageView.ScaleType.FIT_CENTER
            thumbnail.adjustViewBounds = true
            content.addView(thumbnail)
        }

        content.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        content.id = R.id.video_block

        // Create play button
        val playButton = ImageView(context)
        playButton.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.ic_sprite_play, null) ?: error ("Image not found"))
        playButton.scaleType = ImageView.ScaleType.CENTER_INSIDE
        val buttonLayout = FrameLayout.LayoutParams(view.width / 3, view.width / 3)
        buttonLayout.gravity = Gravity.CENTER
        playButton.layoutParams = buttonLayout

        // Add thumbnail and button
        content.addView(playButton)
        content.setOnClickListener{openVideoView(videoURI, title)}
        content.id = R.id.start_video_button
        layout.addView(content)
    }

    override fun getFilePath() : List<String>{
        if(thumbnailURI == Uri.EMPTY) return listOf()
        return listOf(thumbnailURI.toString())
    }

    override fun toString() : String {
        return "{${tagToJsonString(tag)}," +
                "${fileToJsonString(videoURI)}," +
                "${thumbnailToJsonString(thumbnailURI)}}"
    }

    private fun openVideoView(videoURI: Uri, videoTitle : String?){
        val intent = Intent(context, VideoViewer::class.java)

        intent.putExtra("uri", videoURI)
        if(videoTitle != null)
            intent.putExtra("title", videoTitle)
        context.startActivity(intent)
    }

    fun getThumbnailURI() : Uri{
        return thumbnailURI
    }
}

class MCContentBlock(
    private val correctAnswers : List<String>,
    private val incorrectAnswers : List<String>,
    private val reward : Int,
    private val context : Context
)
    : ContentBlockInterface
{
    override var content = TableLayout(context)
    override val tag = BlockTag.MCQUIZ
    override val canCompleteBlock = true
    private var selectedAnswer : Int = -1
    private lateinit var selectedBackground : CardView

    override fun generateContent(blockId : Int, layout: LinearLayout, view : View, parent : Pin?) {
        content = TableLayout(context)

        if(parent == null) {
            Logger.error("PinContent","Mutliple choice quizzes can't be generated without a parent pin")
            return
        }

        selectedBackground = CardView(context)
        parent.addQuestion(blockId, reward)

        val answers : MutableList<Pair<String, Boolean>> = mutableListOf()
        for(answer in correctAnswers) answers.add(Pair(answer, true))
        for(answer in incorrectAnswers) answers.add(Pair(answer, false))

        val shuffledAnswers = answers.shuffled()

        // Create tableLayout with first row
        content.id = R.id.multiple_choice_table
        var currentRow = TableRow(context)
        currentRow.gravity = Gravity.CENTER_HORIZONTAL

        // Insert answers into rows
        for(i in 0 until shuffledAnswers.count()){
            val currentFrame = FrameLayout(context)
            val frameParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            frameParams.setMargins(view.width / 36, view.width / 50, view.width / 36, view.width / 50)

            currentFrame.layoutParams = frameParams

            val background = CardView(context)
            val backgroundParams = TableRow.LayoutParams(
                view.width * 8 / 20,
                view.width * 8 / 20
            )
            background.layoutParams = backgroundParams
            background.radius = 15f
            currentFrame.addView(background)

            val answer = TextView(context)
            answer.text = shuffledAnswers[i].first
            answer.gravity = Gravity.CENTER
            answer.setTextColor(ContextCompat.getColor(context, R.color.BestWhite))
            val textParams = TableLayout.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.MATCH_PARENT
            )
            answer.layoutParams = textParams
            background.addView(answer)

            currentRow.addView(currentFrame)

            if(parent.getStatus() < 2){
                background.setCardBackgroundColor(ContextCompat.getColor(context, R.color.Boyzone))
                currentFrame.setOnClickListener {
                    selectedBackground.setCardBackgroundColor(ContextCompat.getColor(context, R.color.Boyzone))
                    selectedAnswer = i
                    selectedBackground = background
                    background.setCardBackgroundColor(ContextCompat.getColor(context, R.color.OrangeHibiscus))
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
                    background.setCardBackgroundColor(ContextCompat.getColor(context, R.color.ReptileGreen))
                }
                else{
                    background.setCardBackgroundColor(ContextCompat.getColor(context, R.color.FusionRed))
                }
            }

            if(i % 2 == 1){
                content.addView(currentRow)
                currentRow = TableRow(context)
                currentRow.gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        if(currentRow.childCount > 0){
            content.addView(currentRow)
        }
        layout.addView(content)
    }

    override fun getFilePath(): List<String> {
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



