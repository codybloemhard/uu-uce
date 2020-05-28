package com.uu_uce.shapefiles


import android.opengl.GLES20
import com.uu_uce.OpenGL.colorsPerVertex
import com.uu_uce.OpenGL.coordsPerVertex
import com.uu_uce.linewidth
import com.uu_uce.misc.Logger
import com.uu_uce.outlinewidth
import java.nio.*
import kotlin.random.Random

//abstract class for drawing shapes
abstract class DrawInfo{
    abstract fun draw(lineProgram: Int, polygonProgram: Int, scale: FloatArray, trans: FloatArray, color: FloatArray)
    abstract fun finalize()
}

//information for drawing a line
class LineDrawInfo(nrPoints: Int, nrLines: Int): DrawInfo(){
    private var vertices: FloatArray = FloatArray(nrPoints*2)
    var i = 0
    private var indices: IntArray = IntArray(nrLines*2)
    var j = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: IntBuffer
    private var nrIndices = 0
    var curIndex = 0

    fun shapeLength(length: Int){
        for(k in 0 until length){
            indices[j++] = curIndex
            indices[j++] = ++curIndex
        }
        curIndex++
    }

    fun addVertex(item: Float){vertices[i++] = item}

    override fun draw(lineProgram: Int, polygonProgram: Int, scale: FloatArray, trans: FloatArray, color: FloatArray) {
        GLES20.glUseProgram(lineProgram)

        // get handle to vertex shader's vPosition member
        val positionHandle = GLES20.glGetAttribLocation(lineProgram, "vPosition")

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(positionHandle)

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
            positionHandle,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            coordsPerVertex*4,
            vertexBuffer
        )

        val colorHandle = GLES20.glGetUniformLocation(lineProgram, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        val scaleHandle = GLES20.glGetUniformLocation(lineProgram, "scale")
        GLES20.glUniform2fv(scaleHandle, 1, scale, 0)

        val transHandle = GLES20.glGetUniformLocation(lineProgram, "trans")
        GLES20.glUniform2fv(transHandle, 1, trans, 0)

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_LINES, nrIndices, GLES20.GL_UNSIGNED_INT, indexBuffer)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    override fun finalize() {
        vertexBuffer =
            // (# of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(vertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(vertices)
                    position(0)
                }
            }
        indexBuffer=
            // (# of coordinate values * 2 bytes per short)
            ByteBuffer.allocateDirect(indices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asIntBuffer().apply {
                    put(indices)
                    position(0)
                }
            }

        nrIndices = indices.size
        vertices = floatArrayOf()
        indices = intArrayOf()
    }
}

//information for drawing a polygon
class PolygonDrawInfo(nrVertices: Int, nrIndices: Int, outlineLength: Int): DrawInfo(){
    private var vertices = FloatArray(nrVertices* coordsPerVertex)
    private var v = 0
    private var colors = FloatArray(nrVertices*3)
    private var indices = IntArray(nrIndices)
    private var i = 0
    private var outlineIndices = IntArray(outlineLength)
    private var j = 0
    private var indexOffset = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: IntBuffer
    private lateinit var outlineBuffer: IntBuffer
    private lateinit var colorBuffer: FloatBuffer
    private var nrIndices = 0
    private var nrOutlineIndices = 0

    private fun addVertex(item: Float) {vertices[v++]=item}

    fun addPolygon(polygon: PolygonZ){
        for(index in polygon.indices) indices[i++] = indexOffset + index
        if(polygon.style.outline) for(index in polygon.outlineIndices) outlineIndices[j++] = indexOffset + index

        for(i in polygon.vertices.indices){
            colors[(indexOffset + i) * 3 + 0] = polygon.style.color[0]
            colors[(indexOffset + i) * 3 + 1] = polygon.style.color[1]
            colors[(indexOffset + i) * 3 + 2] = polygon.style.color[2]
        }

        for ((x,y,_) in polygon.vertices) {
            addVertex(x.toFloat())
            addVertex(y.toFloat())
        }

        indexOffset += polygon.vertices.size
    }

    override fun draw(lineProgram: Int, polygonProgram: Int, scale: FloatArray, trans: FloatArray, color: FloatArray) {
        GLES20.glUseProgram(polygonProgram)

        var positionHandle = GLES20.glGetAttribLocation(polygonProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            coordsPerVertex*4, //4 bytes per float
            vertexBuffer
        )

        var colorHandle = GLES20.glGetAttribLocation(polygonProgram, "inColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(
            colorHandle,
            colorsPerVertex,
            GLES20.GL_FLOAT,
            false,
            colorsPerVertex * 4, //4 bytes per float
            colorBuffer
        )

        var scaleHandle = GLES20.glGetUniformLocation(lineProgram, "scale")
        GLES20.glUniform2fv(scaleHandle, 1, scale, 0)

        var transHandle = GLES20.glGetUniformLocation(lineProgram, "trans")
        GLES20.glUniform2fv(transHandle, 1, trans, 0)

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, nrIndices, GLES20.GL_UNSIGNED_INT, indexBuffer)


        //draw outline
        GLES20.glUseProgram(lineProgram)

        // get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(lineProgram, "vPosition")

        GLES20.glEnableVertexAttribArray(positionHandle)

        GLES20.glVertexAttribPointer(
            positionHandle,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            coordsPerVertex*4,
            vertexBuffer
        )

        val outlineColor = floatArrayOf(0f,0f,0f,1f)
        colorHandle = GLES20.glGetUniformLocation(lineProgram, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, outlineColor, 0)

        scaleHandle = GLES20.glGetUniformLocation(lineProgram, "scale")
        GLES20.glUniform2fv(scaleHandle, 1, scale, 0)

        transHandle = GLES20.glGetUniformLocation(lineProgram, "trans")
        GLES20.glUniform2fv(transHandle, 1, trans, 0)

        GLES20.glLineWidth(outlinewidth)
        GLES20.glDrawElements(GLES20.GL_LINES, nrOutlineIndices, GLES20.GL_UNSIGNED_INT, outlineBuffer)
        GLES20.glLineWidth(linewidth)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    override fun finalize() {
        vertexBuffer =
                // (# of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(vertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(vertices)
                    position(0)
                }
            }

        colorBuffer=
            ByteBuffer.allocateDirect(colors.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(colors)
                    position(0)
                }
            }

        indexBuffer=
                // (# of coordinate values * 4 bytes per int)
            ByteBuffer.allocateDirect(indices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asIntBuffer().apply {
                    put(indices)
                    position(0)
                }
            }

        outlineBuffer =
                // (# of coordinate values * 4 bytes per int)
            ByteBuffer.allocateDirect(outlineIndices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asIntBuffer().apply {
                    put(outlineIndices)
                    position(0)
                }
            }

        nrIndices = indices.size
        nrOutlineIndices = outlineIndices.size

        vertices = floatArrayOf()
        colors = floatArrayOf()
        indices = intArrayOf()
        outlineIndices = intArrayOf()
    }
}

//generic shape
abstract class ShapeZ(val style: Style){
    abstract fun initDrawInfo(drawInfo: DrawInfo)
    abstract val nrPoints: Int
}

//shape consisting of just lines on the same height
class HeightShapeZ(private var points: List<p2>, style: Style): ShapeZ(style) {
    override val nrPoints = points.size

    override fun initDrawInfo(
        drawInfo: DrawInfo
    ) {
        if (points.size < 2) return

        if(drawInfo is LineDrawInfo) {
            drawInfo.shapeLength(points.size-1)
            for (i in points.indices) {
                drawInfo.addVertex(points[i].first.toFloat())
                drawInfo.addVertex(points[i].second.toFloat())
            }
        }
        else Logger.error("ShapeZ", "wrong draw information for heightshape")
    }
}

//shape consisting of polygons that need to be colorized
class PolygonZ(var vertices: List<p3>, var indices: MutableList<Short>, var outlineIndices: List<Short>, style: Style): ShapeZ(style){
    override val nrPoints: Int = vertices.size

    override fun initDrawInfo(
        drawInfo: DrawInfo
    ) {
        if (drawInfo is PolygonDrawInfo) {
            drawInfo.addPolygon(this)
        } else Logger.error("ShapeZ", "wrong draw information for polygon shape")
    }
}