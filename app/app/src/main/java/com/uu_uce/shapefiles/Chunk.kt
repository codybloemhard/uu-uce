package com.uu_uce.shapefiles

import android.graphics.Canvas
import android.graphics.Paint
import com.uu_uce.mapOverlay.aaBoundingBoxIntersect

typealias ChunkIndex = Triple<Int,Int,Int>
fun chunkName(c: ChunkIndex): String{
    return "${c.third}-${c.first}-${c.second}.chunk"
}

class Chunk(var shapes: List<ShapeZ>, var bmin: p3, var bmax: p3, val type: LayerType){
    fun draw(canvas: Canvas, paint: Paint, viewport : Pair<p2,p2>, width: Int, height: Int){
        val drawInfo = when(type){
            LayerType.Height -> {
                var nrPoints = 0
                for(shape in shapes){
                    nrPoints+=shape.nrPoints
                }
                LineDrawInfo(nrPoints)
            }
            LayerType.Water -> {
                var nrIndices = 0
                var nrPoints = 0
                for(shape in shapes){
                    nrIndices+=(shape as PolygonZ).indices.size
                    nrPoints+=shape.nrPoints
                }
                PolygonDrawInfo(nrPoints, nrIndices)
            }
            else -> throw Exception("chunk type not implemented")
        }
        for(shape in shapes) {
            if (aaBoundingBoxIntersect(shape.bmin, shape.bmax, viewport.first, viewport.second))
                shape.draw(drawInfo, viewport, width, height)
        }
        drawInfo.draw(canvas,paint)
    }
}