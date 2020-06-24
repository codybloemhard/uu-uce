package com.uu_uce.pins

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uu_uce.OpenGL.coordsPerVertex
import com.uu_uce.R
import com.uu_uce.allpins.PinListAdapter
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.defaultPinSize
import com.uu_uce.fieldbook.FieldbookAdapter
import com.uu_uce.fieldbook.FieldbookViewModel
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.pointInAABoundingBox
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.*
import com.uu_uce.shapefiles.p2
import com.uu_uce.shapefiles.p2Zero
import org.jetbrains.annotations.TestOnly
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.roundToInt

/**
 * Abstract pin class includes functionality for drawing the pin, and some abstract methods
 * @property[coordinate] the utm-coordinate at which the pin is located
 * @property[background] a bitmap of the pin that is to be drawn on the map
 * @property[icon] a drawable of the icon that is to be drawn on top of the background
 * @property[pinWidth] the width of the pin on the map, this is set automatically from settings
 */
abstract class Pin(
    val coordinate              : UTMCoordinate,
    protected var background    : Bitmap,
    private var icon            : Drawable,
    var pinWidth                : Float = defaultPinSize.toFloat()
){
    protected var completeRange = 100

    //opengl variables
    private var backgroundHandle: Int = -1
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer
    private lateinit var cubeCoordsBuffer: FloatBuffer

    private var spriteCoords = floatArrayOf(
        - 0.5f, + 1.0f,
        - 0.5f, - 0.0f,
        + 0.5f, - 0.0f,
        + 0.5f, + 1.0f
    )
    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)
    private val vertexStride: Int = coordsPerVertex * 4

    //some lengths are still needed after dismissing the icon/background, so we save them
    private val bitmapIconWidth = icon.intrinsicWidth.toFloat()
    private val bitmapIconHeight = icon.intrinsicHeight.toFloat()
    private val bitmapBackgroundWidth = background.width.toFloat()
    private val bitmapBackgroundHeight = background.height.toFloat()

    // Calculate pin height to maintain aspect ratio
    var pinHeight = pinWidth * (bitmapBackgroundHeight / bitmapBackgroundWidth)

    // Declare variables for icon size
    private var iconWidth  : Double = 0.0
    private var iconHeight : Double = 0.0

    // Initialize variables used in checking for clicks
    private var inScreen: Boolean = true
    private var boundingBox: Pair<p2, p2> = Pair(p2Zero, p2Zero)

    init {
        // Calculate icon measurements
        if (icon.intrinsicHeight > icon.intrinsicWidth) {
            iconHeight = pinHeight * 0.5
            iconWidth = iconHeight * (bitmapIconWidth / bitmapIconHeight)
        } else {
            iconWidth = pinWidth * 0.55
            iconHeight = iconWidth * (bitmapIconHeight / bitmapIconWidth)
        }
    }

    /**
     * To be called once by the GL thread, initializes all buffers related to this pin
     */
    open fun initGL() {
        if (this::vertexBuffer.isInitialized) return
        //initialize opengl drawing
        vertexBuffer =
            ByteBuffer.allocateDirect(spriteCoords.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(spriteCoords)
                    position(0)
                }
            }

        val cubeTextureCoordinateData = floatArrayOf(
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
        )
        cubeCoordsBuffer =
            ByteBuffer.allocateDirect(cubeTextureCoordinateData.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(cubeTextureCoordinateData)
                    position(0)
                }
            }

        indexBuffer =
            // (# of coordinate values * 2 bytes per short)
            ByteBuffer.allocateDirect(drawOrder.size * 2).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    put(drawOrder)
                    position(0)
                }
            }

        // Make background mutable
        background = if (background.isMutable) background else background.copy(
            Bitmap.Config.ARGB_8888,
            true
        )
        val canvas = Canvas(background)

        val iconX = canvas.width  * 0.5
        val iconY = canvas.height * 0.4
        val (localIconWidth, localIconHeight) = if(icon.intrinsicHeight > icon.intrinsicWidth){
            Pair(canvas.height * 0.5 * (bitmapIconWidth / bitmapIconHeight), canvas.height * 0.5)
        }
        else{
            Pair(canvas.width * 0.55, canvas.width * 0.55 * (bitmapIconHeight / bitmapIconWidth))
        }
        if(icon.intrinsicHeight > icon.intrinsicWidth){
            iconHeight = pinHeight * 0.5
            iconWidth = iconHeight * (bitmapIconWidth / bitmapIconHeight)
        } else {
            iconWidth = pinWidth * 0.55
            iconHeight = iconWidth * (bitmapIconHeight / bitmapIconWidth)
        }
        icon.setBounds(
            (iconX - localIconWidth / 2).toInt(),
            (iconY - localIconHeight / 2).toInt(),
            (iconX + localIconWidth / 2).toInt(),
            (iconY + localIconHeight / 2).toInt()
        )
        icon.draw(canvas)

        backgroundHandle = loadTexture(background)
    }

    /**
     * Determines whether tapLocation hits this pin or not
     *
     * @param[tapLocation] the location where a tap occurs
     * @return true if the tap hit the pin, false if it did not
     */
    protected fun isInside(tapLocation: p2): Boolean {
        return pointInAABoundingBox(boundingBox.first, boundingBox.second, tapLocation, 0)
    }

    /**
     * Function that is executed when this pin is tapped.
     */
    abstract fun tap(tapLocation: p2, activity: Activity, view: View, disPerPixel: Float)

    /**
     * Should create a list of links to all content in this pin
     * Used to create the list when a mergedPin is clicked
     */
    abstract fun addContent(): MutableList<String>

    /**
     * Create popup showing this pins content
     *
     * @param[parentView] the view that hosts the popup
     * @param[activity] the current activity
     */
    abstract fun openContent(parentView: View, activity: Activity)

    /**
     * Main drawing function, draws the pin and recalculates the bounding box
     *
     * @param[program] reference to the GL program to use
     * @param[scale] scale vector used to draw everything at the right size
     * @param[trans] translation vector to draw everything in the right place
     * @param[viewport] current viewport of the camera
     * @param[view] the view this is drawn in
     * @param[disPerPixel] the number of UTM units (meters?) that one pixel is wide
     * @return true if this pin still needs to be initialized, false if not
     */
    open fun draw(
        program: Int,
        scale: FloatArray,
        trans: FloatArray,
        viewport: Pair<p2, p2>,
        view: View,
        disPerPixel: Float
    ): Boolean {

        val screenLocation: Pair<Float, Float> =
            coordToScreen(coordinate, viewport, view.width, view.height)

        if (screenLocation.first.isNaN() || screenLocation.second.isNaN()) {
            Logger.error("Pin", "Pin draw called with NaN location")
            return false
        }

        // Calculate pin bounds on canvas
        val minX = (screenLocation.first - pinWidth / 2).roundToInt()
        val minY = (screenLocation.second - pinHeight).roundToInt()
        val maxX = (screenLocation.first + pinWidth / 2).roundToInt()
        val maxY = (screenLocation.second).roundToInt()

        // Check whether pin is out of screen
        if (minX > view.width || maxX < 0 || minY > view.height || maxY < 0) {
            Logger.log(LogType.Event, "Pin", "Pin outside of viewport")
            inScreen = false
            return false
        }
        inScreen = true

        // Set boundingbox for pin tapping
        boundingBox = Pair(p2(minX.toFloat(), minY.toFloat()), p2(maxX.toFloat(), maxY.toFloat()))

        if(!this::vertexBuffer.isInitialized) return true

        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, coordsPerVertex, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        val localtrans = floatArrayOf(trans[0] + coordinate.east, trans[1] + coordinate.north)
        val transHandle = GLES20.glGetUniformLocation(program, "trans")
        GLES20.glUniform2fv(transHandle, 1, localtrans, 0)

        val scaleHandle = GLES20.glGetUniformLocation(program, "scale")
        GLES20.glUniform2fv(scaleHandle, 1, scale, 0)

        val pinScale = floatArrayOf(pinWidth / view.width * 2, pinHeight / view.height * 2)
        val pinScaleHandle = GLES20.glGetUniformLocation(program, "pinScale")
        GLES20.glUniform2fv(pinScaleHandle, 1, pinScale, 0)

        val color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        val textureCoordinateHandle = GLES20.glGetAttribLocation(program, "a_TexCoordinate")
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, cubeCoordsBuffer)
        GLES20.glEnableVertexAttribArray(textureCoordinateHandle)

        val textureUniformHandle = GLES20.glGetAttribLocation(program, "u_Texture")
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundHandle)
        GLES20.glUniform1i(textureUniformHandle, 0)


        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle)

        return false
    }

    /**
     * Changes the width of the pin to pinSize, the height changes accordingly
     * This also changes the icon's size
     *
     * @param[pinSize] the desired width of the pin in pixels
     */
    open fun resize(pinSize: Int) {
        pinWidth = pinSize.toFloat()

        // Calculate pin height to maintain aspect ratio
        pinHeight =
            pinWidth * bitmapBackgroundHeight / bitmapBackgroundWidth

        // Calculate icon measurements
        if (bitmapIconHeight > bitmapIconWidth) {
            iconHeight = pinHeight * 0.5
            iconWidth = iconHeight * (bitmapIconWidth / bitmapIconHeight)
        } else {
            iconWidth = pinWidth * 0.55
            iconHeight = iconWidth * (bitmapIconHeight / bitmapIconWidth)
        }
    }

    /**
     * Helper function to load a bitmap into a buffer
     * @param[bitmap] the bitmap to be made into a texture
     * @return integer reference to the new texture
     */
    private fun loadTexture(bitmap: Bitmap): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        if (textureHandle[0] != 0) {
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

            // Set filtering
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST
            )

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle()
        }
        if (textureHandle[0] == 0) {
            Logger.error("Pin", "Error loading texture")
        }
        return textureHandle[0]
    }
}

/**
 * Singular pin, which shows its content when tapped.
 * @property[id] the id of the pin, should be UUID4 format.
 * @property[title] the title of the pin.
 * @property[content] a json format string containing the content of the pin.
 * @property[status] the current status of the pin.
 * @property[predecessorIds] the ids of pins that need to be completed before this pin is unlocked,
 *  in a string sepparated by commas(,).
 * @property[followIds] the ids of pins that follow this pin, formatted the same way as predecessors.
 * @property[viewModel] the viewModel the pin uses to acccess the database.
 * @constructor creates a single pin.
 */
class SinglePin(
    var id                      : String = "",
    coordinate                  : UTMCoordinate,
    var title                   : String,
    var content                 : PinContent,
    background                  : Bitmap,
    icon                        : Drawable,
    var status                  : Int, //-1 : recalculating, 0 : locked, 1 : unlocked, 2 : completed
    private var predecessorIds  : List<String>,
    private var followIds       : List<String>,
    private val viewModel       : ViewModel
): Pin(coordinate, background, icon) {
    // Used to determine if warning should show when closing pin
    private var madeProgress = false

    // Quiz
    private var answered : Array<Boolean>       = Array(content.contentBlocks.count()) { true }
    private var questionRewards : Array<Int>    = Array(content.contentBlocks.count()) { 0 }
    private var totalReward                     = 0

    var tapAction : ((Activity) -> Unit) = {}

    init{
        // Check if predecessors contain self
        predecessorIds.forEach { I ->
            if (I == id) error("Pin can not be own predecessor")
        }
    }

    override fun draw(
        program: Int,
        scale: FloatArray,
        trans: FloatArray,
        viewport: Pair<p2, p2>,
        view: View,
        disPerPixel: Float
    ): Boolean {
        // Check whether pin is unlocked
        if (status == 0) return false

        return super.draw(program, scale, trans, viewport, view, disPerPixel)
    }

    //show this pins content when tapped
    override fun tap(tapLocation: p2, activity: Activity, view: View, disPerPixel: Float){
        if(status <1) return
        if(isInside(tapLocation)) {
            run{
                tapAction(activity)
            }
        }
    }

    //final pin contains just one piece of content
    override fun addContent(): MutableList<String> {
        return mutableListOf(id)
    }

    /**
     * Check if pin should be unlocked and execute funcion when it is unlocked.
     * @param[action] the action to be executed when the pin is unlocked.
     */
    fun tryUnlock(action: (() -> Unit)) {
        if (predecessorIds[0] != "" && status < 1) {
            (viewModel as PinViewModel).tryUnlock(id, predecessorIds, action)
        } else {
            action()
        }
    }


    override fun openContent(parentView: View, activity: Activity) {
        val layoutInflater = activity.layoutInflater

        var popupWindow: PopupWindow? = null
        // Build an custom view (to be inflated on top of our current view & build it's popup window)
        val viewGroup: ViewGroup
        var newViewGroup: ViewParent =
            if (parentView is ViewParent) parentView else parentView.parent
        while (true) {
            if (newViewGroup is ViewGroup) {
                viewGroup = newViewGroup
                break
            }
            newViewGroup = newViewGroup.parent
        }
        val customView = layoutInflater.inflate(R.layout.pin_content_view, viewGroup, false)
        customView.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0 && popupWindow?.isShowing == true) {
                    popupWindow?.dismiss()
                    true
            }
            else false
        }

        popupWindow = PopupWindow(
            customView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        popupWindow.setBackgroundDrawable(ColorDrawable())
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        // Add the title for the popup window
        val windowTitle = customView.findViewById<TextView>(R.id.popup_window_title)
        windowTitle.text = title

        // Add content to popup window
        val layout: LinearLayout = customView.findViewById(R.id.scrollLayout)

        // Set up quiz
        resetQuestions()
        var containsQuiz = false

        // Get necessary files
        val fileList = mutableListOf<String>()
        for(block in content.contentBlocks){
            for(path in block.getFilePath()){
                fileList.add(path)
            }
        }

        // Gets files
        updateFiles(
            fileList,
            activity,
            {
                //TODO: add thumbnail generation
                activity.runOnUiThread{

                    // Generate content
                    for(i in 0 until content.contentBlocks.count()){
                        val current = content.contentBlocks[i]
                        current.showContent(i, layout, parentView, this)
                        if(current is MCContentBlock) containsQuiz = true
                    }

                    if(LocationServices.lastKnownLocation != null && status == 1 && !containsQuiz){
                        val dist = calculateDistance(degreeToUTM(p2(LocationServices.lastKnownLocation!!.latitude.toFloat(), LocationServices.lastKnownLocation!!.longitude.toFloat())), coordinate)
                        val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
                        completeRange = sharedPref.getInt("com.uu_uce.UNLOCKRANGE", 100)
                        if(dist < completeRange){
                            complete()
                        }
                    }

                    // Set completed visibility
                    val checkMark = customView.findViewById<ImageView>(R.id.completed_marker)
                    if(status == 2){
                        checkMark.visibility = VISIBLE
                    }
                    else{
                        checkMark.visibility = GONE
                    }

                    // Fill layout of popup
                    if(containsQuiz && status < 2){
                        val finishButton = Button(activity)
                        finishButton.id = R.id.finish_quiz_button
                        finishButton.text = activity.getString(R.string.pin_finish)
                        finishButton.setTextColor(ResourcesCompat.getColor(activity.resources, R.color.TextDarkGrey, null))
                        finishButton.isAllCaps = false
                        finishButton.setBackgroundResource(R.drawable.custom_border_button)
                        val buttonLayout = LinearLayout.LayoutParams(
                            TableRow.LayoutParams.MATCH_PARENT,
                            TableRow.LayoutParams.WRAP_CONTENT
                        )
                        buttonLayout.setMargins(parentView.width / 7, parentView.height / 50, parentView.width / 7, parentView.height / 50)
                        finishButton.layoutParams = buttonLayout
                        finishButton.setOnClickListener{
                            finishQuizzes(activity, parentView)
                            popupWindow.dismiss()
                        }
                        layout.addView(finishButton)
                    }

                    // Open popup
                    popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0)

                    // Get elements
                    val btnClosePopupWindow = customView.findViewById<Button>(R.id.popup_window_close_button)

                    // Set onClickListeners
                    btnClosePopupWindow.setOnClickListener {
                        if(madeProgress){
                            AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                                .setIcon(R.drawable.ic_sprite_warning)
                                .setTitle(activity.getString(R.string.pin_close_warning_head))
                                .setMessage(activity.getString(R.string.pin_close_warning_body))
                                .setPositiveButton(activity.getString(R.string.positive_button_text)) { _, _ -> popupWindow.dismiss() }
                                .setNegativeButton(activity.getString(R.string.negative_button_text), null)
                                .show()
                        } else {
                            popupWindow.dismiss()
                        }
                    }
                }
            },
            {}
        )
    }

    /**
     * Completes the current pin and informs following pins that it has been completed.
     */
    private fun complete() {
        status = 2
        (viewModel as PinViewModel).completePin(id, followIds)
    }

    /**
     * Adds a pin to the current pin.
     * @param[questionId] the id of the question to be added.
     * @param[reward] the available reward for completing the new quiz.
     */
    fun addQuestion(questionId: Int, reward: Int) {
        answered[questionId] = false
        totalReward += reward
    }

    /**
     * Marks a quiz as answered.
     * @param[questionId] the quiz to be marked as answered.
     * @param[reward] the reward that has been earned by answering.
     */
    fun answerQuestion(questionId: Int, reward: Int) {
        questionRewards[questionId] = reward
        answered[questionId] = true
        madeProgress = true
    }

    /**
     * Resets all quizzes in the pin.
     */
    private fun resetQuestions() {
        questionRewards.map { 0 }
        totalReward = 0
        madeProgress = false
        answered.map { true }
    }

    /**
     * Finishes all quizzes in the pin.
     * @param[activity] the current activity.
     * @param[parentView] the view in which the result popup should be opened.
     */
    private fun finishQuizzes(activity: Activity, parentView: View) {
        if (answered.all { b -> b }) {
            // All questions answered
            val reward = questionRewards.sum()

            var sufficient = false
            if (reward >= 0.55 * totalReward) {
                sufficient = true
                complete()

                val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
                val prevPoints = sharedPref.getInt("com.uu_uce.USER_POINTS", 0)
                with(sharedPref.edit()) {
                    putInt("com.uu_uce.USER_POINTS", prevPoints + reward)
                    apply()
                }
            }

            // Open popup
            val layoutInflater = activity.layoutInflater

            // Build an custom view (to be inflated on top of our current view & build it's popup window)
            val customView = layoutInflater.inflate(R.layout.quiz_complete_popup, parentView.parent as ViewGroup, false)

            val popupWindow = PopupWindow(
                customView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Open popup
            popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0)

            // Get elements
            val georgeReaction      = customView.findViewById<ImageView>(R.id.george_reaction)
            val quizResultText      = customView.findViewById<TextView>(R.id.quiz_result_text)
            val completeText        = customView.findViewById<TextView>(R.id.complete_text)
            val btnClosePopupWindow = customView.findViewById<Button>(R.id.close_button)
            val btnOpenQuiz         = customView.findViewById<Button>(R.id.reopen_button)
            val rewardText          = customView.findViewById<TextView>(R.id.reward_text)
            val rewardLayout        = customView.findViewById<LinearLayout>(R.id.reward_layout)

            // Set content based on result
            if(sufficient){
                georgeReaction.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_george_celebrating, null))
                quizResultText.text     = activity.getString(R.string.pin_quiz_success_head)
                completeText.text       = activity.getString(R.string.pin_quiz_success_body, title, reward, totalReward)
                btnOpenQuiz.text        = activity.getString(R.string.pin_quiz_reopen_button_success)
                rewardLayout.visibility = VISIBLE
                rewardText.text         = activity.getString(R.string.pin_reward_string, reward)
            }
            else{
                georgeReaction.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.ic_george_sad, null))
                quizResultText.text     = activity.getString(R.string.pin_quiz_fail_head)
                completeText.text       = activity.getString(R.string.pin_quiz_fail_body)
                btnOpenQuiz.text        = activity.getString(R.string.pin_quiz_reopen_button_fail)
                rewardLayout.visibility = GONE
            }

            // Set buttons
            btnClosePopupWindow.setOnClickListener {
                popupWindow.dismiss()
            }

            btnOpenQuiz.setOnClickListener {
                popupWindow.dismiss()

                openContent(parentView, activity)
            }
        }
        else{
            // Questions left unanswered
            Toast.makeText(activity, activity.getString(R.string.pin_missing_answer_message), Toast.LENGTH_SHORT).show()
        }
    }

    @TestOnly
    fun getScreenLocation(viewport: Pair<p2, p2>, width : Int, height : Int) : Pair<Float, Float>{
        return coordToScreen(coordinate, viewport, width, height)
    }
}

/**
 * a merged pin contains two pins a,b which can be merged pins too, creating a tree-structure
 * a and b are drawn/tapped seperately when their hitboxes don't collide
 * a merged pin is drawn/tapped when they do collide
 *
 * @property[a] one of the two children
 * @property[b] the other child
 * @property[actualDis] distance between a and b, but only in the direction in which they will collide, which is the same direction as for nrPixels this will be difference in x coordinate if they approach each other horizontally and difference in y coordinate if they approach each other vertically
 * @property[nrPixels] minimum number of pixels needed between a and b until they collide
 * @property[pinViewModel] the viewModel used to access the database in GeoMap
 * @property[fieldbookViewModel] the viewModel used to access the database in Fieldbook
 * @constructor creates a MergedPin
 */
class MergedPin(
    private val a: Pin?,
    private val b: Pin?,
    private val actualDis: Float,
    private val nrPixels: Float,
    private val pinViewModel: PinViewModel?,
    private val fieldbookViewModel: FieldbookViewModel?,
    coordinate: UTMCoordinate,
    background: Bitmap,
    icon: Drawable,
    pinSize: Float = defaultPinSize.toFloat()
): Pin(coordinate, background, icon, pinSize) {

    /**
     * draw both subpins if they don't collide, draw merge pin if they do
     */
    override fun draw(
        program: Int,
        scale: FloatArray,
        trans: FloatArray,
        viewport: Pair<p2, p2>,
        view: View,
        disPerPixel: Float
    ): Boolean {
        return if (nrPixels * disPerPixel < actualDis) {
            var res = a?.draw(program, scale, trans, viewport, view, disPerPixel) ?: false
            res = res || b?.draw(program, scale, trans, viewport, view, disPerPixel) ?: false
            res
        } else {
            super.draw(program, scale, trans, viewport, view, disPerPixel)
        }
    }

    /**
     * initialize both subpins
     */
    override fun initGL() {
        a?.initGL()
        b?.initGL()
        super.initGL()
    }

    /**
     * tap both subpins if they don't collide, tap merge pin if they do
     */
    override fun tap(tapLocation: p2, activity: Activity, view: View, disPerPixel: Float) {
        if (nrPixels * disPerPixel < actualDis) {
            a?.tap(tapLocation, activity, view, disPerPixel)
            b?.tap(tapLocation, activity, view, disPerPixel)
        } else {
            if (!isInside(tapLocation)) return
            openContent(view, activity)
        }
    }

    /**
     * add content of both subpins
     */
    override fun addContent(): MutableList<String> {
        val alist = a?.addContent() ?: mutableListOf()
        val blist = b?.addContent() ?: mutableListOf()
        alist.addAll(blist)
        return alist
    }

    /**
     * create a popup containing all of the pins inside this pin, in the same style as AllPins
     */
    override fun openContent(parentView: View, activity: Activity) {
        val layoutInflater = activity.layoutInflater

        var popupWindow: PopupWindow? = null

        // Build an custom view (to be inflated on top of our current view & build it's popup window)
        val customView =
            layoutInflater.inflate(R.layout.merged_pin_popup, parentView.parent as ViewGroup, false)
        customView.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0 && popupWindow?.isShowing == true) {
                popupWindow?.dismiss()
                true
            }
            else false
        }

        popupWindow = PopupWindow(
            customView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        popupWindow.setBackgroundDrawable(ColorDrawable())
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        // Add the title for the popup window
        val windowTitle = customView.findViewById<TextView>(R.id.popup_window_title)
        windowTitle.text = activity.getString(R.string.merged_pin_name)

        val btnClosePopupWindow = customView.findViewById<Button>(R.id.popup_window_close_button)
        btnClosePopupWindow.setOnClickListener {
                popupWindow.dismiss()
        }

        val ids =  addContent()
        val viewManager = LinearLayoutManager(activity)
        if(pinViewModel != null) {
            val pinViewAdapter = PinListAdapter(activity)
            pinViewAdapter.view = parentView

            customView.findViewById<RecyclerView>(R.id.allpins_recyclerview).apply {
                layoutManager = viewManager
                adapter = pinViewAdapter
            }

            pinViewModel.getPins(ids) { pindatas ->
                pinViewAdapter.setPins(pindatas,pinViewModel)
            }
        }
        if(fieldbookViewModel != null) {
            val fieldbookViewAdapter = FieldbookAdapter(activity, fieldbookViewModel, parentView)
            fieldbookViewAdapter.view = parentView

            customView.findViewById<RecyclerView>(R.id.allpins_recyclerview).apply {
                layoutManager = viewManager
                adapter = fieldbookViewAdapter
            }

            fieldbookViewModel.getPins(ids) { pindatas ->
                fieldbookViewAdapter.setFieldbook(pindatas)
            }
        }

        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0)
    }

    /**
     * resize both subpins
     */
    override fun resize(pinSize: Int) {
        a?.resize(pinSize)
        b?.resize(pinSize)
        super.resize(pinSize)
    }
}
