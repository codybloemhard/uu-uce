package com.uu_uce

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.uu_uce.ui.DoubleTapper
import com.uu_uce.ui.Scroller
import com.uu_uce.ui.TouchParent
import com.uu_uce.ui.Zoomer
import com.uu_uce.shapefiles.p2
import com.uu_uce.ui.*
import kotlinx.android.synthetic.main.activity_geo_map.*

class GeoMap : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)

        button.setOnClickListener{customMap.zoomToDevice()}
        //button2.setOnClickListener{customMap.toggleLayer(1)}
    }

    private fun onSingleTap(tapLocation : p2){
        customMap.tapPin(tapLocation)
    }
}
