package com.uu_uce.mapOverlay

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.uu_uce.R
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.shapefiles.p2

class ScaleWidget: AppCompatTextView {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    private val minmaxFac = 3f
    private val minLengthFac = 1f/minmaxFac
    private var minLength = 0
    private var lineLength = 0f

    private var minScreenFac = 0f
    private var maxScreenFac = 0f
    private var screenWidth = 0

    val values = listOf(1,2,5,10,20,50,100,200,500,1000,2000,5000,10000,20000,50000,100000,200000,500000)

    private var cur = -1

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init{
        paint.color = Color.BLACK
    }

    fun setScreenWidth(screenWidth: Int){
        minLength = (width*minLengthFac).toInt()
        minScreenFac = minLength.toFloat()/screenWidth
        maxScreenFac = width.toFloat()/screenWidth
        this.screenWidth = screenWidth
        lineLength = minLength.toFloat()
    }

    private fun findCur(width: Float){
        if(values[0]/width > maxScreenFac){
            cur = -1
            Logger.log(LogType.Continuous, "ScaleWidget", "Scale does not fit")
            return
        }
        for(i in values.indices){
            val lineLength = values[i]/width
            if(lineLength > minScreenFac){
                cur = i
                return
            }
        }
        cur = -1
        Logger.log(LogType.Continuous, "ScaleWidget", "Scale does not fit")
    }

    private fun isCurOkay(width: Float): Boolean{
        return values[cur] / width > minScreenFac &&
                values[cur] / width < maxScreenFac
    }

    fun update(viewport: Pair<p2,p2>, activity: Activity){
        val width = viewport.second.first - viewport.first.first

        if(cur == -1 || !isCurOkay(width)) findCur(width)

        activity.runOnUiThread{}
        if(cur == -1){
            activity.runOnUiThread{text = (context as Activity).getString(R.string.geomap_scale_text, 0, "m")}
            return
        }
        activity.runOnUiThread{
            text = if(values[cur] < 1000) {
                (context as Activity).getString(R.string.geomap_scale_text, values[cur], "m")
            } else{
                (context as Activity).getString(R.string.geomap_scale_text, values[cur]/1000, "km")
            }
        }
        lineLength = values[cur] / width * screenWidth
        lineLength = (lineLength.toInt()).toFloat()
        activity.runOnUiThread{invalidate()}
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawLine(0f,height-1f,lineLength, height-1f, paint)
        canvas.drawLine(lineLength,height-1f,lineLength, height/2f, paint)
    }
}