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
    var content     : PinContent
) {

    fun draw(viewport : Pair<p3,p3>, view : View, canvas : Canvas, context : Context, pinSize : Int){
        val image : Drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.pin, null) ?: error("Pin image not found")
        val location : Pair<Float, Float> =
            coordToScreen(coordinate, viewport, view)
        image.setBounds((location.first - pinSize/2).roundToInt(), (location.second - pinSize/2).roundToInt(), (location.first + pinSize/2).roundToInt(), (location.second + pinSize/2).roundToInt())
        //image.setBounds(970, 550, 1070, 200)
        image.draw(canvas)
    }
}

