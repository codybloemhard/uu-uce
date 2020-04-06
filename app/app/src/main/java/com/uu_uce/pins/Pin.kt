package com.uu_uce.pins

import android.app.Activity
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.uu_uce.R
import com.uu_uce.database.PinViewModel
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.shapefiles.p2
import com.uu_uce.shapefiles.p2Zero
import kotlin.math.roundToInt

enum class PinType {
    TEXT,
    VIDEO,
    IMAGE,
    MCQUIZ
}

class Pin(
    val id : Int,
    private var coordinate      : UTMCoordinate,
    /*private var difficulty      : Int,
    private var type            : PinType,*/
    private var title           : String,
    private var content         : PinContent,
    private var image           : Drawable,
    private var status          : Int,              //-1 : recalculating, 0 : locked, 1 : unlocked, 2 : completed
    private var predecessorIds  : List<Int>,
    private var followIds       : List<Int>,
    private val viewModel       : PinViewModel
) {
    init {
        predecessorIds.map { I ->
            if (I == id) error("Pin can not be own predecessor")
        }
    }

    private val pinWidth = 60 // TODO: set this in settings somewhere

    // Calculate pin height to maintain aspect ratio
    private val pinHeight =
        pinWidth * (image.intrinsicHeight.toFloat() / image.intrinsicWidth.toFloat())


    // Initialize variables used in checking for clicks
    var inScreen: Boolean = true
    var boundingBox: Pair<p2, p2> = Pair(p2Zero, p2Zero)

    var popupWindow: PopupWindow? = null

    // Quiz
    private var unansweredCount = 0
    private var totalReward     = 0


    fun draw(viewport: Pair<p2, p2>, width : Int, height : Int, view: View, canvas: Canvas) {
        val screenLocation: Pair<Float, Float> =
            coordToScreen(coordinate, viewport, view.width, view.height)

        if(screenLocation.first.isNaN() || screenLocation.second.isNaN())
            return //TODO: Should not be called with NaN

        // Calculate pin bounds on canvas
        val minX = (screenLocation.first - pinWidth / 2).roundToInt()
        val minY = (screenLocation.second - pinHeight).roundToInt()
        val maxX = (screenLocation.first + pinWidth / 2).roundToInt()
        val maxY = (screenLocation.second).roundToInt()

        // Check whether pin is unlocked
        if (status == 0) return

        // Check whether pin is out of screen
        if (
            minX > width    ||
            maxX < 0        ||
            minY > height   ||
            maxY < 0
        ) {
            Logger.log(LogType.Event, "Pin", "Pin outside of viewport")
            inScreen = false
            return
        }
        inScreen = true

        // Set boundingbox for pin tapping
        boundingBox =
            Pair(p2(minX.toDouble(), minY.toDouble()), p2(maxX.toDouble(), maxY.toDouble()))

        image.setBounds(minX, minY, maxX, maxY)
        image.draw(canvas)
    }

    // Check if pin should be unlocked
    fun tryUnlock(action : (() -> Unit)){
        if(predecessorIds[0] != -1 && status < 1){
            viewModel.tryUnlock(id, predecessorIds, action)
        }
        else{
            action()
        }
    }

    fun openPinPopupWindow(parentView: View, activity : Activity, onDissmissAction: () -> Unit) {
        val layoutInflater = activity.layoutInflater

        // Build an custom view (to be inflated on top of our current view & build it's popup window)
        val customView = layoutInflater.inflate(R.layout.pin_content_view, null, false)

        popupWindow = PopupWindow(
            customView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        popupWindow?.setOnDismissListener {
            popupWindow = null

            if(unansweredCount == 0) {
                complete()

            }

            onDissmissAction()
        }


        // Add the title for the popup window
        val windowTitle = customView.findViewById<TextView>(R.id.popup_window_title)
        windowTitle.text = title

        // Add content to popup window
        val layout: LinearLayout = customView.findViewById(R.id.scrollLayout)

        // Fill layout of popup
        resetQuizzes()
        content.contentBlocks.forEach { cb ->
            cb.generateContent(layout, activity, this)
        }

        // Open popup
        popupWindow?.showAtLocation(parentView, Gravity.CENTER, 0, 0)

        // Get elements
        val btnClosePopupWindow = customView.findViewById<Button>(R.id.popup_window_close_button)
        val checkBoxCompletePin = customView.findViewById<CheckBox>(R.id.complete_box)

        // Set checkbox to correct state
        checkBoxCompletePin.isChecked = (getStatus() == 2)

        // Set onClickListeners
        btnClosePopupWindow.setOnClickListener {
            popupWindow?.dismiss()
        }
        checkBoxCompletePin.setOnClickListener {
            if (checkBoxCompletePin.isChecked) {
                complete()
            }
        }
    }

    private fun complete() {
        if (status < 2)
            viewModel.completePin(id, followIds)
    }

    fun addQuiz(reward : Int){
        unansweredCount++
        totalReward += reward
    }

    fun finishQuiz(){
        unansweredCount--
    }

    private fun resetQuizzes(){
        totalReward = 0
        unansweredCount = 0
    }

    fun getTitle(): String {
        return title
    }

    fun getContent(): PinContent {
        return content
    }

    fun setStatus(newStatus: Int) {
        status = newStatus
    }

    fun getStatus(): Int {
        return status
    }
}

