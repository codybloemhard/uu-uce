package com.uu_uce.OpenGL

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.views.CustomMap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

const val coordsPerVertex= 2

class CustomMapGLRenderer(private val map: CustomMap): GLSurfaceView.Renderer{
    private val vertexShaderCode =
                "uniform vec2 trans;\n" +
                "uniform vec2 scale;\n" +
                "attribute vec4 vPosition;\n" +
                "void main() {\n" +
                "  gl_Position = vec4((trans.x + vPosition.x)*scale.x, (trans.y + vPosition.y) * scale.y, 0.0, 1.0);\n" +
                "}\n"

    private val fragmentShaderCode =
                "precision mediump float;\n" +
                "uniform vec4 vColor;\n" +
                "void main() {\n" +
                "  gl_FragColor = vColor;\n" +
                "}\n"

    private val indices = shortArrayOf(0,1,2,0,2,3)

    private var program: Int = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.9f, 0.9f, 0.9f, 1.0f)
        //GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        program = GLES20.glCreateProgram().also {
            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)
            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)
            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }
        /*indexBuffer=
            // (# of coordinate values * 2 bytes per short)
            ByteBuffer.allocateDirect(indices.size * 2).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    put(indices)
                    position(0)
                }
            }*/
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glUseProgram(program)

        map.onDrawFrame(program)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }


    private fun loadShader(type: Int, shaderCode: String): Int {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        return GLES20.glCreateShader(type).also { shader ->
            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            Logger.error("Renderer", GLES20.glGetShaderInfoLog(shader))
        }
    }
}