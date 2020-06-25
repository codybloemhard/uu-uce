package com.uu_uce.mapOverlay

import android.content.Context
import android.opengl.GLES20
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.OpenGL.coordsPerVertex
import com.uu_uce.R
import com.uu_uce.services.UTMCoordinate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * location of the user, and potentially other users
 *
 * @param[utm] the current UTM location
 * @param[context] the context this location is in
 * @constructor initializes all buffers
 */
class Location(var utm: UTMCoordinate, context: Context){
    private var innerVertexBuffer: FloatBuffer
    private var outerVertexBuffer: FloatBuffer
    private var vertexCount = 0
    private val outerEdgePercent = 1.25f
    private val color: FloatArray
    private val outerColor: FloatArray

    init{
        color = colorIntToFloatArray(
            ResourcesCompat.getColor(context.resources, R.color.HighBlue, null).toUInt()
        )

        outerColor = colorIntToFloatArray(
            ResourcesCompat.getColor(
                context.resources,
                R.color.BestWhite,
                null
            ).toUInt()
        )

        val nrSegments = 20
        val verticesMutable: MutableList<Float> = mutableListOf()
        verticesMutable.add(0f)
        verticesMutable.add(0f)
        for (i in 0..nrSegments) {
            val angle = i.toFloat()/nrSegments*2* PI
            verticesMutable.add((sin(angle)).toFloat())
            verticesMutable.add((cos(angle)).toFloat())
        }
        val innerVertices = verticesMutable.toFloatArray()
        val outerVertices = verticesMutable.map{ f -> f*outerEdgePercent}.toFloatArray()
        innerVertexBuffer =
            ByteBuffer.allocateDirect(innerVertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(innerVertices)
                    position(0)
                }
            }
        outerVertexBuffer =
            ByteBuffer.allocateDirect(outerVertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(outerVertices)
                    position(0)
                }
            }
        vertexCount = innerVertices.size / coordsPerVertex
    }

    /**
     * draws this location
     *
     * @param[program] the GL program to use while drawing
     * @param[scale] scale vector used to draw everything at the right size
     * @param[trans] translation vector to draw everything in the right place
     * @param[radius] the radius of the circle to draw
     * @param[width] width of view this location is in
     * @param[height] height of view this location is in
     */
    fun draw(
        program: Int,
        scale: FloatArray,
        trans: FloatArray,
        radius: Float,
        width: Int,
        height: Int
    ) {
        GLES20.glUseProgram(program)

        //set different shader arguments
        val localTrans = floatArrayOf(trans[0] + utm.east, trans[1] + utm.north)
        val transHandle = GLES20.glGetUniformLocation(program, "trans")
        GLES20.glUniform2fv(transHandle, 1, localTrans, 0)

        val scaleHandle = GLES20.glGetUniformLocation(program, "scale")
        GLES20.glUniform2fv(scaleHandle, 1, scale, 0)

        val pinScale = floatArrayOf(radius / width, radius / height)
        val pinScaleHandle = GLES20.glGetUniformLocation(program, "locScale")
        GLES20.glUniform2fv(pinScaleHandle, 1, pinScale, 0)

        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)

        //draw outer circle
        GLES20.glUniform4fv(colorHandle, 1, outerColor, 0)
        GLES20.glVertexAttribPointer(
            positionHandle,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            coordsPerVertex *4,
            outerVertexBuffer
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)

        //draw inner circle over outer circle
        GLES20.glUniform4fv(colorHandle, 1, color, 0)
        GLES20.glVertexAttribPointer(
            positionHandle,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            coordsPerVertex * 4,
            innerVertexBuffer
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    /**
     * helper function to turn standard Android Studio color into a GL floatarray
     * @param[col] color in argb int format
     * @return color in rgba float format
     */
    private fun colorIntToFloatArray(col: UInt): FloatArray {
        return floatArrayOf(
            ((col and (255u shl 16)) shr 16).toFloat() / 255,
            ((col and (255u shl 8)) shr 8).toFloat() / 255,
            ((col and (255u shl 0)) shr 0).toFloat() / 255,
            ((col and (255u shl 24)) shr 24).toFloat() / 255
        )
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

