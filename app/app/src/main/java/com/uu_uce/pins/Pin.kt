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

        val minX = (location.first - pinSize/2).roundToInt()
        val minY = (location.second - imageHeight).roundToInt()
        val maxX = (location.first + pinSize/2).roundToInt()
        val maxY = (location.second).roundToInt()

        if(!pointInBoundingBox(viewport.first, viewport.second, p3(coordinate.east, coordinate.north, 0.0))){
            Log.d("Pin.draw", "pin outside of viewport")
            return
        }

        image.setBounds(minX, minY, maxX, maxY)
        image.draw(canvas)
    }
}

