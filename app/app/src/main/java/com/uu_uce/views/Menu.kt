package com.uu_uce.views

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.Display
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import com.uu_uce.R
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.shapefiles.p2
import com.uu_uce.ui.*
import kotlinx.android.synthetic.main.activity_geo_map.*

class Menu : ViewTouchParent {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var dragStatus = DragStatus.Down

    private val downPercent = 0.1f
    private val barPercent = 0.2f
    private val upPercent = 1f
    private var downY = 200f
    private var barY = 300f
    private var upY = 600f
    private var image : Drawable? = context.getDrawable(R.drawable.menu)
    private var screenHeight = 0

    init{
        //addChild(Scroller(context, ::scroll))
        addChild(SingleTapper(context, ::tap))

        //make the menu start at the bottom of the screen
        var listener =
            OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                y = oldTop.toFloat()
            }
        addOnLayoutChangeListener(listener)
        post{
            updateLayoutParams{height = screenHeight}
            y = screenHeight - downY
        }
    }

    fun setScreenHeight(h: Int){
        screenHeight = h
        downY = h * downPercent
        barY = h * barPercent
        upY = h * upPercent
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.argb(255,254,212,2))
        image?.draw(canvas)
    }

    private fun tap(loc: p2){
        when(dragStatus){
            DragStatus.Down ->{
                dragStatus = DragStatus.Bar
                animate().y(screenHeight - barY)
            }
            DragStatus.Bar ->{
                dragStatus = DragStatus.Down
                animate().y(screenHeight - downY)
            }
        }
    }
}

enum class DragStatus{
    Down, Bar, Up
}