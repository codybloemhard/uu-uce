package com.uu_uce

import android.graphics.Point
import android.os.Bundle
import android.view.Display
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.uu_uce.shapefiles.p2
import kotlinx.android.synthetic.main.activity_geo_map.*


class GeoMap : AppCompatActivity() {
    private var screenDim = Point(0,0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)

        button.setOnClickListener{customMap.zoomToDevice()}
        //button2.setOnClickListener{customMap.toggleLayer(1)}

        initMenu()
    }

    private fun initMenu(){

        (Display::getSize)(windowManager.defaultDisplay, screenDim)
        menu.setScreenHeight(screenDim.y)
    }

    private fun onSingleTap(tapLocation : p2){
        customMap.tapPin(tapLocation)
    }
}
