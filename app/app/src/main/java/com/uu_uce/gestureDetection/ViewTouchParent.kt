package com.uu_uce.gestureDetection

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent


interface TouchChild{
    fun getOnTouchEvent(event: MotionEvent)
}
/*
Easy way to add a gesturedetector to a view without having to override all the methods
Make your view a ViewTouchParent and add childs with the desired behaviour
 */
@SuppressLint("ClickableViewAccessibility")
open class ViewTouchParent: GLSurfaceView {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    private var children = mutableListOf<TouchChild>()

    init{
        setOnTouchListener { _, e -> updateChildren(e)}
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }

    fun addChild(child: TouchChild){
        children.add(child)
    }

    private fun updateChildren(event: MotionEvent): Boolean {
        children.forEach { c -> c.getOnTouchEvent(event) }
        return super.onTouchEvent(event)
    }
}