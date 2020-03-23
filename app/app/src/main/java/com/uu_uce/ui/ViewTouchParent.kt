package com.uu_uce.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


interface TouchChild{
    fun getOnTouchEvent(event: MotionEvent?)
}

open class ViewTouchParent: View{
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var children = mutableListOf<TouchChild>()

    init{
        setOnTouchListener { _, e -> updateChildren(e)}
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
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