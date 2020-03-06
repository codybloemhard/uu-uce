package com.uu_uce.mapOverlay

import com.uu_uce.services.UTMCoordinate
import com.uu_uce.shapefiles.p2
import com.uu_uce.shapefiles.p3

/*
Calculates where on the screen a coordinate is.
coordinate: the coordinate to be mapped onto the screen.
viewport: the current viewport.
view: the current map that the function is called in.
It will provide you with the screen location of a certain coordinate.
 */
fun coordToScreen(coordinate  : UTMCoordinate, viewport : Pair<p2, p2>, viewWidth : Int, viewHeight : Int) : Pair<Float, Float>{
    val mapX = (coordinate.east - viewport.first.first)
    val mapY = (coordinate.north - viewport.first.second)

    val coordRangeX = (viewport.second.first - viewport.first.first)
    val coordRangeY = (viewport.second.second - viewport.first.second)

    val screenX = mapX / coordRangeX * viewWidth
    val screenY = viewHeight - mapY / coordRangeY * viewHeight

    return Pair(screenX.toFloat(), screenY.toFloat())
}

/*
Calculates where on the map a screen coordinate is.
screenLoc: the location on the screen you would like the map coordinate of.
viewport: the current viewport.
view: the current map that the function is called in.
It will provide you with the screen location of a certain coordinate.
 */
fun screenToCoord(screenLoc : Pair<Float, Float>, viewport : Pair<p2, p2>, viewWidth : Int, viewHeight : Int) : UTMCoordinate{
    val screenX = screenLoc.first / viewWidth
    val screenY = (viewHeight - screenLoc.second) / viewHeight

    val coordRangeX = (viewport.second.first - viewport.first.first)
    val coordRangeY = (viewport.second.second - viewport.first.second)

    val easting = screenX * coordRangeX + viewport.first.first
    val northing = screenY * coordRangeY + viewport.first.second

    return UTMCoordinate(31, 'N', easting, northing)
}

/*
Calculates if two boundingboxes intersect.
bb1Min: The top-left coordinate of the first bounding box.
bb1Max: The bottom-right coordinate of the first bounding box.
bb2Min: The top-left coordinate of the second bounding box.
bb2Max: The bottom-right coordinate of the second bounding box.
It will provide you with a boolean value that says if the bounding boxes intersect or not.
 */
fun boundingBoxContains(bb1Min: p2, bb1Max: p2, bb2Min: p2, bb2Max: p2) : Boolean{
    return !(
                bb1Min.first    >   bb2Max.first  ||
                bb1Max.first    <   bb2Min.first  ||
                bb1Min.second   >   bb2Max.second ||
                bb1Max.second   <   bb2Min.second
            )
}

fun boundingBoxIntersect(bb1Min: p3, bb1Max: p3, bb2Min: p2, bb2Max: p2) : Boolean{
    return boundingBoxContains(p2(bb1Min.first, bb1Min.second), p2(bb1Max.first, bb1Max.second), bb2Min, bb2Max)
}

/*
Calculates if a point is inside of a boundingbox.
bb1Min: The top-left coordinate of the first bounding box.
bb1Max: The bottom-right coordinate of the first bounding box.
point : A point that you wish to know of if it is in the screen.
bufferSize: How far outside the boundingbox can the point be to still be considered inside.
It will provide you with a boolean value that says if the bounding boxes intersect or not.
 */
fun pointInBoundingBox(bbMin: p3, bbMax: p3, point : p3, bufferSize : Int) : Boolean{
    return(
                point.first     < bbMax.first   + bufferSize    &&
                point.first     > bbMin.first   - bufferSize    &&
                point.second    < bbMax.second  + bufferSize    &&
                point.second    > bbMin.second  - bufferSize
            )
}