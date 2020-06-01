package com.uu_uce.gestureDetection

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat

//performs action(dx,dy) when scrolled
class Scroller(
    parent: Context,
    var action: (Float, Float) -> Unit,
    var flingAction: () -> Unit = {})
    : TouchChild, GestureDetector.OnGestureListener{
    private var gestureDetector: GestureDetectorCompat = GestureDetectorCompat(parent, this)

    override fun getOnTouchEvent(event: MotionEvent) {
        if(event.action == MotionEvent.ACTION_UP) flingAction()
        gestureDetector.onTouchEvent(event)
    }

    override fun onFling(downEv: MotionEvent?, moveEv: MotionEvent?, velocityX: Float, velocityY: Float ): Boolean { return false }
    override fun onShowPress(e: MotionEvent?) {}
    override fun onSingleTapUp(e: MotionEvent?): Boolean { return false }
    override fun onDown(e: MotionEvent?): Boolean { return true }
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean
    {
        action(distanceX, distanceY)
        return true
    }
    override fun onLongPress(e: MotionEvent?) {}
}