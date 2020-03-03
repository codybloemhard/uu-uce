package com.uu_uce

import android.os.Bundle
import com.uu_uce.ui.DoubleTapper
import com.uu_uce.ui.Scroller
import com.uu_uce.ui.TouchParent
import com.uu_uce.ui.Zoomer
import kotlinx.android.synthetic.main.activity_geo_map.*

class GeoMap : TouchParent() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geo_map)
        addChild(Zoomer(this, ::onZoom))
        addChild(Scroller(this, ::onScroll))
        addChild(DoubleTapper(this, ::onDoubleTap))
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
}
