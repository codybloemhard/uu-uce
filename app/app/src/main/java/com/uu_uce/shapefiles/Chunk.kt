package com.uu_uce.shapefiles

//chunkindex (x,y,z) is the x'th chunk from the left, the y'th
//from the bottom, in zoomlevel z
typealias ChunkIndex = Triple<Int,Int,Int>
fun chunkName(c: ChunkIndex): String{
    return "${c.third}-${c.first}-${c.second}.hlinechunk"
    }
fun polyChunkName(c: ChunkIndex): String{
    return "${c.first}-${c.second}.polychunk"
}

/*
a chunk holds all shapes of a layer that are in a specific AABB
shapes: all shapes present in the chunk
bmin/bmax: the bounding box of all shapes
type: what type of content is in this chunk
 */
class Chunk(private var shapes: List<Shape>, var bmin: p3, var bmax: p3, val type: LayerType){
    private val drawInfo: DrawInfo = when(type){
        LayerType.Height -> {
            HeightlineDrawInfo()
        }
        LayerType.Water -> {
            PolygonDrawInfo()
        }
        else -> throw Exception("chunk type not implemented")
    }

    init{
        for(shape in shapes) {
            shape.initDrawInfo(drawInfo)
        }
        drawInfo.finalize()

        shapes = listOf()
    }

    //display all chunks to the canvas
    fun draw(lineProgram: Int, varyingColorProgram: Int, scale: FloatArray, trans: FloatArray, color: FloatArray){
        drawInfo.draw(lineProgram, varyingColorProgram, scale, trans, color)
    }
}