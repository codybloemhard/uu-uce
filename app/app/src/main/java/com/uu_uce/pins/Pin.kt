package com.uu_uce.pins

import android.app.Activity
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.uu_uce.R
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.shapefiles.p3
import kotlin.math.roundToInt

enum class PinType {
    TEXT,
    VIDEO,
    IMAGE
}

class Pin(
    var coordinate  : UTMCoordinate,
    var difficulty  : Int,
    var type        : PinType,
    var title       : String,
    var content     : PinContent,
    var pinSize     : Int,
    var image       : Drawable
) {
    val imageHeight = pinSize * (image.intrinsicHeight.toFloat() / image.intrinsicWidth.toFloat())

    fun draw(viewport : Pair<p3,p3>, view : View, canvas : Canvas){
        val location : Pair<Float, Float> = coordToScreen(coordinate, viewport, view)
        image.setBounds((location.first - pinSize/2).roundToInt(), (location.second - imageHeight).roundToInt(), (location.first + pinSize/2).roundToInt(), (location.second).roundToInt())
        image.draw(canvas)
    }

    fun openPopupWindow(parentLayout: ConstraintLayout, activity : Activity) {
        // make sure we can access the Pin in the fragment

        val layoutInflater = activity.layoutInflater

        // build an custom view (to be inflated on top of our current view & build it's popup window)
        val customView = layoutInflater.inflate(R.layout.popup_window, null)
        val popupWindow = PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // add the title for the popup window
        val windowTitle = customView.findViewById<TextView>(R.id.popup_window_title)
        windowTitle.text = title

        popupWindow.showAtLocation(parentLayout, Gravity.CENTER, 0, 0)

        val btnClosePopupWindow = customView.findViewById<Button>(R.id.popup_window_close_button)

        btnClosePopupWindow.setOnClickListener {
            popupWindow.dismiss()
        }
    }
}

