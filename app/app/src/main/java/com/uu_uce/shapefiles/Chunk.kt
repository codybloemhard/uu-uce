package com.uu_uce.shapefiles

import android.graphics.Paint
import com.uu_uce.mapOverlay.aaBoundingBoxIntersect

//chunkindex (x,y,z) is the x'th chunk from the left, the y'th
//from the bottom, in zoomlevel z
typealias ChunkIndex = Triple<Int,Int,Int>
fun chunkName(c: ChunkIndex): String{
    return "${c.third}-${c.first}-${c.second}.chunk"
    }

/*
a chunk holds all shapes of a layer that are in a specific AABB
shapes: all shapes present in the chunk
bmin/bmax: the bounding box of all shapes
type: what type of content is in this chunk
 */
class Chunk(var shapes: List<ShapeZ>, var bmin: p3, var bmax: p3, val type: LayerType){
    private val drawInfo: DrawInfo

    init{
        drawInfo = when(type){
            LayerType.Height -> {
                var nrPoints = 0
                var nrLines = 0
                for(shape in shapes){
                    nrPoints+=shape.nrPoints
                    nrLines += shape.nrPoints - 1
                }
                LineDrawInfo(nrPoints, nrLines)
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
            shape.initDrawInfo(drawInfo)
        }
        drawInfo.finalize()
    }

    //display all chunks to the canvas
    fun draw(program: Int, scale: FloatArray, trans: FloatArray, paint: Paint, viewport : Pair<p2,p2>, width: Int, height: Int){
        drawInfo.draw(program, scale, trans, paint)
    }
}