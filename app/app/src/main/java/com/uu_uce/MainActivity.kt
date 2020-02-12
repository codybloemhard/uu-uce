package com.uu_uce

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import com.uu_uce.ui.FlingDir
import com.uu_uce.ui.Flinger

class MainActivity : AppCompatActivity(){
    var flinger: Flinger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        flinger = Flinger(this, ::action)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        flinger!!.getOnTouchEvent(event)
        return super.onTouchEvent(event)
    }

    fun action(dir: FlingDir, delta: Float){
        val intent = Intent(this, geo_map::class.java)
        startActivity(intent)
        if(dir == FlingDir.VER) return;
        if(delta > 0.0f)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        else
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}
