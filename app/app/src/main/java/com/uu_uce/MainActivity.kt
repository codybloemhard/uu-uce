package com.uu_uce

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent

class MainActivity : AppCompatActivity(),  GestureDetector.OnGestureListener{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun finish() {
        val intent = Intent(this, geo_map::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onShowPress(e: MotionEvent?) {}

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return false
    }

    override fun onFling(
        downEv: MotionEvent?,
        moveEv: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        var res = false
        var dy = (moveEv?.getY() ?: 0.0f) - (downEv?.getY() ?: 0.0f)
        var dx = (moveEv?.getX() ?: 0.0f) - (downEv?.getX() ?: 0.0f)
        if (Math.abs(dy) > Math.abs(dx)) return false
        val intent = Intent(this, geo_map::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        return res
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {}
}
