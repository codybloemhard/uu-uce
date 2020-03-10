package com.uu_uce.ui

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.uu_uce.shapefiles.p2

class SingleTapper(
    parent: AppCompatActivity,
    var action: (p2) -> Unit)
    : TouchChild,
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener{
        private var gestureDetector: GestureDetectorCompat? = null

        init{
            gestureDetector = GestureDetectorCompat(parent, this)
            gestureDetector?.setOnDoubleTapListener(this)
        }

        override fun getOnTouchEvent(event: MotionEvent?) {
            gestureDetector?.onTouchEvent(event)
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            action(Pair((e?.x ?: 0.0f).toDouble(), (e?.y ?: 0.0f).toDouble()))
            return true
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean { return true }
        override fun onDoubleTapEvent(e: MotionEvent?): Boolean { return true }
        override fun onShowPress(e: MotionEvent?) {}
        override fun onSingleTapUp(e: MotionEvent?): Boolean { return true }
        override fun onDown(e: MotionEvent?): Boolean { return false }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean { return false }
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean { return false }
        override fun onLongPress(e: MotionEvent?) {}
}