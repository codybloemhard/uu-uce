package com.uu_uce.pins

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import com.uu_uce.mapOverlay.boundingBoxContains
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.mapOverlay.screenToCoord
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.shapefiles.p2
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

    private var inScreen : Boolean = true

    private var minX : Int = 0
    private var minY : Int = 0
    private var maxX : Int = 0
    private var maxY : Int = 0

    private var pinBbMin : UTMCoordinate = coordinate
    private var pinBbMax : UTMCoordinate = coordinate

    fun draw(viewport : Pair<p2,p2>, view : View, canvas : Canvas){
        val location : Pair<Float, Float> = coordToScreen(coordinate, viewport, view.width, view.height)

        minX = (location.first - pinSize/2).roundToInt()
        minY = (location.second - imageHeight).roundToInt()
        maxX = (location.first + pinSize/2).roundToInt()
        maxY = (location.second).roundToInt()

        pinBbMin = screenToCoord(Pair(minX.toFloat(), maxY.toFloat()), viewport, view.width, view.height)
        pinBbMax = screenToCoord(Pair(maxX.toFloat(), minY.toFloat()), viewport, view.width, view.height)

        if(!boundingBoxContains(viewport.first, viewport.second, p2(pinBbMin.east, pinBbMin.north), p2(pinBbMax.east, pinBbMax.north))){
            //Log.d("Pin.draw", "pin outside of viewport")
            inScreen = false
            return
        }

        inScreen = true
        image.setBounds(minX, minY, maxX, maxY)
        image.draw(canvas)

    }
}

