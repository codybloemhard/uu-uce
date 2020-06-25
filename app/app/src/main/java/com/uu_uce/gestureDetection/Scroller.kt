package com.uu_uce.gestureDetection

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat

/**
 * performs action on scroll and fling
 * @param[parent] the context this lives in
 * @param[action] action to be performed after scrolling
 * @param[flingAction] action to be performed when the touch is released
 * @constructor creates a Scroller TouchChild
 */
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

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

