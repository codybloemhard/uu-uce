package com.uu_uce.shapefiles


import android.opengl.GLES20
import com.uu_uce.OpenGL.colorsPerVertex
import com.uu_uce.OpenGL.coordsPerVertex
import com.uu_uce.misc.Logger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * a style to draw a polygon/line in
 */
class PolyStyle(val color: FloatArray)
class LineStyle(val thickness: Float, val color: FloatArray)

//thickness of heightline
const val defaultThickness = 1f

//thickness of special line (breakline, outline etc)
const val lineThickness = 3f

/**
 * abstract class for storing all information required for drawing, and then drawing it
 */
abstract class DrawInfo {

    /**
     * add a single shape to this draw info
     * @param[shape] the shape to add
     */
    abstract fun addShape(shape: Shape)

    /**
     * draw all information inside this DrawInfo
     * @param[uniColorProgram] the GL program to draw unicolor shapes with
     * @param[varyingColorProgram] the GL program to draw different colored shapes with
     * @param[scale] scale vector used to draw everything at the right size
     * @param[trans] translation vector to draw everything in the right place
     */
    abstract fun draw(
        uniColorProgram: Int,
        varyingColorProgram: Int,
        scale: FloatArray,
        trans: FloatArray
    )

    /**
     * create all necessary GL buffers (needs to be called in the GL thread)
     */
    abstract fun finalize()
}

/**
 * DrawInfo for drawing heightlines
 * @constructor creates a HeightlineDrawInfo
 *
 * @property[vertices] all points of the lines
 * @property[indices] indices into vertices to draw lines
 * @property[vertexBuffer] buffer where vertices are stored
 * @property[indexBuffer] buffer where indices are stored
 * @property[nrIndices] number of indices
 * @property[curIndex] used to keep track at what index we are
 */
class HeightlineDrawInfo: DrawInfo() {
    private var vertices: MutableList<Float> = mutableListOf()
    private var indices: MutableList<Int> = mutableListOf()
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: IntBuffer
    private var nrIndices = 0
    private var curIndex = 0

    override fun addShape(shape: Shape) {
        if (shape !is Heightline) {
            Logger.error("Shape", "Shape can't be added, is not a heightline")
            return
        }
        for (k in 0 until shape.points.size - 1) {
            indices.add(curIndex)
            indices.add(++curIndex)
        }
        curIndex++
        for (k in shape.points.indices) {
            vertices.add(shape.points[k].first)
            vertices.add(shape.points[k].second)
        }
    }

    override fun draw(
        uniColorProgram: Int,
        varyingColorProgram: Int,
        scale: FloatArray,
        trans: FloatArray
    ) {
        GLES20.glUseProgram(uniColorProgram)

        val positionHandle = GLES20.glGetAttribLocation(uniColorProgram, "vPosition")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            coordsPerVertex * 4,
            vertexBuffer
        )

        val color = floatArrayOf(0f, 0f, 0f, 1f)
        val colorHandle = GLES20.glGetUniformLocation(uniColorProgram, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        val scaleHandle = GLES20.glGetUniformLocation(uniColorProgram, "scale")
        GLES20.glUniform2fv(scaleHandle, 1, scale, 0)

        val transHandle = GLES20.glGetUniformLocation(uniColorProgram, "trans")
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

/**
 * DrawInfo for drawing colored lines
 * @constructor creates a ColoredLineDrawInfo
 *
 * @property[vertices] all points of the lines
 * @property[indices] indices into vertices to draw lines
 * @property[colors] colors of all vertices
 * @property[vertexBuffer] buffer where vertices are stored
 * @property[indexBuffer] buffer where indices are stored
 * @property[colorBuffer] buffer where colors are stored
 * @property[nrIndices] number of indices
 * @property[curIndex] used to keep track at what index we are
 */
class ColoredLineDrawInfo: DrawInfo() {
    private var vertices: MutableList<Float> = mutableListOf()
    private var indices: MutableList<Int> = mutableListOf()
    private var colors: MutableList<Float> = mutableListOf()
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: IntBuffer
    private lateinit var colorBuffer: FloatBuffer
    private var nrIndices = 0
    private var curIndex = 0

    override fun addShape(shape: Shape) {
        if (shape !is ColoredLine) {
            Logger.error("Shape", "Shape can't be added, is not a colored line")
            return
        }
        for (k in 0 until shape.points.size - 1) {
            indices.add(curIndex)
            indices.add(++curIndex)
        }
        curIndex++
        for (k in shape.points.indices) {
            vertices.add(shape.points[k].first)
            vertices.add(shape.points[k].second)
        }
        for (i in shape.points.indices) {
            colors.add(shape.lineStyle.color[0])
            colors.add(shape.lineStyle.color[1])
            colors.add(shape.lineStyle.color[2])
        }
    }

    override fun draw(
        uniColorProgram: Int,
        varyingColorProgram: Int,
        scale: FloatArray,
        trans: FloatArray
    ) {
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

/**
 * DrawInfo for drawing polygons
 * @constructor creates a PolygonDrawInfo
 *
 * @property[vertices] all points of the polygons
 * @property[indices] indices into vertices to draw polygons
 * @property[colors] colors of all vertices
 * @property[vertexBuffer] buffer where vertices are stored
 * @property[indexBuffer] buffer where indices are stored
 * @property[colorBuffer] buffer where colors are stored
 * @property[nrIndices] number of indices
 * @property[indexOffset] used to keep track at what index we are
 */
class PolygonDrawInfo: DrawInfo() {
    private var vertices: MutableList<Float> = mutableListOf()
    private var colors: MutableList<Float> = mutableListOf()
    private var indices: MutableList<Int> = mutableListOf()
    private var indexOffset = 0
    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: IntBuffer
    private lateinit var colorBuffer: FloatBuffer
    private var nrIndices = 0

    override fun addShape(shape: Shape) {
        if (shape !is Polygon) {
            Logger.error("Shape", "Shape can't be added, is not a polygon")
            return
        }
        for (index in shape.indices) indices.add(indexOffset + index)

        for (i in shape.vertices.indices) {
            colors.add(shape.polyStyle.color[0])
            colors.add(shape.polyStyle.color[1])
            colors.add(shape.polyStyle.color[2])
        }

        for ((x, y) in shape.vertices) {
            vertices.add(x)
            vertices.add(y)
        }

        indexOffset += shape.vertices.size
    }

    override fun draw(
        uniColorProgram: Int,
        varyingColorProgram: Int,
        scale: FloatArray,
        trans: FloatArray
    ) {
        GLES20.glUseProgram(varyingColorProgram)

        val positionHandle = GLES20.glGetAttribLocation(varyingColorProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            coordsPerVertex * 4,
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

/**
 * abstract class for all shapes
 */
abstract class Shape {
    /**
     * add all information regarding this shape to the DrawInfo
     */
    fun initDrawInfo(drawInfo: DrawInfo) {
        drawInfo.addShape(this)
    }
}

/**
 * shape consisting of black lines, all on the same height
 * @param[points] the points that make up this heightline
 * @constructor creates a new Heightline
 */
class Heightline(var points: List<p2>) : Shape()

/**
 * shape consisting of uni colored line
 * @param[points] the points that make up this line
 * @param[lineStyle] the style to draw this line with
 * @constructor creates a new ColoredLine
 */
class ColoredLine(var points: List<p2>, val lineStyle: LineStyle) : Shape()

/**
 * polygon with a color
 * @param[vertices] the vertices that make up this polygon
 * @param[indices] the indices to use to draw triangles
 * @param[polyStyle] the style to draw this line with
 * @constructor creates a new Polygon
 */
class Polygon(var vertices: List<p2>, var indices: MutableList<Short>, val polyStyle: PolyStyle) :
    Shape()


