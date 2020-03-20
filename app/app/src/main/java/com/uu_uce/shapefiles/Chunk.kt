package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Paint
import com.uu_uce.mapOverlay.aaBoundingBoxIntersect

typealias ChunkIndex = Triple<Int,Int,Int>

class Chunk(
    private var shapes: List<ShapeZ>,
    var bmin: p3,
    var bmax: p3
) {

    fun draw(canvas: Canvas, paint: Paint, viewport : Pair<p2,p2>, width: Int, height: Int){
        for(shape in shapes)
            if(aaBoundingBoxIntersect(shape.bMin, shape.bMax, viewport.first, viewport.second))
                shape.draw(canvas,paint,viewport,width,height)
    }
}