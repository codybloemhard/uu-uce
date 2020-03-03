package com.uu_uce.ui

import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat


class Zoomer(
    parent: AppCompatActivity,
    var action: (Float) -> Unit)
    : TouchChild, ScaleGestureDetector.SimpleOnScaleGestureListener() {
    private var scaleGestureDetector: ScaleGestureDetector? = null

    init{
        scaleGestureDetector = ScaleGestureDetector(parent, this)
    }

    override fun getOnTouchEvent(event: MotionEvent?) {
        scaleGestureDetector?.onTouchEvent(event)
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        super.onScaleBegin(detector)
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {
        super.onScaleEnd(detector)
    }

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        val d = scaleGestureDetector?.scaleFactor ?: 0.0f
        if(d == 0.0f)
            return false
        action(d)
        return true
    }
}