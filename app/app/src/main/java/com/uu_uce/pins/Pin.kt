package com.uu_uce.pins

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.shapefiles.p3
import com.uu_uce.mapOverlay.coordToScreen
import kotlin.math.roundToInt

enum class PinType(){
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
}

