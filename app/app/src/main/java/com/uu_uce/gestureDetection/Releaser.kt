package com.uu_uce.gestureDetection

import android.view.MotionEvent

class Releaser(
    var action: () -> Unit): TouchChild {
    override fun getOnTouchEvent(event: MotionEvent?) {
        if(event?.action == MotionEvent.ACTION_UP) action()
    }
}