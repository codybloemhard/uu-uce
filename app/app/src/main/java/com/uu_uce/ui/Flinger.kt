package com.uu_uce.ui

import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat

enum class FlingDir{
    HOR, VER
}

class Flinger(
        parent: AppCompatActivity,
        var action: (FlingDir, Float) -> Unit)
        : GestureDetector.OnGestureListener{
    private var gestureDetector: GestureDetectorCompat? = null

    init{
        gestureDetector = GestureDetectorCompat(parent, this)
    }

    fun getOnTouchEvent(event: MotionEvent?) {
        gestureDetector!!.onTouchEvent(event)
    }

    override fun onFling(
        downEv: MotionEvent?,
        moveEv: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        var dy = (moveEv?.getY() ?: 0.0f) - (downEv?.getY() ?: 0.0f)
        var dx = (moveEv?.getX() ?: 0.0f) - (downEv?.getX() ?: 0.0f)
        if (Math.abs(dy) > Math.abs(dx))
            action(FlingDir.VER, dy)
        else
            action(FlingDir.HOR, dy)
        return true
    }

    override fun onShowPress(e: MotionEvent?) {}
    override fun onSingleTapUp(e: MotionEvent?): Boolean { return false }
    override fun onDown(e: MotionEvent?): Boolean { return false }
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean { return false }
    override fun onLongPress(e: MotionEvent?) {}
}