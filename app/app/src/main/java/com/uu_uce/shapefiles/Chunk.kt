package com.uu_uce.shapefiles

/**
 * chunkindex (x,y,z) is the x'th chunk from the left, the y'th from the bottom, in zoomlevel z
 */
typealias ChunkIndex = Triple<Int,Int,Int>
fun chunkName(c: ChunkIndex): String{
    return "${c.third}-${c.first}-${c.second}.hlinechunk"
    }
fun polyChunkName(c: ChunkIndex): String{
    return "${c.first}-${c.second}.polychunk"
}
fun geolineChunkName(c: ChunkIndex): String{
    return "${c.first}-${c.second}.geolinechunk"
}

/**
 * a chunk holds all shapes of a layer that are in a specific AABB
 * @param[shapes] all shapes present in the chunk
 * @param[type] what type of content is in this chunk
 * @constructor merge all shapes into the drawinfo
 *
 * @property[drawInfo] the drawInfo containing all information of shapes in this chunk
 */
class Chunk(
    private var shapes: List<Shape>,
    val type: LayerType
) {
    private val drawInfo: DrawInfo = when (type) {
        LayerType.Height -> {
            HeightlineDrawInfo()
        }
        LayerType.Water -> {
            PolygonDrawInfo()
        }
        LayerType.Lines -> {
            ColoredLineDrawInfo()
        }
        else -> throw Exception("chunk type not implemented")
    }

    init {
        for (shape in shapes) {
            shape.initDrawInfo(drawInfo)
        }
        drawInfo.finalize()

        shapes = listOf()
    }

    /**
     * draws all the shapes in this chunk
     * @param[uniColorProgram] the GL program to draw unicolor shapes with
     * @param[varyingColorProgram] the GL program to draw different colored shapes with
     * @param[scale] scale vector used to draw everything at the right size
     * @param[trans] translation vector to draw everything in the right place
     */
    fun draw(uniColorProgram: Int, varyingColorProgram: Int, scale: FloatArray, trans: FloatArray) {
        drawInfo.draw(uniColorProgram, varyingColorProgram, scale, trans)
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

