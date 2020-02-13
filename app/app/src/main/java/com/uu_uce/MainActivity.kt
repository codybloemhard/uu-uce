package com.uu_uce

import android.content.Intent
import android.os.Bundle
import com.uu_uce.ui.FlingDir
import com.uu_uce.ui.Flinger
import com.uu_uce.ui.TouchParent

class MainActivity : TouchParent() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addChild(Flinger(this, ::action))
    }

    private fun action(dir: FlingDir, delta: Float){
        val intent = Intent(this, GeoMap::class.java)
        startActivity(intent)
        if(dir == FlingDir.VER) return
        if(delta > 0.0f)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        else
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}
