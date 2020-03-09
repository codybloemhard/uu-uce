package com.uu_uce.pins

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import com.uu_uce.mapOverlay.aaBoundingBoxContains
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.screenToCoord
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.shapefiles.p2
import com.uu_uce.shapefiles.p2Zero
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
    var image       : Drawable
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
            //Log.d("Pin.draw", "pin outside of viewport")
            inScreen = false
            return
        }

        inScreen = true
        boundingBox = Pair(p2(minX.toDouble(), minY.toDouble()), p2(maxX.toDouble(), maxY.toDouble()))
        image.setBounds(minX, minY, maxX, maxY)
        image.draw(canvas)
    }
}

