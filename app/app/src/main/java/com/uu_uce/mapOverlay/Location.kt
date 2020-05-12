package com.uu_uce.mapOverlay

import android.opengl.GLES20
import com.uu_uce.OpenGL.coordsPerVertex
import com.uu_uce.services.UTMCoordinate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Location(val utm: UTMCoordinate){
    private var innerVertexBuffer: FloatBuffer
    private var outerVertexBuffer: FloatBuffer
    private var vertexCount = 0
    private var radius = 50f
    private val outerEdgePercent = 1.25f
    init{
        val nrSegments = 20
        val verticesMutable: MutableList<Float> = mutableListOf()
        verticesMutable.add(0f)
        verticesMutable.add(0f)
        for (i in 0..nrSegments)
        {
            val angle = i.toFloat()/nrSegments*2* PI
            verticesMutable.add((sin(angle)).toFloat())
            verticesMutable.add((cos(angle)).toFloat())
        }
        val innerVertices = verticesMutable.toFloatArray()
        val outerVertices = verticesMutable.map{f -> f*outerEdgePercent}.toFloatArray()
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
        vertexCount = innerVertices.size/ coordsPerVertex
    }

    fun draw(program: Int, trans: FloatArray, width: Int, height: Int){
        val scale = floatArrayOf(radius/width, radius / height)
        val localTrans = floatArrayOf(trans[0] + utm.east.toFloat(), trans[1] + utm.north.toFloat())
        val color = floatArrayOf(0.1f, 0.2f, 0.8f, 1.0f)
        val outerColor = floatArrayOf(0.0f,0.0f,0.0f,1.0f)

        GLES20.glUseProgram(program)

        //set different shader arguments
        val scaleHandle = GLES20.glGetUniformLocation(program, "scale")
        GLES20.glUniform2fv(scaleHandle, 1, scale, 0)

        val transHandle = GLES20.glGetUniformLocation(program, "trans")
        GLES20.glUniform2fv(transHandle, 1, localTrans, 0)

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
            coordsPerVertex *4,
            innerVertexBuffer
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)
    }
}
