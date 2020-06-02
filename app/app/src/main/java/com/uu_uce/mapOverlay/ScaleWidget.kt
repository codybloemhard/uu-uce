package com.uu_uce.mapOverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.uu_uce.GeoMap
import com.uu_uce.shapefiles.p2
import com.uu_uce.shapefiles.p3
import com.uu_uce.shapefiles.p3NaN
import kotlinx.android.synthetic.main.activity_geo_map.*

class ScaleWidget: AppCompatTextView {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    private val minLengthFac = 1f/4
    private var minLength = 0

    private var minScreenFac = 0f
    private var maxScreenFac = 0f

    val values = listOf(1,2,5,10,20,50,100,200,500,1000,2000,5000,10000,20000,50000,100000,200000,500000)

    private var cur = 0

    fun setScreenWidth(screenWidth: Int){
        minLength = (width*minLengthFac).toInt()
        minScreenFac = minLength.toFloat()/screenWidth
        maxScreenFac = width.toFloat()/screenWidth
    }

    private fun findCur(width: Float){

    }

    fun update(viewport: Pair<p2,p2>){
        val width = viewport.second.first - viewport.first.first
        text = width.toString()
        text = "test"


    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.RED)
        super.onDraw(canvas)
    }
}