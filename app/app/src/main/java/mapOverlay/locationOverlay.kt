package mapOverlay

import android.graphics.Canvas
import android.graphics.Paint

/*
Calculates where on the screen a coordinate is.
screenLoc: the coordinate on the screen where the device location should be drawn.
canvas: the canvas that the location should be drawn on.
It will draw a circle on the screen at the desired location.
*/
fun drawDeviceLocation(screenLoc : Pair<Float, Float>, canvas : Canvas, paint : Paint, edgePaint : Paint, size : Float, edgeSize : Float){
    canvas.drawCircle(screenLoc.first, screenLoc.second, size + edgeSize, edgePaint)
    canvas.drawCircle(screenLoc.first, screenLoc.second, size, paint)
}