package com.uu_uce.OpenGL

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.uu_uce.misc.Logger
import com.uu_uce.views.CustomMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

const val coordsPerVertex= 2

class CustomMapGLRenderer(private val map: CustomMap): GLSurfaceView.Renderer{
    private val vertexShaderCode =
                "uniform vec2 trans;\n" +
                "uniform vec2 scale;\n" +
                "attribute vec4 vPosition;\n" +
                "void main() {\n" +
                "  gl_Position = vec4((trans.x + vPosition.x)*scale.x, (trans.y + vPosition.y) * scale.y, -1.0, 1.0);\n" +
                "}\n"

    private val fragmentShaderCode =
                "precision mediump float;\n" +
                "uniform vec4 vColor;\n" +
                "void main() {\n" +
                "  gl_FragColor = vColor;\n" +
                "}\n"

    private val locVertexShaderCode =
                "uniform vec2 trans;\n" +
                "uniform vec2 scale;\n" +
                "uniform vec2 locScale;\n" +
                "attribute vec4 vPosition;\n" +
                "void main() {\n" +
                "    gl_Position = vec4(trans.x * scale.x + vPosition.x*locScale.x, trans.y * scale.y + vPosition.y*locScale.y, 0.0, 1.0);\n" +
                "}\n"

    private val pinVertexShaderCode =
                "attribute vec2 a_TexCoordinate;\n" +
                "varying vec2 v_TexCoordinate;\n" +
                "uniform vec2 trans;\n" +
                "uniform vec2 scale;\n" +
                "uniform vec2 pinScale;\n" +
                "attribute vec4 vPosition;\n" +
                "void main() {\n" +
                "    gl_Position = vec4(trans.x * scale.x + vPosition.x*pinScale.x, trans.y * scale.y + vPosition.y*pinScale.y, 0.0, 1.0);\n" +
                "    v_TexCoordinate = a_TexCoordinate;\n" +
                "}\n"

    private val pinFragmentShaderCode =
            "precision mediump float;\n" +
            "uniform vec4 vColor;\n" +
            "uniform sampler2D u_Texture;\n" +
            "varying vec2 v_TexCoordinate;\n" +
            "void main() {\n" +
            "    gl_FragColor = (vColor * texture2D(u_Texture, v_TexCoordinate));\n" +
            "}\n"

    private var standardProgram: Int = 0
    private var pinProgram: Int = 0
    private var locProgram: Int = 0

    var pinsChanged = true

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.9f, 0.9f, 0.9f, 1.0f)

        //opacity stuff
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)


        var vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        var fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        standardProgram = GLES20.glCreateProgram().also {
            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)
            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)
            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }

        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, locVertexShaderCode)
        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        locProgram = GLES20.glCreateProgram().also {
            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)
            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)
            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }

        vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, pinVertexShaderCode)
        fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, pinFragmentShaderCode)

        // create empty OpenGL ES Program
        pinProgram = GLES20.glCreateProgram().also {
            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)
            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)
            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }
        GLES20.glBindAttribLocation(pinProgram, 0, "a_TexCoordinate")
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glUseProgram(standardProgram)

        if(pinsChanged) {
            map.initPinsGL()
            pinsChanged = false
        }

        map.onDrawFrame(standardProgram, pinProgram, locProgram)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }


    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            Logger.error("Renderer", GLES20.glGetShaderInfoLog(shader))
        }
    }
}