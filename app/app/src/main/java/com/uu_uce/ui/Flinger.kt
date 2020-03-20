package com.uu_uce.ui

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs

enum class FlingDir{
    HOR, VER
}

class Flinger(
        parent: Context,
        var action: (FlingDir, Float) -> Unit)
        : TouchChild, GestureDetector.OnGestureListener{
    private var gestureDetector: GestureDetectorCompat? = null

    init{
        gestureDetector = GestureDetectorCompat(parent, this)
    }

    override fun getOnTouchEvent(event: MotionEvent?) {
        gestureDetector?.onTouchEvent(event)
    }

    override fun onFling(
        downEv: MotionEvent?,
        moveEv: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val dy = (moveEv?.y ?: 0.0f) - (downEv?.y ?: 0.0f)
        val dx = (moveEv?.x ?: 0.0f) - (downEv?.x ?: 0.0f)
        if (abs(dy) > abs(dx))
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