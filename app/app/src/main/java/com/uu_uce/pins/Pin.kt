package com.uu_uce.pins

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import com.uu_uce.mapOverlay.boundingBoxIntersect
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.pointInBoundingBox
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.shapefiles.p3
import kotlin.math.roundToInt

enum class PinType {
    TEXT,
    VIDEO,
    IMAGE
}

class Pin(
    private var coordinate  : UTMCoordinate,
    private var difficulty  : Int,
    private var type        : PinType,
    private var title       : String,
    private var content     : PinContent,
    private var image       : Drawable
) {
    private val pinSize = 60
    private val imageHeight = pinSize * (image.intrinsicHeight.toFloat() / image.intrinsicWidth.toFloat())

    var inScreen : Boolean = true

    var minX : Int = 0
    var minY : Int = 0
    var maxX : Int = 0
    var maxY : Int = 0

    fun draw(viewport : Pair<p3,p3>, view : View, canvas : Canvas){
        val location : Pair<Float, Float> = coordToScreen(coordinate, viewport, view.width, view.height)

        minX = (location.first - pinSize/2).roundToInt()
        minY = (location.second - imageHeight).roundToInt()
        maxX = (location.first + pinSize/2).roundToInt()
        maxY = (location.second).roundToInt()

        if(!pointInBoundingBox(viewport.first, viewport.second, p3(coordinate.east, coordinate.north, 0.0), pinSize)){
            //Log.d("Pin.draw", "pin outside of viewport")
            inScreen = false
            return
        }

        inScreen = true
        image.setBounds(minX, minY, maxX, maxY)
        image.draw(canvas)

    }
}

