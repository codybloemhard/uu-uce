package com.uu_uce.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.uu_uce.shapefiles.Camera
import com.uu_uce.shapefiles.LayerType
import com.uu_uce.shapefiles.ShapeMap
import diewald_shapeFile.files.shp.SHP_File
import java.io.File
import kotlin.system.measureTimeMillis

class CustomMap : View {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var smap : ShapeMap
    private var camera: Camera? = null

    init{
        Log.d("CustomMap", "Init")
        val dir = File(context.filesDir, "mydir")
        val path = File(dir, "bt25mv10sh0f6422al1r020.shp")
        SHP_File.LOG_INFO = false
        SHP_File.LOG_ONLOAD_HEADER = false
        SHP_File.LOG_ONLOAD_CONTENT = false
        val file = SHP_File(null, path)
        val timeRead = measureTimeMillis {
            file.read()
        }
        Log.i("CustomMap", "Read file: $timeRead")
        smap = ShapeMap(10)
        val timeParse = measureTimeMillis {
            smap.addLayer(LayerType.Height, file)
            camera = smap.initialize()
        }
        Log.i("CustomMap", "Parse file: $timeParse")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val timeDraw = measureTimeMillis {
            canvas.drawColor(Color.rgb(234, 243, 245))
            smap.draw(canvas, width, height)

        }
        Log.i("CustomMap", "Draw: $timeDraw")
        invalidate()
    }

    fun zoomMap(zoom: Double){
        val deltaOne = 1.0 - zoom.coerceIn(0.5, 1.5)
        camera?.zoomIn(1.0 + deltaOne)
    }

    fun moveMap(dxpx: Double, dypx: Double){
        val dx = dxpx / width
        val dy = dypx / height
        camera?.moveView(dx * 2, dy * -2)
    }

    fun zoomOutMax(){
        camera?.zoomOutMax()
    }
}