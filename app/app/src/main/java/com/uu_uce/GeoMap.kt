package com.uu_uce

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.uu_uce.ui.FlingDir
import com.uu_uce.ui.Flinger

class GeoMap : AppCompatActivity(){
    private var flinger: Flinger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)
        flinger = Flinger(this, ::action)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        flinger!!.getOnTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun action(dir: FlingDir, delta: Float){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        if(dir == FlingDir.VER) return
        if(delta > 0.0f)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        else
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}
