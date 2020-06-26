package com.uu_uce.mapOverlay

import com.uu_uce.services.UTMCoordinate
import com.uu_uce.shapefiles.p2
import kotlin.math.abs
import kotlin.math.pow

/**
 * Calculates where on the screen a coordinate is.
 * @param[coordinate] the coordinate to be mapped onto the screen.
 * @param[viewport] the current viewport.
 * @param[viewWidth] the width of the current map.
 * @param[viewHeight] the height of the current map.
 * @return the screen location of a certain coordinate.
 */
fun coordToScreen(
    coordinate: UTMCoordinate,
    viewport: Pair<p2, p2>,
    viewWidth: Int,
    viewHeight: Int
): Pair<Float, Float> {
    val mapX = (coordinate.east - viewport.first.first)
    val mapY = (coordinate.north - viewport.first.second)

    val coordRangeX = (viewport.second.first - viewport.first.first)
    val coordRangeY = (viewport.second.second - viewport.first.second)

    val screenX = mapX / coordRangeX * viewWidth
    val screenY = viewHeight - mapY / coordRangeY * viewHeight

    return Pair(screenX, screenY)
}

/**
 * Calculates where on the map a screen coordinate is.
 * @param[screenLoc] the location on the screen you would like the map coordinate of.
 * @param[viewport] the current viewport.
 * @param[viewWidth] the width of the current map.
 * @param[viewHeight] the height of the current map.
 * @return the map coordinate of a certain screen position.
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

/**
 * Calculates if two boundingboxes intersect.
 * @param[bb1Min] the top-left coordinate of the first bounding box.
 * @param[bb1Max] the bottom-right coordinate of the first bounding box.
 * @param[bb2Min] the top-left coordinate of the second bounding box.
 * @param[bb2Max] the bottom-right coordinate of the second bounding box.
 * @return a boolean value that says if the bounding boxes intersect or not.
 */
fun aaBoundingBoxContains(bb1Min: p2, bb1Max: p2, bb2Min: p2, bb2Max: p2) : Boolean{
    return !(
                bb1Min.first    >   bb2Max.first  ||
                bb1Max.first    <   bb2Min.first  ||
                bb1Min.second   >   bb2Max.second ||
                bb1Max.second   <   bb2Min.second
            )
}

/**
 * Calculates if a point is inside of a boundingbox.
 * @param[bbMin] the top-left coordinate of the first bounding box.
 * @param[bbMax] the bottom-right coordinate of the first bounding box.
 * @param[point] a point that you wish to know of if it is in the screen.
 * @param[bufferSize] how far outside the boundingbox can the point be to still be considered inside.
 * @return a boolean value that says if the bounding boxes intersect or not.
 */
fun pointInAABoundingBox(bbMin: p2, bbMax: p2, point : p2, bufferSize : Int) : Boolean{
    return (
            point.first < bbMax.first + bufferSize &&
                    point.first > bbMin.first - bufferSize &&
                    point.second < bbMax.second + bufferSize &&
                    point.second > bbMin.second - bufferSize
            )
}

/**
 * Calculates the euclidean distance between two points.
 * @param[p1] the first point
 * @param[p2] the second point
 * @return the distance between the points as a double.
 */
fun pointDistance(p1 : Pair<Float, Float>, p2 : Pair<Float, Float>) : Double{
    return abs((p1.first - p2.first).toDouble()).pow(2) + abs((p1.second - p2.second).toDouble()).pow(2)
}





