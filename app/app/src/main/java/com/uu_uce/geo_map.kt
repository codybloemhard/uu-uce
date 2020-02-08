package com.uu_uce

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat

class geo_map : AppCompatActivity(),  GestureDetector.OnGestureListener {

    private var gestureDetector: GestureDetectorCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)
        gestureDetector = GestureDetectorCompat(this, this)
    }

    override fun finish() {
        val intent = Intent(this, geo_map::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        gestureDetector!!.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun onFling(
        downEv: MotionEvent?,
        moveEv: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        var dy = (moveEv?.getY() ?: 0.0f) - (downEv?.getY() ?: 0.0f)
        var dx = (moveEv?.getX() ?: 0.0f) - (downEv?.getX() ?: 0.0f)
        if (Math.abs(dy) > Math.abs(dx)) return false
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        return true
    }

    override fun onShowPress(e: MotionEvent?) {}
    override fun onSingleTapUp(e: MotionEvent?): Boolean { return false }
    override fun onDown(e: MotionEvent?): Boolean { return false }
    override fun onScroll( e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean { return false }
    override fun onLongPress(e: MotionEvent?) {}
}
