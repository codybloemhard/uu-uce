package com.uu_uce.gestureDetection

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector

/**
 * performs action om zoom
 * @param[parent] the context this lives in
 * @param[action] action to be performed on zoom
 * @constructor creates a Zoomer TouchChild
 */
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

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

