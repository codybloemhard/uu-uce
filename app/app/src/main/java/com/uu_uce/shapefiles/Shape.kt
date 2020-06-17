package com.uu_uce.shapefiles


import android.opengl.GLES20
import com.uu_uce.OpenGL.colorsPerVertex
import com.uu_uce.OpenGL.coordsPerVertex
import com.uu_uce.misc.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

//style for polygons/polylyes
class PolyStyle(val outline: Boolean, val color: FloatArray)
class LineStyle(val thickness: Float, val color: FloatArray)

const val defaultThickness = 1f
const val lineThickness = 3f

//abstract class for drawing shapes
abstract class DrawInfo{
    abstract fun draw(lineProgram: Int, varyingColorProgram: Int, scale: FloatArray, trans: FloatArray, color: FloatArray)
    abstract fun finalize()
}

//information for drawing a line
class HeightlineDrawInfo: DrawInfo(){
    private var vertices: MutableList<Float> = mutableListOf()
    private var indices: MutableList<Int> = mutableListOf()
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: IntBuffer
    private var nrIndices = 0
    private var curIndex = 0

    //add a single HeightLine shape
    fun addLine(line: Heightline){
        for(k in 0 until line.points.size-1){
            indices.add(curIndex)
            indices.add(++curIndex)
        }
        curIndex++
        for (k in line.points.indices) {
            vertices.add(line.points[k].first)
            vertices.add(line.points[k].second)
        }
    }

    override fun draw(lineProgram: Int, varyingColorProgram: Int, scale: FloatArray, trans: FloatArray, color: FloatArray) {
        GLES20.glUseProgram(lineProgram)

        val positionHandle = GLES20.glGetAttribLocation(lineProgram, "vPosition")

        GLES20.glEnableVertexAttribArray(positionHandle)
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

        GLES20.glDrawElements(GLES20.GL_LINES, nrIndices, GLES20.GL_UNSIGNED_INT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    override fun finalize() {
        vertexBuffer =
            ByteBuffer.allocateDirect(vertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(vertices.toFloatArray())
                    position(0)
                }
            }
        indexBuffer=
            ByteBuffer.allocateDirect(indices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asIntBuffer().apply {
                    put(indices.toIntArray())
                    position(0)
                }
            }

        nrIndices = indices.size
        vertices.clear()
        indices.clear()
    }
}

//information for drawing a polygon
class ColoredLineDrawInfo: DrawInfo(){
    private var vertices: MutableList<Float> = mutableListOf()
    private var indices: MutableList<Int> = mutableListOf()
    private var colors: MutableList<Float> = mutableListOf()
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: IntBuffer
    private lateinit var colorBuffer: FloatBuffer
    private var nrIndices = 0
    private var curIndex = 0

    fun addLine(line: ColoredLineShape){
        for(k in 0 until line.points.size-1){
            indices.add(curIndex)
            indices.add(++curIndex)
        }
        curIndex++
        for (k in line.points.indices) {
            vertices.add(line.points[k].first)
            vertices.add(line.points[k].second)
        }
        for(i in line.points.indices){
            colors.add(line.polyStyle.color[0])
            colors.add(line.polyStyle.color[1])
            colors.add(line.polyStyle.color[2])
        }
    }

    override fun draw(lineProgram: Int, varyingColorProgram: Int, scale: FloatArray, trans: FloatArray, color: FloatArray) {
        GLES20.glUseProgram(varyingColorProgram)
        GLES20.glLineWidth(lineThickness)

        val positionHandle = GLES20.glGetAttribLocation(varyingColorProgram, "vPosition")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            coordsPerVertex*4,
            vertexBuffer
        )

        val colorHandle = GLES20.glGetAttribLocation(varyingColorProgram, "inColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(
            colorHandle,
            colorsPerVertex,
            GLES20.GL_FLOAT,
            false,
            colorsPerVertex * 4,
            colorBuffer
        )

        val scaleHandle = GLES20.glGetUniformLocation(varyingColorProgram, "scale")
        GLES20.glUniform2fv(scaleHandle, 1, scale, 0)

        val transHandle = GLES20.glGetUniformLocation(varyingColorProgram, "trans")
        GLES20.glUniform2fv(transHandle, 1, trans, 0)

        GLES20.glDrawElements(GLES20.GL_LINES, nrIndices, GLES20.GL_UNSIGNED_INT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
        GLES20.glLineWidth(defaultThickness)
    }

    override fun finalize() {
        vertexBuffer =
            ByteBuffer.allocateDirect(vertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(vertices.toFloatArray())
                    position(0)
                }
            }
        indexBuffer=
            ByteBuffer.allocateDirect(indices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asIntBuffer().apply {
                    put(indices.toIntArray())
                    position(0)
                }
            }
        colorBuffer=
            ByteBuffer.allocateDirect(colors.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(colors.toFloatArray())
                    position(0)
                }
            }

        nrIndices = indices.size
        vertices.clear()
        indices.clear()
        colors.clear()
    }
}

//information for drawing a polygon
class PolygonDrawInfo: DrawInfo(){
    private var vertices: MutableList<Float> = mutableListOf()
    private var colors: MutableList<Float> = mutableListOf()
    private var indices: MutableList<Int> = mutableListOf()
    private var indexOffset = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: IntBuffer
    private lateinit var colorBuffer: FloatBuffer
    private var nrIndices = 0

    fun addPolygon(polygon: Polygon){
        for(index in polygon.indices) indices.add(indexOffset + index)

        for(i in polygon.vertices.indices){
            colors.add(polygon.polyStyle.color[0])
            colors.add(polygon.polyStyle.color[1])
            colors.add(polygon.polyStyle.color[2])
        }

        for ((x,y) in polygon.vertices) {
            vertices.add(x)
            vertices.add(y)
        }

        indexOffset += polygon.vertices.size
    }

    override fun draw(lineProgram: Int, varyingColorProgram: Int, scale: FloatArray, trans: FloatArray, color: FloatArray) {
        drawPolygon(varyingColorProgram, scale, trans)
    }

    private fun drawPolygon(varyingColorProgram: Int, scale: FloatArray, trans: FloatArray){
        GLES20.glUseProgram(varyingColorProgram)

        val positionHandle = GLES20.glGetAttribLocation(varyingColorProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            coordsPerVertex*4,
            vertexBuffer
        )

        val colorHandle = GLES20.glGetAttribLocation(varyingColorProgram, "inColor")
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(
            colorHandle,
            colorsPerVertex,
            GLES20.GL_FLOAT,
            false,
            colorsPerVertex * 4,
            colorBuffer
        )

        val scaleHandle = GLES20.glGetUniformLocation(varyingColorProgram, "scale")
        GLES20.glUniform2fv(scaleHandle, 1, scale, 0)

        val transHandle = GLES20.glGetUniformLocation(varyingColorProgram, "trans")
        GLES20.glUniform2fv(transHandle, 1, trans, 0)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, nrIndices, GLES20.GL_UNSIGNED_INT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    override fun finalize() {
        vertexBuffer =
            ByteBuffer.allocateDirect(vertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(vertices.toFloatArray())
                    position(0)
                }
            }

        colorBuffer=
            ByteBuffer.allocateDirect(colors.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(colors.toFloatArray())
                    position(0)
                }
            }

        indexBuffer=
            ByteBuffer.allocateDirect(indices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asIntBuffer().apply {
                    put(indices.toIntArray())
                    position(0)
                }
            }

        nrIndices = indices.size

        vertices.clear()
        colors.clear()
        indices.clear()
    }
}

//generic shape
abstract class Shape(){
    abstract fun initDrawInfo(drawInfo: DrawInfo)
    abstract val nrPoints: Int
}

//shape consisting of just lines on the same height
class Heightline(var points: List<p2>): Shape() {
    override val nrPoints = points.size

    override fun initDrawInfo(
        drawInfo: DrawInfo
    ) {
        if (points.size < 2) return

        if(drawInfo is HeightlineDrawInfo) {
            drawInfo.addLine(this)
        }
        else Logger.error("ShapeZ", "wrong draw information for heightline")
    }
}

//shape consisting of colored lines
class ColoredLineShape(var points: List<p2>, val polyStyle: LineStyle): Shape() {
    override val nrPoints = points.size

    override fun initDrawInfo(
        drawInfo: DrawInfo
    ) {
        if (points.size < 2) return

        if (drawInfo is ColoredLineDrawInfo) {
            drawInfo.addLine(this)
        } else Logger.error("ShapeZ", "wrong draw information for coloredlineshape")
    }
}

//shape consisting of colored polygons
class Polygon(var vertices: List<p2>, var indices: MutableList<Short>, val polyStyle: PolyStyle): Shape(){
    override val nrPoints: Int = vertices.size

    override fun initDrawInfo(
        drawInfo: DrawInfo
    ) {
        if (drawInfo is PolygonDrawInfo) {
            drawInfo.addPolygon(this)
        }
        else Logger.error("ShapeZ", "wrong draw information for polygon shape")
    }
}