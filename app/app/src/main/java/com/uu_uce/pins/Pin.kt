package com.uu_uce.pins

import android.app.Activity
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.uu_uce.R
import com.uu_uce.mapOverlay.aaBoundingBoxContains
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.screenToCoord
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.services.getFiles
import com.uu_uce.services.updateFiles
import com.uu_uce.shapefiles.p2
import com.uu_uce.shapefiles.p2Zero
import kotlin.math.roundToInt

enum class PinType {
    TEXT,
    VIDEO,
    IMAGE
}

class Pin(
    private var coordinate      : UTMCoordinate,
    private var difficulty      : Int,
    private var type            : PinType,
    private var title           : String,
    private var content         : PinContent,
    private var image           : Drawable
) {
    private val pinSize = 60
    private val imageHeight = pinSize * (image.intrinsicHeight.toFloat() / image.intrinsicWidth.toFloat())

    var inScreen : Boolean = true
    var boundingBox : Pair<p2, p2> = Pair(p2Zero, p2Zero)

    private var pinBbMin : UTMCoordinate = coordinate
    private var pinBbMax : UTMCoordinate = coordinate

    fun draw(viewport : Pair<p2,p2>, view : View, canvas : Canvas){
        val location : Pair<Float, Float> = coordToScreen(coordinate, viewport, view.width, view.height)

        val minX = (location.first - pinSize/2).roundToInt()
        val minY = (location.second - imageHeight).roundToInt()
        val maxX = (location.first + pinSize/2).roundToInt()
        val maxY = (location.second).roundToInt()

        pinBbMin = screenToCoord(Pair(minX.toFloat(), maxY.toFloat()), viewport, view.width, view.height)
        pinBbMax = screenToCoord(Pair(maxX.toFloat(), minY.toFloat()), viewport, view.width, view.height)

        if(!aaBoundingBoxContains(viewport.first, viewport.second, p2(pinBbMin.east, pinBbMin.north), p2(pinBbMax.east, pinBbMax.north))){
            Logger.log(LogType.Event,"Pin", "Pin outside of viewport")
            inScreen = false
            return
        }

        inScreen = true
        boundingBox = Pair(p2(minX.toDouble(), minY.toDouble()), p2(maxX.toDouble(), maxY.toDouble()))

        image.setBounds(minX, minY, maxX, maxY)
        image.draw(canvas)
    }

    fun getTitle() : String{
        return title
    }

    fun getContent() : PinContent{
        return content
    }
}

fun openPinPopupWindow(title : String, content : PinContent, parentView: View, activity : Activity) {
    val layoutInflater = activity.layoutInflater

    // build an custom view (to be inflated on top of our current view & build it's popup window)
    val customView = layoutInflater.inflate(R.layout.pin_content_view, null, false)
    val popupWindow = PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    // add the title for the popup window
    val windowTitle = customView.findViewById<TextView>(R.id.popup_window_title)
    windowTitle.text = title

    // add content to popup window
    val layout : LinearLayout = customView.findViewById(R.id.scrollLayout)
    content.contentBlocks.map { cB ->
        val filePath : List<String> = cB.getFilePath()
        updateFiles(filePath, activity){
            cB.generateContent(layout, activity)
        }
    }

    popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0)

    val btnClosePopupWindow = customView.findViewById<Button>(R.id.popup_window_close_button)

    btnClosePopupWindow.setOnClickListener {
        popupWindow.dismiss()
    }
}

