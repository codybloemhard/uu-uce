package com.uu_uce.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import com.uu_uce.shapefiles.LayerType
import com.uu_uce.shapefiles.ShapeMap
import diewald_shapeFile.files.shp.SHP_File
import java.io.File
import kotlin.system.measureTimeMillis

class CustomMap : View {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    @SuppressLint("NewApi")
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    private var smap : ShapeMap

    init{
        Log.d("CustomMap", "Init")
        var dir = File(context.filesDir, "mydir")
        var path = File(dir, "bt25mv10sh0f6422al1r020.shp")
        SHP_File.LOG_INFO = false
        SHP_File.LOG_ONLOAD_HEADER = false
        SHP_File.LOG_ONLOAD_CONTENT = false
        var file = SHP_File(null, path)
        val timeRead = measureTimeMillis {
            file.read()
        }
        Log.i("CustomMap", "Read file: $timeRead")
        smap = ShapeMap()
        val timeParse = measureTimeMillis {
            smap.addLayer(LayerType.Height, file)
        }
        Log.i("CustomMap", "Parse file: $timeParse")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val timeDraw = measureTimeMillis {
            smap.draw(canvas, width, height)
        }
        Log.i("CustomMap", "Draw: $timeDraw")
    }
}