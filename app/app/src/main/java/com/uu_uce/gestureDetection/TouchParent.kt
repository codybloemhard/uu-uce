package com.uu_uce.gestureDetection

import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity



open class TouchParent: AppCompatActivity(){
    private var children = mutableListOf<TouchChild>()

    fun addChild(child: TouchChild){
        children.add(child)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        children.forEach { c -> c.getOnTouchEvent(event) }
        return super.onTouchEvent(event)
    }
}