package com.uu_uce.gestureDetection

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector

//perform action(f) when zoomed by a factor f
class Zoomer(
    parent: Context,
    var action: (Float) -> Unit)
    : TouchChild, ScaleGestureDetector.SimpleOnScaleGestureListener() {
    private var scaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(parent, this)

    override fun getOnTouchEvent(event: MotionEvent) {
        scaleGestureDetector.onTouchEvent(event)
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        super.onScaleBegin(detector)
        return true
    }

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        val d = scaleGestureDetector.scaleFactor
        if(d == 0.0f)
            return false
        action(d)
        return true
    }
}