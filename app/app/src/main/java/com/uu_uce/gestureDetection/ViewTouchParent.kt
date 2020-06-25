package com.uu_uce.gestureDetection

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * a touchchild has to do something on a touchevent
 */
interface TouchChild {
    /**
     * does action on a touchevent
     * @param[event] the current touchevent
     */
    fun getOnTouchEvent(event: MotionEvent)
}

/**
 * Easy way to add a gesturedetector to a view without having to override all the methods
 */
@SuppressLint("ClickableViewAccessibility")
open class ViewTouchParent: GLSurfaceView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private var children = mutableListOf<TouchChild>()

    init {
        setOnTouchListener { _, e -> updateChildren(e) }
    }

    /**
     * @return true means we handle the event
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }

    /**
     * add a new child to this parent
     *
     * @param[child] the new child to be added
     */
    fun addChild(child: TouchChild) {
        children.add(child)
    }

    /**
     * update all children with the current motionevent
     *
     * @paran[event] current motionevent
     */
    private fun updateChildren(event: MotionEvent): Boolean {
        children.forEach { c -> c.getOnTouchEvent(event) }
        return super.onTouchEvent(event)
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

