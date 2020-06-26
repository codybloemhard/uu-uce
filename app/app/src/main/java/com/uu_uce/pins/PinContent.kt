package com.uu_uce.pins

import android.app.Activity
import android.content.Intent
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
import com.uu_uce.ImageViewer
import com.uu_uce.R
import com.uu_uce.VideoViewer
import com.uu_uce.contentFolderName
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.MediaServices
import java.io.File
import java.io.StringReader

/**
 * The content of a pin.
 * @property[contentString] a json format string containing the content of the pin.
 * @property[activity] the current activity.
 * @property[fieldbookPin] a boolean representing if the current pin is a fieldbook or regular pin.
 * @constructor parses the contentString and creates a PinContent object with its contents.
 */
class PinContent(
    private val contentString: String,
    private val activity: Activity,
    private val fieldbookPin: Boolean
) {
    val contentBlocks: MutableList<ContentBlock>

    lateinit var parent: SinglePin

    init {
        contentBlocks = getContent()
    }

    /**
     * Parses the contentString and splits its content into multiple blocks.
     * @return list of content blocks created from contentString.
     */
    private fun getContent(): MutableList<ContentBlock> {
        val reader = JsonReader(StringReader(contentString))

        return readContentBlocks(reader)
    }

    /**
     * Parse all ContentBlocks from the contentString
     * @param[reader] the json reader that is reading the contentString.
     * @return a list of all ContentBlocks parsed from the contentString.
     */
    private fun readContentBlocks(reader: JsonReader): MutableList<ContentBlock> {
        val contentBlocks: MutableList<ContentBlock> = mutableListOf()

        reader.beginArray()
        while (reader.hasNext()) {
            val curBlock = readBlock(reader)
            if (curBlock != null) {
                contentBlocks.add(curBlock)
            }
        }
        reader.endArray()
        return contentBlocks
    }

    /**
     * Parse a single ContentBlock from json string.
     * @param[reader] the json reader that is reading the contentString.
     * @return a nullable ContentBlock that was parsed, null when an malformed block was parsed.
     */
    private fun readBlock(reader: JsonReader): ContentBlock? {
        var blockTag = BlockTag.UNDEFINED
        var textString = ""
        var filePath = ""
        val filePathBuilder = StringBuilder()
        var title = ""
        var thumbnailURI = ""
        val thumbnailURIBuilder = StringBuilder()
        val mcCorrectOptions: MutableList<String> = mutableListOf()
        val mcIncorrectOptions: MutableList<String> = mutableListOf()
        var reward = 0

        if(!fieldbookPin){
            filePathBuilder.append(activity.getExternalFilesDir(null)?.path + File.separator + contentFolderName + File.separator)
            thumbnailURIBuilder.append(activity.getExternalFilesDir(null)?.path + File.separator + contentFolderName + File.separator)
        }

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
                    when(blockTag) {
                        BlockTag.UNDEFINED  -> {
                            Logger.error("PinContent", "Tag needs to be specified before file_path")
                        }
                        BlockTag.TEXT       -> {
                            Logger.log(LogType.NotImplemented, "PinContent", "file reading not implemented")
                            reader.nextString()
                        }
                        BlockTag.IMAGE      -> filePath = reader.nextString()
                        BlockTag.VIDEO      -> filePath = reader.nextString()
                        BlockTag.MCQUIZ     -> {
                            Logger.error("PinContent", "multiple choice quiz can not be read from file")
                            reader.nextString()
                        }
                    }
                }

                "title" -> {
                    title = reader.nextString()
                }
                "thumbnail" -> {
                    thumbnailURI = reader.nextString()
                }
                "mc_correct_option" -> {
                    mcCorrectOptions.add(reader.nextString())
                }
                "mc_incorrect_option" -> {
                    mcIncorrectOptions.add(reader.nextString())
                }
                "reward" -> {
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
        if(thumbnailURI != ""){
            thumbnailURI = thumbnailURIBuilder.append(thumbnailURI).toString()
        }
        if(filePath != ""){
            filePath = filePathBuilder.append(filePath).toString()
        }
        return when(blockTag){
            BlockTag.UNDEFINED  -> {
                Logger.error("PinContent", "No BlockTag specified")
                return null
            }
            BlockTag.TEXT   -> TextContentBlock(textString, activity)
            BlockTag.IMAGE  -> {
                if(filePath != "") {
                    ImageContentBlock(Uri.parse(filePath), Uri.parse(thumbnailURI), activity, title)
                } else null
            }
            BlockTag.VIDEO  -> {
                if(filePath != ""){
                    VideoContentBlock(Uri.parse(filePath), Uri.parse(thumbnailURI), activity, title)
                } else null
            }
            BlockTag.MCQUIZ -> {
                if(mcIncorrectOptions.count() < 1 && mcCorrectOptions.count() < 1) {
                    Logger.error(
                        "PinContent",
                        "Mutliple choice questions require at least one correct and one incorrect answer")
                    return null
                }
                MCContentBlock( mcCorrectOptions, mcIncorrectOptions, reward, activity)
            }
        }
    }
}

/**
 * An interface for ContentBlocks.
 * @property[content] the view that is added to a pin popup when this block is present.
 * @property[tag] the type of the ContentBlock.
 */
interface ContentBlock {
    val content: View
    val tag: BlockTag

    /**
     * Generates all views that will be shown in the pin popup.
     * @param[blockId] the id that this block is assigned.
     * @param[layout] the layout that this blocks content should be added to.
     * @param[view] the current view of the app, used for scaling.
     * @param[parent] the pin that this ContentBlock belongs to.
     */
    fun showContent(blockId: Int, layout: LinearLayout, view: View, parent: SinglePin?)

    /**
     * Makes a ContentBlock in a fieldbook pin editable.
     * @param[blockId] the id that this block is assigned.
     * @param[layout] the layout that this blocks content should be added to.
     * @param[view] the current view of the app, used for scaling.
     * @param[action] the action to be exectuted with the editable ContentBlock.
     * @return the editable ContentBlock.
     */
    fun makeEditable(
        blockId: Int,
        layout: LinearLayout,
        view: View,
        action: ((ContentBlock) -> Boolean)
    ): ContentBlock {
        showContent(blockId, layout, view, null)
        content.setOnLongClickListener {
            return@setOnLongClickListener action(this)
        }
        return this
    }

    /**
     * Removes this ContentBlock from the content of a pin.
     * @param[layout] the layout this ContentBlock is currently in.
     */
    fun removeContent(layout: LinearLayout) = layout.removeView(content)

    /**
     * Gets the file paths of all content in this ContentBlock.
     * @return a list of file paths in string format.
     */
    fun getFilePath(): List<String> {
        return listOf()
    }

    /**
     * Generates a string which can be stored in the database for fieldbook pins.
     * @return the json string generated from this ContentBlock.
     */
    override fun toString(): String
}

/**
 * A ContentBlock which contains an editable textfield.
 */
class EditTextContentBlock(private val activity: Activity) : ContentBlock {
    override var content = EditText(activity)
    override val tag = BlockTag.TEXT
    override fun showContent(blockId: Int, layout: LinearLayout, view: View, parent: SinglePin?) {
        content = EditText(activity).apply {
            inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isSingleLine = false
            imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION
            background =
                ResourcesCompat.getDrawable(activity.resources, R.drawable.custom_border_edgy, null)
            id = R.id.text_field
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 10)
            }
        }

        layout.addView(content,blockId)
    }

    override fun toString() : String {
        return "{${tagToJsonString(tag)}," + "${textToJsonString(content.text.toString())}}"
    }
}

/**
 * A ContentBlock which contains a textfield.
 * @param[textContent] a string of text.
 * @param[activity] the current activity.
 * @constructor a TextContentBlock.
 */
class TextContentBlock(
    private val textContent: String,
    private val activity: Activity
) : ContentBlock {
    override var content = TextView(activity)
    override val tag = BlockTag.TEXT
    override fun showContent(blockId: Int, layout: LinearLayout, view: View, parent: SinglePin?) {
        content = TextView(activity).apply {
            text = textContent
            setPadding(12, 12, 12, 20)
            gravity = Gravity.CENTER_HORIZONTAL
        }.also {
            layout.addView(it, blockId)
        }
    }

    override fun makeEditable(
        blockId: Int,
        layout: LinearLayout,
        view: View,
        action: (ContentBlock) -> Boolean
    ): ContentBlock {
        val editable = EditTextContentBlock(activity)
        editable.makeEditable(blockId, layout, view, action)
        editable.content.setText(textContent)
        return editable
    }

    override fun toString(): String {
        return "{${tagToJsonString(tag)}," +
                "${textToJsonString(textContent)}}"
    }

    /**
     * Returns the text that is in this block.
     * @return the text contained in this TextContentBlock.
     */
    fun getTextContent(): String {
        return textContent
    }
}

/**
 * A ContentBlock which contains an image
 * @param[imageURI] the Uri of the image on the device.
 * @param[thumbnailURI] the thumbnail of the image, this is only used in the fieldbook when the original image is no longer available.
 * @param[activity] the current activity.
 * @param[title] the title of the image.
 * @constructor an ImageContentBlock.
 */
class ImageContentBlock(
    private val imageURI: Uri,
    private val thumbnailURI: Uri,
    private val activity: Activity,
    private val title: String? = null
) : ContentBlock {
    override var content = ImageView(activity)
    override val tag = BlockTag.IMAGE
    override fun showContent(blockId: Int, layout: LinearLayout, view: View, parent: SinglePin?) {
        content = ImageView(activity)
        try {
            content.apply {
                setImageURI(imageURI)
                content.setOnClickListener {
                    openImageView(activity, imageURI, title)
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
            }
        } catch (e: Exception) {
            Logger.error("PinContent","Couldn't load $imageURI, so loaded the thumbnail $thumbnailURI instead")
            content.setOnClickListener{
                openImageView(activity, thumbnailURI, title)
            }
            content.setImageURI(thumbnailURI)
        }
        val imageLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 10)
        }
        content.layoutParams = imageLayoutParams
        content.id = R.id.image_block

        layout.addView(content,blockId)
    }

    override fun removeContent(layout: LinearLayout) {
        super.removeContent(layout)
        totallyExterminateFileExistence(activity, thumbnailURI)
    }

    override fun getFilePath(): List<String> {
        return listOf(imageURI.toString())
    }

    override fun toString(): String {
        return "{${tagToJsonString(tag)}," +
                "${fileToJsonString(imageURI)}," +
                "${thumbnailToJsonString(thumbnailURI)}}"
    }

    /**
     * Returns the Uri of the thumbnail.
     * @return thumbnail Uri.
     */
    fun getThumbnailURI(): Uri {
        return thumbnailURI
    }
}

/**
 * A ContentBlock which contains a video.
 * @param[videoURI] the Uri of the video on the device.
 * @param[thumbnailURI] the Uri of the video on the device.
 * @param[activity] the current activity.
 * @param[title] the title of the video.
 * @constructor a VideoContentBlock.
 */
class VideoContentBlock(
    private val videoURI: Uri,
    private var thumbnailURI: Uri,
    private val activity: Activity,
    private val title: String? = null
) : ContentBlock {
    companion object {
        private const val THUMBNAIL_DIRECTORY = "PinContent/Videos/Thumbnails"
    }

    override var content = FrameLayout(activity)
    override val tag = BlockTag.VIDEO
    override fun showContent(blockId: Int, layout: LinearLayout, view: View, parent: SinglePin?) {
        content = FrameLayout(activity)

        // Create thumbnail image
        if (thumbnailURI == Uri.EMPTY) {
            thumbnailURI =
                MediaServices(activity).generateMissingVideoThumbnail(videoURI, THUMBNAIL_DIRECTORY)
        }

        val thumbnail = ImageView(activity)
        thumbnail.setImageURI(thumbnailURI)
        thumbnail.scaleType = ImageView.ScaleType.FIT_CENTER
        thumbnail.adjustViewBounds = true
        content.addView(thumbnail)

        content.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 10)
        }
        content.id = R.id.video_block

        // Create play button
        val playButton = ImageView(activity)
        playButton.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_sprite_play, null) ?: error ("Image not found"))
        playButton.scaleType = ImageView.ScaleType.CENTER_INSIDE
        val buttonLayout = FrameLayout.LayoutParams(view.width / 3, view.width / 3)
        buttonLayout.gravity = Gravity.CENTER
        playButton.layoutParams = buttonLayout

        // Add thumbnail and button
        content.addView(playButton)
        content.setOnClickListener {
            openVideoView(activity, videoURI, title)
        }
        layout.addView(content, blockId)
    }

    override fun removeContent(layout: LinearLayout) {
        super.removeContent(layout)
        totallyExterminateFileExistence(activity, thumbnailURI)
    }

    override fun getFilePath(): List<String>{
        if (thumbnailURI == Uri.EMPTY) return listOf(videoURI.toString())
        return listOf(thumbnailURI.toString(), videoURI.toString())
    }

    override fun toString(): String {
        return "{${tagToJsonString(tag)}," +
                "${fileToJsonString(videoURI)}," +
                "${thumbnailToJsonString(thumbnailURI)}}"
    }

    /**
     * Returns the Uri of the thumbnail.
     * @return thumbnail Uri.
     */
    fun getThumbnailURI(): Uri {
        return thumbnailURI
    }
}

/**
 * A ContentBlock which contains a multiple choice quiz.
 * @param[correctAnswers] a list of correct answers to the question.
 * @param[incorrectAnswers] a list of incorrect answers to the question.
 * @param[reward] the reward the user recieves upon correctly answering the questions in this pin.
 * @param[activity] the current activity.
 * @constructor a MCContentBlock.
 */
class MCContentBlock(
    private val correctAnswers: List<String>,
    private val incorrectAnswers: List<String>,
    private val reward: Int,
    private val activity: Activity
) : ContentBlock {
    override var content = TableLayout(activity)
    override val tag = BlockTag.MCQUIZ
    private var selectedAnswer: Int = -1
    private lateinit var selectedBackground: CardView
    override fun showContent(blockId: Int, layout: LinearLayout, view: View, parent: SinglePin?) {
        content = TableLayout(activity)

        if (parent == null) {
            Logger.error(
                "PinContent",
                "Mutliple choice quizzes can't be generated without a parent pin"
            )
            return
        }

        selectedBackground = CardView(activity)
        parent.addQuestion(blockId, reward)

        val answers : MutableList<Pair<String, Boolean>> = mutableListOf()
        for(answer in correctAnswers) answers.add(Pair(answer, true))
        for(answer in incorrectAnswers) answers.add(Pair(answer, false))

        val shuffledAnswers = answers.shuffled()

        // Create tableLayout with first row
        content.id = R.id.multiple_choice_table
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

            if(parent.status < 2){
                background.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.Boyzone))
                currentFrame.setOnClickListener {
                    selectedBackground.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.Boyzone))
                    selectedAnswer = i
                    selectedBackground = background
                    background.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.OrangeHibiscus))
                    if(shuffledAnswers[i].second){
                        parent.answerQuestion(blockId, reward)
                    } else{
                        parent.answerQuestion(blockId, 0)
                    }
                }
            } else{
                if(shuffledAnswers[i].second){
                    background.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.ReptileGreen))
                } else{
                    background.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.FusionRed))
                }
            }

            if(i % 2 == 1){
                content.addView(currentRow)
                currentRow = TableRow(activity)
                currentRow.gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        if(currentRow.childCount > 0){
            content.addView(currentRow)
        }
        layout.addView(content,blockId)
    }

    override fun makeEditable(
        blockId: Int,
        layout: LinearLayout,
        view: View,
        action: (ContentBlock) -> Boolean
    ): ContentBlock {
        return this
    }

    override fun removeContent(layout: LinearLayout) {

    }

    override fun toString(): String {
        return ""
    }
}

/**
 * The different types of pins.
 */
enum class BlockTag{
    UNDEFINED,
    TEXT,
    IMAGE,
    MCQUIZ,
    VIDEO;
}

/**
 * Converts a string to a BlockTag.
 * @param[tagString] a string which can be parsed to a BlockTag.
 * @return the blocktag parsed from tagString.
 */
fun blockTagFromString(tagString: String) : BlockTag{
    return when (tagString) {
        "TEXT"      -> BlockTag.TEXT
        "IMAGE"     -> BlockTag.IMAGE
        "VIDEO"     -> BlockTag.VIDEO
        "MCQUIZ"    -> BlockTag.MCQUIZ
        else        -> BlockTag.UNDEFINED
    }
}

/**
 * Creates a json format string for a list of content which can be stored in the database.
 * @param[content] the content that is to be converted to a json string.
 * @return the json format string generated from the supplied content.
 */
fun buildJSONContent(content: List<ContentBlock>): String {
    return content.joinToString(
        prefix = "[",
        separator = ",",
        postfix = "]"
    )
}

/**
 * Creates a partial json string for a BlockTag.
 * @param[tag] the tag to be embedded into the json string.
 * @return a partial json string containing the BlockTag.
 */
fun tagToJsonString(tag: BlockTag): String {
    return "\"tag\":\"$tag\""
}

/**
 * Creates a partial json string for text content.
 * @param[text] the text to be embedded into the json string.
 * @return a partial json string containing the text content.
 */
fun textToJsonString(text: String): String {
    return "\"text\":\"$text\""
}

/**
 * Creates a partial json string for a file path.
 * @param[filePath] the file path to be embedded into the json string.
 * @return a partial json string containing the file path.
 */
fun fileToJsonString(filePath: Uri): String {
    return "\"file_path\":\"$filePath\""
}

/**
 * Creates a partial json string for a thumbnail file path.
 * @param[thumbnail] the thumbnail file path to be embedded into the json string.
 * @return a partial json string containing the thumbnail file path.
 */
fun thumbnailToJsonString(thumbnail: Uri): String {
    return "\"thumbnail\":\"$thumbnail\""
}

/**
 * Removes a file completely from device storage.
 * @param[activity] the current activity.
 * @param[filePath] the path to the file that is to be deleted.
 */
fun totallyExterminateFileExistence(activity: Activity, filePath: Uri) {
    val toBeDeleted = File(filePath.path!!)
    if (toBeDeleted.exists()) {
        if (toBeDeleted.delete()) {
            if (toBeDeleted.exists()) {
                toBeDeleted.canonicalFile.delete()
                if (toBeDeleted.exists())
                    activity.deleteFile(toBeDeleted.name)
            }
            Logger.log(LogType.Event, "PinContent", "Thumbnail deleted $filePath")
        } else {
            Logger.log(LogType.Info, "PinContent", "Thumbnail not deleted $filePath")
        }
    } else {
        Logger.log(LogType.Info,"PinContent","This thumbnail doesn't exist")
    }
}

/**
 * Opens the ImageViewer activity with the specified image.
 * @param[activity] the current activity.
 * @param[imageURI] the image to be shown in the ImageViewer.
 * @param[imageTitle] the title of the image that is to be shown.
 */
fun openImageView(activity: Activity, imageURI: Uri, imageTitle : String?){
    val intent = Intent(activity, ImageViewer::class.java)

    intent.putExtra("uri", imageURI)
    if(imageTitle != null)
        intent.putExtra("title", imageTitle)
    activity.startActivity(intent)
}

/**
 * Opens the VideoViewer activity with the specified image.
 * @param[activity] the current activity.
 * @param[videoURI] the video to be shown in the VideoViewer.
 * @param[videoTitle] the title of the video that is to be shown.
 */
fun openVideoView(activity: Activity, videoURI: Uri, videoTitle: String?){
    val intent = Intent(activity, VideoViewer::class.java)

    intent.putExtra("uri", videoURI)
    if(videoTitle != null)
        intent.putExtra("title", videoTitle)
    activity.startActivity(intent)
}





