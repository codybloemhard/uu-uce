package com.uu_uce.views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent

abstract class MenuChild(
    var minx: Float,
    var miny: Float,
    var maxx: Float,
    var maxy: Float){

    abstract fun onDraw(canvas: Canvas)

    open fun onTouchEvent(event: MotionEvent) : Boolean {
        return event.x > minx && event.x < maxx &&
                event.y > miny && event.y < maxy
    }
}

class MenuButton(
    minx: Float,
    miny: Float,
    maxx: Float,
    maxy: Float,
    var action: () -> Unit,
    var paint: Paint
): MenuChild(minx, miny, maxx, maxy){

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(minx, miny, maxx, maxy, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(super.onTouchEvent(event)){
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    action()
                    return true
                }
            }
        }
        return false
    }
}