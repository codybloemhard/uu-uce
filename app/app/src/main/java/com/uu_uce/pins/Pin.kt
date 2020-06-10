package com.uu_uce.pins

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.uu_uce.OpenGL.coordsPerVertex
import com.uu_uce.R
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.defaultPinSize
import com.uu_uce.mapOverlay.coordToScreen
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

open class Pin(
    var id                      : String = "",
    var coordinate              : UTMCoordinate,
    var title           : String,
    var content         : PinContent,
    protected var background      : Bitmap,
    protected var icon            : Drawable,
    var status          : Int,              //-1 : recalculating, 0 : locked, 1 : unlocked, 2 : completed
    protected var predecessorIds  : List<String>,
    protected var followIds       : List<String>,
    protected val viewModel       : ViewModel
){
    // Used to determine if warning should show when closing pin
    private var madeProgress = false

    // Set default pin size
    private var pinWidth = defaultPinSize.toFloat()
    private var completeRange = 100

    //opengl stuff
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

    private val bitmapIconWidth = icon.intrinsicWidth.toFloat()
    private val bitmapIconHeight = icon.intrinsicHeight.toFloat()
    private val bitmapBackgroundWidth = background.width.toFloat()
    private val bitmapBackgroundHeight = background.height.toFloat()

    // Calculate pin height to maintain aspect ratio
    private var pinHeight =
        pinWidth * (bitmapBackgroundHeight / bitmapBackgroundWidth)

    // Declare variables for icon size
    private var iconWidth  : Double = 0.0
    private var iconHeight : Double = 0.0

    init {
        // Check if predecessors contain self
        predecessorIds.forEach { I ->
            if (I == id) error("Pin can not be own predecessor")
        }

        // Calculate icon measurements
        if(icon.intrinsicHeight > icon.intrinsicWidth){
            iconHeight = pinHeight * 0.5
            iconWidth = iconHeight * (bitmapIconWidth / bitmapIconHeight)
        }
        else{
            iconWidth = pinWidth * 0.55
            iconHeight = iconWidth * (bitmapIconHeight / bitmapIconWidth)
        }
    }

    // Initialize variables used in checking for clicks
    var inScreen: Boolean = true
    var boundingBox: Pair<p2, p2> = Pair(p2Zero, p2Zero)

    var popupWindow: PopupWindow? = null

    var tapAction : ((Activity) -> Unit) = {}

    // Quiz
    private var answered : Array<Boolean>       = Array(content.contentBlocks.count()) { true }
    private var questionRewards : Array<Int>    = Array(content.contentBlocks.count()) { 0 }
    private var totalReward                     = 0

    private var initialized = false

    fun initGL(){
        if(initialized) return
        initialized = true
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
        }
        else{
            iconWidth = pinWidth * 0.55
            iconHeight = iconWidth * (bitmapIconHeight / bitmapIconWidth)
        }
        icon.setBounds((iconX - localIconWidth / 2).toInt(), (iconY - localIconHeight / 2).toInt(), (iconX + localIconWidth / 2).toInt(), (iconY + localIconHeight / 2).toInt())
        icon.draw(canvas)

        backgroundHandle = loadTexture(background)
    }

    open fun draw(program: Int, scale: FloatArray, trans: FloatArray, viewport: Pair<p2,p2>, width : Int, height : Int, view: View) {

        val screenLocation: Pair<Float, Float> = coordToScreen(coordinate, viewport, view.width, view.height)

        if(screenLocation.first.isNaN() || screenLocation.second.isNaN()){
            Logger.error("Pin", "Pin draw called with NaN location")
            return
        }

        // Calculate pin bounds on canvas
        val minX = (screenLocation.first - pinWidth / 2).roundToInt()
        val minY = (screenLocation.second - pinHeight).roundToInt()
        val maxX = (screenLocation.first + pinWidth / 2).roundToInt()
        val maxY = (screenLocation.second).roundToInt()

        // Check whether pin is unlocked
        if (status == 0) return

        // Check whether pin is out of screen
        if (minX > width || maxX < 0 || minY > height || maxY < 0) {
            Logger.log(LogType.Event, "Pin", "Pin outside of viewport")
            inScreen = false
            return
        }
        inScreen = true

        // Set boundingbox for pin tapping
        boundingBox = Pair(p2(minX.toFloat(), minY.toFloat()), p2(maxX.toFloat(), maxY.toFloat()))

        if(!this::vertexBuffer.isInitialized) return

        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, coordsPerVertex, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        val localtrans = floatArrayOf(trans[0] + coordinate.east.toFloat(), trans[1] + coordinate.north.toFloat())
        val transHandle = GLES20.glGetUniformLocation(program, "trans")
        GLES20.glUniform2fv(transHandle, 1, localtrans, 0)

        val scaleHandle = GLES20.glGetUniformLocation(program, "scale")
        GLES20.glUniform2fv(scaleHandle, 1, scale, 0)

        val pinScale = floatArrayOf(pinWidth / width * 2, pinHeight / height * 2)
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


        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle)
    }

    // Check if pin should be unlocked
    fun tryUnlock(action : (() -> Unit)){
        if(predecessorIds[0] != "" && status < 1){
            (viewModel as PinViewModel).tryUnlock(id, predecessorIds, action)
        }
        else{
            action()
        }
    }

    fun openContent(parentView: View, activity : Activity, onDissmissAction: () -> Unit) {
        val layoutInflater = activity.layoutInflater

        // Build an custom view (to be inflated on top of our current view & build it's popup window)
        val customView = layoutInflater.inflate(R.layout.pin_content_view, parentView.parent as ViewGroup, false)

        popupWindow = PopupWindow(
            customView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        popupWindow?.setOnDismissListener {
            popupWindow = null

            onDissmissAction()
        }

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
                            finishQuiz(activity, parentView)
                        }
                        layout.addView(finishButton)
                    }

                    // Open popup
                    popupWindow?.showAtLocation(parentView, Gravity.CENTER, 0, 0)

                    // Get elements
                    val btnClosePopupWindow = customView.findViewById<Button>(R.id.popup_window_close_button)

                    // Set onClickListeners
                    btnClosePopupWindow.setOnClickListener {
                        if(madeProgress){
                            AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                                .setIcon(R.drawable.ic_sprite_warning)
                                .setTitle(activity.getString(R.string.pin_close_warning_head))
                                .setMessage(activity.getString(R.string.pin_close_warning_body))
                                .setPositiveButton(activity.getString(R.string.positive_button_text)) { _, _ -> popupWindow?.dismiss() }
                                .setNegativeButton(activity.getString(R.string.negative_button_text), null)
                                .show()
                        }
                        else{
                            popupWindow?.dismiss()
                        }
                    }
                }
            },
            {}
        )
    }

    private fun complete() {
        status = 2
        (viewModel as PinViewModel).completePin(id, followIds)
    }

    fun addQuestion(questionId : Int, reward: Int){
        answered[questionId] = false
        totalReward += reward
    }

    fun answerQuestion(questionId : Int, reward : Int){
        questionRewards[questionId] = reward
        answered[questionId] = true
        madeProgress = true
    }

    private fun resetQuestions(){
        questionRewards.map{0}
        totalReward = 0
        madeProgress = false
        answered.map{true}
    }

    private fun finishQuiz(activity : Activity, parentView: View){
        if(answered.all{b -> b}){
            // All questions answered
            val reward = questionRewards.sum()
            popupWindow?.dismiss()

            var sufficient = false
            if(reward >= 0.55 * totalReward){
                sufficient = true
                complete()

                val sharedPref =  PreferenceManager.getDefaultSharedPreferences(activity)
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

            popupWindow = PopupWindow(
                customView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            popupWindow?.setOnDismissListener {
                popupWindow = null
            }

            // Open popup
            popupWindow?.showAtLocation(parentView, Gravity.CENTER, 0, 0)

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
                popupWindow?.dismiss()
            }

            btnOpenQuiz.setOnClickListener {
                popupWindow?.dismiss()

                openContent(parentView, activity){}
            }
        }
        else{
            // Questions left unanswered
            Toast.makeText(activity, activity.getString(R.string.pin_missing_answer_message), Toast.LENGTH_SHORT).show()
        }
    }

    fun resize(pinSize : Int){
        pinWidth = pinSize.toFloat()

        // Calculate pin height to maintain aspect ratio
        pinHeight =
            pinWidth * bitmapBackgroundHeight / bitmapBackgroundWidth

        // Calculate icon measurements
        if(bitmapIconHeight > bitmapIconWidth){
            iconHeight = pinHeight * 0.5
            iconWidth = iconHeight * (bitmapIconWidth / bitmapIconHeight)
        }
        else{
            iconWidth = pinWidth * 0.55
            iconHeight = iconWidth * (bitmapIconHeight / bitmapIconWidth)
        }
    }

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

    @TestOnly
    fun getScreenLocation(viewport: Pair<p2, p2>, width : Int, height : Int) : Pair<Float, Float>{
        return coordToScreen(coordinate, viewport, width, height)
    }
}

class MergedPin(
    id                      : String = "",
    coordinate              : UTMCoordinate,
    title           : String,
    content         : PinContent,
    background      : Bitmap,
    icon            : Drawable,
    status          : Int,              //-1 : recalculating, 0 : locked, 1 : unlocked, 2 : completed
    predecessorIds  : List<String>,
    followIds       : List<String>,
    viewModel       : ViewModel
): Pin(id,coordinate,title,content,background,icon,status,predecessorIds,followIds,viewModel) {
    val pins: MutableList<Pin> = mutableListOf()

    override fun draw(
        program: Int,
        scale: FloatArray,
        trans: FloatArray,
        viewport: Pair<p2, p2>,
        width: Int,
        height: Int,
        view: View
    ) {
        for(pin in pins){
            pin.draw(program,scale,trans,viewport,width,height,view)
        }
    }
}
