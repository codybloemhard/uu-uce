package com.uu_uce.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.uu_uce.shapefiles.LayerType
import com.uu_uce.shapefiles.ShapeMap
import diewald_shapeFile.files.shp.SHP_File
import java.io.File

class CustomMap : View {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    @SuppressLint("NewApi")
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    var smap : ShapeMap

    init{
        var dir = File(context.filesDir, "mydir")
        var path = File(dir, "bt25mv10sh0f6422al1r020.shp")
        var file = SHP_File(null,  path)
        file.read()
        smap = ShapeMap()
        smap.addLayer(LayerType.Height, file)
        var lol = 0
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        smap.draw(canvas, width, height)
    }
}