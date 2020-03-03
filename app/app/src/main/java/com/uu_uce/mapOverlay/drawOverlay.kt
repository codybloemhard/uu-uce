package com.uu_uce.mapOverlay

import android.view.View
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.shapefiles.p3

/*
Calculates where on the screen a coordinate is.
coordinate: the coordinate to be mapped onto the screen.
viewport: the current viewport.
It will provide you with the screen location of a certain coordinate.
 */
fun coordToScreen(coordinate  : UTMCoordinate, viewport : Pair<p3, p3>, view : View) : Pair<Float, Float>{
    return Pair(((coordinate.east - viewport.first.first) / (viewport.second.first - viewport.first.first) * view.width).toFloat(),
        (view.height - (coordinate.north - viewport.first.second) / (viewport.second.second - viewport.first.second) * view.height).toFloat())
}