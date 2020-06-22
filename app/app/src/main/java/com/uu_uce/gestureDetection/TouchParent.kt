package com.uu_uce.gestureDetection

import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity


//same as viewtouchparent, but for activities
open class TouchParent: AppCompatActivity(){
    private var children = mutableListOf<TouchChild>()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        children.forEach { c -> c.getOnTouchEvent(event) }
        return super.onTouchEvent(event)
    }
}