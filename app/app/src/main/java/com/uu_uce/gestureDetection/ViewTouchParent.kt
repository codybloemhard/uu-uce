package com.uu_uce.gestureDetection

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


interface TouchChild{
    fun getOnTouchEvent(event: MotionEvent?)
}
/*
Easy way to add a gesturedetector to a view without having to override all the methods
Make your view a ViewTouchParent and add childs with the desired behaviour
 */
open class ViewTouchParent: View{
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

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

    private fun updateChildren(event: MotionEvent?): Boolean {
        children.forEach { c -> c.getOnTouchEvent(event) }
        return super.onTouchEvent(event)
    }
}