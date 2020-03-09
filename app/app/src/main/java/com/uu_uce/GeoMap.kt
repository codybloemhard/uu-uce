package com.uu_uce

import android.os.Bundle
import com.uu_uce.shapefiles.p2
import com.uu_uce.ui.*
import kotlinx.android.synthetic.main.activity_geo_map.*

class GeoMap : TouchParent() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)
        addChild(Zoomer(this, ::onZoom))
        addChild(Scroller(this, ::onScroll))
        addChild(DoubleTapper(this, ::onDoubleTap))
        addChild(SingleTapper(this, ::onSingleTap))

        button2.setOnClickListener{customMap.zoomToDevice()}
    }

    private fun onZoom(delta: Float){
        this.customMap.zoomMap(delta.toDouble())
    }

    private fun onScroll(dx: Float, dy: Float){
        this.customMap.moveMap(dx.toDouble(), dy.toDouble())
    }

    private fun onDoubleTap(){
        this.customMap.zoomOutMax()
    }

    private fun onSingleTap(tapLocation : p2){
        customMap.tapPin(tapLocation)
    }
}
