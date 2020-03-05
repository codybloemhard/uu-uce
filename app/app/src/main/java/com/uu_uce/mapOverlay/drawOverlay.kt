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

/*
Calculates if two boundingboxes intersect.
bb1Min: The top-left coordinate of the first bounding box.
bb1Max: The bottom-right coordinate of the first bounding box.
bb2Min: The top-left coordinate of the second bounding box.
bb2Max: The bottom-right coordinate of the second bounding box.
It will provide you with a boolean value that says if the bounding boxes intersect or not.
 */
fun boundingBoxIntersect(bb1Min: p3, bb1Max: p3, bb2Min: p3, bb2Max: p3) : Boolean{
    return !(
                bb1Min.first    >   bb2Max.first  ||
                bb1Max.first    <   bb2Min.first  ||
                bb1Min.second   >   bb2Max.second ||
                bb1Max.second   <   bb2Min.second
            )
}

/*
Calculates if two boundingboxes intersect.
bb1Min: The top-left coordinate of the first bounding box.
bb1Max: The bottom-right coordinate of the first bounding box.
point : A point that you wish to know of if it is in the screen.
It will provide you with a boolean value that says if the bounding boxes intersect or not.
 */
fun pointInBoundingBox(bbMin: p3, bbMax: p3, point : p3) : Boolean{
    return(
                point.first     < bbMax.first   &&
                point.first     > bbMin.first   &&
                point.second    < bbMax.second  &&
                point.second    > bbMin.second
            )
}